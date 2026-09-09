package de.jkueck.monitor.backend.service;

public final class AlarmTitleParser {

    private static final String SEPARATOR = " - ";

    private AlarmTitleParser() {
    }

    public record TitleParts(String keyword, String description) {
    }

    public static TitleParts parse(String title) {
        if (title == null) {
            return new TitleParts(null, null);
        }
        int idx = title.indexOf(SEPARATOR);
        if (idx < 0) {
            return new TitleParts(title.trim(), null);
        }
        String keyword = title.substring(0, idx).trim();
        String description = title.substring(idx + SEPARATOR.length()).trim();
        return new TitleParts(keyword, description);
    }
}