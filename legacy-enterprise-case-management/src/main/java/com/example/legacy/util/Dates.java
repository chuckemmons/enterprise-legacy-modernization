package com.example.legacy.util;
import java.time.*;
import java.time.format.*;
import java.util.Date;
public final class Dates {
    private Dates() {}
    public static Date dueDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try { return Date.from(LocalDate.parse(text).atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Due date must be YYYY-MM-DD"); }
    }
    public static String dateInput(Date value) {
        if (value == null) return "";
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }
}
