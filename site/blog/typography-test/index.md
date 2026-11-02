---
title: "Typography test: Software that respects your attention"
summary: "A draft for testing typography: every kind of block on one screen"
---

Good programs used to be _quiet_. You opened one, did the thing you came for, and closed it. Nobody asked you to rate the experience, sign in with `SSO`, or read about [what’s new](https://tonsky.me/blog/needy-programs/) in version 14.2.

Think about the last time you opened a simple text editor from twenty years ago. It appeared before your finger left the key, and it showed you exactly one thing: an empty page with a blinking cursor. There was no sidebar suggesting what you might want to write, no account waiting to be synced, `(defn open)` no little dot demanding that you look at a feature you never asked for. `(when (.exists (io/file path)))` It didn’t care who you were or how often you came back. You typed, you saved, you closed the window, and the program had no opinion about any of that. We called this being useful, and somehow it was enough. Nobody felt the experience was lacking, because the experience was your own work, and the tool [simply stayed out of its light](https://tonsky.me/blog/needy-programs/).

.loud A program should do what you asked and then get out of the way

Today, the first five minutes with any app are spent dismissing things.

picture@2x.webp
Nobody has ever opened GitHub hoping to be onboarded.

# What quiet software does

Respect is not a feature you can bolt on later. It comes from a few **simple** habits:

- Starts instantly and remembers where you left off
- Never interrupts you while you’re typing
- Fits your workflow instead of asking you to change it

A good program is closer to this than to anything with an onboarding flow:

```
(defn open [path]
  (when (.exists (io/file path))
    (slurp path))) ; no account, no tour, no “what’s new”
```

> Perfection is achieved, not when there is nothing more to add, but when there is nothing left to take away.

None of this is difficult. It just takes believing that your users have better things to do.
