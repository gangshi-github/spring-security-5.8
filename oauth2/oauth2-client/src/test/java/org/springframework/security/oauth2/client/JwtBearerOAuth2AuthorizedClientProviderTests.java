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

package org.springframework.security.oauth2.client;

import java.time.Duration;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2JwtBearerGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.TestOAuth2AccessTokenResponses;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.TestJwts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JwtBearerOAuth2AuthorizedClientProvider}.
 *
 * @author Hassene Laaribi
 */
public class JwtBearerOAuth2AuthorizedClientProviderTests {

	private JwtBearerOAuth2AuthorizedClientProvider authorizedClientProvider;

	private OAuth2AccessTokenResponseClient<OAuth2JwtBearerGrantRequest> accessTokenResponseClient;

	private ClientRegistration clientRegistration;

	private Authentication principal;

	private Jwt jwtBearerToken;

	@Before
	public void setup() {
		this.authorizedClientProvider = new JwtBearerOAuth2AuthorizedClientProvider();
		this.accessTokenResponseClient = mock(OAuth2AccessTokenResponseClient.class);
		this.authorizedClientProvider.setAccessTokenResponseClient(this.accessTokenResponseClient);
		this.clientRegistration = TestClientRegistrations.jwtBearer().build();
		this.jwtBearerToken = TestJwts.jwt().build();
		this.principal = new TestingAuthenticationToken("principal", this.jwtBearerToken);
	}

	@Test
	public void setAccessTokenResponseClientWhenClientIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.authorizedClientProvider.setAccessTokenResponseClient(null))
				.withMessage("accessTokenResponseClient cannot be null");
	}

	@Test
	public void setClockSkewWhenNullThenThrowIllegalArgumentException() {
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.authorizedClientProvider.setClockSkew(null))
				.withMessage("clockSkew cannot be null");
		// @formatter:on
	}

	@Test
	public void setClockSkewWhenNegativeSecondsThenThrowIllegalArgumentException() {
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.authorizedClientProvider.setClockSkew(Duration.ofSeconds(-1)))
				.withMessage("clockSkew must be >= 0");
		// @formatter:on
	}

	@Test
	public void setClockWhenNullThenThrowIllegalArgumentException() {
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.authorizedClientProvider.setClock(null))
				.withMessage("clock cannot be null");
		// @formatter:on
	}

	@Test
	public void authorizeWhenContextIsNullThenThrowIllegalArgumentException() {
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.authorizedClientProvider.authorize(null))
				.withMessage("context cannot be null");
		// @formatter:on
	}

	@Test
	public void authorizeWhenNotJwtBearerThenUnableToAuthorize() {
		ClientRegistration clientRegistration = TestClientRegistrations.clientCredentials().build();
		// @formatter:off
		OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext
				.withClientRegistration(clientRegistration)
				.principal(this.principal)
				.build();
		// @formatter:on
		assertThat(this.authorizedClientProvider.authorize(authorizationContext)).isNull();
	}

	@Test
	public void authorizeWhenJwtBearerAndNotAuthorizedAndEmptyJwtThenUnableToAuthorize() {
		// @formatter:off
		OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext
				.withClientRegistration(this.clientRegistration)
				.principal(this.principal)
				.attribute(OAuth2AuthorizationContext.JWT_ATTRIBUTE_NAME, null)
				.build();
		// @formatter:on
		assertThat(this.authorizedClientProvider.authorize(authorizationContext)).isNull();
	}

	@Test
	public void authorizeWhenJwtBearerAndNotAuthorizedThenAuthorize() {
		OAuth2AccessTokenResponse accessTokenResponse = TestOAuth2AccessTokenResponses.accessTokenResponse().build();
		given(this.accessTokenResponseClient.getTokenResponse(any())).willReturn(accessTokenResponse);
		// @formatter:off
		OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext
				.withClientRegistration(this.clientRegistration)
				.principal(this.principal)
				.attribute(OAuth2AuthorizationContext.JWT_ATTRIBUTE_NAME, this.jwtBearerToken)
				.build();
		// @formatter:on
		OAuth2AuthorizedClient authorizedClient = this.authorizedClientProvider.authorize(authorizationContext);
		assertThat(authorizedClient.getClientRegistration()).isSameAs(this.clientRegistration);
		assertThat(authorizedClient.getPrincipalName()).isEqualTo(this.principal.getName());
		assertThat(authorizedClient.getAccessToken()).isEqualTo(accessTokenResponse.getAccessToken());
	}

	@Test
	public void authorizeWhenJwtBearerAndAuthorizedAndTokenExpiredThenReauthorize() {
		Instant issuedAt = Instant.now().minus(Duration.ofDays(1));
		Instant expiresAt = issuedAt.plus(Duration.ofMinutes(60));
		OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
				"access-token-expired", issuedAt, expiresAt);
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(this.clientRegistration,
				this.principal.getName(), accessToken);
		OAuth2AccessTokenResponse accessTokenResponse = TestOAuth2AccessTokenResponses.accessTokenResponse().build();
		given(this.accessTokenResponseClient.getTokenResponse(any())).willReturn(accessTokenResponse);
		// @formatter:off
		OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext
				.withAuthorizedClient(authorizedClient)
				.attribute(OAuth2AuthorizationContext.JWT_ATTRIBUTE_NAME, this.jwtBearerToken)
				.principal(this.principal)
				.build();
		// @formatter:on
		authorizedClient = this.authorizedClientProvider.authorize(authorizationContext);
		assertThat(authorizedClient.getClientRegistration()).isSameAs(this.clientRegistration);
		assertThat(authorizedClient.getPrincipalName()).isEqualTo(this.principal.getName());
		assertThat(authorizedClient.getAccessToken()).isEqualTo(accessTokenResponse.getAccessToken());
	}

	@Test
	public void authorizeWhenJwtBearerAndAuthorizedAndTokenNotExpiredButClockSkewForcesExpiryThenReauthorize() {
		Instant now = Instant.now();
		Instant issuedAt = now.minus(Duration.ofMinutes(60));
		Instant expiresAt = now.plus(Duration.ofMinutes(1));
		OAuth2AccessToken expiresInOneMinAccessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
				"access-token-1234", issuedAt, expiresAt);
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(this.clientRegistration,
				this.principal.getName(), expiresInOneMinAccessToken); // without refresh
		// token
		// Shorten the lifespan of the access token by 90 seconds, which will ultimately
		// force it to expire on the client
		this.authorizedClientProvider.setClockSkew(Duration.ofSeconds(90));
		OAuth2AccessTokenResponse accessTokenResponse = TestOAuth2AccessTokenResponses.accessTokenResponse().build();
		given(this.accessTokenResponseClient.getTokenResponse(any())).willReturn(accessTokenResponse);
		// @formatter:off
		OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext
				.withAuthorizedClient(authorizedClient)
				.attribute(OAuth2AuthorizationContext.JWT_ATTRIBUTE_NAME, this.jwtBearerToken)
				.principal(this.principal)
				.build();
		// @formatter:on
		OAuth2AuthorizedClient reauthorizedClient = this.authorizedClientProvider.authorize(authorizationContext);
		assertThat(reauthorizedClient.getClientRegistration()).isSameAs(this.clientRegistration);
		assertThat(reauthorizedClient.getPrincipalName()).isEqualTo(this.principal.getName());
		assertThat(reauthorizedClient.getAccessToken()).isEqualTo(accessTokenResponse.getAccessToken());
	}

}
