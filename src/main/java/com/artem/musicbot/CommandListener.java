package com.artem.musicbot;

import java.util.Locale;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandListener extends ListenerAdapter {
    private final String defaultPrefix;
    private final MusicController musicController;
    private final I18n i18n;
    private final GuildSettingsStore settingsStore;

    public CommandListener(String defaultPrefix, MusicController musicController, I18n i18n, GuildSettingsStore settingsStore) {
        this.defaultPrefix = defaultPrefix;
        this.musicController = musicController;
        this.i18n = i18n;
        this.settingsStore = settingsStore;
    }

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().updateCommands()
                .addCommands(
                        Commands.slash("play", "Play from URL or search")
                                .addOption(OptionType.STRING, "query", "URL or search query", true),
                        Commands.slash("skip", "Skip current track"),
                        Commands.slash("pause", "Pause playback"),
                        Commands.slash("resume", "Resume playback"),
                        Commands.slash("stop", "Stop and disconnect"),
                        Commands.slash("queue", "Show queue"),
                        Commands.slash("player", "Show interactive player panel"),
                        Commands.slash("volume", "Set or show volume")
                                .addOption(OptionType.INTEGER, "value", "0-200", false),
                        Commands.slash("bass", "Set or show bass")
                                .addOption(OptionType.INTEGER, "value", "0-5", false),
                        Commands.slash("remove", "Remove a queue item by index")
                                .addOption(OptionType.INTEGER, "index", "Queue position (starting at 1)", true),
                        Commands.slash("shuffle", "Shuffle queue"),
                        Commands.slash("clear", "Clear queue"),
                        Commands.slash("loop", "Set loop mode")
                                .addOption(OptionType.STRING, "mode", "off|track|queue", true),
                        Commands.slash("seek", "Seek current track in seconds")
                                .addOption(OptionType.INTEGER, "seconds", "Position in seconds", true),
                        Commands.slash("autoplay", "Enable/disable autoplay")
                                .addOption(OptionType.BOOLEAN, "enabled", "true or false", true),
                        Commands.slash("settings", "Show guild settings"),
                        Commands.slash("setprefix", "Set guild text-command prefix")
                                .addOption(OptionType.STRING, "prefix", "New prefix", true),
                        Commands.slash("setlang", "Set guild language for player/status")
                                .addOption(OptionType.STRING, "language", "Language code or name", true),
                        Commands.slash("setdj", "Set DJ role (admins always allowed)")
                                .addOption(OptionType.ROLE, "role", "Role to require for DJ commands", true),
                        Commands.slash("health", "Show runtime health summary")
                )
                .queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) {
            return;
        }

        if (!(event.getChannel() instanceof TextChannel channel)) {
            return;
        }

        GuildSettings settings = settingsStore.get(event.getGuild().getIdLong());
        String prefix = settings.prefix();
        String content = event.getMessage().getContentRaw().trim();
        if (!content.startsWith(prefix)) {
            return;
        }

        String withoutPrefix = content.substring(prefix.length()).trim();
        if (withoutPrefix.isEmpty()) {
            return;
        }

        String[] parts = withoutPrefix.split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].trim() : "";

        switch (command) {
            case "play" -> {
                if (argument.isEmpty()) {
                    channel.sendMessage("Usage: " + prefix + "play <url or search>").queue();
                } else {
                    musicController.loadAndPlay(channel, event.getMember(), argument);
                }
            }
            case "skip" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.skip(channel));
            case "pause" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.pause(channel));
            case "resume" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.resume(channel));
            case "stop", "leave", "disconnect", "dc" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.stop(channel));
            case "volume", "vol" -> {
                if (argument.isBlank()) {
                    musicController.showVolume(channel);
                } else {
                    Integer value = parseInt(argument);
                    if (value == null) {
                        channel.sendMessage("Usage: " + prefix + "volume <0-200>").queue();
                    } else {
                        doDjChecked(channel, event.getMember(), settings, () -> musicController.setVolume(channel, value));
                    }
                }
            }
            case "bass" -> {
                if (argument.isBlank()) {
                    musicController.showBass(channel);
                } else {
                    Integer value = parseInt(argument);
                    if (value == null) {
                        channel.sendMessage("Usage: " + prefix + "bass <0-5>").queue();
                    } else {
                        doDjChecked(channel, event.getMember(), settings, () -> musicController.setBass(channel, value));
                    }
                }
            }
            case "loud" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.setLoudPreset(channel, prefix));
            case "normal", "reset" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.resetNormal(channel));
            case "queue" -> musicController.queue(channel);
            case "remove" -> {
                Integer value = parseInt(argument);
                if (value == null) {
                    channel.sendMessage("Usage: " + prefix + "remove <index>").queue();
                } else {
                    doDjChecked(channel, event.getMember(), settings, () -> musicController.remove(channel, value));
                }
            }
            case "shuffle" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.shuffleQueue(channel));
            case "clear" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.clearQueue(channel));
            case "loop" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.setLoop(channel, argument));
            case "seek" -> {
                Integer value = parseInt(argument);
                if (value == null) {
                    channel.sendMessage("Usage: " + prefix + "seek <seconds>").queue();
                } else {
                    doDjChecked(channel, event.getMember(), settings, () -> musicController.seek(channel, value.longValue()));
                }
            }
            case "autoplay" -> {
                if (!"on".equalsIgnoreCase(argument) && !"off".equalsIgnoreCase(argument)) {
                    channel.sendMessage("Usage: " + prefix + "autoplay <on|off>").queue();
                } else {
                    doDjChecked(channel, event.getMember(), settings, () -> musicController.setAutoplay(channel, "on".equalsIgnoreCase(argument)));
                }
            }
            case "player", "panel" -> musicController.sendPlayerPanel(channel, prefix);
            case "setprefix" -> doAdminChecked(channel, event.getMember(), () -> setPrefix(channel, argument, settings));
            case "setlang" -> doAdminChecked(channel, event.getMember(), () -> setLanguage(channel, argument, settings));
            case "setdj" -> channel.sendMessage("Use slash command /setdj to select a role.").queue();
            case "settings" -> channel.sendMessage(formatSettings(settings)).queue();
            case "health" -> channel.sendMessage(musicController.healthSummary()).queue();
            case "help" -> channel.sendMessage(helpText(prefix)).queue();
            default -> {
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || !(event.getChannel() instanceof TextChannel channel)) {
            return;
        }

        GuildSettings settings = settingsStore.get(event.getGuild().getIdLong());
        Member member = event.getMember();
        String prefix = settings.prefix();

        switch (event.getName()) {
            case "play" -> musicController.loadAndPlay(channel, member, event.getOption("query", "", OptionMapping::getAsString));
            case "skip" -> runDjSlash(event, member, settings, () -> musicController.skip(channel));
            case "pause" -> runDjSlash(event, member, settings, () -> musicController.pause(channel));
            case "resume" -> runDjSlash(event, member, settings, () -> musicController.resume(channel));
            case "stop" -> runDjSlash(event, member, settings, () -> musicController.stop(channel));
            case "queue" -> musicController.queue(channel);
            case "player" -> musicController.sendPlayerPanel(channel, prefix);
            case "volume" -> {
                OptionMapping value = event.getOption("value");
                if (value == null) {
                    musicController.showVolume(channel);
                } else {
                    runDjSlash(event, member, settings, () -> musicController.setVolume(channel, value.getAsInt()));
                }
            }
            case "bass" -> {
                OptionMapping value = event.getOption("value");
                if (value == null) {
                    musicController.showBass(channel);
                } else {
                    runDjSlash(event, member, settings, () -> musicController.setBass(channel, value.getAsInt()));
                }
            }
            case "remove" -> runDjSlash(event, member, settings, () -> musicController.remove(channel, event.getOption("index", 1, OptionMapping::getAsInt)));
            case "shuffle" -> runDjSlash(event, member, settings, () -> musicController.shuffleQueue(channel));
            case "clear" -> runDjSlash(event, member, settings, () -> musicController.clearQueue(channel));
            case "loop" -> runDjSlash(event, member, settings, () -> musicController.setLoop(channel, event.getOption("mode", "off", OptionMapping::getAsString)));
            case "seek" -> runDjSlash(event, member, settings, () -> musicController.seek(channel, event.getOption("seconds", 0L, OptionMapping::getAsLong)));
            case "autoplay" -> runDjSlash(event, member, settings, () -> musicController.setAutoplay(channel, event.getOption("enabled", false, OptionMapping::getAsBoolean)));
            case "settings" -> channel.sendMessage(formatSettings(settings)).queue();
            case "setprefix" -> runAdminSlash(event, member, () -> setPrefix(channel, event.getOption("prefix", defaultPrefix, OptionMapping::getAsString), settings));
            case "setlang" -> runAdminSlash(event, member, () -> setLanguage(channel, event.getOption("language", settings.language(), OptionMapping::getAsString), settings));
            case "setdj" -> runAdminSlash(event, member, () -> {
                Role role = event.getOption("role", null, OptionMapping::getAsRole);
                if (role == null) {
                    channel.sendMessage("Role is required.").queue();
                    return;
                }
                GuildSettings next = new GuildSettings(settings.guildId(), settings.prefix(), settings.language(), role.getIdLong(), settings.defaultVolume(), settings.autoplay());
                settingsStore.upsert(next);
                channel.sendMessage("DJ role set to @" + role.getName()).queue();
            });
            case "health" -> channel.sendMessage(musicController.healthSummary()).queue();
            default -> {
            }
        }

        event.reply("Done.").setEphemeral(true).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.isFromGuild() || !event.getComponentId().startsWith("player:")) {
            return;
        }

        if (!(event.getChannel() instanceof TextChannel channel)) {
            event.deferEdit().queue();
            return;
        }

        GuildSettings settings = settingsStore.get(event.getGuild().getIdLong());
        switch (event.getComponentId()) {
            case "player:pause" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.pause(channel));
            case "player:resume" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.resume(channel));
            case "player:skip" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.skip(channel));
            case "player:stop" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.stop(channel));
            case "player:queue" -> musicController.queue(channel);
            case "player:volup" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.adjustVolume(channel, 10));
            case "player:voldown" -> doDjChecked(channel, event.getMember(), settings, () -> musicController.adjustVolume(channel, -10));
            case "player:refresh" -> {
            }
            default -> {
            }
        }

        event.deferEdit().queue(ignored -> musicController.refreshPlayerPanel(event.getMessage(), settings.prefix()));
    }

    private void setPrefix(TextChannel channel, String newPrefix, GuildSettings current) {
        String trimmed = newPrefix == null ? "" : newPrefix.trim();
        if (trimmed.isEmpty() || trimmed.length() > 3) {
            channel.sendMessage("Prefix must be 1-3 characters.").queue();
            return;
        }

        GuildSettings next = new GuildSettings(current.guildId(), trimmed, current.language(), current.djRoleId(), current.defaultVolume(), current.autoplay());
        settingsStore.upsert(next);
        channel.sendMessage("Prefix updated to `" + trimmed + "`").queue();
    }

    private void setLanguage(TextChannel channel, String languageInput, GuildSettings current) {
        String normalized = I18n.Language.from(languageInput).code();
        GuildSettings next = new GuildSettings(current.guildId(), current.prefix(), normalized, current.djRoleId(), current.defaultVolume(), current.autoplay());
        settingsStore.upsert(next);
        channel.sendMessage("Language updated to `" + normalized + "`").queue();
    }

    private boolean hasDjPermission(Member member, GuildSettings settings) {
        if (member == null) {
            return false;
        }

        if (member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }

        long djRoleId = settings.djRoleId();
        if (djRoleId == 0L) {
            return true;
        }

        return member.getRoles().stream().anyMatch(role -> role.getIdLong() == djRoleId);
    }

    private void doDjChecked(TextChannel channel, Member member, GuildSettings settings, Runnable action) {
        if (!hasDjPermission(member, settings)) {
            channel.sendMessage("You need the DJ role (or admin rights) for this command.").queue();
            return;
        }
        action.run();
    }

    private void runDjSlash(SlashCommandInteractionEvent event, Member member, GuildSettings settings, Runnable action) {
        if (!hasDjPermission(member, settings)) {
            return;
        }
        action.run();
    }

    private void doAdminChecked(TextChannel channel, Member member, Runnable action) {
        if (member == null || !(member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR))) {
            channel.sendMessage("Admin permission required.").queue();
            return;
        }
        action.run();
    }

    private void runAdminSlash(SlashCommandInteractionEvent event, Member member, Runnable action) {
        if (member == null || !(member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR))) {
            return;
        }
        action.run();
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String formatSettings(GuildSettings settings) {
        return String.join("\n",
                "Settings:",
                "prefix=" + settings.prefix(),
                "language=" + settings.language(),
                "djRoleId=" + settings.djRoleId(),
                "defaultVolume=" + settings.defaultVolume(),
                "autoplay=" + settings.autoplay());
    }

    private String helpText(String prefix) {
        return String.join("\n",
                "Commands:",
                prefix + "play <url or search>",
                prefix + "skip",
                prefix + "pause",
                prefix + "resume",
                prefix + "stop",
                prefix + "leave",
                prefix + "volume <0-200>",
                prefix + "bass <0-5>",
                prefix + "loud",
                prefix + "normal",
                prefix + "queue",
                prefix + "remove <index>",
                prefix + "shuffle",
                prefix + "clear",
                prefix + "loop <off|track|queue>",
                prefix + "seek <seconds>",
                prefix + "autoplay <on|off>",
                prefix + "setprefix <value>",
                prefix + "setlang <value>",
                prefix + "settings",
                prefix + "player",
                prefix + "health",
                prefix + "debugaudio",
                prefix + "help");
    }
}
