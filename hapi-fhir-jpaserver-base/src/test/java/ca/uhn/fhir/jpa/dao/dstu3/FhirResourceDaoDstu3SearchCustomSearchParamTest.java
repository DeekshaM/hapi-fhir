package ca.uhn.fhir.jpa.dao.dstu3;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;

import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.SearchParameter;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.AfterClass;
import org.junit.Test;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.TestUtil;

public class FhirResourceDaoDstu3SearchCustomSearchParamTest extends BaseJpaDstu3Test {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirResourceDaoDstu3SearchCustomSearchParamTest.class);

	@Test
	public void testCreateInvalidParamInvalidResourceName() {
		SearchParameter fooSp = new SearchParameter();
		fooSp.setCode("foo");
		fooSp.setType(org.hl7.fhir.dstu3.model.Enumerations.SearchParamType.TOKEN);
		fooSp.setTitle("FOO SP");
		fooSp.setXpath("PatientFoo.gender");
		fooSp.setXpathUsage(org.hl7.fhir.dstu3.model.SearchParameter.XPathUsageType.NORMAL);
		fooSp.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE);
		try {
			mySearchParameterDao.create(fooSp, mySrd);
			fail();
		} catch (UnprocessableEntityException e) {
			assertEquals("Invalid path value \"PatientFoo.gender\": Unknown resource name \"PatientFoo\" (this name is not known in FHIR version \"DSTU3\")", e.getMessage());
		}
	}

	@Test
	public void testCreateInvalidParamMismatchedResourceName() {
		SearchParameter fooSp = new SearchParameter();
		fooSp.setCode("foo");
		fooSp.setType(org.hl7.fhir.dstu3.model.Enumerations.SearchParamType.TOKEN);
		fooSp.setTitle("FOO SP");
		fooSp.setXpath("Patient.gender or Observation.code");
		fooSp.setXpathUsage(org.hl7.fhir.dstu3.model.SearchParameter.XPathUsageType.NORMAL);
		fooSp.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE);
		try {
			mySearchParameterDao.create(fooSp, mySrd);
			fail();
		} catch (UnprocessableEntityException e) {
			assertEquals("Invalid path value \"Observation.code\". All paths in a single SearchParameter must match the same resource type", e.getMessage());
		}
	}

	@Test
	public void testCreateInvalidParamNoPath() {
		SearchParameter fooSp = new SearchParameter();
		fooSp.setCode("foo");
		fooSp.setType(org.hl7.fhir.dstu3.model.Enumerations.SearchParamType.TOKEN);
		fooSp.setTitle("FOO SP");
		fooSp.setXpathUsage(org.hl7.fhir.dstu3.model.SearchParameter.XPathUsageType.NORMAL);
		fooSp.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE);
		try {
			mySearchParameterDao.create(fooSp, mySrd);
			fail();
		} catch (UnprocessableEntityException e) {
			assertEquals("Resource.xpath is missing", e.getMessage());
		}
	}

	@Test
	public void testCreateInvalidParamNoResourceName() {
		SearchParameter fooSp = new SearchParameter();
		fooSp.setCode("foo");
		fooSp.setType(org.hl7.fhir.dstu3.model.Enumerations.SearchParamType.TOKEN);
		fooSp.setTitle("FOO SP");
		fooSp.setXpath("gender");
		fooSp.setXpathUsage(org.hl7.fhir.dstu3.model.SearchParameter.XPathUsageType.NORMAL);
		fooSp.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE);
		try {
			mySearchParameterDao.create(fooSp, mySrd);
			fail();
		} catch (UnprocessableEntityException e) {
			assertEquals("Invalid path value \"gender\". Must start with a resource name", e.getMessage());
		}
	}

	@Test
	public void testCreateInvalidParamParamNullStatus() {

		SearchParameter fooSp = new SearchParameter();
		fooSp.setCode("foo");
		fooSp.setType(org.hl7.fhir.dstu3.model.Enumerations.SearchParamType.TOKEN);
		fooSp.setTitle("FOO SP");
		fooSp.setXpath("Patient.gender");
		fooSp.setXpathUsage(org.hl7.fhir.dstu3.model.SearchParameter.XPathUsageType.NORMAL);
		fooSp.setStatus(null);
		try {
			mySearchParameterDao.create(fooSp, mySrd);
			fail();
		} catch (UnprocessableEntityException e) {
			assertEquals("Resource.status is missing or invalid: null", e.getMessage());
		}

	}

	@Test
	public void testSearchWithCustomParam() {

		SearchParameter fooSp = new SearchParameter();
		fooSp.setCode("foo");
		fooSp.setType(org.hl7.fhir.dstu3.model.Enumerations.SearchParamType.TOKEN);
		fooSp.setTitle("FOO SP");
		fooSp.setXpath("Patient.gender");
		fooSp.setXpathUsage(org.hl7.fhir.dstu3.model.SearchParameter.XPathUsageType.NORMAL);
		fooSp.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE);
		mySearchParameterDao.create(fooSp, mySrd);

		mySearchParamRegsitry.forceRefresh();

		Patient pat = new Patient();
		pat.setGender(AdministrativeGender.MALE);
		IIdType patId = myPatientDao.create(pat, mySrd).getId().toUnqualifiedVersionless();

		Patient pat2 = new Patient();
		pat.setGender(AdministrativeGender.FEMALE);
		IIdType patId2 = myPatientDao.create(pat2, mySrd).getId().toUnqualifiedVersionless();

		SearchParameterMap map;
		IBundleProvider results;
		List<String> foundResources;

		// Try with custom gender SP
		map = new SearchParameterMap();
		map.add("foo", new TokenParam(null, "male"));
		results = myPatientDao.search(map);
		foundResources = toUnqualifiedVersionlessIdValues(results);
		assertThat(foundResources, contains(patId.getValue()));

		// Try with normal gender SP
		map = new SearchParameterMap();
		map.add("gender", new TokenParam(null, "male"));
		results = myPatientDao.search(map);
		foundResources = toUnqualifiedVersionlessIdValues(results);
		assertThat(foundResources, contains(patId.getValue()));

	}

	@Test
	public void testSearchWithCustomParamDraft() {

		SearchParameter fooSp = new SearchParameter();
		fooSp.setCode("foo");
		fooSp.setType(org.hl7.fhir.dstu3.model.Enumerations.SearchParamType.TOKEN);
		fooSp.setTitle("FOO SP");
		fooSp.setXpath("Patient.gender");
		fooSp.setXpathUsage(org.hl7.fhir.dstu3.model.SearchParameter.XPathUsageType.NORMAL);
		fooSp.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.DRAFT);
		mySearchParameterDao.create(fooSp, mySrd);

		mySearchParamRegsitry.forceRefresh();

		Patient pat = new Patient();
		pat.setGender(AdministrativeGender.MALE);
		IIdType patId = myPatientDao.create(pat, mySrd).getId().toUnqualifiedVersionless();

		Patient pat2 = new Patient();
		pat.setGender(AdministrativeGender.FEMALE);
		IIdType patId2 = myPatientDao.create(pat2, mySrd).getId().toUnqualifiedVersionless();

		SearchParameterMap map;
		IBundleProvider results;
		List<String> foundResources;

		// Try with custom gender SP (should find nothing)
		map = new SearchParameterMap();
		map.add("foo", new TokenParam(null, "male"));
		results = myPatientDao.search(map);
		foundResources = toUnqualifiedVersionlessIdValues(results);
		assertThat(foundResources, empty());

		// Try with normal gender SP
		map = new SearchParameterMap();
		map.add("gender", new TokenParam(null, "male"));
		results = myPatientDao.search(map);
		foundResources = toUnqualifiedVersionlessIdValues(results);
		assertThat(foundResources, contains(patId.getValue()));

	}

	@AfterClass
	public static void afterClassClearContext() {
		TestUtil.clearAllStaticFieldsForUnitTest();
	}

}
