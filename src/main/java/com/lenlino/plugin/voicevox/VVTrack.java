package com.lenlino.plugin.voicevox;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggAudioTrack;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class VVTrack extends DelegatedAudioTrack {
    private final VVSourceManager sourceManager;

    public VVTrack(AudioTrackInfo trackInfo, VVSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        final VVConfig config = this.sourceManager.parseURI(this.trackInfo.identifier);

        if (config == null) {
            return;
        }

        final byte[] audio = getAudio(config);

        if (audio == null) {
            return;
        }
        // use NonSeekableInputStream + ByteBufferInputStream/ByteArrayInputStream?
        // make a custom impl of SeekableInputStream with ByteArrayInputStream?
        // audio track: OggAudioTrack

        try (NonSeekableInputStream stream = new NonSeekableInputStream(new ByteArrayInputStream(audio))) {
            processDelegate(new WavAudioTrack(this.trackInfo, stream), executor);
        }
    }

    @Nullable
    private byte[] getAudio(VVConfig config) throws UnsupportedEncodingException {
        int timeout = 5000; // 5秒
        RequestConfig timeout_config = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .setSocketTimeout(timeout)
            .build();
        HttpPost reqGet = new HttpPost("http://" + config.queryAddress + "/audio_query?text=" +
            URLEncoder.encode(config.text, StandardCharsets.UTF_8) + "&speaker=" + config.speaker);
        reqGet.setConfig(timeout_config);
        String resultJson = null;
        try (final CloseableHttpResponse response = this.sourceManager.getHttpInterface().execute(reqGet)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }

            resultJson = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new FriendlyException("Could not generate audio", FriendlyException.Severity.COMMON, e);
        }

        if (resultJson == null) {
            return null;
        }

        HttpPost req = new HttpPost("http://"+ config.address + "/synthesis?speaker=" + config.speaker);
        req.setConfig(timeout_config);

        req.setEntity(new StringEntity(resultJson, ContentType.APPLICATION_JSON));

        try (final CloseableHttpResponse response = this.sourceManager.getHttpInterface().execute(req)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }

            return response.getEntity().getContent().readAllBytes();
        } catch (IOException e) {
            throw new FriendlyException("Could not generate audio", FriendlyException.Severity.COMMON, e);
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new VVTrack(this.trackInfo, this.sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return this.sourceManager;
    }
}
