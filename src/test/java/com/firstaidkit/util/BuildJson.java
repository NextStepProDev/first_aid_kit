package com.firstaidkit.util;

public class BuildJson {
    public static String buildJson(String name, String form, Integer year, Integer month, String description) {
        return """
                {
                  "name": %s,
                  "form": %s,
                  "expirationYear": %s,
                  "expirationMonth": %s,
                  "description": %s
                }
                """.formatted(
                name == null ? "null" : "\"" + name + "\"",
                form == null ? "null" : "\"" + form + "\"",
                year == null ? "null" : year,
                month == null ? "null" : month,
                description == null ? "null" : "\"" + description + "\""
        );
    }
}
