---
title: JavaScript Bloat in 2024
summary: "What is the average size of JavaScript code downloaded per website? Fuck around and find out!"
published: 2024-02-22
hackernews_id: 39471221
---

I was a bit out of touch with modern front-end development. I also remembered articles about web bloat, how the average web page size was approaching several megabytes!

So all this time I was living under impression that, for example, if the average web page size is 3 MB, then JavaScript bundle should be around 1 MB. Surely content should still take the majority, no?

Well, the only way to find out is to fuck around. Let’s do a reality check!

I’m writing this in 2024, so maybe do a sequel in a few years?

# Method

- Firefox on macOS (but should be the same in any browser)
- Not incognito (I want to see numbers _inside_ the app, and there’s a better chance it will resemble actual everyday experience)
- All extensions disabled
- JavaScript only
- Uncompressed
- Service Workers enabled (again, more real-life)
- All caching disabled (cold load)

Why only JavaScript? Content varies a lot from site to site (surely videos on YouTube are heavier than text messages on Slack), but JavaScript is a universal metric for “complexity of interactions”.

The main goal is to evaluate how much work the browser has to do to parse and execute code.

To set some baseline, let’s start with this blog:

tonsky@2x.webp

The number here would be 0.004 MB. I also highlighted all the important bits you need to set if you decide to reproduce this at home.

# Landings

Okay, let’s start with something simple, like landing pages/non-interactive apps.

A normal slightly interactive page looks like this — Wikipedia, 0.2 MB:

wikipedia@2x.webp

Slightly bloated — like this — Linear, 3 MB:

linear@2x.webp

Remember: that’s without images, or videos, or even styles! Just JS code.

A bad landing page looks like this — Zoom, 6 MB:

zoom@2x.webp

or like Vercel, 6 MB:

vercel@2x.webp

Yes, this is just a landing page. No app, no functionality, no calls. 6 MB of JavaScript just for that.

You can do a lot worse, though — Gitlab, 13 MB:

gitlab@2x.webp

Still just the landing.

# Mostly static websites

Nothing simpler than showing a static wall of text. Medium needs 3 MB just to do that:

medium@2x.webp

Substack needs 4 MB:

substack@2x.webp

Progress?

Quora, 4.5 MB:

quora@2x.webp

Pinterest, 10 MB:

pinterest@2x.webp

Patreon, 11 MB:

patreon@2x.webp

And all this could’ve been a static page...

# Search

When your app’s interactivity is limited to mostly search. Type the query — show the list of results. How heavy is that?

StackOverflow, 3.5 MB:

stackoverflow@2x.webp

NPM, 4 MB:

npmjs@2x.webp

Airbnb, 7 MB:

airbnb@2x.webp

Booking.com, 12 MB:

booking@2x.webp

But Niki, booking is complicated! Look at all this UI! All these filters. All these popups about people near you stealing your vacation!

Okay, okay. Something simpler then. Google. How about Google? One text field, list of links. Right?

Well, it’ll cost you whooping 9 MB:

google@2x.webp

Just to show a list of links.

# Simple one-interaction apps

Google Translate is just two text boxes. For that, you need 2.5 MB:

google_translate@2x.webp

ChatGPT is _one_ text box. 7 MB:

openai@2x.webp

I mean, surely, ChatGPT is complex. But on the server, not in the browser!

# Videos

Loom — 7 MB:

loom@2x.webp

YouTube — 12 MB:

youtube@2x.webp

Compare it to people who really care about performance — Pornhub, 1.4 MB:

pornhub@2x.webp

# Audio

I guess audio just requires 12 MB no matter what:

SoundCloud:

soundcloud@2x.webp

Spotify:

spotify@2x.webp

# Email

Okay, video and audio are probably heavy stuff (even though we are not measuring content, just JS, remember!). Let’s move to simpler office tasks.

Google Mail is just (just!) 20 MB:

gmail@2x.webp

It’s a freaking mailbox!!! How on earth is it almost as big as Figma, who ships entire custom C++/OpenGL rendering for their app?

figma@2x.webp

And if you are thinking: mail is complicated, too. Lots of UI, lots of interactivity. Maybe 20 MB is okay?

No!

Just no. See, FastMail, same deal, but only 2 MB. 10× less!

fastmail@2x.webp

# Productivity

Okay, maybe e-mail is too complicated? How about something even simpler? Like a TODO list?

Well, meet Todoist, 9 MB:

todoist@2x.webp

Showing you a list of files in folders requires 10 MB in Dropbox:

dropbox@2x.webp

List of passwords? That’ll be 13 MB on 1Password:

1password@2x.webp

Cards? Add 0.5 MB more, up to 13.5 MB. Trello:

trello@2x.webp

Okay, maybe TODO lists are too complex, too? How about chatting?

Well, Discord needs 21 MB to do that:

discord@2x.webp

# Document editing

Okay, document editing is hard, right? You have to implement cursor movement, synchronization, etc.

Google Docs, 13.5 MB:

google_docs@2x.webp

Something simpler? Notion, 16 MB:

notion@2x.webp

# Social Networks

The typical size of code that social networks need for like buttons to go brrr is 12 MB.

Twitter, 11 MB:

twitter@2x.webp

Facebook, 12 MB:

facebook@2x.webp

TikTok, 12.5 MB:

tiktok@2x.webp

Instagram is somehow bigger than Facebook, despite having like 10× less functions. 16 MB:

instagram@2x.webp

LinkedIn. Is it a blog? A platform? It has search, it has messaging, it has social functions. Anyways, that’ll be 31 MB:

linkedin@2x.webp

By the way, I'd like to add you to my professional network on LinkedIn.

# Elephants — its own category

Sometimes websites are so stupidly, absurdly large that they deserve their own category.

Here, Jira, a task management software. Almost 50 MB!

jira@2x.webp

Do they ship the entire Electron compiled WASM or what?

But that’s not the limit! Slack adds 5 more MB, up to 55 MB:

slack@2x.webp

Yes, it’s a chat. You know, list of users, messages, reactions. Stuff we did on raw HTML, even before JS was invented?

That’s 55 MB in today’s world. It’s almost like they are trying to see how much more bullshit can they put in a browser before it breaks.

Finally, this blew my mind. Somehow [react.dev](https://react.dev/blog/2023/03/16/introducing-react-dev) starts with a modest 2 MB but as you scroll back and forth, it grows indefinitely. Just for fun, I got it to 100 MB (of JavaScript!), but you can go as far as you like:

react@2x.mp4

What is going on there? Even if it unloads and downloads parts of that blog post, how is it growing so quickly? The text itself is probably only 50 KB (0.05 MB).

UPD: It has been brought to my attention that this behavior is not, in fact, representative of normal user experience. Normally embedded code editors will be cached after first load and subsequent loads will be served from disk cache. So as you scroll, you will see no network traffic, but these 100 MB of JS will still be parsed, evaluated and initialized over and over as you scroll.

# How fast are we degrading?

Look how cute! In 2015 average web page size was approaching shareware version of Doom 1 (2.5 MB):

bloat_2015@2x.webp
[Source](https://twitter.com/xbs/status/626781529054834688)

Well, in 2024, Slack pulls up 55 MB, the size of the original Quake 1 with all the resources. But now it’s just in JavaScript alone.

For a chat app!

# How big is 10 MB anyway?

To be honest, after typing all these numbers, 10 MB doesn’t even feel that big or special. Seems like shipping 10 MB of *code* is normal now.

If we assume that the average code line is about 65 characters, that would mean we are shipping ~150,000 lines of code. With every website! Sometimes just to show static content!

And that code is minified already. So it’s more like 300K+ LoC just for one website.

But are modern websites really _that_ complex? The poster child of SPAs, Google Maps, is quite modest by modern standards — is _still_ just 4.5 MB:

google_maps@2x.webp

Somebody at Google is seriously falling behind. Written with modern front-end technologies, it should be at least 20 MB.

And if you, like me, thought that “Figma is a really complex front-end app, so it must have a huge javascript download size”, well, that’s correct, but then Gmail is about as complex as Figma, LinkedIn is 1.5× more complex and Slack is 2.5× more ¯\\\_(ツ)\_/¯

# Conclusion

It’s not just about download sizes. I welcome high-speed internet as much as the next guy. But code — JavaScript — is something that your browser has to parse, keep in memory, execute. It’s not free. And these people talk about performance and battery life...

Call me old-fashioned, but I firmly believe content should outweigh code size. If you are writing a blog post for 10K characters, you don’t need 1000× more JavaScript to render it.

This site is doing it right:

jquery@2x.webp

That’s 0.1 MB. And that’s enough!

And yet, on the same internet, in the same timeline, Gitlab needs 13 MB of code, 500K+ LoC of JS, just to display a static landing page.

Fuck me.

