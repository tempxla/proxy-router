# proxy-router

## これは何？

### できること

- HTTP(S) 通信をトンネリングする。
- HTTPリクエストヘッダのURLパターンで接続先を振り分ける。

### 使い方

1. proxy-router.edn を作る。([edn](https://github.com/edn-format/edn))

#proxy-router/regex という正規表現を使用するためのカスタムリーダーを使用しています。

```
% cat ~/.config/proxy-router.edn 
```

```clojure
{:proxy-router.service/default-service
 {:port 3030}  ; proxy-routerが使用するポート

 :proxy-router.handler/default-handler
 {:routes
  {:route-table
   [{:url-pattern #proxy-router/regex "^[^\\.]+\\.5ch\\.net:443"
     :dest        :direct}  ; :directはurl宛に直接トンネリングする
    {:url-pattern #proxy-router/regex "^http://[^\\.]+\\.5ch\\.net/test/bbs\\.cgi.*"
     :dest        :direct}
    {:url-pattern #proxy-router/regex "^http://[^\\.]+\\.5ch\\.net/.*"
     :dest        {:host "localhost" :port 8085}   ; 特定のプロキシサーバーにトンネリングする
     ;;:dest        {:host nil :port 8085}  ; hostをnilにすると接続先を見に行かないで接続を切る(FINを返す)。デバッグとかで使う。
     }]
   :default-dest   ; :default-destは上記のroute-tableにマッチしないときの行き先
   :direct}}}
```

2. jarを起動する

```sh
java -jar proxy-router-0.1.0-SNAPSHOT-standalone.jar
```

### リリースノート

- 0.1.0 初版

### テスト環境

- OpenJDK Runtime Environment 18.9 (build 11.0.13+8)
- Linux + [Siki](https://sikiapp.net/) + [2chproxy.pl](https://github.com/yama-natuki/2chproxy.pl/)

### ビルド環境

- Leiningen 2.9.8 on Java 11.0.13 OpenJDK 64-Bit Server VM

## Developing

### Setup

When you first clone this repository, run:

```sh
lein duct setup
```

This will create files for local configuration, and prep your system
for the project.

### Environment

To begin developing, start with a REPL.

```sh
lein repl
```

Then load the development environment.

```clojure
user=> (dev)
:loaded
```

Run `go` to prep and initiate the system.

```clojure
dev=> (go)
:duct.server.http.jetty/starting-server {:port 3000}
:initiated
```

By default this creates a web server at <http://localhost:3000>.

When you make changes to your source files, use `reset` to reload any
modified files and reset the server.

```clojure
dev=> (reset)
:reloading (...)
:resumed
```

### Testing

Testing is fastest through the REPL, as you avoid environment startup
time.

```clojure
dev=> (test)
...
```

But you can also run tests through Leiningen.

```sh
lein test
```

## Licnese

Copyright © 2021 tempxla

Distributed under the MIT license.
