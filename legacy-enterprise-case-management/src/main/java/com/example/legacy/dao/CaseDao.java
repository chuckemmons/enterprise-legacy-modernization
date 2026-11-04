package com.example.legacy.dao;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.util.Strings;
import java.util.*;
import org.hibernate.*;
/** Hibernate 3.5 native Session/HQL. No EntityManager, JPA, or annotations. */
public final class CaseDao {
    private final Session session;
    public CaseDao(Session s) { session=s; }
    public ServiceCase id(long id) { return (ServiceCase)session.get(ServiceCase.class,id); }
    public void save(ServiceCase item) { session.save(item); }
    public void flush() { session.flush(); }
    @SuppressWarnings("unchecked") public List<ServiceCase> search(String search,String status,String assignee,int page,int size) {
        String hql="from ServiceCase c where c.archived=false";
        if(search!=null&&!search.trim().isEmpty()) hql+=" and (lower(c.title) like :q or lower(c.caseNumber) like :q)";
        if(status!=null&&!status.isEmpty()) hql+=" and c.status=:status";
        if(assignee!=null&&!assignee.isEmpty()) hql+=" and c.assignedTo=:assignee";
        Query q=session.createQuery(hql+" order by c.updatedAt desc");
        if(search!=null&&!search.trim().isEmpty()) q.setString("q","%"+Strings.lower(Strings.clean(search,80))+"%");
        if(status!=null&&!status.isEmpty()) q.setString("status",status);
        if(assignee!=null&&!assignee.isEmpty()) q.setString("assignee",assignee);
        return q.setFirstResult(Math.max(0,page)*size).setMaxResults(size).list();
    }
    @SuppressWarnings("unchecked") public List<Object[]> grouped(String property) {
        // Property must be selected by server code, not supplied by HTTP request.
        if(!"status".equals(property)&&!"priority".equals(property)) throw new IllegalArgumentException("Invalid grouping");
        return session.createQuery("select "+property+", count(id) from ServiceCase where archived=false group by "+property+" order by "+property).list();
    }
    public Long countOverdue() {
        return (Long)session.createQuery("select count(id) from ServiceCase where archived=false and status <> 'CLOSED' and dueAt < :now")
            .setTimestamp("now",new Date()).uniqueResult();
    }
    @SuppressWarnings("unchecked") public List<ServiceCase> overdueUnnotified() {
        return session.createQuery("from ServiceCase where archived=false and status <> 'CLOSED' and dueAt < :now and overdueNoticeSent=false")
            .setTimestamp("now",new Date()).setMaxResults(300).list();
    }
}
