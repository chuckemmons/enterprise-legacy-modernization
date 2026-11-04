package com.example.legacy.dao;
import com.example.legacy.model.CaseDocument;
import java.util.List;
import org.hibernate.Session;
public final class CaseDocumentDao {
    private final Session session;
    public CaseDocumentDao(Session s) { session=s; }
    public void save(CaseDocument item) { session.save(item); }
    public CaseDocument id(long id) { return (CaseDocument)session.get(CaseDocument.class,id); }
    @SuppressWarnings("unchecked") public List<CaseDocument> byCase(long id) {
        return session.createQuery("from CaseDocument where caseId=:id order by createdAt desc").setLong("id",id).list();
    }
}
