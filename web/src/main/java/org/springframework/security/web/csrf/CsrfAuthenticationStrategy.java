/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.web.csrf;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.util.Assert;

/**
 * {@link CsrfAuthenticationStrategy} is in charge of removing the {@link CsrfToken} upon
 * authenticating. A new {@link CsrfToken} will then be generated by the framework upon
 * the next request.
 *
 * @author Rob Winch
 * @since 3.2
 */
public final class CsrfAuthenticationStrategy implements SessionAuthenticationStrategy {

	private final CsrfTokenRepository csrfTokenRepository;

	/**
	 * Creates a new instance
	 * @param csrfTokenRepository the {@link CsrfTokenRepository} to use
	 */
	public CsrfAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
		Assert.notNull(csrfTokenRepository, "csrfTokenRepository cannot be null");
		this.csrfTokenRepository = csrfTokenRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
	 * #onAuthentication(org.springframework.security.core.Authentication,
	 * javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void onAuthentication(Authentication authentication,
			HttpServletRequest request, HttpServletResponse response)
			throws SessionAuthenticationException {
		boolean containsToken = this.csrfTokenRepository.loadToken(request) != null;
		if (containsToken) {
			this.csrfTokenRepository.saveToken(null, request, response);

			CsrfToken newToken = this.csrfTokenRepository.generateToken(request);
			CsrfToken tokenForRequest = new SaveOnAccessCsrfToken(csrfTokenRepository,
					request, response, newToken);

			request.setAttribute(CsrfToken.class.getName(), tokenForRequest);
			request.setAttribute(newToken.getParameterName(), tokenForRequest);
		}
	}
}
