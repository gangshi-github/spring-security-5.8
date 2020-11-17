/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.oauth2.client.endpoint;

import java.util.Collections;
import java.util.function.Function;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.TestJwks;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link NimbusJwtClientAuthenticationCustomizer}.
 *
 * @author Joe Grandja
 */
public class NimbusJwtClientAuthenticationCustomizerTests {

	private Function<ClientRegistration, JWK> jwkResolver;

	private NimbusJwtClientAuthenticationCustomizer<OAuth2ClientCredentialsGrantRequest> customizer;

	@Before
	public void setup() {
		this.jwkResolver = mock(Function.class);
		this.customizer = new NimbusJwtClientAuthenticationCustomizer<>(this.jwkResolver);
	}

	@Test
	public void constructorWhenJwkResolverNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new NimbusJwtClientAuthenticationCustomizer<>(null))
				.withMessage("jwkResolver cannot be null");
	}

	@Test
	public void setJwtCustomizerWhenNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.customizer.setJwtCustomizer(null))
				.withMessage("jwtCustomizer cannot be null");
	}

	@Test
	public void customizeWhenAuthorizationGrantRequestNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.customizer.customize(null, new HttpHeaders(), new LinkedMultiValueMap<>()))
				.withMessage("authorizationGrantRequest cannot be null");
	}

	@Test
	public void customizeWhenHeadersNullThenThrowIllegalArgumentException() {
		OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest = new OAuth2ClientCredentialsGrantRequest(
				TestClientRegistrations.clientCredentials().build());
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.customizer.customize(clientCredentialsGrantRequest, null, new LinkedMultiValueMap<>()))
				.withMessage("headers cannot be null");
	}

	@Test
	public void customizeWhenParametersNullThenThrowIllegalArgumentException() {
		OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest = new OAuth2ClientCredentialsGrantRequest(
				TestClientRegistrations.clientCredentials().build());
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.customizer.customize(clientCredentialsGrantRequest, new HttpHeaders(), null))
				.withMessage("parameters cannot be null");
	}

	@Test
	public void customizeWhenOtherClientAuthenticationMethodThenNotCustomized() {
		// @formatter:off
		ClientRegistration clientRegistration = TestClientRegistrations.clientCredentials()
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.build();
		// @formatter:on
		OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest = new OAuth2ClientCredentialsGrantRequest(
				clientRegistration);
		this.customizer.customize(clientCredentialsGrantRequest, new HttpHeaders(), new LinkedMultiValueMap<>());
		verifyNoInteractions(this.jwkResolver);
	}

	@Test
	public void customizeWhenJwkNotResolvedThenThrowOAuth2AuthorizationException() {
		// @formatter:off
		ClientRegistration clientRegistration = TestClientRegistrations.clientCredentials()
				.clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
				.build();
		// @formatter:on
		OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest = new OAuth2ClientCredentialsGrantRequest(
				clientRegistration);
		assertThatExceptionOfType(OAuth2AuthorizationException.class)
				.isThrownBy(() -> this.customizer.customize(clientCredentialsGrantRequest, new HttpHeaders(),
						new LinkedMultiValueMap<>()))
				.withMessage("[invalid_key] Failed to resolve JWK signing key for client registration '"
						+ clientRegistration.getRegistrationId() + "'.");
	}

	@Test
	public void customizeWhenPrivateKeyJwtClientAuthenticationMethodThenCustomized() throws Exception {
		RSAKey rsaJwk = TestJwks.DEFAULT_RSA_JWK;
		given(this.jwkResolver.apply(any())).willReturn(rsaJwk);

		// Add custom claim
		this.customizer.setJwtCustomizer(
				(authorizationGrantRequest, headers, claims) -> claims.put("custom-claim", "custom-value"));

		// @formatter:off
		ClientRegistration clientRegistration = TestClientRegistrations.clientCredentials()
				.clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
				.build();
		// @formatter:on

		OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest = new OAuth2ClientCredentialsGrantRequest(
				clientRegistration);
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

		this.customizer.customize(clientCredentialsGrantRequest, new HttpHeaders(), parameters);

		assertThat(parameters.getFirst(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE))
				.isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
		String encodedJws = parameters.getFirst(OAuth2ParameterNames.CLIENT_ASSERTION);
		assertThat(encodedJws).isNotNull();

		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaJwk.toRSAPublicKey()).build();
		Jwt jws = jwtDecoder.decode(encodedJws);

		assertThat(jws.getHeaders().get(JoseHeaderNames.ALG)).isEqualTo(SignatureAlgorithm.RS256.getName());
		assertThat(jws.getHeaders().get(JoseHeaderNames.KID)).isEqualTo(rsaJwk.getKeyID());
		assertThat(jws.<String>getClaim(JwtClaimNames.ISS)).isEqualTo(clientRegistration.getClientId());
		assertThat(jws.getSubject()).isEqualTo(clientRegistration.getClientId());
		assertThat(jws.getAudience())
				.isEqualTo(Collections.singletonList(clientRegistration.getProviderDetails().getTokenUri()));
		assertThat(jws.getId()).isNotNull();
		assertThat(jws.getIssuedAt()).isNotNull();
		assertThat(jws.getExpiresAt()).isNotNull();
		assertThat(jws.<String>getClaim("custom-claim")).isEqualTo("custom-value");
	}

	@Test
	public void customizeWhenClientSecretJwtClientAuthenticationMethodThenCustomized() {
		OctetSequenceKey secretJwk = TestJwks.DEFAULT_SECRET_JWK;
		given(this.jwkResolver.apply(any())).willReturn(secretJwk);

		// Add custom claim
		this.customizer.setJwtCustomizer(
				(authorizationGrantRequest, headers, claims) -> claims.put("custom-claim", "custom-value"));

		// @formatter:off
		ClientRegistration clientRegistration = TestClientRegistrations.clientCredentials()
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT)
				.build();
		// @formatter:on

		OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest = new OAuth2ClientCredentialsGrantRequest(
				clientRegistration);
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

		this.customizer.customize(clientCredentialsGrantRequest, new HttpHeaders(), parameters);

		assertThat(parameters.getFirst(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE))
				.isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
		String encodedJws = parameters.getFirst(OAuth2ParameterNames.CLIENT_ASSERTION);
		assertThat(encodedJws).isNotNull();

		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretJwk.toSecretKey()).build();
		Jwt jws = jwtDecoder.decode(encodedJws);

		assertThat(jws.getHeaders().get(JoseHeaderNames.ALG)).isEqualTo(MacAlgorithm.HS256.getName());
		assertThat(jws.getHeaders().get(JoseHeaderNames.KID)).isEqualTo(secretJwk.getKeyID());
		assertThat(jws.<String>getClaim(JwtClaimNames.ISS)).isEqualTo(clientRegistration.getClientId());
		assertThat(jws.getSubject()).isEqualTo(clientRegistration.getClientId());
		assertThat(jws.getAudience())
				.isEqualTo(Collections.singletonList(clientRegistration.getProviderDetails().getTokenUri()));
		assertThat(jws.getId()).isNotNull();
		assertThat(jws.getIssuedAt()).isNotNull();
		assertThat(jws.getExpiresAt()).isNotNull();
		assertThat(jws.<String>getClaim("custom-claim")).isEqualTo("custom-value");
	}

}
