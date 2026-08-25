package org.openmrs.module.fhirExtension.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Visit;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.dao.TaskDao;
import org.openmrs.module.fhirExtension.model.FhirTaskRequestedPeriod;
import org.openmrs.module.fhirExtension.model.Task;
import org.openmrs.module.fhirExtension.model.TaskSearchRequest;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaskDaoImpl implements TaskDao {
	
	private SessionFactory sessionFactory;
	
	private Log log = LogFactory.getLog(this.getClass());
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public List<Task> getTasksByVisitFilteredByTimeFrame(Visit visit, Date startTime, Date endTime) {
		try {
			CriteriaBuilder criteriaBuilder = sessionFactory.getCurrentSession().getCriteriaBuilder();
			CriteriaQuery<Task> criteriaQuery = criteriaBuilder.createQuery(Task.class);
			Root<FhirTaskRequestedPeriod> fhirTaskRequestedPeriod = criteriaQuery.from(FhirTaskRequestedPeriod.class);
			Join<FhirTask, FhirTaskRequestedPeriod> fhirTaskJoin = fhirTaskRequestedPeriod.join("task");

			criteriaQuery.select(criteriaBuilder.construct(Task.class, fhirTaskJoin, fhirTaskRequestedPeriod));
			criteriaQuery.where(
					criteriaBuilder.and(
							criteriaBuilder.equal(fhirTaskJoin.get("forReference").get("targetUuid"),visit.getUuid()),
							criteriaBuilder.between(fhirTaskRequestedPeriod.get("requestedStartTime"), startTime, endTime)
					)
			);

			TypedQuery<Task> query = sessionFactory.getCurrentSession().createQuery(criteriaQuery);

			return query.getResultList();

		}
		catch (Exception ex){
			log.error("Error while getTasksByVisitFilteredByTimeFrame ",ex);
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<Task> getTasksByPatientUuidsFilteredByTimeFrame(List<String> patientUuids, Date startTime, Date endTime) {
		try {
			CriteriaBuilder criteriaBuilder = sessionFactory.getCurrentSession().getCriteriaBuilder();

			CriteriaQuery<Task> criteriaQuery = criteriaBuilder.createQuery(Task.class);
			Root<FhirTaskRequestedPeriod> fhirTaskRequestedPeriod = criteriaQuery.from(FhirTaskRequestedPeriod.class);
			Join<FhirTask, FhirTaskRequestedPeriod> fhirTaskJoin = fhirTaskRequestedPeriod.join("task");

			Subquery<String> visitSubQuery = criteriaQuery.subquery(String.class);
			Root<Visit> visitRoot = visitSubQuery.from(Visit.class);
			Predicate activeVisits = criteriaBuilder.and(
					criteriaBuilder.in(visitRoot.join("patient").get("uuid")).value(patientUuids),
					criteriaBuilder.isNull(visitRoot.get("stopDatetime"))
			);
			visitSubQuery.where(activeVisits);
			visitSubQuery.select(visitRoot.get("uuid"));

			criteriaQuery.select(criteriaBuilder.construct(Task.class, fhirTaskJoin, fhirTaskRequestedPeriod));
			criteriaQuery.where(
					criteriaBuilder.and(
							criteriaBuilder.in(fhirTaskJoin.get("forReference").get("targetUuid")).value(visitSubQuery),
							criteriaBuilder.between(fhirTaskRequestedPeriod.get("requestedStartTime"), startTime, endTime)
					)
			);

			TypedQuery<Task> query = sessionFactory.getCurrentSession().createQuery(criteriaQuery);

			return query.getResultList();

		}
		catch (Exception ex){
			log.error("Error while getTasksByPatientUuidsFilteredByTimeFrame ",ex);
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<Task> getTasksByUuids(List<String> listOfUuids) {
		try {
			CriteriaBuilder criteriaBuilder = sessionFactory.getCurrentSession().getCriteriaBuilder();
			CriteriaQuery<Task> criteriaQuery = criteriaBuilder.createQuery(Task.class);
			Root<FhirTaskRequestedPeriod> fhirTaskRequestedPeriod = criteriaQuery.from(FhirTaskRequestedPeriod.class);
			Join<FhirTask, FhirTaskRequestedPeriod> fhirTaskJoin = fhirTaskRequestedPeriod.join("task");

			criteriaQuery.select(criteriaBuilder.construct(Task.class, fhirTaskJoin, fhirTaskRequestedPeriod)).where(
			    fhirTaskJoin.get("uuid").in(listOfUuids));
			TypedQuery<Task> query = sessionFactory.getCurrentSession().createQuery(criteriaQuery);

			return query.getResultList();
		}
		catch (Exception e) {
			log.error("Error while getTasksByPatientUuidsFilteredByTimeFrame ",e);
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<Task> searchTasks(TaskSearchRequest taskSearchRequest) {
		try {
			CriteriaBuilder criteriaBuilder = sessionFactory.getCurrentSession().getCriteriaBuilder();
			CriteriaQuery<Task> criteriaQuery = criteriaBuilder.createQuery(Task.class);
			Root<FhirTaskRequestedPeriod> fhirTaskRequestedPeriod = criteriaQuery.from(FhirTaskRequestedPeriod.class);
			Join<FhirTask, FhirTaskRequestedPeriod> fhirTaskJoin = fhirTaskRequestedPeriod.join("task");

			Predicate searchCondition=null;

			if (taskSearchRequest.getTaskName()!=null && !taskSearchRequest.getTaskName().isEmpty()){
				searchCondition = fhirTaskJoin.get("name").in(taskSearchRequest.getTaskName());
			}

			if (taskSearchRequest.getTaskStatus()!=null && !taskSearchRequest.getTaskStatus().isEmpty()){
				Predicate statusSearchCondition = fhirTaskJoin.get("status").in(taskSearchRequest.getTaskStatus());
				searchCondition = searchCondition ==null ? statusSearchCondition : criteriaBuilder.and(searchCondition,statusSearchCondition);
			}

			criteriaQuery.select(criteriaBuilder.construct(Task.class, fhirTaskJoin, fhirTaskRequestedPeriod)).where(searchCondition);
			TypedQuery<Task> query = sessionFactory.getCurrentSession().createQuery(criteriaQuery);
			return query.getResultList();
		}
		catch (Exception e) {
			log.error("Error while searchTasks ",e);
		}
		return new ArrayList<>();
	}
	
	@Override
	public FhirTask saveOrUpdate(FhirTask task) {
		if (task == null) {
			return null;
		}
		Session session = sessionFactory.getCurrentSession();
		if (task.getId() == null) {
			session.persist(task);
		} else {
			session.merge(task);
		}
		session.flush();
		return task;
	}
	
	@Override
	public List<FhirTask> save(List<FhirTask> tasks) {
		tasks.forEach(task -> {
			sessionFactory.getCurrentSession().persist(task);
		});
		sessionFactory.getCurrentSession().flush();
		return tasks;
	}
}
