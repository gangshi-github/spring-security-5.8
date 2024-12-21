/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.TestJwts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JwtAuthenticationToken}
 *
 * @author Josh Cummings
 */
@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationTokenTests {

	@Test
	public void getNameWhenJwtHasSubjectThenReturnsSubject() {
		Jwt jwt = builder().subject("Carl").build();
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
		assertThat(token.getName()).isEqualTo("Carl");
	}

	@Test
	public void getNameWhenJwtHasNoSubjectThenReturnsNull() {
		Jwt jwt = builder().claim("claim", "value").build();
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
		assertThat(token.getName()).isNull();
	}

	@Test
	public void constructorWhenJwtIsNullThenThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtAuthenticationToken(null))
			.withMessageContaining("token cannot be null");
	}

	@Test
	public void constructorWhenUsingCorrectParametersThenConstructedCorrectly() {
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("test");
		Jwt jwt = builder().claim("claim", "value").build();
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);
		assertThat(token.getAuthorities()).isEqualTo(authorities);
		assertThat(token.getPrincipal()).isEqualTo(jwt);
		assertThat(token.getCredentials()).isEqualTo(jwt);
		assertThat(token.getToken()).isEqualTo(jwt);
		assertThat(token.getTokenAttributes()).isEqualTo(jwt.getClaims());
		assertThat(token.isAuthenticated()).isTrue();
	}

	@Test
	public void constructorWhenUsingOnlyJwtThenConstructedCorrectly() {
		Jwt jwt = builder().claim("claim", "value").build();
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
		assertThat(token.getAuthorities()).isEmpty();
		assertThat(token.getPrincipal()).isEqualTo(jwt);
		assertThat(token.getCredentials()).isEqualTo(jwt);
		assertThat(token.getToken()).isEqualTo(jwt);
		assertThat(token.getTokenAttributes()).isEqualTo(jwt.getClaims());
		assertThat(token.isAuthenticated()).isFalse();
	}

	@Test
	public void getNameWhenConstructedWithJwtThenReturnsSubject() {
		Jwt jwt = builder().subject("Hayden").build();
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);
		assertThat(token.getName()).isEqualTo("Hayden");
	}

	@Test
	public void getNameWhenConstructedWithJwtAndAuthoritiesThenReturnsSubject() {
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("test");
		Jwt jwt = builder().subject("Hayden").build();
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);
		assertThat(token.getName()).isEqualTo("Hayden");
	}

	@Test
	public void getNameWhenConstructedWithNameThenReturnsProvidedName() {
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("test");
		Jwt jwt = builder().claim("claim", "value").build();
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities, "Hayden");
		assertThat(token.getName()).isEqualTo("Hayden");
	}

	@Test
	public void getNameWhenConstructedWithNoSubjectThenReturnsNull() {
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("test");
		Jwt jwt = builder().claim("claim", "value").build();
		assertThat(new JwtAuthenticationToken(jwt, authorities, null).getName()).isNull();
		assertThat(new JwtAuthenticationToken(jwt, authorities).getName()).isNull();
		assertThat(new JwtAuthenticationToken(jwt).getName()).isNull();
	}

	@Test
	public void testConstructorWithPrincipal() {
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("test");
		User principal = mock(User.class);
		Jwt jwt = TestJwts.user();
		JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, principal, authorities, "Hayden");
		assertThat(token.getToken()).isSameAs(jwt);
		assertThat(token.getCredentials()).isSameAs(jwt);
		assertThat(token.getPrincipal()).isSameAs(principal);
		assertThat(token.getAuthorities()).isEqualTo(authorities);
		assertThat(token.isAuthenticated()).isTrue();
		assertThat(token.getName()).isEqualTo("Hayden");
	}

	private Jwt.Builder builder() {
		return Jwt.withTokenValue("token").header("alg", JwsAlgorithms.RS256);
	}

}
