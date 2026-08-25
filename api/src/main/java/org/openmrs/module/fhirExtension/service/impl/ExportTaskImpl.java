package org.openmrs.module.fhirExtension.service.impl;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhir2.model.FhirTaskInput;
import org.openmrs.module.fhir2.model.FhirTaskOutput;
import org.openmrs.module.fhirExtension.dao.TaskDao;
import org.openmrs.module.fhirExtension.service.ExportTask;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.openmrs.module.fhirExtension.export.Exporter.DATE_FORMAT;

@Log4j2
@Transactional
public class ExportTaskImpl implements ExportTask {
	
	private static final String NON_ANONYMISE_REQUIRED_PRIVILEGE = "Export Non Anonymised Patient Data";
	
	private TaskDao taskDao;
	
	private ConceptService conceptService;
	
	public ExportTaskImpl(TaskDao taskDao, ConceptService conceptService) {
		this.taskDao = taskDao;
		this.conceptService = conceptService;
	}
	
	@Override
	public FhirTask getInitialTaskResponse(String startDate, String endDate, String downloadUrl, boolean isAnonymise) {
		FhirTask fhirTask = new FhirTask();
		if (!isAnonymise) {
			Context.requirePrivilege(NON_ANONYMISE_REQUIRED_PRIVILEGE);
		}
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		String logMessage = "Patient Data Export by " + Context.getAuthenticatedUser().getUsername();
		fhirTask.setName(logMessage);
		log.info(logMessage);
		fhirTask.setIntent(FhirTask.TaskIntent.ORDER);
		FhirTaskOutput fhirTaskOutput = getFhirTaskOutput(fhirTask, downloadUrl);
		fhirTask.setOutput(Collections.singleton(fhirTaskOutput));
		Set<FhirTaskInput> fhirTaskInputs = getFhirTaskInputs(fhirTask, startDate, endDate, isAnonymise);
		fhirTask.setInput(fhirTaskInputs);
		FhirReference basedOnReference = new FhirReference();
		basedOnReference.setType(FhirConstants.SERVICE_REQUEST);
		basedOnReference.setReference(conceptService.getConceptByName(ANONYMISE_CONCEPT).getUuid()); // Assume concept uuid is 8741c3e7-a250-4808-977c-a89459bb6c9d
		basedOnReference.setName("Patient Data Export");
		fhirTask.setBasedOnReferences(Collections.singleton(basedOnReference));
		taskDao.saveOrUpdate(fhirTask);
		
		return fhirTask;
	}
	
	@Override
	public String validateParams(String startDateStr, String endDateStr) {
		Date startDate = null;
		Date endDate = null;
		String validationErrorMessage = null;
		try {
			if (startDateStr != null)
				startDate = DateUtils.parseDateStrictly(startDateStr, DATE_FORMAT);
			if (endDateStr != null)
				endDate = DateUtils.parseDateStrictly(endDateStr, DATE_FORMAT);
		}
		catch (Exception e) {
			validationErrorMessage = "Invalid Date Format [yyyy-mm-dd]";
		}
		if (startDate != null && endDate != null && startDate.after(endDate)) {
			validationErrorMessage = String.format("End date [%s] should be on or after start date [%s]", endDateStr,
			    startDateStr);
		}
		return validationErrorMessage;
	}
	
	private Set<FhirTaskInput> getFhirTaskInputs(FhirTask fhirTask, String startDate, String endDate, boolean isAnonymise) {
		FhirTaskInput userNameFhirTaskInput = createFHIRTaskInput(fhirTask, USER_NAME_CONCEPT,	Context.getAuthenticatedUser().getUsername());
		FhirTaskInput startDateFhirTaskInput = createFHIRTaskInput(fhirTask, START_DATE_CONCEPT, startDate);
		FhirTaskInput endDateFhirTaskInput = createFHIRTaskInput(fhirTask, END_DATE_CONCEPT, endDate);
		FhirTaskInput anonymiseFhirTaskInput = createFHIRTaskInput(fhirTask, ANONYMISE_CONCEPT, Boolean.toString(isAnonymise));
		return new HashSet<>(Arrays.asList(userNameFhirTaskInput, startDateFhirTaskInput, endDateFhirTaskInput, anonymiseFhirTaskInput));
	}
	
	private FhirTaskInput createFHIRTaskInput(FhirTask fhirTask, String conceptName, String valueText) {
		Concept concept = conceptService.getConceptByName(conceptName);
		if (concept == null) {
			throw new RuntimeException("Concept with name " + conceptName + " not found");
		}
		FhirTaskInput fhirTaskInput = new FhirTaskInput();
		fhirTaskInput.setName(conceptName);
		fhirTaskInput.setTask(fhirTask);
		fhirTaskInput.setValueText(valueText);
		fhirTaskInput.setType(concept);
		return fhirTaskInput;
	}
	
	private FhirTaskOutput getFhirTaskOutput(FhirTask fhirTask, String downloadUrl) {
		FhirTaskOutput fhirTaskOutput = new FhirTaskOutput();
		fhirTaskOutput.setName("Download Link Name");
		fhirTaskOutput.setTask(fhirTask);
		fhirTaskOutput.setValueText(downloadUrl + "?file=" + fhirTask.getUuid());
		fhirTaskOutput.setType(conceptService.getConceptByName(DOWNLOAD_URL));
		return fhirTaskOutput;
	}
}
