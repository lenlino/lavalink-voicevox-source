package com.lenlino.plugin.voicevox;

public class VVConfig {
    public String text;
    public String speaker;
    public String address;
    public String queryAddress;
    public int retry;

    public VVConfig(String text, String speaker, String address, String queryAddress) {
        this(text, speaker, address, queryAddress, 0);
    }

    public VVConfig(String text, String speaker, String address, String queryAddress, int retry) {
        this.text = text;
        this.speaker = speaker;
        this.address = address;
        this.queryAddress = queryAddress;
    }
}
