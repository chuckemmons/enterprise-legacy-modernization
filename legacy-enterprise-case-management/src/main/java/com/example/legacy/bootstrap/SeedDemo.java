package com.example.legacy.bootstrap;
import com.example.legacy.db.Database;
import com.example.legacy.dao.AccountDao;
import com.example.legacy.model.*;
import com.example.legacy.service.*;
import com.example.legacy.util.Passwords;
import java.util.*;
import org.hibernate.Session;
public final class SeedDemo {
    public static void main(String[] args) {
        final String password=System.getenv("CASE_DEMO_PASSWORD");
        if(password==null || password.length()<12) throw new IllegalArgumentException("Set CASE_DEMO_PASSWORD to a nontrivial password (12+ chars)");
        Database.tx(new Database.Work<Void>() {
            public Void run(Session s) {
                for(String[] row:new String[][] { {"supervisor","SUPERVISOR"},{"worker","WORKER"},{"auditor","AUDITOR"} }) {
                    if(Services.account(s,row[0])!=null) continue;
                    Account a=new Account();a.setUsername(row[0]);a.setRole(row[1]);a.setPasswordHash(Passwords.hash(password));
                    a.setDisplayName("Demo " + row[1]);a.setDepartment("Operations");a.setEmail("");a.setEnabled(true);a.setCreatedAt(new Date());new AccountDao(s).save(a);
                }
                return null;
            }
        });
        for(int i=0;i<8;i++) {
            ServiceCase c=Services.createCase("worker","Demo request " + (i+1),"Sample case for workflow exercises", 
                        i%2==0?"ACCESS":"GENERAL",i%3==0?"HIGH":"MEDIUM","Operations",new Date(System.currentTimeMillis()+86400000L*(i-2)));
            Services.assign("supervisor",c.getId(),"worker");
        }
        System.out.println("Created demo users and eight cases. Do not use shared demo passwords in production.");
        Database.shutdown();
    }
}
