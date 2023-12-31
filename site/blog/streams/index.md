---
title: "Streams: Mail 3.0 concept"
summary: "Introducing concept of Streams, aiming to fix most of email-as-a-medium flaws"
published: 2014-10-27
hackernews_id: 9829614
---

It’s hard to believe people still use email. It has so many flaws yet still remains die-hard to replace. The amount of email shortcomings can compete only with the amount of email usages.

Google’s Inbox is an attempt to fix non-communication part of email. It claims to solve problem of managing todos, reminders, notifications and subscriptions in your inbox. With Google’s resources, they sure can fix that by categorizing every service and subscription provider on a planet and show their emails in the best possible way. Yet, from a history perspective, it’s some polishing (ok, a lot of it) on top of completely messed up foundation. Most importantly, it does not fix communications.

Email was envisioned as a metaphor of a post office mail. Real-world, pencil-on-paper letters are good for very specific type of communications. Transferred to the internet, email inherited these limitations: electronic messages are not real-time, they’re hard to manage in huge amounts, they work best for person-to-person dialogs.

Working with email at per-message level makes communication heavy, slow and boring. It’s awkward to ping somebody with email, or ask “HYD”, or say “ok”. Waste of a letter, right? You need subject, you need greeting, you need signature. Well, you don’t, but that’s kind of a format, an _etiquette_. Think about it: in most clients you have to “open” messages (with click, but anyway) to see what’s inside. 

To fix email, we should stop thinking about it as email at all.

email_noise.jpg
You can’t get away with that much noise

Better conversation models are threads, topics and chat rooms. There, individual messages are not as important. You operate on a level of the entire messages flow. Forums, chats and web 2.0 networks are trying to make conversations feel as lightweight and barrier-free as possible.

We should definitely get rid of legacy tech stack. There’re so many features people want from a modern communication tool email cannot deliver: sharing threads, managing permissions and user groups (because dedicated admins are so 2003), post-factum message editing. New developer comes to your company, you add him to “devs@acme.com” group and he gets access to all communications in the past that happened in that group before him. Sounds fun, right? Unlike email, which isn’t even web-friendly: e.g., you can’t share a link to the letter.

This is about where Google Wave team stopped: (apart from really insane stuff) they concentrated on fixing email threads, made them real-time and almost lightweight. Big improvement, no doubt, enough to call it Email 2.0, but not improvement enough. Your inbox was still a mess, and they expected you to clean it.

Email gets tricky with a scale. Starting with 50+ messages per day, even if individual messages are grouped to threads, there’re still _a lot_ of threads and no clue how to group them together. Google tried to fix that first with filters (failed), then by applying machine learning and other heuristics in Priority Inbox and later in Google Inbox. But there’s just not enough structure in emails or threads to make meaningful, reliable sorting.

Let’s introduce one more layer of abstraction: _stream_. Stream is a group of threads. Simple as that. When you start new thread, you don’t just start it by filling `To:` and `Subject:`, you also have to put it to some stream.

What are examples of streams? I’m a programmer and use email heavily at work. We could’ve created stream “Code review” and start all new threads about reviews here. We’d put everything non-technical to stream “Offtopic”, like “Ideas for birthday party” or “Who lost the keys?” or threads like that. Paperwork goes to “Papers” stream, and clearly not everybody would subscribe to that.

Streams are not created on a personal basis, they are shared between people interested in them. Streams are like shared folders for individual threads. This additional layer keeps your communications organized: you don’t anymore have one cluttered inbox, threads are now sorted in streams with authors’ hands. Streams are high-level enough to make inbox organization effortless and not to introduce significant overhead.

This is a principal difference from filters: filters are personal, sender doesn’t know how receivers are going to sort each particular email, and thus cannot help. Receiver have to figure out complex filter pipeline and enforce strict conventions on subjects and aliases in order for filters to work. One can break conventions by being lazy, forgetful or by mistake but cannot avoid choosing a stream.

filters.jpg
I’ve spent a lot of time tuning filters and still only ~50% satisfied

Streams also change traditional push notification model to a pull one. Robots used to ask your email address and then _push_ notifications to your inbox. With Streams, they just generate a stream and publish all notifications there. Now _you_ decide if you want to subscribe to it or not. For example, you’re on Amazon and they generate a personal stream for you: `amazon.com/182adb92-...`. You can put that stream to your inbox now, or three days later — every notification will be there no matter if you’re reading it or not. You can even share it with your family members. Notice that Amazon didn’t ask you about your address, it doesn’t, in fact, need anything from you. It always felt wrong in traditional email subscriptions that _you_ have to share your email, and that decision to unsubscribe is not on your side. Streams fix that.

Can we get more ambitious? Yes we can.

With proper user/access management Streams could replace not just email, chats and group chats but also forums, blogs, mail lists, RSS and most of web 2.0 communities. Okay, mail lists have “mail” in it. But can you imagine writing blog in email? With Streams, blog is just a stream where only you can create threads (posts) and anyone can read and reply.

blog.png
Me writing this post as a Stream

Forums and lists are most relaxed forms of a stream: everyone can create threads and comment on them. Personal chat is just an endless thread with two people having read/write access to it. To move from personal to group chat, just add more people. Private stream could be used for notes, todos and personal journal.

chats.png
Personal and group chats

Email rooms does not differ from chat rooms, there’s no strict boundary between chats and what used to be email. It’s all just communication, just threads. UI should maintain same level of comfort for both long and short messages — tricky, subtle, but doable.

For resources published in Streams you don’t even need a feed aggregator, you can subscribe and read streams directly. You can follow blogs and will be notified about both new posts (threads) and new comments (messages), and will even be able to reply in-place. It feels right when, unlike RSS, writing and reading are both supported by a single platform.

feed.png
Me reading newsfeed and answering to blog comments in-place

With all these goals, it’s easy to imagine hundreds of streams per user. To keep concept simple, on implementation side streams are always flat, you cannot put a stream into a stream. But in UI user can create stream groups and put couple of similar streams inside. E.g. it makes sense to group “Amazon”, “PayPal”, “iTunes” and “Steam” streams into a “Shopping” group:

orders.png
Streams group and Amazon thread about one specific order

In the picture above note that Amazon stream is read-only for me, meaning I _physically_ cannot reply to that. This fixes ambiguity around do-not-reply@ addresses.

Today’s inbox is both communication tool and notification center. In both cases there’re hundreds independent information streams fighting for your attention. As a platform, email can’t solve this problem: best email providers could offer is guessing, which is expensive, fragile and non-customizable. Streams concept represents “independent information stream” metaphor directly. With Streams, unsolvable inbox organization problem becomes couple of drag-and-drops. Unlike email, where communication happens by passing letters through letterbox holes, Streams are collaborative medium: single, shared place everybody look at. It could easily replace many modern social models: blogs, forums, mail lists, feeds. It’s an abstraction right where it is needed.
