package com.example.legacy.bootstrap;
import com.example.legacy.db.Database;
import com.example.legacy.dao.AccountDao;
import com.example.legacy.model.Account;
import com.example.legacy.service.Services;
import com.example.legacy.util.Passwords;
import java.util.Date;
import org.hibernate.Session;
public final class SeedAdmin {
    public static void main(String[] args) {
        final String user=System.getenv("CASE_ADMIN_USER");
        final String password=System.getenv("CASE_ADMIN_PASSWORD");
        if(user==null || !user.matches("[a-z][a-z0-9._-]{2,59}") || password==null || password.length()<12) {
            throw new IllegalArgumentException("Set CASE_ADMIN_USER and CASE_ADMIN_PASSWORD (12+ chars) environment variables");
        }
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                if(Services.account(s,user)!=null) throw new IllegalStateException("Account already exists; no changes made");
                Account a=new Account();a.setUsername(user);a.setPasswordHash(Passwords.hash(password));
                a.setDisplayName("System Administrator");a.setEmail("");a.setRole("ADMIN");
                a.setDepartment("Operations");a.setEnabled(true);a.setCreatedAt(new Date());new AccountDao(s).save(a);
                System.out.println("Created administrator " + user);return null;
            }
        });
        Database.shutdown();
    }
}
