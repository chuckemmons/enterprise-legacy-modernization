package com.example.legacy.dao;
import org.hibernate.Session;
/** Thin session-scoped DAO factory; service layer controls transaction boundaries. */
public final class Repositories {
    private final Session session;
    public Repositories(Session s) { session=s; }
    public AccountDao users() { return new AccountDao(session); }
    public CaseDao cases() { return new CaseDao(session); }
    public CaseNoteDao notes() { return new CaseNoteDao(session); }
    public CaseDocumentDao documents() { return new CaseDocumentDao(session); }
    public AuditDao audits() { return new AuditDao(session); }
    public NoticeDao notices() { return new NoticeDao(session); }
}
