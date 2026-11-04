package com.example.legacy.util;
import java.util.Locale;
public final class Strings {
    private Strings() {}
    public static String clean(String s, int max) {
        if (s == null) return "";
        String x = s.trim().replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        if (x.length() > max) throw new IllegalArgumentException("Field exceeds " + max + " characters");
        return x;
    }
    public static String required(String s, int max, String label) {
        String v = clean(s, max);
        if (v.length() == 0) throw new IllegalArgumentException(label + " is required");
        return v;
    }
    public static String choice(String s, String... choices) {
        for (String v : choices) if (v.equals(s)) return s;
        throw new IllegalArgumentException("Invalid selection");
    }
    public static String filename(String s) {
        String value = s == null ? "document" : s.replace('\\', '/');
        int i = value.lastIndexOf('/');
        if (i >= 0) value = value.substring(i + 1);
        value = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (value.length() > 120) value = value.substring(value.length() - 120);
        return value.length() == 0 ? "document" : value;
    }
    public static String csv(String x) {
        String value = x == null ? "" : x.replace("\r", " ").replace("\n", " ");
        if (value.matches("^[=+@\\-\\t].*")) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    public static String lower(String x) { return x == null ? "" : x.toLowerCase(Locale.ROOT); }
}
