package ca.uhn.fhir.jpa.dao;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2017 University Health Network
 * %%
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
 * #L%
 */

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;

public abstract class BaseSearchParamRegistry implements ISearchParamRegistry {

	private static final Map<String, RuntimeSearchParam> EMPTY_SP_MAP = Collections.emptyMap();

	private Map<String, Map<String, RuntimeSearchParam>> myBuiltInSearchParams;

	@Autowired
	private FhirContext myCtx;

	@Autowired
	private Collection<IFhirResourceDao<?>> myDaos;

	public BaseSearchParamRegistry() {
		super();
	}

	@Override
	public void forceRefresh() {
		// nothing by default
	}

	public Map<String, Map<String, RuntimeSearchParam>> getBuiltInSearchParams() {
		return myBuiltInSearchParams;
	}

	@Override
	public Map<String,RuntimeSearchParam> getActiveSearchParams(String theResourceName) {
		Validate.notBlank(theResourceName, "theResourceName must not be blank or null");

		return myBuiltInSearchParams.get(theResourceName);
	}

	@PostConstruct
	public void postConstruct() {
		Map<String, Map<String, RuntimeSearchParam>> resourceNameToSearchParams = new HashMap<String, Map<String,RuntimeSearchParam>>();

		for (IFhirResourceDao<?> nextDao : myDaos) {
			RuntimeResourceDefinition nextResDef = myCtx.getResourceDefinition(nextDao.getResourceType());
			String nextResourceName = nextResDef.getName();
			HashMap<String, RuntimeSearchParam> nameToParam = new HashMap<String, RuntimeSearchParam>();
			resourceNameToSearchParams.put(nextResourceName, nameToParam);
			
			for (RuntimeSearchParam nextSp : nextResDef.getSearchParams()) {
				nameToParam.put(nextSp.getName(), nextSp);
			}
		}

		myBuiltInSearchParams = Collections.unmodifiableMap(resourceNameToSearchParams);
	}

	@Override
	public Collection<RuntimeSearchParam> getAllSearchParams(String theResourceName) {
		Validate.notBlank(theResourceName, "theResourceName must not be null or blank");
		
		Map<String, RuntimeSearchParam> map = myBuiltInSearchParams.get(theResourceName);
		map = ObjectUtils.defaultIfNull(map, EMPTY_SP_MAP);
		return Collections.unmodifiableCollection(map.values());
	}

}
