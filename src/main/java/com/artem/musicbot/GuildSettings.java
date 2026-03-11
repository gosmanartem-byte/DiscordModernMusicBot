package com.artem.musicbot;

public record GuildSettings(
        long guildId,
        String prefix,
        String language,
        long djRoleId,
        int defaultVolume,
        boolean autoplay
) {
    public static GuildSettings defaults(long guildId, String fallbackPrefix, String fallbackLanguage) {
        return new GuildSettings(guildId, fallbackPrefix, fallbackLanguage, 0L, 100, false);
    }
}
