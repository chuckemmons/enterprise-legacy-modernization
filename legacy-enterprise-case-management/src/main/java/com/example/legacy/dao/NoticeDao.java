package com.example.legacy.dao;
import com.example.legacy.model.CaseNotice;
import java.util.List;
import org.hibernate.Session;
public final class NoticeDao {
    private final Session session;
    public NoticeDao(Session s) { session=s; }
    public void save(CaseNotice n) { session.save(n); }
    public CaseNotice id(long id) { return (CaseNotice)session.get(CaseNotice.class,id); }
    @SuppressWarnings("unchecked") public List<CaseNotice> forUser(String user) {
        return session.createQuery("from CaseNotice where recipient=:user order by createdAt desc")
            .setString("user",user).setMaxResults(100).list();
    }
    @SuppressWarnings("unchecked") public List<Long> pendingIds() {
        return session.createQuery("select id from CaseNotice where sentAt is null order by createdAt").setMaxResults(50).list();
    }
}
