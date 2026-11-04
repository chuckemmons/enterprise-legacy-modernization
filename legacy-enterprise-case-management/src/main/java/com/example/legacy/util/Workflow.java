package com.example.legacy.util;
import java.util.*;
public final class Workflow {
    public static final String[] STATUS = {"NEW", "TRIAGED", "IN_PROGRESS", "PENDING_APPROVAL", "APPROVED", "REJECTED", "CLOSED"};
    public static final String[] ROLES = {"ADMIN", "SUPERVISOR", "WORKER", "AUDITOR"};
    private static final Map<String, Set<String>> NEXT = new HashMap<String, Set<String>>();
    static {
        allow("NEW", "TRIAGED"); allow("TRIAGED", "IN_PROGRESS");
        allow("IN_PROGRESS", "PENDING_APPROVAL");
        allow("PENDING_APPROVAL", "APPROVED", "REJECTED");
        allow("REJECTED", "IN_PROGRESS"); allow("APPROVED", "CLOSED");
    }
    private Workflow() {}
    private static void allow(String from, String... to) { NEXT.put(from, new LinkedHashSet<String>(Arrays.asList(to))); }
    public static boolean allowed(String from, String to) {
        return NEXT.containsKey(from) && NEXT.get(from).contains(to);
    }
    public static boolean mayTransition(String role, String from, String to) {
        if (!allowed(from, to)) return false;
        if ("ADMIN".equals(role) || "SUPERVISOR".equals(role)) return true;
        return "WORKER".equals(role) && (("TRIAGED".equals(from) && "IN_PROGRESS".equals(to)) ||
               ("IN_PROGRESS".equals(from) && "PENDING_APPROVAL".equals(to)) ||
               ("REJECTED".equals(from) && "IN_PROGRESS".equals(to)));
    }
    public static boolean canWork(String role) { return "ADMIN".equals(role) || "SUPERVISOR".equals(role) || "WORKER".equals(role); }
    public static boolean isSupervisor(String role) { return "ADMIN".equals(role) || "SUPERVISOR".equals(role); }
    public static Set<String> next(String status, String role) {
        Set<String> results = new LinkedHashSet<String>();
        if (NEXT.containsKey(status)) for (String x : NEXT.get(status)) if (mayTransition(role, status, x)) results.add(x);
        return results;
    }
}
