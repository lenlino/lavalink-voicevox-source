# lavalink-voicevox-plugin

VOICEVOX用に作成したLavalinkプラグインです。

## 使用方法

### VOICEVOXから生成

identityに以下のように入力し、送信してください。

```
vv://voicevox?text=<入力>&speaker=<ボイスid>&address=<VOICEVOXのアドレス>&query-address=<audio_queryを行うアドレス>
```
・query-addressの指定がない場合はaddressが利用されます。

### wavファイルを直接送る

identityにbyteを16進数変換したものを送信してください。

この機能を利用する場合はwavファイルの最大サイズに合わせて以下の設定をapplication.ymlに追加してください。

```yml
server:
  max-http-request-header-size: 20MB
```

