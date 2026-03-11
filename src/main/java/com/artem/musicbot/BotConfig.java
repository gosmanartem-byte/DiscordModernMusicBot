package com.artem.musicbot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public record BotConfig(
    String token,
    String prefix,
    String youtubePoToken,
    String youtubeVisitorData,
    String languageCode,
    boolean dashboardEnabled,
    int dashboardPort
) {
    public static BotConfig load(Path path) throws IOException {
        if (looksLikeLegacyConfig(path)) {
            return loadLegacy(path);
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        }

        String token = required(properties, "bot.token");
        String prefix = properties.getProperty("bot.prefix", "!").trim();
        String youtubePoToken = properties.getProperty("youtube.poToken", "").trim();
        String youtubeVisitorData = properties.getProperty("youtube.visitorData", "").trim();
        String languageCode = properties.getProperty("bot.language", "en").trim();
        boolean dashboardEnabled = Boolean.parseBoolean(properties.getProperty("bot.dashboard.enabled", "true").trim());
        int dashboardPort = parsePort(properties.getProperty("bot.dashboard.port", "8090").trim());
        validate(prefix, languageCode, dashboardPort);
        return new BotConfig(token, prefix, youtubePoToken, youtubeVisitorData, languageCode, dashboardEnabled, dashboardPort);
    }

    private static boolean looksLikeLegacyConfig(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.equals("config.txt") || name.endsWith(".txt");
    }

    private static BotConfig loadLegacy(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Properties properties = new Properties();
        for (String line : lines) {
            String trimmed = stripInlineComment(line).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int separatorIndex = trimmed.indexOf('=');
            if (separatorIndex < 0) {
                continue;
            }

            String key = trimmed.substring(0, separatorIndex).trim();
            String value = trimmed.substring(separatorIndex + 1).trim();
            properties.setProperty(key, value);
        }

        String token = required(properties, "token");
        String prefix = properties.getProperty("prefix", "!").trim();
        if (prefix.isEmpty()) {
            prefix = "!";
        }

        String languageCode = properties.getProperty("language", "en").trim();
        boolean dashboardEnabled = Boolean.parseBoolean(properties.getProperty("dashboardEnabled", "true").trim());
        int dashboardPort = parsePort(properties.getProperty("dashboardPort", "8090").trim());
        validate(prefix, languageCode, dashboardPort);
        return new BotConfig(token, prefix, "", "", languageCode, dashboardEnabled, dashboardPort);
    }

    private static String stripInlineComment(String value) {
        int commentIndex = value.indexOf("//");
        if (commentIndex >= 0) {
            return value.substring(0, commentIndex);
        }

        return value;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }

    private static void validate(String prefix, String languageCode, int dashboardPort) {
        if (prefix.isBlank() || prefix.length() > 3) {
            throw new IllegalStateException("bot.prefix must be 1-3 non-space characters.");
        }

        if (dashboardPort < 1 || dashboardPort > 65535) {
            throw new IllegalStateException("bot.dashboard.port must be between 1 and 65535.");
        }

        I18n.Language.from(languageCode);
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid bot.dashboard.port: " + value);
        }
    }
}
