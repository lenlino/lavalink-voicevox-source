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
import java.util.Arrays;
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
            throw new FriendlyException("Invalid URI", FriendlyException.Severity.COMMON, null);
        }

        final byte[] audio = getAudio(config);

        if (audio == null) {
            throw new FriendlyException("Could not generate audio NULL " + config.address + " " + config.text + " " + config.speaker, FriendlyException.Severity.COMMON, null);
        }

        if (!isWavFormat(audio)) {
            throw new FriendlyException("Invalid audio format " + config.address + " " + config.text + " " + config.speaker
                + " " + Arrays.toString(audio), FriendlyException.Severity.COMMON, null);
        }
        // use NonSeekableInputStream + ByteBufferInputStream/ByteArrayInputStream?
        // make a custom impl of SeekableInputStream with ByteArrayInputStream?
        // audio track: OggAudioTrack

        try (NonSeekableInputStream stream = new NonSeekableInputStream(new ByteArrayInputStream(audio))) {
            processDelegate(new WavAudioTrack(this.trackInfo, stream), executor);
        }
    }

    private boolean isWavFormat(byte[] audioData) {
        if (audioData.length < 12) {
            return false; // サイズ不足
        }
        // RIFFヘッダーかどうかを確認
        return audioData[0] == 'R' && audioData[1] == 'I' && audioData[2] == 'F' && audioData[3] == 'F'
            && audioData[8] == 'W' && audioData[9] == 'A' && audioData[10] == 'V' && audioData[11] == 'E';
    }

    @Nullable
    private byte[] getAudio(VVConfig config) throws UnsupportedEncodingException {
        HttpPost reqGet = new HttpPost("http://" + config.queryAddress + "/audio_query?text=" +
            URLEncoder.encode(config.text, StandardCharsets.UTF_8) + "&speaker=" + config.speaker);
        String resultJson = null;
        try (final CloseableHttpResponse response = this.sourceManager.getHttpInterface().execute(reqGet)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                return failedGetAudio(config);
            }

            if (response.getHeaders("Content-Type").length != 0 &&
                Arrays.asList(response.getHeaders("Content-Type")).get(0).getValue().equals("audio/wav")) {
                return response.getEntity().getContent().readAllBytes();
            }

            resultJson = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            byte[] audio = failedGetAudio(config);
            if (audio != null) {
                return audio;
            }
            throw new FriendlyException("Could not generate query"+config.queryAddress+ " " + config.speaker + " " + config.text, FriendlyException.Severity.COMMON, e);
        }

        if (resultJson == null) {
            return failedGetAudio(config);
        }

        HttpPost req = new HttpPost("http://"+ config.address + "/synthesis?speaker=" + config.speaker);

        req.setEntity(new StringEntity(resultJson, ContentType.APPLICATION_JSON));

        try (final CloseableHttpResponse response = this.sourceManager.getHttpInterface().execute(req)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                return failedGetAudio(config);
            }
            return response.getEntity().getContent().readAllBytes();
        } catch (IOException e) {
            byte[] audio = failedGetAudio(config);
            if (audio != null) {
                return audio;
            }
            throw new FriendlyException("Could not generate synthesis "+config.address+ " " + config.speaker + " " + config.text, FriendlyException.Severity.COMMON, e);
        }
    }

    public byte[] failedGetAudio(VVConfig config) throws UnsupportedEncodingException {
        if (config.retry > 0) {
            return getAudio(new VVConfig(config.text, config.speaker, config.address, config.queryAddress, config.retry - 1));
        } else if (this.sourceManager.backupIp != null && !this.sourceManager.backupIp.equals(config.address)) {
            return getAudio(new VVConfig(config.text, "3", this.sourceManager.backupIp, config.queryAddress, config.retry));
        }
        return null;
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
