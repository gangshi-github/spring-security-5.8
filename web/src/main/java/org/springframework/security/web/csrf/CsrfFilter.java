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

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>
 * Applies
 * <a href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)" >CSRF</a>
 * protection using a synchronizer token pattern. Developers are required to ensure that
 * {@link CsrfFilter} is invoked for any request that allows state to change. Typically
 * this just means that they should ensure their web application follows proper REST
 * semantics (i.e. do not change state with the HTTP methods GET, HEAD, TRACE, OPTIONS).
 * </p>
 *
 * <p>
 * Typically the {@link CsrfTokenRepository} implementation chooses to store the
 * {@link CsrfToken} in {@link HttpSession} with {@link HttpSessionCsrfTokenRepository}
 * wrapped by a {@link LazyCsrfTokenRepository}. This is preferred to storing the token in
 * a cookie which can be modified by a client application.
 * </p>
 *
 * @author Rob Winch
 * @since 3.2
 */
public final class CsrfFilter extends OncePerRequestFilter {
	/**
	 * The default {@link RequestMatcher} that indicates if CSRF protection is required or
	 * not. The default is to ignore GET, HEAD, TRACE, OPTIONS and process all other
	 * requests.
	 */
	public static final RequestMatcher DEFAULT_CSRF_MATCHER = new DefaultRequiresCsrfMatcher();

	private static final SecureRandom secureRandom = new SecureRandom();

	private final Log logger = LogFactory.getLog(getClass());
	private final CsrfTokenRepository tokenRepository;
	private RequestMatcher requireCsrfProtectionMatcher = DEFAULT_CSRF_MATCHER;
	private AccessDeniedHandler accessDeniedHandler = new AccessDeniedHandlerImpl();

	public CsrfFilter(CsrfTokenRepository csrfTokenRepository) {
		Assert.notNull(csrfTokenRepository, "csrfTokenRepository cannot be null");
		this.tokenRepository = csrfTokenRepository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.web.filter.OncePerRequestFilter#doFilterInternal(javax.servlet
	 * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse,
	 * javax.servlet.FilterChain)
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
		request.setAttribute(HttpServletResponse.class.getName(), response);

		CsrfToken csrfToken = this.tokenRepository.loadToken(request);
		final boolean missingToken = csrfToken == null;
		if (missingToken) {
			csrfToken = this.tokenRepository.generateToken(request);
			this.tokenRepository.saveToken(csrfToken, request, response);
		}
		XorEncodedToken xorEncodedToken = new XorEncodedToken(csrfToken);
		request.setAttribute(CsrfToken.class.getName(), xorEncodedToken);
		request.setAttribute(xorEncodedToken.getParameterName(), xorEncodedToken);

		if (!this.requireCsrfProtectionMatcher.matches(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String actualToken = request.getHeader(csrfToken.getHeaderName());
		if (actualToken == null) {
			actualToken = request.getParameter(csrfToken.getParameterName());
		}
		if (!csrfToken.getToken().equals(xorDecodeToken(actualToken))) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Invalid CSRF token found for "
						+ UrlUtils.buildFullRequestUrl(request));
			}
			if (missingToken) {
				this.accessDeniedHandler.handle(request, response,
						new MissingCsrfTokenException(actualToken));
			}
			else {
				this.accessDeniedHandler.handle(request, response,
						new InvalidCsrfTokenException(csrfToken, actualToken));
			}
			return;
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Specifies a {@link RequestMatcher} that is used to determine if CSRF protection
	 * should be applied. If the {@link RequestMatcher} returns true for a given request,
	 * then CSRF protection is applied.
	 *
	 * <p>
	 * The default is to apply CSRF protection for any HTTP method other than GET, HEAD,
	 * TRACE, OPTIONS.
	 * </p>
	 *
	 * @param requireCsrfProtectionMatcher the {@link RequestMatcher} used to determine if
	 * CSRF protection should be applied.
	 */
	public void setRequireCsrfProtectionMatcher(
			RequestMatcher requireCsrfProtectionMatcher) {
		Assert.notNull(requireCsrfProtectionMatcher,
				"requireCsrfProtectionMatcher cannot be null");
		this.requireCsrfProtectionMatcher = requireCsrfProtectionMatcher;
	}

	/**
	 * Specifies a {@link AccessDeniedHandler} that should be used when CSRF protection
	 * fails.
	 *
	 * <p>
	 * The default is to use AccessDeniedHandlerImpl with no arguments.
	 * </p>
	 *
	 * @param accessDeniedHandler the {@link AccessDeniedHandler} to use
	 */
	public void setAccessDeniedHandler(AccessDeniedHandler accessDeniedHandler) {
		Assert.notNull(accessDeniedHandler, "accessDeniedHandler cannot be null");
		this.accessDeniedHandler = accessDeniedHandler;
	}

	private static final class DefaultRequiresCsrfMatcher implements RequestMatcher {
		private final HashSet<String> allowedMethods = new HashSet<String>(
				Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS"));

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.security.web.util.matcher.RequestMatcher#matches(javax.
		 * servlet.http.HttpServletRequest)
		 */
		@Override
		public boolean matches(HttpServletRequest request) {
			return !this.allowedMethods.contains(request.getMethod());
		}
	}

	static String xorEncodeToken(String token) {
		// XOR the token with random values to protect against
		// a BREACH attack
		int tokenLength = token.length();
		byte[] encodedToken = new byte[tokenLength*2];
		byte[] salt = new byte[token.length()];
		secureRandom.nextBytes(salt);

		for (int i=0; i < tokenLength; i++) {
			encodedToken[i] = salt[i];
			encodedToken[i+tokenLength] = (byte)(token.charAt(i) ^ salt[i]);
		}

		return new String(Base64.encode(encodedToken));
	}

	static String xorDecodeToken(String encodedToken) {
		if (encodedToken == null)
			return null;

		byte[] tokenBytes;
		try {
			tokenBytes = Base64.decode(encodedToken.getBytes());
		} catch (IllegalArgumentException e) {
			// If the Base64 decode failed then return null
			return null;
		}

		StringBuilder builder = new StringBuilder();
		int tokenLength = tokenBytes.length /2;
		for (int i=0; i < tokenLength; i++)
			builder.append((char)(tokenBytes[i] ^ tokenBytes[i+tokenLength]));
		return builder.toString();
	}

	static class XorEncodedToken implements CsrfToken {

		protected final CsrfToken delegate;

		private XorEncodedToken(CsrfToken delegate) {
			this.delegate = delegate;
		}

		@Override
		public String getHeaderName() {
			return delegate.getHeaderName();
		}

		@Override
		public String getParameterName() {
			return delegate.getParameterName();
		}

		@Override
		public String getToken() {
			return xorEncodeToken(delegate.getToken());
		}
	}
}
