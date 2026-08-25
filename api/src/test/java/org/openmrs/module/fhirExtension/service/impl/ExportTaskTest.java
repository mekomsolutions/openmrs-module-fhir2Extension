package org.openmrs.module.fhirExtension.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.dao.TaskDao;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
@PowerMockIgnore("javax.management.*")
public class ExportTaskTest {
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private TaskDao taskDao;
	
	@InjectMocks
	private ExportTaskImpl exportTask;
	
	@Before
	public void setUp() {
		PowerMockito.mockStatic(Context.class);
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		when(Context.getAuthenticatedUser()).thenReturn(authenticatedUser);
	}
	
	@Test
	public void shouldCreateFhirTask_whenRequestedForPatientDataExport() {
		when(taskDao.saveOrUpdate(any(FhirTask.class))).thenReturn(mockFhirTask());
		when(conceptService.getConceptByName(any())).thenReturn(new Concept());
		FhirTask initialTaskResponse = exportTask.getInitialTaskResponse("2023-05-01", "2023-05-31", "http://dummyUrl",
		    false);
		
		assertNotNull(initialTaskResponse);
		verify(conceptService, times(1)).getConceptByName("Download URL");
		verify(conceptService, times(1)).getConceptByName("FHIR Export User Name");
		verify(conceptService, times(1)).getConceptByName("FHIR Export Start Date");
		verify(conceptService, times(1)).getConceptByName("FHIR Export End Date");
		verify(conceptService, times(2)).getConceptByName("FHIR Export Anonymise Flag");
		verify(taskDao, times(1)).saveOrUpdate(any(FhirTask.class));
		assertEquals(FhirTask.TaskStatus.ACCEPTED, initialTaskResponse.getStatus());
	}
	
	@Test
	public void shouldNotReturnErrorMessage_whenNoDateRangeProvided() {
		String errorMessage = exportTask.validateParams(null, null);
		assertNull(errorMessage);
	}
	
	@Test
	public void shouldNotReturnErrorMessage_whenValidDateRangeProvided() {
		String errorMessage = exportTask.validateParams("2023-05-01", "2023-05-31");
		assertNull(errorMessage);
	}
	
	@Test
	public void shouldReturnErrorMessage_whenValidStartDateGreaterThanEndDate() {
		String errorMessage = exportTask.validateParams("2023-06-01", "2023-05-31");
		assertNotNull(errorMessage);
		assertEquals("End date [2023-05-31] should be on or after start date [2023-06-01]", errorMessage);
	}
	
	@Test
	public void shouldReturnErrorMessage_whenInvalidStartDateProvided() {
		String errorMessage = exportTask.validateParams("2023-05-AB", "2023-05-31");
		assertNotNull(errorMessage);
		assertEquals("Invalid Date Format [yyyy-mm-dd]", errorMessage);
	}
	
	@Test
	public void shouldReturnErrorMessage_whenInvalidEndDateProvided() {
		String errorMessage = exportTask.validateParams("2023-05-01", "2023-05-AB");
		assertNotNull(errorMessage);
		assertEquals("Invalid Date Format [yyyy-mm-dd]", errorMessage);
	}
	
	private FhirTask mockFhirTask() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		return fhirTask;
	}
}
