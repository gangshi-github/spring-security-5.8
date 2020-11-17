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

import java.net.URI;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A {@link Converter} that converts the provided {@link OAuth2PasswordGrantRequest} to a
 * {@link RequestEntity} representation of an OAuth 2.0 Access Token Request for the
 * Resource Owner Password Credentials Grant.
 *
 * @author Joe Grandja
 * @since 5.2
 * @see OAuth2AuthorizationGrantRequestEntityConverter
 * @see OAuth2PasswordGrantRequest
 * @see RequestEntity
 */
public class OAuth2PasswordGrantRequestEntityConverter
		implements OAuth2AuthorizationGrantRequestEntityConverter<OAuth2PasswordGrantRequest> {

	private Customizer<OAuth2PasswordGrantRequest> customizer = (request, headers, parameters) -> {
	};

	/**
	 * Returns the {@link RequestEntity} used for the Access Token Request.
	 * @param passwordGrantRequest the password grant request
	 * @return the {@link RequestEntity} used for the Access Token Request
	 */
	@Override
	public RequestEntity<?> convert(OAuth2PasswordGrantRequest passwordGrantRequest) {
		ClientRegistration clientRegistration = passwordGrantRequest.getClientRegistration();
		HttpHeaders headers = OAuth2AuthorizationGrantRequestEntityUtils.getTokenRequestHeaders(clientRegistration);
		MultiValueMap<String, String> parameters = createParameters(passwordGrantRequest);
		this.customizer.customize(passwordGrantRequest, headers, parameters);
		URI uri = UriComponentsBuilder.fromUriString(clientRegistration.getProviderDetails().getTokenUri()).build()
				.toUri();
		return new RequestEntity<>(parameters, headers, HttpMethod.POST, uri);
	}

	/**
	 * Sets the {@link Customizer} to be provided the opportunity to customize the
	 * {@link HttpHeaders headers} and/or {@link MultiValueMap parameters} of the OAuth
	 * 2.0 Access Token Request.
	 * @param customizer the {@link Customizer} to be provided the opportunity to
	 * customize the OAuth 2.0 Access Token Request
	 * @since 5.5
	 */
	public final void setCustomizer(Customizer<OAuth2PasswordGrantRequest> customizer) {
		Assert.notNull(customizer, "customizer cannot be null");
		this.customizer = customizer;
	}

	/**
	 * Returns a {@link MultiValueMap} of the form parameters used for the Access Token
	 * Request body.
	 * @param passwordGrantRequest the password grant request
	 * @return a {@link MultiValueMap} of the form parameters used for the Access Token
	 * Request body
	 */
	private MultiValueMap<String, String> createParameters(OAuth2PasswordGrantRequest passwordGrantRequest) {
		ClientRegistration clientRegistration = passwordGrantRequest.getClientRegistration();
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		parameters.add(OAuth2ParameterNames.GRANT_TYPE, passwordGrantRequest.getGrantType().getValue());
		parameters.add(OAuth2ParameterNames.USERNAME, passwordGrantRequest.getUsername());
		parameters.add(OAuth2ParameterNames.PASSWORD, passwordGrantRequest.getPassword());
		if (!CollectionUtils.isEmpty(clientRegistration.getScopes())) {
			parameters.add(OAuth2ParameterNames.SCOPE,
					StringUtils.collectionToDelimitedString(clientRegistration.getScopes(), " "));
		}
		if (ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(clientRegistration.getClientAuthenticationMethod())
				|| ClientAuthenticationMethod.POST.equals(clientRegistration.getClientAuthenticationMethod())) {
			parameters.add(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
			parameters.add(OAuth2ParameterNames.CLIENT_SECRET, clientRegistration.getClientSecret());
		}
		return parameters;
	}

}
