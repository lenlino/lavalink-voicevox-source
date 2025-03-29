package com.lenlino.plugin.voicevox;

import com.lenlino.plugin.LavalinkVVConfig;
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// VVSourceManager.java
@Service
public class VVSourceManager implements AudioSourceManager {
    private final HttpInterfaceManager httpInterfaceManager;
    private static final Logger log = LoggerFactory.getLogger(VVSourceManager.class);
    public String backupIp = null;

    public VVSourceManager(LavalinkVVConfig config) {
        if (config.getBackupIp() != null) {
            log.info("Using backup ip: " + config.getBackupIp());
            backupIp = config.getBackupIp();
        }
        httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
        httpInterfaceManager.configureBuilder(builder -> {
            builder.setMaxConnPerRoute(5);
        });
        httpInterfaceManager.configureRequests(requestConfig -> {
            return RequestConfig.custom().setConnectTimeout(5000).setConnectionRequestTimeout(5000).setSocketTimeout(5000).setCookieSpec("ignoreCookies").build();
        });
    }

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
        try {
            this.httpInterfaceManager.close();
        } catch (IOException e) {
            log.error("Failed to close HTTP interface manager", e);
        }
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
            final VVConfig config = new VVConfig(null, null, null, null);

            if (!queryParams.isEmpty()) {
                if (queryParams.stream().anyMatch((p) -> "text".equals(p.getName()))) {
                    final NameValuePair jsonConfig = queryParams.stream()
                        .filter(
                            (p) -> "text".equals(p.getName())
                        )
                        .findFirst()
                        .orElse(null);
                    config.text = jsonConfig.getValue();
                }

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

                if (queryParams.stream().anyMatch((p) -> "query-address".equals(p.getName()))) {
                    final NameValuePair jsonConfig = queryParams.stream()
                        .filter(
                            (p) -> "query-address".equals(p.getName())
                        )
                        .findFirst()
                        .orElse(null);
                    config.queryAddress = jsonConfig == null ? config.address : jsonConfig.getValue();
                }

                if (queryParams.stream().anyMatch((p) -> "retry".equals(p.getName()))) {
                    final NameValuePair jsonConfig = queryParams.stream()
                        .filter(
                            (p) -> "retry".equals(p.getName())
                        )
                        .findFirst()
                        .orElse(null);
                    config.retry = Integer.parseInt(jsonConfig.getValue());
                }
            }

            return config;
        } catch (URISyntaxException e) {
            log.error("Failed to get voicevox server", e);
        }

        return null;
    }

    public HttpInterface getHttpInterface() {
        return this.httpInterfaceManager.getInterface();
    }
}
