/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.security.authentication;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.event.KeyValuesObservationEvent;

/**
 * A strategy to collect {@link KeyValues} for
 * {@link org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent}s
 *
 * @author Josh Cummings
 * @since 6.0
 */
public final class AuthenticationFailureObservationEventConverter
		implements Converter<AbstractAuthenticationFailureEvent, Observation.Event> {

	private final AuthenticationObservationConvention convention = new AuthenticationObservationConvention();

	@Override
	public Observation.Event convert(AbstractAuthenticationFailureEvent event) {
		AuthenticationObservationContext context = AuthenticationObservationContext.fromEvent(event);
		KeyValues kv = this.convention.getLowCardinalityKeyValues(context);
		return new KeyValuesObservationEvent(kv, Observation.Event.of(context.getName()));
	}

}
