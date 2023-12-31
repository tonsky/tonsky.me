---
title: Another powered-by-DataScript example
summary: "Git Achievements application built around DataScript"
published: 2014-10-06
---

Last weekend my friends and I built Acha-Acha — a Git Achievements web app for <a href="https://clojurecup.com/#/apps/acha">ClojureCup hackathon</a>. It’s the most clojure-y application I’ve ever written: we used Clojure, core.async, transducers, http-kit, Transit, ClojureScript, sablono, and DataScript — of course. (plus React and JGit from non-clojure stack). My part was UI development, and DataScript helped a lot.

acha.jpg

Acha-Acha follows the idea of a dumb but persistent server and smart ephemeral client: all data is loaded via single fetch on a page load and everything else is handled by a client. It’s literally everything, including page navigations, data sorting, filtering, aggregating: server just sends every fact it’s aware of, and client deduces lists, totals and sums from raw data on the fly. For example, on index page there’s achievement count on each user’s badge: it’s not stored in a database, but calculated on a client from a raw achievements list (equivalent of `select count group by`).

aggregations@2x.png

Once app is loaded, you can go to any page and there’ll be not a single ajax call for that. Each page is just a couple of queries to already fetched DB. Doing zero ajax calls is wickedly fast, much faster than talking to server, even very good one.

There’s no “RESTful” server API, of course — just one endpoint (`/api/db/`) that dumps everything. We can probably generate response once and serve it from disk via Nginx, it’s not dynamic at all.

Actually, we’ve added some dynamism after the contest: list of new achievements, new users and new repos are delivered to the browser when someone adds new repo or old one gets updated. Here’s all the code (literally, there’s not a line more) that handles all server pushes:

```
(let [socket (js/WebSocket. url)]
  (set! (.-onmessage socket)
        (fn [event]
          (let [tx-data (read-transit (.-data event))]
            (d/transact! conn tx-data)))))
```

This little snippet is everything you need to magically transform a fully static web app to a fully dynamic one. On any page, wherever you are, you’ll see up-to-date information and all the changes in real-time. If a user gets awarded, you’ll see the new achievement popping up on user’s page, on index page you’ll see how his medal’s counter gets advanced, and on repo's page you’ll see his avatar being added to the achievements list. Amazing thing is that there’s no code to support any of this. There’s just one listener that puts everything it sees to the database.

It wouldn't be possible without React, of course. Where DataScript gives you simplicity with “just put everything to the DB”, React gives you simplicity with “just take the DB and render everything from it”. In Acha-Acha, every page is always rendered from scratch, including data queries for everything it needs; we don’t even have Om/Quiescent style optimisations of `shouldComponentUpdate` — and still, it’s quite performant and perfectly useful.

Speaking of performance — we haven’t implemented any limits for main page, we aren’t even truncating list of users and just render everybody. I presumed that my browser will start suffering after 10th repository added, but real-world experience was much more positive. We’ve seen up to ~50K datoms kept in the DB, main time spent on initial DB population, working fast after that (speaking of which, I found a good potential to speed up initial bulk import ~5-10 times in DataScript). 26K datoms were inserted in ~800ms, not that lethal, especially after I’ve added progress bar (not in the deployed version yet). Again, 26K datoms is for ~30 repos and ~1200 users, much bigger than typical app will probably need to load at once. Still faster that GMail though :)

Another huge performance revelation was Transit. It’s a serialization format that keeps all your Clojure data structures intact (persistent maps/vectors/sets/lists, keywords, dates — yes, we send dates as-is, totally transparent and very handy) and still performs on par with browser’s built-in `JSON.parse`. Here’s totally unscientific comparison of deserialization speeds:

```
json              8.338ms   1006 Kb
transit          14.228ms    950 Kb
json + js->clj  213.135ms   1006 Kb
edn             358.302ms   1055 Kb
```

.foot (in our case, 1Mb of data corresponds to those 50K datoms, if I remember correctly)

Overall experience from using DataScript was very smooth and it performed with dignity in tough conditions of limited time and unexpected performance demands (e.g. each page re-rendering is a couple of queries and _a lot_ of entities lookups over a database that contains every fact about everything). It leads to an architecture that is quick to develop and requires very little effort to achieve outstanding results.

Now about fun stuff.

~~You can check out live version at [acha-acha.co](http://acha-acha.co). Everything is still not smooth enough[^1] — beware. Please do not abuse it, as we’re still not prepared for real-world SaaS.~~

We also have the source code available [at github](https://github.com/someteam/acha). It was updated with most urgent patches, including real-time page updates, missing images, better repo handling, progress-bar, etc.

And we have [portable version](https://github.com/someteam/acha/releases). It's totally autonomous, only prerequisite is Java. It’s for private network setups, when you want to play with achievements but don’t want to share your secret code with us. Feel free to. [Instructions](https://github.com/someteam/acha/blob/master/README.md).

As for the achievements themselves, we’ve implemented just about ⅓ of what we’ve planned. Much more to come.

tease.png

[^1]: Except for pixel art, which is far more than I could dream of. It’s remarkable.