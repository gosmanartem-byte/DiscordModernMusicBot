package com.artem.musicbot;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;

import net.dv8tion.jda.api.audio.AudioSendHandler;

public class AudioPlayerSendHandler implements AudioSendHandler {
    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer;
    private final MutableAudioFrame mutableFrame;
    private final AtomicLong providedFrames = new AtomicLong();
    private final AtomicLong provideCalls = new AtomicLong();
    private volatile long lastFrameNanos = 0L;
    private volatile String lastCodecName = "n/a";
    private volatile int lastDataLength = 0;

    public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.buffer = ByteBuffer.allocate(2048);
        this.mutableFrame = new MutableAudioFrame();
        this.mutableFrame.setBuffer(buffer);
    }

    @Override
    public boolean canProvide() {
        provideCalls.incrementAndGet();
        ((Buffer) buffer).clear();
        boolean provided = audioPlayer.provide(mutableFrame);
        if (!provided) {
            return false;
        }

        ((Buffer) buffer).flip();
        providedFrames.incrementAndGet();
        lastFrameNanos = System.nanoTime();
        lastCodecName = mutableFrame.getFormat() == null ? "n/a" : mutableFrame.getFormat().codecName();
        lastDataLength = buffer.remaining();
        return true;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return buffer;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    public long getProvidedFrames() {
        return providedFrames.get();
    }

    public long getProvideCalls() {
        return provideCalls.get();
    }

    public long getMillisSinceLastFrame() {
        long timestamp = lastFrameNanos;
        if (timestamp == 0L) {
            return -1L;
        }
        return (System.nanoTime() - timestamp) / 1_000_000L;
    }

    public String getLastCodecName() {
        return lastCodecName;
    }

    public int getLastDataLength() {
        return lastDataLength;
    }
}
