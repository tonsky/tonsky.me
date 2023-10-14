---
title: "Humble Chronicles: Developer Experience"
summary: "Using Humble UI from the REPL"
published: 2022-02-22
---

This is a third post documenting the process of developing [Humble UI](https://github.com/HumbleUI/HumbleUI/), a Clojure UI framework. In this post, we discuss cross-platform REPL-driven development.

# Interactive development

When building UIs, fast iteration times are crucial for good results. The quality of the result is directly proportional to the amount of iterations you go through during the development.

Another thing about UI development: most of the time it’s a constant stream of small tweaks: change a color here, add a padding there, make line shorter over there. Almost never building a UI is a huge blind upfront effort, it’s just doesn’t happen.

Now, statically typed languages traditionally struggle with that type of experience. You either need to recompile and re-run your app, which takes time and loses you your state, or build and approximate version of the UI from the XML description or code, which might differ from a real thing.

https://www.youtube.com/watch?v=OMzREI8W4G8

Clojure is uniquely positioned for UI development specifically: it’s a language designed around REPL and live modifications, which is [great for any day-to-day coding](https://tonsky.me/blog/interactive-development/), but really starts to shine when applied to UI.

In fact, REPL-driven development is the main reason why I think this effort has any chance of getting traction: it’s qualitative difference from almost any other tool out there (except, maybe, browser, if cooked right).

Here, watch me changing constants, updating the layout, modifying _the UI code_ and running the tests all without ever leaving the editor. With instant feedback and full state persistence:

repl.mp4

Can your UI framework do that?

# Crossing platforms

Humble UI is a cross-platform framework, which might be both good and bad thing.

Bad because it doesn’t make use of platform-native widgets and as a result apps don’t look native. But, in the age of web and Electron, I don’t think it’s a big deal.

Good because it somewhat delivers on “Write once, run anywhere” mantra.

With a slight twist: we do try to abstract common stuff (e.g. window size or mouse events) but also respect platform special traits (e.g. toolbar styles on macOS). Latter will only work on specific platforms, but if you want to go extra mile, there’ll be an option for you.

# Building an app

After many months working on JWM and Humble UI and a few days getting calculator app to work on macOS, I finally booted Windows and it was there, too, real, flesh and bone, working with no extra effort. I booted in Linux and it also was a success!

calculator.png

It’s a fantastic feeling, after working on the internals for so long, finally apply it to something and feel that it does something useful and has a reason to exist.

If you are wondering what it feels building an app from scratch, there’s a video of Wordle implementation from start to finish in \~3 hours:

https://www.youtube.com/watch?v=qSswvHrVnvo

Humble UI if far from being usable: it lacks a lot, and what it got will very likely change in the near future. Yet, for the curious, the link:

banner@2x.png https://github.com/HumbleUI/HumbleUI/