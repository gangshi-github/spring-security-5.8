/*
 * Copyright 2017-2017 the original author or authors.
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
package org.springframework.security.abac.model;

import org.springframework.expression.Expression;

/**
 * Defines the policy format. Based on that the permission to access is granted
 *
 * @author Renato Soppelsa
 * @since 5.0
 */
public interface Policy {

	/**
	 * @return id, if applicable. i.e when stored in db
	 */
	public Long getId();

	/**
	 * @return human readable name for a policy
	 */
	String getName();

	/**
	 * @return description of the policy
	 */
	String getDescription();

	/**
	 * @return a String used to filter the policies. Usualy the simple file name of a class. i.e "String"
	 */
	String getType();

	/**
	 * @return SpEl expression used to determine if the policy is applicable in this case
	 */
	Expression getApplicable();

	/**
	 *
	 * @return SpEl expression to define the contition, meaning the policy itself
	 */
	Expression  getCondition();

}
