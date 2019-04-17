/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.security.oauth2.server.resource.authentication;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

/**
 * Reactive version of {@link JwtAuthenticationConverter} for converting a {@link Jwt}
 * to a {@link AbstractAuthenticationToken Mono&lt;AbstractAuthenticationToken&gt;}.
 *
 * @author Eric Deandrea
 * @since 5.2
 */
public final class ReactiveJwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {
	private final Converter<Jwt, Flux<GrantedAuthority>> jwtGrantedAuthoritiesConverter;
	
	@Autowired
	public ReactiveJwtAuthenticationConverter(final Converter<Jwt, Flux<GrantedAuthority>> jwtGrantedAuthoritiesConverter) {
		this.jwtGrantedAuthoritiesConverter = jwtGrantedAuthoritiesConverter;
	}
	
	/**
	 * @deprecated inject your own granted-authorities converter instead
	 */
	@Deprecated
	public ReactiveJwtAuthenticationConverter() {
		this(new ReactiveJwtGrantedAuthoritiesConverterAdapter(new JwtScopesGrantedAuthoritiesConverter()));
	}

	@Override
	public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
		return this.jwtGrantedAuthoritiesConverter.convert(jwt)
				.collectList()
				.map(authorities -> new JwtAuthenticationToken(jwt, authorities));
	}
}
