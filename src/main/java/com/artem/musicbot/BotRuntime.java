package com.artem.musicbot;

import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.function.Consumer;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.audio.factory.DefaultSendFactory;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class BotRuntime {
    private JDA jda;
    private LocalDashboardServer dashboardServer;

    public synchronized boolean isRunning() {
        return jda != null;
    }

    public synchronized void start(Path configPath, boolean waitUntilReady, Consumer<String> logger) throws Exception {
        if (jda != null) {
            throw new IllegalStateException("Bot is already running.");
        }

        BotConfig config = BotConfig.load(configPath);
        I18n i18n = new I18n(config.languageCode());
        GuildSettingsStore settingsStore = new GuildSettingsStore(Path.of("guild-settings.db"), config.prefix(), config.languageCode());

        AudioModuleConfig audioModuleConfig = new AudioModuleConfig()
            .withDaveSessionFactory(new JDaveSessionFactory())
            .withAudioSendFactory(new DefaultSendFactory());

        MusicController musicController = new MusicController(config, i18n, settingsStore);

        JDA built = JDABuilder.createDefault(config.token(), EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.MESSAGE_CONTENT))
            .disableCache(
                CacheFlag.EMOJI,
                CacheFlag.STICKER,
                CacheFlag.SCHEDULED_EVENTS)
            .enableCache(CacheFlag.VOICE_STATE)
            .setMemberCachePolicy(MemberCachePolicy.VOICE)
            .setActivity(Activity.playing(i18n.t("status.waiting")))
            .setStatus(OnlineStatus.ONLINE)
            .setAudioModuleConfig(audioModuleConfig)
                .addEventListeners(new CommandListener(
                    config.prefix(),
                    musicController,
                    i18n,
                    settingsStore,
                    config.dashboardEnabled(),
                    config.dashboardPort()))
            .build();

        if (waitUntilReady) {
            built.awaitReady();
        }

        jda = built;

        if (config.dashboardEnabled()) {
            dashboardServer = new LocalDashboardServer(
                    config.dashboardPort(),
                    musicController::metricsSnapshot,
                    musicController::healthSummary,
                    () -> this.jda == null ? "stopped" : this.jda.getStatus().name(),
                    Instant.now()
            );
            dashboardServer.start();
            logger.accept("Dashboard started at " + dashboardServer.baseUrl());
        }

        logger.accept("Bot started successfully.");
    }

    public synchronized void stop(Consumer<String> logger) {
        if (jda == null) {
            logger.accept("Bot is not running.");
            return;
        }

        jda.shutdownNow();
        jda = null;

        if (dashboardServer != null) {
            dashboardServer.stop();
            dashboardServer = null;
        }

        logger.accept("Bot stopped.");
    }
}
