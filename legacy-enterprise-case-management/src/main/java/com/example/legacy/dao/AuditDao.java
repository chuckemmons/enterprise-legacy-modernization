package com.example.legacy.dao;
import com.example.legacy.model.AuditEvent;
import java.util.List;
import org.hibernate.Session;
public final class AuditDao {
    private final Session session;
    public AuditDao(Session s) { session=s; }
    public void save(AuditEvent event) { session.save(event); }
    @SuppressWarnings("unchecked") public List<AuditEvent> recent(int page) {
        return session.createQuery("from AuditEvent order by createdAt desc").setFirstResult(Math.max(0,page)*50).setMaxResults(50).list();
    }
}
