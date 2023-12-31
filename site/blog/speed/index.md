---
title: "Speed is a feature"
summary: "When speed is a crucial UX factor"
published: 2018-04-04
---

Jane Street engineers have built their own intergrated code review UI that unifies local directories, local Mercurial repos, remote Mercurial repos and pull-requests/task management. [Details](https://blog.janestreet.com/putting-the-i-back-in-ide-towards-a-github-explorer/).

todo.png

What I find particulary interesting is this line:

> One of the things we found with our system is that it didn’t really click until we had a sub-100ms code review server.

So they’ve designed couple of features for a very narrow and specific use-case, polished the integration, removed all other barries and otherwise did everything right and, apparently, brought a lot of value. Yet nobody considered it valuable enough until they got the response time right.

When they did crossed that 100ms barrier, though, a qualitative change happened. People changed their views of a tool from something they have to cope with to something that’s fun, valuable and eventually become their second nature. Now they can’t imagine how they lived otherwise.

Now imagine: how many programs and services make us wait more than 100ms? I’d say more or less all of them, starting with almost every website, with a few exceptions. You don’t think about “requesting” Google when you type your search query and it auto-completes after each letter. But you do think about rebooting your computer. Or booting up your text editor. Or even loading a Medium article (6 sec last time I checked). Because they’re all so slow you have to adjust and build your habits around that waiting time. Heck, Jane Street built the integration because waiting on Web UI to load was an eternity.

Millions of programs have that unfulfilled potential of becoming your second nature, something you don’t even think about when interacting with. They’re waiting to be enabled to “click”. Speed is a feature.