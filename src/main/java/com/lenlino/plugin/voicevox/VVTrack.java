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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    private byte[] getAudio(VVConfig config) {

        HttpPost req = new HttpPost("http://"+ config.address + "/synthesis?speaker=" + config.speaker);

        req.setEntity(new StringEntity(config.json, ContentType.APPLICATION_JSON));

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
