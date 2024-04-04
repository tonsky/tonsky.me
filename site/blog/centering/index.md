---
title: We lost the secret of centering things
---

This is my claim: we, as a civilization, forgot how to center things.

I mean, we know _how_ to do it. It has never been simpler:

```
display: flex;
justify-content: center; /* Horizontal centering */
align-items: center; /* Vertical centering */
```

(don’t ask why you need to remember four words instead of just horizontal/vertical, _it’s still better than before_)

Or you can use grids, if you want:

```
display: grid;
justify-items: center; /* Horizontal centering */
align-items: center; /* Vertical centering */
```

(also don’t ask why `justify-content` became `justify-items`)

We can even deduce it from the first principles:

```
x₂ = x₁ + (w₁ - w₂) / 2
y₂ = y₁ + (h₁ - h₂) / 2
```

Even ChatGPT knows how to center things:

chatgpt@2x.png

Okay, maybe not right away, but eventually it gets there.

What I’m saying is: everybody knows how to center things. It’s trivial. And if you are lost, the knowledge is right there.

Yet, when we look at actual applications, we see that these methods are not used. We see this:

telegram_date@2x.webp

or this:

google_maps_cross@2x.webp

or even this:

feedly_beta@2x.webp

So something is clearly getting lost between know-how and applying that knowledge.

something.png

In theory, there’s no difference between theory and practice. Unfortunately, we live in practice.

So what’s happening? Let’s find out.

# Fonts

Fonts are one of the biggest offenders. You can see poorly-alignet text everywhere.

Apple can’t do it:

apple_buttons_big_sur@2x.png

Event though they knew how in the past, that secret is long gone:

spotlight@2x.webp

Microsoft can’t do it:

windows@2x.webp

Github can’t do it:

github@2x.webp

Valve can’t do it:

steam@2x.webp

Slack can’t do it:

slack_button@2x.webp

Sometimes I think they don’t even try:

slack@2x.webp

Notion can’t do it:

notion@2x.webp

YouTube can’t do it:

youtube@2x.webp

Telegram can’t do it:

telegram@2x.webp

Google Maps can’t do it:

google_maps@2x.webp

I can do this all day, but I think you get the idea. Companies big and small, native or web, none of them are safe from text centering problem.

So what _is_ the problem?

It all starts with the font. Right now, bounding box of a text block looks like this:

text_bounding_box@2x.png

The problem is, it can also look like this:

text_bounding_box_2@2x.png

or this:

text_bounding_box_3@2x.png

Now, what will happen if you try to center text by centering its bounding box?

text_bounding_box_4.png

Text will be off! Even though rectangles are perfectly centered.

But even if font _can_ have its metrics unbalanced, it doesn’t mean it does. What happens in reality?

In reality, _most_ of the popular fonts have metrics slightly off. Many have it _significantly_ off:

metrics@2x.png
Percentages are of cap-height

10% is not a small number. It’s a whole pixel on font-size 13! Two, if you have retina! It’s easily noticeable.

The solution is simple: make tight bounding boxes and centering will become trivial:

text_bounding_box_5.png

If you use Figma, it already can do this (although it’s not the default):

figma_vertical_trim@2x.png

If you use web, try [Capsize](https://seek-oss.github.io/capsize/).

If you are a font designer, make life easier for everybody by setting your metrics so that `ascender − cap-height = descender`:

font_metrics_numbers@2x.png

Or the same idea, visually:

font_metrics@2x.png

Important! You don’t have to _actually_ extend your ascenders/descenders to these boundaries. As you can see in the picture, my ascender space, for example, is way underutilized. Just make the numbers match.

See [Font size is useless; let’s fix it](https://tonsky.me/blog/font-size/) for more information.


