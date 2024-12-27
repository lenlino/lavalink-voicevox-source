package com.lenlino.plugin.wav;

import com.sedmelluq.discord.lavaplayer.container.wav.WavAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.NonSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import io.micrometer.common.lang.Nullable;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class WavTrack extends DelegatedAudioTrack {
    private final WavSourceManager sourceManager;

    public WavTrack(AudioTrackInfo trackInfo, WavSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        UUID uuid = UUID.fromString(this.trackInfo.identifier);

        byte[] bytes = this.sourceManager.popWavBytes(uuid);
        if (bytes == null) {
            return;
        }

        try (NonSeekableInputStream stream = new NonSeekableInputStream(new ByteArrayInputStream(bytes))) {
            processDelegate(new WavAudioTrack(this.trackInfo, stream), executor);
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new WavTrack(this.trackInfo, this.sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return this.sourceManager;
    }
}
