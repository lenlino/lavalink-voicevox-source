package com.lenlino.plugin;

import com.lenlino.plugin.voicevox.VVSourceManager;
import com.lenlino.plugin.wav.WavSourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.arbjerg.lavalink.api.AudioPlayerManagerConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VVSourcePlugin implements AudioPlayerManagerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VVSourcePlugin.class);

    public VVSourcePlugin() {
        log.info("Hello, world!");
    }


    @NotNull
    @Override
    public AudioPlayerManager configure(AudioPlayerManager manager) {
        log.info("Registring source manager");
        manager.registerSourceManager(new VVSourceManager());
        manager.registerSourceManager(new WavSourceManager());

        return manager;
    }
}
