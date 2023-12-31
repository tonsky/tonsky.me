---
title: "Grumpy chronicles: deps and uberdeps"
summary: "Building uberjar from deps.edn"
published: 2019-06-03
---

I have a small website called [Grumpy Website](https://grumpy.website/) for which I implemented my own engine in Clojure. The original development was [streamed on YouTube](https://www.youtube.com/playlist?list=PLdSfLyn35ej8por7aH-5wYvOyDTu-bPoH) (videos are in Russian).

Recently I figured it could be a nice playground for trying out new things in Clojure, especially related to web development. This series will cover my adventures in finding the One True Clojure Web Stack.

Please keep in mind that not all of these changes were absolutely necessary. Some of them are purely aesthetic, or just different for the sake of being different. Mainly I just wanted to play with new toys. The code I’ll be discussing lives at https://github.com/tonsky/grumpy.

# deps.edn

The first thing I wanted to do was migrating to [deps.edn](https://clojure.org/reference/deps_and_cli). The changes are in [this diff](https://github.com/tonsky/grumpy/commit/0b50ae3073d5dfdcd7bb79965a5142a53493049a).

That went pretty well. Dependency resolution and classpath building are working flawlessly. Time to REPL into an empty namespace reduced from 4 seconds to just 1.5 seconds (still a terribly long time though).

The difference with Lein was that `deps.edn` leaves you on your own. Where Lein can do most of the tasks you normally need (packaging, deploying to Clojars, release management, local and remote nREPL etc) deps can only do local REPL. For everything else you need to find a library, which are in a pretty raw state as far as I can see.

TL;DR I like the performance but miss the battle-tested batteries that come with lein.

# Uberjars

To deploy Grumpy I use uberjar — a single flat jar file that packages all dependencies’ content, including the transitive ones.

One of the goals for using Grumpy as a playground was to try to use other people’s solutions as much as possible and avoid rolling out my own thing.

So I started looking for existing solutions. Surprisingly, all of them failed in various ways.

A couple of tools (Juxt’s [pack](https://github.com/juxt/pack.alpha) and Michał Buczko’s [revolt](https://github.com/mbuczko/revolt)) offer to build a Capsule instead of uberjar. It might be a more powerful approach overall, but I wasn’t sure I wanted to bring another Java tool and deal with it just to run my program when uberjars work just fine. Revolt itself offers a whole new ecosystem of plugins and tasks which frankly was too much for me.

And then there’re tools that do traditional flat uberjars.

[Cambada](https://github.com/luchiniatwork/cambada) just died with ArtifactNotFoundException. Honestly, I haven’t really looked if I could make it work, expecting there are better tools around. People are using `deps.edn` somehow, right? They ought to.

[Depstar](https://github.com/healthfinch/depstar) died with IOException. This time I looked for a solution and found a fork of it [by Sean Corfield](https://github.com/seancorfield/depstar). It worked, but looking at the output I noticed a couple of odd things.

First, it seemed to use classpath of JVM it runs in instead of deducing classpath from the original `deps.edn`. This is really inconvenient since your build classpath usually includes stuff you don’t necessarily need in a resulting uberjar. The packaging script itself, for starters: how do you exclude that? Then depstar own files (they have a special case for it, but for it only).  Additionally, I have ClojureScript on _build_ classpath which is pretty huge to simply ignore. I didn’t want to spin up a separate JVM to build everything and then spin another one just to package. Given how slow Clojure is to start that was not a viable solution.

The second problem was that apparently, Depstar unpacks everything to a temp directory before packing it back into uberjar. This seemed like a huge performance bottleneck. Zip files are perfectly streamable, both on read and on write, so there seems to be no reason to unpack everything first when you can just pass bytes from one file to another on the fly.

# Meet Uberdeps

So I figured I had to build my own uberjar packager. It’s pretty simple: resolve dependencies at runtime using tools.deps, read them one-by-one, write to the resulting file. Zip/jar reading and writing are both very straightforward in raw Java, you don’t even need Clojure to do that. Though `clojure.java.io/copy` always comes in handy. And tools.deps was meant to be consumed as a library as much as a command-line tool, so relying on it seemed like a good idea too.

The project lives at [github.com/tonsky/uberdeps](https://github.com/tonsky/uberdeps/). Grumpy builder uses it as a library [here](https://github.com/tonsky/grumpy/blob/875d0bd15fbf643bc48e34b4a6be3d26e6040140/package/grumpy/package.clj). You can also see that is has a few additional handles for fine customization:

```
(binding [uberdeps/exclusions
          (into uberdeps/exclusions
            [#"\.DS_Store"
             #".*\.cljs"
             #"cljsjs/.*"
             #"META-INF/maven/cljsjs/.*"])
          uberdeps/level :debug]
  (uberdeps/package
    (edn/read-string (slurp "deps.edn"))
    "target/grumpy.jar"
    {:aliases #{:uberjar}})))
```

For example, why package cljs sources if we already have them compiled down to `main.js`?

Another thing that uberdeps does by default is printing the tree of dependencies. It looks like this:

```
[uberdeps] Packaging MyProject.uberdeps.jar...
+ cheshire/cheshire 5.8.1
.   com.fasterxml.jackson.core/jackson-core 2.9.6
.   com.fasterxml.jackson.dataformat/jackson-dataformat-cbor 2.9.6
! Duplicate entry "META-INF/services/com.fasterxml.jackson.core.JsonFactory" from "com.fasterxml.jackson.dataformat/jackson-dataformat-cbor 2.9.6" already seen in "com.fasterxml.jackson.core/jackson-core 2.9.6"
.   com.fasterxml.jackson.dataformat/jackson-dataformat-smile 2.9.6
! Duplicate entry "META-INF/services/com.fasterxml.jackson.core.JsonFactory" from "com.fasterxml.jackson.dataformat/jackson-dataformat-smile 2.9.6" already seen in "com.fasterxml.jackson.core/jackson-core 2.9.6"
.   tigris/tigris 0.1.1
+ clj-http/clj-http 3.9.1
.   commons-io/commons-io 2.6
.   org.apache.httpcomponents/httpasyncclient 4.1.3
.     org.apache.httpcomponents/httpcore-nio 4.4.6
.   org.apache.httpcomponents/httpclient 4.5.5
.     commons-logging/commons-logging 1.2
.   org.apache.httpcomponents/httpclient-cache 4.5.5
.   org.apache.httpcomponents/httpcore 4.4.9
.   org.apache.httpcomponents/httpmime 4.5.5
.   potemkin/potemkin 0.4.5
.     clj-tuple/clj-tuple 0.2.2
.     riddley/riddley 0.1.12
.   slingshot/slingshot 0.12.2
+ cljs-drag-n-drop/cljs-drag-n-drop 0.1.0
+ com.cognitect/transit-clj 0.8.313
.   com.cognitect/transit-java 0.8.337
.     javax.xml.bind/jaxb-api 2.3.0
.     org.msgpack/msgpack 0.6.12
.       com.googlecode.json-simple/json-simple 1.1.1
.       org.javassist/javassist 3.18.1-GA
+ com.cognitect/transit-cljs 0.8.256
.   com.cognitect/transit-js 0.8.846
+ com.stuartsierra/component 0.4.0
.   com.stuartsierra/dependency 0.2.0
+ compact-uuids/compact-uuids 0.2.0
+ io.pedestal/pedestal.immutant 0.5.5
.   org.jboss.logging/jboss-logging 3.3.2.Final
+ io.pedestal/pedestal.route 0.5.5
.   org.clojure/core.incubator 0.1.4
+ io.pedestal/pedestal.service 0.5.5
.   commons-codec/commons-codec 1.11
.   crypto-equality/crypto-equality 1.0.0
.   crypto-random/crypto-random 1.2.0
.   io.pedestal/pedestal.interceptor 0.5.5
.     org.clojure/core.match 0.3.0-alpha5
.   io.pedestal/pedestal.log 0.5.5
.     io.dropwizard.metrics/metrics-core 4.0.2
.     io.dropwizard.metrics/metrics-jmx 4.0.2
.     io.opentracing/opentracing-api 0.31.0
.     io.opentracing/opentracing-util 0.31.0
.       io.opentracing/opentracing-noop 0.31.0
.     org.slf4j/slf4j-api 1.7.25
.   org.clojure/core.async 0.4.474
.   org.clojure/tools.analyzer.jvm 0.7.2
.     org.clojure/core.memoize 0.5.9
.       org.clojure/core.cache 0.6.5
.         org.clojure/data.priority-map 0.0.7
.     org.clojure/tools.analyzer 0.6.9
.     org.ow2.asm/asm-all 4.2
.   org.clojure/tools.reader 1.2.2
+ juxt/crux 19.04-1.0.3-alpha
.   com.taoensso/nippy 2.14.0
.     com.taoensso/encore 2.93.0
.       com.taoensso/truss 1.5.0
.     net.jpountz.lz4/lz4 1.3
.     org.iq80.snappy/snappy 0.4
.     org.tukaani/xz 1.6
.   org.agrona/agrona 0.9.33
.   org.clojure/tools.logging 0.4.1
+ org.clojure/clojure 1.10.1-RC1
.   org.clojure/core.specs.alpha 0.2.44
.   org.clojure/spec.alpha 0.2.176
+ org.immutant/web 2.1.10
.   org.immutant/core 2.1.10
.     org.clojure/java.classpath 0.2.3
.   org.projectodd.wunderboss/wunderboss-clojure 0.13.1
.   org.projectodd.wunderboss/wunderboss-web-undertow 0.13.1
.     io.undertow/undertow-core 1.4.14.Final
.       org.jboss.xnio/xnio-api 3.3.6.Final
.       org.jboss.xnio/xnio-nio 3.3.6.Final
.     io.undertow/undertow-servlet 1.4.14.Final
! Duplicate entry "META-INF/services/io.undertow.server.handlers.builder.HandlerBuilder" from "io.undertow/undertow-servlet 1.4.14.Final" already seen in "io.undertow/undertow-core 1.4.14.Final"
! Duplicate entry "META-INF/services/io.undertow.attribute.ExchangeAttributeBuilder" from "io.undertow/undertow-servlet 1.4.14.Final" already seen in "io.undertow/undertow-core 1.4.14.Final"
! Duplicate entry "META-INF/services/io.undertow.predicate.PredicateBuilder" from "io.undertow/undertow-servlet 1.4.14.Final" already seen in "io.undertow/undertow-core 1.4.14.Final"
.       org.jboss.spec.javax.annotation/jboss-annotations-api_1.2_spec 1.0.0.Final
.     io.undertow/undertow-websockets-jsr 1.4.14.Final
.     org.projectodd.wunderboss/wunderboss-core 0.13.1
.       ch.qos.logback/logback-classic 1.1.3
.         ch.qos.logback/logback-core 1.1.3
.     org.projectodd.wunderboss/wunderboss-web 0.13.1
.       org.jboss.spec.javax.servlet/jboss-servlet-api_3.1_spec 1.0.0.Final
.       org.jboss.spec.javax.websocket/jboss-websocket-api_1.1_spec 1.1.0.Final
+ org.rocksdb/rocksdbjni 5.17.2
+ ring/ring-core 1.6.3
.   clj-time/clj-time 0.11.0
.     joda-time/joda-time 2.8.2
.   commons-fileupload/commons-fileupload 1.3.3
.   ring/ring-codec 1.0.1
+ rum/rum 0.11.3
.   cljsjs/react 16.2.0-3
.   cljsjs/react-dom 16.2.0-3
! Duplicate entry "deps.cljs" from "cljsjs/react-dom 16.2.0-3" already seen in "cljsjs/react 16.2.0-3"
.   sablono/sablono 0.8.1
.     org.omcljs/om 1.0.0-alpha48
! Duplicate entry "data_readers.clj" from "org.omcljs/om 1.0.0-alpha48" already seen in "juxt/crux 19.04-1.0.3-alpha"
! Duplicate entry "deps.cljs" from "org.omcljs/om 1.0.0-alpha48" already seen in "cljsjs/react 16.2.0-3"
+ resources/**
+ src/**
[uberdeps] Packaged MyProject.uberdeps.jar in 6832 ms
```

The motivation for this was that you might accidentally spot something that goes into the jar that shouldn’t. E.g. just from reading it now I noticed that `transit-clj` brings in message pack which is not really used anywhere at runtime.

As you can guess, I failed my own goal of only using existing tools for Grumpy. The upside is that somebody else might find it useful. Uberdeps has been powering Grumpy builds since April 2019 without any issues. If you are in need of such tool, consider using it.

P.S. Don’t worry too much about `Duplicate entry` warnings. Everything works fine without those for simple cases. For the rest they’ll be handled in [github.com/tonsky/uberdeps/issues/1](https://github.com/tonsky/uberdeps/issues/1) and [github.com/tonsky/uberdeps/issues/2](https://github.com/tonsky/uberdeps/issues/2).