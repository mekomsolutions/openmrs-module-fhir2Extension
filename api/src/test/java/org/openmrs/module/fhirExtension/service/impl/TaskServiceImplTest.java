package org.openmrs.module.fhirExtension.service.impl;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.dao.TaskDao;
import org.openmrs.module.fhirExtension.dao.TaskRequestedPeriodDao;
import org.openmrs.module.fhirExtension.model.FhirTaskRequestedPeriod;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.model.TaskSearchRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class TaskServiceImplTest {
	
	@Mock
	private VisitService visitService;
	
	@Mock
	private TaskDao taskDao;
	
	@Mock
	private TaskRequestedPeriodDao taskRequestedPeriodDao;
	
	@InjectMocks
	private TaskServiceImpl taskService;
	
	public TaskServiceImplTest() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testSaveTask() {
		FhirTask fhirTask = new FhirTask();
		FhirTaskRequestedPeriod requestedPeriod = new FhirTaskRequestedPeriod();
		requestedPeriod.setRequestedStartTime(new Date());
		Task task = new Task(fhirTask, requestedPeriod);
		
		when(taskDao.saveOrUpdate(any(FhirTask.class))).thenReturn(task.getFhirTask());
		when(taskRequestedPeriodDao.save((FhirTaskRequestedPeriod) any())).thenReturn(task.getFhirTaskRequestedPeriod());
		
		Task savedTask = taskService.saveTask(task);
		
		verify(taskDao, times(1)).saveOrUpdate(any(FhirTask.class));
		verify(taskRequestedPeriodDao, times(1)).save((FhirTaskRequestedPeriod) any());
	}
	
	@Test
    public void testGetTasksByVisitFilteredByTimeFrame() {
        String visitUuid = "sample-visit-uuid";
        Date startTime = new Date();
        Date endTime = new Date();

        List<Task> tasks = new ArrayList<>();

        when(visitService.getVisitByUuid(visitUuid)).thenReturn(null); // Mock return value of VisitService
        when(taskDao.getTasksByVisitFilteredByTimeFrame(any(), any(), any())).thenReturn(tasks);

        List<Task> result = taskService.getTasksByVisitFilteredByTimeFrame(visitUuid, startTime, endTime);

        verify(taskDao, times(1)).getTasksByVisitFilteredByTimeFrame(any(), any(), any());
    }
	
	@Test
    public void testGetTasksByPatientUuidsByTimeFrame() {
        List<String> patientUuids = new ArrayList<>();
        Date startTime = new Date();
        Date endTime = new Date();

        List<Task> tasks = new ArrayList<>();

        when(taskDao.getTasksByPatientUuidsFilteredByTimeFrame(any(), any(), any())).thenReturn(tasks);

        List<Task> result = taskService.getTasksByPatientUuidsByTimeFrame(patientUuids, startTime, endTime);

        verify(taskDao, times(1)).getTasksByPatientUuidsFilteredByTimeFrame(any(), any(), any());
    }
	
	@Test
	public void testGetTasksByUuids() {

		List<Task> tasks = new ArrayList<>();
		List<String> uuids = new ArrayList<>(Arrays.asList(UUID.randomUUID().toString()));

		when(taskDao.getTasksByUuids(uuids)).thenReturn(tasks);

		List<Task> result = taskService.getTasksByUuids(uuids);

		verify(taskDao, times(1)).getTasksByUuids(uuids);
	}
	
	@Test
	public void testSaveTaskList() {

		FhirTask fhirTask = new FhirTask();
		FhirTaskRequestedPeriod requestedPeriod = new FhirTaskRequestedPeriod();
		requestedPeriod.setRequestedStartTime(new Date());
		Task task = new Task(fhirTask, requestedPeriod);

		List<Task> tasks=new ArrayList<>();
		tasks.add(task);

		List<FhirTask> fhirTasks=new ArrayList<>();
		fhirTasks.add(fhirTask);

		List<FhirTaskRequestedPeriod> fhirTaskRequestedPeriods=new ArrayList<>();
		fhirTaskRequestedPeriods.add(requestedPeriod);

		when(taskDao.save(fhirTasks)).thenReturn(fhirTasks);
		when(taskRequestedPeriodDao.save(fhirTaskRequestedPeriods)).thenReturn(fhirTaskRequestedPeriods);

		taskService.saveTask(tasks);

		verify(taskDao, times(1)).save(fhirTasks);
		verify(taskRequestedPeriodDao, times(1)).save(fhirTaskRequestedPeriods);
	}
	
	@Test
	public void testSearchTasks() {

		List<Task> tasks = new ArrayList<>();
		TaskSearchRequest searchRequest=new TaskSearchRequest();

		when(taskDao.searchTasks(searchRequest)).thenReturn(tasks);

		List<Task> result = taskService.searchTasks(searchRequest);

		verify(taskDao, times(1)).searchTasks(searchRequest);

	}
}
