package org.openmrs.module.fhirExtension.service.impl;

import lombok.extern.log4j.Log4j2;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.dao.TaskDao;
import org.openmrs.module.fhirExtension.export.Exporter;
import org.openmrs.module.fhirExtension.export.anonymise.handler.AnonymiseHandler;
import org.openmrs.module.fhirExtension.export.anonymise.impl.CorrelationCache;
import org.openmrs.module.fhirExtension.service.ExportAsyncService;
import org.openmrs.module.fhirExtension.service.FileExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Log4j2
@Component
@Transactional
public class ExportAsyncServiceImpl implements ExportAsyncService {
	
	private TaskDao taskDao;
	
	private ConceptService conceptService;
	
	private FileExportService fileExportService;
	
	private AnonymiseHandler anonymiseHandler;
	
	private CorrelationCache correlationCache;
	
	@Autowired
	public ExportAsyncServiceImpl(TaskDao taskDao, ConceptService conceptService, FileExportService fileExportService,
	    AnonymiseHandler anonymiseHandler, CorrelationCache correlationCache) {
		this.taskDao = taskDao;
		this.conceptService = conceptService;
		this.fileExportService = fileExportService;
		this.anonymiseHandler = anonymiseHandler;
		this.correlationCache = correlationCache;
	}
	
	@Override
	@Async("export-fhir-data-threadPoolTaskExecutor")
	public void export(FhirTask fhirTask, String startDate, String endDate, UserContext userContext, boolean isAnonymise) {
		FhirTask.TaskStatus taskStatus = null;
		
		try {
			Context.openSession();
			Context.setUserContext(userContext);
			List<Exporter> fhirExporters = Context.getRegisteredComponents(Exporter.class);
			initialize(fhirTask, isAnonymise);
			
			for (Exporter fhirExporter : fhirExporters) {
				List<IBaseResource> fhirResources = fhirExporter.export(startDate, endDate);
				anonymise(fhirResources, fhirExporter.getResourceType(), isAnonymise);
				fileExportService.createAndWriteToFile(fhirResources, fhirTask.getUuid());
			}
			
			fileExportService.createZipWithExportedNdjsonFiles(fhirTask.getUuid());
			fileExportService.deleteDirectory(fhirTask.getUuid());
		}
		catch (Exception exception) {
			taskStatus = FhirTask.TaskStatus.REJECTED;
			log.error("Exception occurred while exporting data in FHIR format ", exception);
		}
		finally {
			if (taskStatus == null) {
				taskStatus = FhirTask.TaskStatus.COMPLETED;
			}
			fhirTask.setStatus(taskStatus);
			taskDao.saveOrUpdate(fhirTask);
		}
	}
	
	private void initialize(FhirTask fhirTask, boolean isAnonymise) {
		fileExportService.createDirectory(fhirTask.getUuid());
		correlationCache.reset();
		anonymiseHandler.loadAnonymiserConfig(isAnonymise);
	}
	
	private void anonymise(List<IBaseResource> iBaseResources, String resourceType, boolean isAnonymise) {
        if (isAnonymise) {
            iBaseResources.forEach(iBaseResource -> anonymiseHandler.anonymise(iBaseResource, resourceType));
        }
    }
}
