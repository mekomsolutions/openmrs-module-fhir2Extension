package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
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
import org.openmrs.module.fhir2.api.FhirConditionService;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.dao.TaskDao;
import org.openmrs.module.fhir2.model.FhirTaskInput;
import org.openmrs.module.fhirExtension.export.Exporter;
import org.openmrs.module.fhirExtension.export.anonymise.handler.AnonymiseHandler;
import org.openmrs.module.fhirExtension.export.anonymise.impl.CorrelationCache;
import org.openmrs.module.fhirExtension.export.impl.ConditionExport;
import org.openmrs.module.fhirExtension.service.impl.ExportAsyncServiceImpl;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
@PowerMockIgnore("javax.management.*")
public class ExportAsyncServiceTest {
	
	@Mock
	private TaskDao taskDao;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private FileExportService fileExportService;
	
	@Mock
	private FhirConditionService fhirConditionService;
	
	@Mock
	private AnonymiseHandler anonymiseHandler;
	
	@Mock
	private CorrelationCache correlationCache;
	
	@InjectMocks
	private ExportAsyncServiceImpl exportAsyncServiceImpl;
	
	@Before
	public void setUp() {
		PowerMockito.mockStatic(Context.class);
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		authenticatedUser.setUsername("dummy");
		when(Context.getAuthenticatedUser()).thenReturn(authenticatedUser);
	}
	
	@Test
	public void shouldExportPatientDataAndUpdateFhirTaskStatusToCompleted_whenValidDateRangeProvided() {
		FhirTask fhirTask = mockFhirTask();
		when(conceptService.getConceptByName(any())).thenReturn(new Concept());
		exportAsyncServiceImpl.export(fhirTask, "2023-01-01", "2023-12-31", Context.getUserContext(), false);
		
		assertEquals(FhirTask.TaskStatus.COMPLETED, fhirTask.getStatus());
		assertEquals(4, fhirTask.getInput().size());
		verify(taskDao, times(1)).saveOrUpdate(any(FhirTask.class));
	}
	
	@Test
	public void shouldChangeFhirTaskStatusToRejected_whenInvalidDateRangeProvided() {
		List<Exporter> exporters = new ArrayList<>();
		exporters.add(new ConditionExport(fhirConditionService));
		when(Context.getRegisteredComponents(Exporter.class)).thenReturn(exporters);

		FhirTask fhirTask = mockFhirTask();

		exportAsyncServiceImpl.export(fhirTask, "2023-AB-CD", "2023-12-31", Context.getUserContext(), false);

		assertEquals(FhirTask.TaskStatus.REJECTED, fhirTask.getStatus());
		verify(taskDao, times(1)).saveOrUpdate(any(FhirTask.class));
	}
	
	@Test
	public void shouldCallAnonymiseHandler_whenValidDateRangeProvided() {
		List<Exporter> exporters = new ArrayList<>();
		exporters.add(new ConditionExport(fhirConditionService));
		when(Context.getRegisteredComponents(Exporter.class)).thenReturn(exporters);
		when(fhirConditionService.searchConditions(any())).thenReturn(getMockConditionBundle(1));
		when(conceptService.getConceptByName(any())).thenReturn(new Concept());

		FhirTask fhirTask = mockFhirTask();

		exportAsyncServiceImpl.export(fhirTask, "2023-01-01", "2023-12-31", Context.getUserContext(), true);

		assertEquals(FhirTask.TaskStatus.COMPLETED, fhirTask.getStatus());
		verify(anonymiseHandler, times(1)).anonymise(any(IBaseResource.class), eq("condition"));
		verify(taskDao, times(1)).saveOrUpdate(any(FhirTask.class));
	}
	
	private FhirTask mockFhirTask() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		fhirTask.setInput(getFhirTaskInputs(fhirTask, "2023-01-01", "2023-12-31", true));
		return fhirTask;
	}
	
	private Set<FhirTaskInput> getFhirTaskInputs(FhirTask fhirTask, String startDate, String endDate, boolean isAnonymise) {
		FhirTaskInput userNameFhirTaskInput = createFHIRTaskInput(fhirTask, ExportTask.USER_NAME_CONCEPT,	"dummy");
		FhirTaskInput startDateFhirTaskInput = createFHIRTaskInput(fhirTask, ExportTask.START_DATE_CONCEPT, startDate);
		FhirTaskInput endDateFhirTaskInput = createFHIRTaskInput(fhirTask, ExportTask.END_DATE_CONCEPT, endDate);
		FhirTaskInput anonymiseFhirTaskInput = createFHIRTaskInput(fhirTask, ExportTask.ANONYMISE_CONCEPT, Boolean.toString(isAnonymise));
		return new HashSet<>(Arrays.asList(userNameFhirTaskInput, startDateFhirTaskInput, endDateFhirTaskInput, anonymiseFhirTaskInput));
	}
	
	private FhirTaskInput createFHIRTaskInput(FhirTask fhirTask, String conceptName, String valueText) {
		FhirTaskInput fhirTaskInput = new FhirTaskInput();
		fhirTaskInput.setName(conceptName);
		fhirTaskInput.setTask(fhirTask);
		fhirTaskInput.setValueText(valueText);
		fhirTaskInput.setType(new Concept());
		return fhirTaskInput;
	}
	
	private IBundleProvider getMockConditionBundle(int count) {
		Condition activeConditionResource = new Condition();
		CodeableConcept activeClinicalStatus = new CodeableConcept();
		activeClinicalStatus.setCoding(Collections.singletonList(new Coding("dummy", "active", "active")));
		activeConditionResource.setClinicalStatus(activeClinicalStatus);
		Condition inactiveConditionResource = new Condition();
		CodeableConcept inactiveClinicalStatus = new CodeableConcept();
		inactiveClinicalStatus.setCoding(Collections.singletonList(new Coding("dummy", "history", "history")));
		inactiveConditionResource.setClinicalStatus(inactiveClinicalStatus);
		
		IBundleProvider iBundleProvider = null;
		if (count == 1) {
			iBundleProvider = new SimpleBundleProvider(Arrays.asList(activeConditionResource));
		} else {
			iBundleProvider = new SimpleBundleProvider(Arrays.asList(activeConditionResource, inactiveConditionResource));
		}
		
		return iBundleProvider;
	}
}
