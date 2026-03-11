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
        String sql = "SELECT guild_id, prefix, language, dj_role_id, default_volume, autoplay, command_channel_id, blocked_role_id FROM guild_settings WHERE guild_id = ?";
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
                            rs.getInt("autoplay") == 1,
                            rs.getLong("command_channel_id"),
                            rs.getLong("blocked_role_id")
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
        String sql = "INSERT INTO guild_settings (guild_id, prefix, language, dj_role_id, default_volume, autoplay, command_channel_id, blocked_role_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT(guild_id) DO UPDATE SET prefix=excluded.prefix, language=excluded.language, dj_role_id=excluded.dj_role_id, default_volume=excluded.default_volume, autoplay=excluded.autoplay, command_channel_id=excluded.command_channel_id, blocked_role_id=excluded.blocked_role_id";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, settings.guildId());
            statement.setString(2, settings.prefix());
            statement.setString(3, settings.language());
            statement.setLong(4, settings.djRoleId());
            statement.setInt(5, settings.defaultVolume());
            statement.setInt(6, settings.autoplay() ? 1 : 0);
            statement.setLong(7, settings.commandChannelId());
            statement.setLong(8, settings.blockedRoleId());
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
                "autoplay INTEGER NOT NULL DEFAULT 1, " +
                "command_channel_id INTEGER NOT NULL DEFAULT 0, " +
                "blocked_role_id INTEGER NOT NULL DEFAULT 0" +
                ")";

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
            ensureColumnExists(connection, "command_channel_id", "INTEGER NOT NULL DEFAULT 0");
            ensureColumnExists(connection, "blocked_role_id", "INTEGER NOT NULL DEFAULT 0");
            runAutoplayDefaultMigration(connection);
        } catch (SQLException ignored) {
        }
    }

    private void runAutoplayDefaultMigration(Connection connection) throws SQLException {
        createMetaTable(connection);
        if (isMigrationApplied(connection, "autoplay_default_on_v1")) {
            return;
        }

        // One-time migration: align pre-existing guild rows with new autoplay default.
        try (PreparedStatement statement = connection.prepareStatement("UPDATE guild_settings SET autoplay = 1 WHERE autoplay = 0")) {
            statement.executeUpdate();
        }

        markMigrationApplied(connection, "autoplay_default_on_v1");
    }

    private void createMetaTable(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS app_meta (meta_key TEXT PRIMARY KEY, meta_value TEXT NOT NULL)")) {
            statement.execute();
        }
    }

    private boolean isMigrationApplied(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT meta_value FROM app_meta WHERE meta_key = ?")) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void markMigrationApplied(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO app_meta (meta_key, meta_value) VALUES (?, ?)")) {
            statement.setString(1, key);
            statement.setString(2, "1");
            statement.executeUpdate();
        }
    }

    private void ensureColumnExists(Connection connection, String columnName, String definition) throws SQLException {
        if (hasColumn(connection, columnName)) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "ALTER TABLE guild_settings ADD COLUMN " + columnName + " " + definition)) {
            statement.execute();
        }
    }

    private boolean hasColumn(Connection connection, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(guild_settings)");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }
}
