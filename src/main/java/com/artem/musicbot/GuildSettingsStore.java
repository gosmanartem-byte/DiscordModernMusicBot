package com.artem.musicbot;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GuildSettingsStore {
    private final String jdbcUrl;
    private final String defaultPrefix;
    private final String defaultLanguage;

    public GuildSettingsStore(Path dbPath, String defaultPrefix, String defaultLanguage) {
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        this.defaultPrefix = defaultPrefix;
        this.defaultLanguage = defaultLanguage;
        initSchema();
    }

    public GuildSettings get(long guildId) {
        String sql = "SELECT guild_id, prefix, language, dj_role_id, default_volume, autoplay FROM guild_settings WHERE guild_id = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, guildId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new GuildSettings(
                            rs.getLong("guild_id"),
                            rs.getString("prefix"),
                            rs.getString("language"),
                            rs.getLong("dj_role_id"),
                            rs.getInt("default_volume"),
                            rs.getInt("autoplay") == 1
                    );
                }
            }
        } catch (SQLException ignored) {
        }

        GuildSettings defaults = GuildSettings.defaults(guildId, defaultPrefix, defaultLanguage);
        upsert(defaults);
        return defaults;
    }

    public void upsert(GuildSettings settings) {
        String sql = "INSERT INTO guild_settings (guild_id, prefix, language, dj_role_id, default_volume, autoplay) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(guild_id) DO UPDATE SET prefix=excluded.prefix, language=excluded.language, dj_role_id=excluded.dj_role_id, default_volume=excluded.default_volume, autoplay=excluded.autoplay";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, settings.guildId());
            statement.setString(2, settings.prefix());
            statement.setString(3, settings.language());
            statement.setLong(4, settings.djRoleId());
            statement.setInt(5, settings.defaultVolume());
            statement.setInt(6, settings.autoplay() ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private void initSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS guild_settings (" +
                "guild_id INTEGER PRIMARY KEY, " +
                "prefix TEXT NOT NULL, " +
                "language TEXT NOT NULL, " +
                "dj_role_id INTEGER NOT NULL DEFAULT 0, " +
                "default_volume INTEGER NOT NULL DEFAULT 100, " +
                "autoplay INTEGER NOT NULL DEFAULT 0" +
                ")";

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException ignored) {
        }
    }
}
