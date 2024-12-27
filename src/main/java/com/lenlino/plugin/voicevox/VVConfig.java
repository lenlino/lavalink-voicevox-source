package com.lenlino.plugin.voicevox;

public class VVConfig {
    public String text;
    public String speaker;
    public String address;
    public String queryAddress;

    public VVConfig(String text, String speaker, String address, String queryAddress) {
        this.text = text;
        this.speaker = speaker;
        this.address = address;
        this.queryAddress = queryAddress;
    }
}
