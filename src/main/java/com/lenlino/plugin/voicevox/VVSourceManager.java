package com.lenlino.plugin.voicevox;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.micrometer.common.lang.Nullable;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VVSourceManager implements AudioSourceManager {

    private final HttpInterfaceManager httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();

    @Override
    public String getSourceName() {
        return "voicevox";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager audioPlayerManager, AudioReference audioReference) {
        final VVConfig config = this.parseURI(audioReference.identifier);

        if (config == null) {
            return null;
        }

        return new VVTrack(new AudioTrackInfo(config.speaker,
            "VOICEVOX", Units.CONTENT_LENGTH_UNKNOWN, audioReference.identifier, false,
            audioReference.identifier), this);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack audioTrack) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack audioTrack, DataOutput dataOutput) throws IOException {

    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo audioTrackInfo, DataInput dataInput) throws IOException {
        return new VVTrack(audioTrackInfo, this);
    }

    @Override
    public void shutdown() {

    }

    @Nullable
    public VVConfig parseURI(String uri) {
        if (uri == null || !uri.startsWith("vv://")) {
            return null;
        }

        try {
            final URIBuilder parsed = new URIBuilder(uri);
            final URI builtUri = parsed.build();
            final List<NameValuePair> queryParams = parsed.getQueryParams();
            final VVConfig config = new VVConfig(null, null, null);

            if (!queryParams.isEmpty()) {
                if (queryParams.stream().anyMatch((p) -> "json".equals(p.getName()))) {
                    final NameValuePair jsonConfig = queryParams.stream()
                        .filter(
                            (p) -> "json".equals(p.getName())
                        )
                        .findFirst()
                        .orElse(null);

                    config.json = jsonConfig.getValue();
                }

                // parse predefined query params
                if (queryParams.stream().anyMatch((p) -> "speaker".equals(p.getName()))) {
                    final NameValuePair jsonConfig = queryParams.stream()
                        .filter(
                            (p) -> "speaker".equals(p.getName())
                        )
                        .findFirst()
                        .orElse(null);

                    config.speaker = jsonConfig.getValue();

                }

                if (queryParams.stream().anyMatch((p) -> "address".equals(p.getName()))) {
                    final NameValuePair jsonConfig = queryParams.stream()
                        .filter(
                            (p) -> "address".equals(p.getName())
                        )
                        .findFirst()
                        .orElse(null);

                    config.address = jsonConfig.getValue();

                }
            }

            return config;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }

    public HttpInterface getHttpInterface() {
        return this.httpInterfaceManager.getInterface();
    }
    // ...
}
