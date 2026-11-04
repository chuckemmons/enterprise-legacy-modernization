package com.example.legacy.bootstrap;
import com.example.legacy.service.BatchJobs;
import com.example.legacy.db.Database;
/** Trigger from a controlled scheduler or a cron calling Ant. */
public final class RunJobs {
    public static void main(String[] args) {
        if(args.length!=1) throw new IllegalArgumentException("Expected overdue or email");
        try {
            if("overdue".equals(args[0])) BatchJobs.createOverdueNotifications();
            else if("email".equals(args[0])) BatchJobs.sendPendingEmail();
            else throw new IllegalArgumentException("Expected overdue or email");
        } finally {Database.shutdown();}
    }
}
