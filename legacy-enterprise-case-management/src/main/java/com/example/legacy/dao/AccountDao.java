package com.example.legacy.dao;
import com.example.legacy.model.Account;
import java.util.List;
import org.hibernate.Session;
/** All user-table database access lives in this DAO. */
public final class AccountDao {
    private final Session session;
    public AccountDao(Session s) { session=s; }
    public Account username(String username) { return (Account)session.createQuery("from Account where username=:u").setString("u",username).uniqueResult(); }
    public Account id(long id) { return (Account)session.get(Account.class,id); }
    @SuppressWarnings("unchecked") public List<Account> all() { return session.createQuery("from Account order by username").list(); }
    public void save(Account account) { session.save(account); }
    public void flush() { session.flush(); }
}
