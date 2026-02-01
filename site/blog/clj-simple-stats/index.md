---
title: Statistics made simple
summary: "Announcing a simple statistics library for Clojure web servers"
published: 2025-12-15
---

I have a weird relationship with statistics: on one hand, I try not to look at it too often. Maybe once or twice a year. It’s because analytics is not actionable: what difference does it make if a thousand people saw my article or ten thousand?

I mean, sure, you might try to guess people’s tastes and only write about what’s popular, but that will destroy your soul pretty quickly.

On the other hand, I feel nervous when something is not accounted for, recorded, or saved for future reference. I might not need it now, but what if ten years later I change my mind?

Seeing your readers also helps to know you are not writing into the void. So I really don’t need much, something very basic: the number of readers per day/per article, maybe, would be enough.

Final piece of the puzzle: I self-host my web projects, and I use an old-fashioned web server instead of delegating that task to Nginx.

Static sites are popular and for a good reason: they are fast, lightweight, and fulfil their function. I, on the other hand, might have an unfinished gestalt or two: I want to feel the full power of the computer when serving my web pages, to be able to do fun stuff that is beyond static pages. I need that freedom that comes with a full programming language at your disposal. I want to program my own web server (in Clojure, sorry everybody else).

# Existing options

All this led me on a quest for a statistics solution that would uniquely fit my needs. Google Analytics was out: bloated, not privacy-friendly, terrible UX, Google is evil, etc.

ga@2x.webp
What is going on?

Some other JS solution might’ve been possible, but still questionable: SaaS? Paid? Will they be around in 10 years? Self-host? Are their cookies GDPR-compliant? How to count RSS feeds?

Nginx has access logs, so I tried server-side statistics that feed off those (namely, Goatcounter). Easy to set up, but then I needed to create domains for them, manage accounts, monitor the process, and it wasn’t even performant enough on my server/request volume!

# My solution

So I ended up building my own. You are welcome to join, if your constraints are similar to mine. This is how it looks:

screenshot@2x.webp

It’s pretty basic, but does a few things that were important to me.

## Setup

Extremely easy to set up. And I mean it as a feature.

Just add our middleware to your Ring stack and get everything automatically: collecting and reporting.

```
(def app
  (-> routes
    ...
    (ring.middleware.params/wrap-params)
    (ring.middleware.cookies/wrap-cookies)
    ...
    (clj-simple-stats.core/wrap-stats))) ;; <-- just add this
```

It’s zero setup in the best sense: nothing to configure, nothing to monitor, minimal dependency. It starts to work immediately and doesn’t ask anything from you, ever.

See, you already have your web server, why not reuse all the setup you did for it anyway?

## Request types

We distinguish between request types. In my case, I am only interested in live people, so I count them separately from RSS feed requests, favicon requests, redirects, wrong URLs, and bots. Bots are particularly active these days. Gotta get that AI training data from somewhere.

RSS feeds are live people in a sense, so extra work was done to count them properly. Same reader requesting `feed.xml` 100 times in a day will only count as one request.

Hosted RSS readers often report user count in User-Agent, like this:

```
Feedly/1.0 (+http://www.feedly.com/fetcher.html; 457 subscribers; like FeedFetcher-Google)

Mozilla/5.0 (compatible; BazQux/2.4; +https://bazqux.com/fetcher; 6 subscribers)

Feedbin feed-id:1373711 - 142 subscribers
```

My personal respect and thank you to everybody on this list. I see you.

readers@2x.webp

## Graphs

Visualization is important, and so is choosing the correct graph type. This is wrong:

goat@2x.webp

Continuous line suggests interpolation. It reads like between 1 visit at 5am and 11 visits at 6am there were points with 2, 3, 5, 9 visits in between. Maybe 5.5 visits even! That is not the case.

This is how a semantically correct version of that graph should look:

graph@2x.webp

Some attention was also paid to having reasonable labels on axes. You won’t see something like 117, 234, 10875. We always choose round numbers appropriate to the scale: 100, 200, 500, 1K etc.

Goes without saying that all graphs have the same vertical scale and syncrhonized horizontal scroll.

## Insights

We don’t offer much (as I don’t need much), but you can narrow reports down by page, query, referrer, user agent, and any date slice.

## Not implemented (yet)

It would be nice to have some insights into “What was this spike caused by?”

Some basic breakdown by country would be nice. I do have IP addresses (for what they are worth), but I need a way to package GeoIP into some reasonable size (under 1 Mb, preferably; some loss of resolution is okay).

Finally, one thing I am really interested in is “Who wrote about me?” I do have referrers, only question is how to separate signal from noise.

Performance. DuckDB is a sport: it compresses data and runs column queries, so storing extra columns per row doesn’t affect query performance. Still, each dashboard hit is a query across the entire database, which at this moment (~3 years of data) sits around 600 MiB. I definitely need to look into building some pre-calculated aggregates.

One day.

## How to get

Head to [github.com/tonsky/clj-simple-stats](https://github.com/tonsky/clj-simple-stats) and follow the instructions:

banner@2x.webp https://github.com/tonsky/clj-simple-stats

Let me know what you think! Is it usable to you? What could be improved?
