---
title: Every Frame Perfect
summary: "How imprecise UI animations erode trust in product"
published: 2026-06-13
hackernews_id: 48516251
lobsters_url: https://lobste.rs/s/bt7rtp/every_frame_perfect
starred: true
---

_Translations: [Russian](https://habr.com/ru/articles/1051492/)_

A while ago I was reading about Wayland and this quote stuck with me:

> A stated goal of Wayland is “[every frame is perfect](https://wayland-book.com/protocol-design/design-patterns.html)”.

And I think this is a goal we should all aspire to. Wayland is talking about the technical side of things (modern GPU stacks are very complex and Wayland is trying to take control back) but it could be applied to UI too.

The rule of thumb is:

.loud If I take a screenshot of your app at any moment, you should be able to explain what I see

.note EDIT: This used to say “..., it must make sense” but that doesn’t account for advanced animation techniques such as smear frames etc.

Why care about every frame? It builds trust. Users can’t see the code, so UI is the only way for them to judge the quality of the app. If UI looks good, that means developers had time to polish it, which means that they probably spent a comparable amount of time to iron out the code. It’s a heuristic, but a reasonable one.

Now, what does it mean in practice? I can think of a few things:

- No white flashes between screens.
- No partially loaded content.
- No relayout while content loads.
- Internally consistent. If one part of the UI says “1 update available”, another part should not say “Checking for updates...”
- Precise animations.

Animations often end up being forgotten. A UI might look great in both start and end states but very janky in between. Like this:

toolbar@2x.mp4

If you feel like there are weird things going on there, there are! Look at slowed down version:

toolbar_slow_reencode@2x.mp4

Now let’s apply our rule and take screenshots in the middle of the animation. This doesn’t look right:

toolbar_still_2@2x.png

Neither does this:

toolbar_still_1@2x.png

Both of these frames are not perfect.

Let’s look at another example. Safari:

safari@2x.mp4

Placeholder text here moves from the center but cursor animates from the left position:

safari_slow_reencode@2x.mp4

Not the end of the world by any means, but it does create a feeling that these two components are not in sync with each other. Next thought: maybe they weren’t designed together? If so, then they might not work well together. That’s how trust is lost.

This desynchronization can lead to a lot of confusion. For example, in Photos, when switching between Crop and Adjust mode, picture snaps into place immediately but the crop border is animated:

photos@1x.mp4

This creates a _false_ feeling that something subtly changes when you switch between modes. And you know what? I don’t want my UI to give me false feelings. I want it to be a precise instrument, not an animated toy.

Sometimes animations are supposed to help you understand a transition, so it’s doubly sad when they make it harder. Follow the magnifying glass:

search_slow_reencode@2x.mp4

Same with Youtube. They had the simplest task in the world: move a rectangle from one position to another! Yet they decided to do something very strange:

youtube@1x.mp4

Can you explain this? Does it make sense?

youtube@1x.png

Probably a technical limitation of the DOM architecture they decided earlier on. I call these situations “The technology has outsmarted the programmer”. But no matter the reason, the result is an imperfect frame.

Sometimes animations are left out as an afterthought. Whatever happens, happens. Then we get this:

save@1x.mp4

The details are fascinating to watch:

save_slow_reencode@1x.mp4

So yeah. Please pay attention not only to the start and end states, but also to everything in between. _Every frame matters._

I’ll leave you with this unprovoked zoom animation from Preview app. Take care!

preview@1x.mp4