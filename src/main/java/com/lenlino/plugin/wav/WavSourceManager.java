package com.lenlino.plugin.wav;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
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
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class WavSourceManager implements AudioSourceManager {
    private final Map<UUID, byte[]> wavConfigMap = new HashMap<>();

    @Override
    public String getSourceName() {
        return "wav";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager audioPlayerManager, AudioReference audioReference) {

        byte[] bytes = Base64.getUrlDecoder().decode(audioReference.identifier.replace("wav:", ""));

        UUID uuid = UUID.randomUUID();
        wavConfigMap.put(uuid, bytes);

        return new WavTrack(new AudioTrackInfo("wav",
            "wav", Units.CONTENT_LENGTH_UNKNOWN, uuid.toString(), false,
            "uri"), this);
    }

    // 16進数文字列をバイト配列に変換するヘルパーメソッド
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
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
        return new WavTrack(audioTrackInfo, this);
    }

    @Override
    public void shutdown() {

    }

    public byte[] popWavBytes(UUID uuid) {
        if (!this.wavConfigMap.containsKey(uuid)) {
            return null;
        }
        return this.wavConfigMap.remove(uuid);
    }
    // ...
}
