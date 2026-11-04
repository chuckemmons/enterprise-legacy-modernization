package com.example.legacy;
import com.example.legacy.util.*;
public final class FrameworkFreeTests {
    private static void check(boolean good,String message){if(!good)throw new AssertionError(message);}
    public static void main(String[]args) {
        check(Workflow.allowed("NEW","TRIAGED"),"triage");
        check(!Workflow.allowed("NEW","CLOSED"),"no skipped steps");
        check(Workflow.mayTransition("SUPERVISOR","PENDING_APPROVAL","APPROVED"),"approval");
        check(!Workflow.mayTransition("WORKER","PENDING_APPROVAL","APPROVED"),"worker cannot approve");
        check(Workflow.mayTransition("WORKER","IN_PROGRESS","PENDING_APPROVAL"),"worker requests approval");
        String hash=Passwords.hash("SomeUniquePassword!2026");
        check(Passwords.matches("SomeUniquePassword!2026",hash),"password hash");
        check(!Passwords.matches("wrong",hash),"wrong password");
        check(!Strings.csv("=SUM(1,1)").startsWith("\"="),"CSV formula safety");
        check(!Strings.filename("../../etc/passwd").contains("/"),"filename normalization");
        System.out.println("PASS: workflow transitions, authorization, hashing and CSV/filename safeguards");
    }
}
