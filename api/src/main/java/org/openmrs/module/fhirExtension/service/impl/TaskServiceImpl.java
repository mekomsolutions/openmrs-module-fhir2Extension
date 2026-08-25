package org.openmrs.module.fhirExtension.service.impl;

import org.openmrs.api.VisitService;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.dao.TaskDao;
import org.openmrs.module.fhirExtension.dao.TaskRequestedPeriodDao;
import org.openmrs.module.fhirExtension.model.FhirTaskRequestedPeriod;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.model.TaskSearchRequest;
import org.openmrs.module.fhirExtension.service.TaskService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Transactional
public class TaskServiceImpl implements TaskService {
	
	private VisitService visitService;
	
	private TaskDao taskDao;
	
	private TaskRequestedPeriodDao taskRequestedPeriodDao;
	
	@Override
	public Task saveTask(Task task) {
		taskDao.saveOrUpdate(task.getFhirTask());
		if (task.getFhirTaskRequestedPeriod() != null) {
			taskRequestedPeriodDao.save(task.getFhirTaskRequestedPeriod());
		}
		return task;
	}
	
	@Override
	public List<Task> saveTask(List<Task> tasks) {
		
		List<FhirTask> fhirTasks = new ArrayList<FhirTask>();
		List<FhirTaskRequestedPeriod> fhirTaskRequestedPeriods = new ArrayList<FhirTaskRequestedPeriod>();
		
		for (Task task : tasks) {
			fhirTasks.add(task.getFhirTask());
			if (task.getFhirTaskRequestedPeriod() != null) {
				fhirTaskRequestedPeriods.add(task.getFhirTaskRequestedPeriod());
			}
		}
		taskDao.save(fhirTasks);
		taskRequestedPeriodDao.save(fhirTaskRequestedPeriods);
		return tasks;
	}
	
	@Override
	public List<Task> getTasksByVisitFilteredByTimeFrame(String visitUuid, Date startTime, Date endTime) {
		return taskDao.getTasksByVisitFilteredByTimeFrame(visitService.getVisitByUuid(visitUuid), startTime, endTime);
	}
	
	@Override
	public List<Task> getTasksByPatientUuidsByTimeFrame(List<String> patientUuids, Date startTime, Date endTime) {
		return taskDao.getTasksByPatientUuidsFilteredByTimeFrame(patientUuids, startTime, endTime);
	}
	
	@Override
	public List<Task> getTasksByUuids(List<String> listOdUuids) {
		return taskDao.getTasksByUuids(listOdUuids);
	}
	
	@Override
	public List<Task> searchTasks(TaskSearchRequest taskSearchRequest) {
		return taskDao.searchTasks(taskSearchRequest);
	}
	
	public void setVisitService(VisitService visitService) {
		this.visitService = visitService;
	}
	
	public void setTaskDao(TaskDao taskDao) {
		this.taskDao = taskDao;
	}
	
	public void setTaskRequestedPeriodDao(TaskRequestedPeriodDao taskRequestedPeriodDao) {
		this.taskRequestedPeriodDao = taskRequestedPeriodDao;
	}
}
