package com.lenlino.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "plugins.vvsource.voicevox")
public class LavalinkVVConfig {
    private String backupIp = null;

    public String getBackupIp() {
        return backupIp;
    }

    public void setBackupIp(String backupIp) {
        this.backupIp = backupIp;
    }
}
