package com.example.legacy.dao;
import com.example.legacy.model.CaseNote;
import java.util.List;
import org.hibernate.Session;
public final class CaseNoteDao {
    private final Session session;
    public CaseNoteDao(Session s) { session=s; }
    public void save(CaseNote item) { session.save(item); }
    @SuppressWarnings("unchecked") public List<CaseNote> byCase(long id) {
        return session.createQuery("from CaseNote where caseId=:id order by createdAt desc").setLong("id",id).list();
    }
}
