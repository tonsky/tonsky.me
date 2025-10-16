---
title: Hardest Problem in Computer Science: Centering Things
summary: "Somehow we forgot how to center rectangles and must find our way back"
published: 2024-04-16
hackernews_id: 40069599
lobsters_url: https://lobste.rs/s/xcwla4/hardest_problem_computer_science
starred: true
---

_Translations: [Chinese](https://nptr.cc/posts/tonsky-blog-centering/) [Japanese](https://coliss.com/articles/build-websites/operation/work/about-centering.html) [Russian](https://habr.com/ru/companies/ruvds/articles/810311/)_

This is my claim: we, as a civilization, forgot how to center things.

I mean, we know _how_ to do it. It has never been simpler:

```
display: flex;
justify-content: center; /* Horizontal centering */
align-items: center; /* Vertical centering */
```

(don’t ask why you need to remember four words instead of just horizontal/vertical, _it’s still better than before_)

Or you can use grids if you want:

```
display: grid;
justify-items: center; /* Horizontal centering */
align-items: center; /* Vertical centering */
```

(also don’t ask why `justify-content` became `justify-items`)

If you feel like school today, we can deduce it from the first principles:

formula@2x.png

Hey, even ChatGPT knows how to center things:

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

Fonts are one of the biggest offenders. You can see poorly aligned text everywhere. Let me showcase.

Apple can’t do it:

apple_buttons_big_sur@2x.png

Microsoft can’t do it:

windows@2x.webp

GitHub can’t do it:

github@2x.webp

Valve can’t do it:

steam@2x.webp

Slack can’t do it:

slack_button@2x.webp

Telegram can’t do it:

telegram@2x.webp

Google Maps can’t do it:

google_maps@2x.webp

Honestly, I can provide an endless supply of poorly-aligned buttons without even having to look for them:

buttons@2x.png

I think you get the idea. Myriad companies big and small, native or web, and none are safe from text-centering problems.

# Line height

If font metrics are not enough, the next problem on our way to perfect centering is line-height.

Line height is... complicated. A canonical article to learn about it is Vincent De Oliveira’s [Deep dive CSS: font metrics, line-height and vertical-align](https://iamvdo.me/en/blog/css-font-metrics-line-height-and-vertical-align).

This is how it looks applied in practice. Slack:

slack@2x.webp

Notion:

notion@2x.webp

Airbnb:

airbnb@2x.webp

YouTube:

youtube@2x.webp

Aligning two things in different containers is almost impossible:

name@2x.webp

Although many have tried:

american_airlines@2x.webp

Not many have succeeded:

addons@2x.webp

CSS might get in the way (different controls having different defaults which you have to undo before even starting trying to align):

controls@2x.webp

No easy solution here, just roll up your sleeves and delve into specifications.

# Icons

Icons are like small rectangles put in line with text. Therefore all problems caused by text AND line height apply here. Aligning icons next to text is a notoriously hard task.

Atom:

atom@2x.webp

Platform formerly known as Twitter:

twitter@2x.webp

iOS:

ios@2x.webp

Mozilla:

mozilla@2x.webp

YouTube:

youtube_likes@2x.webp

Sometimes icon wins over text:

meet@2x.webp

Sometimes text wins over icon:

ical@2x.webp

Sometimes both lose:

name_button@2x.webp

Some icons are just plain old HTML form controls:

git_butler@2x.webp

Some are stylized:

by_bee@2x.webp
Thanks @bee for the picture

Sometimes people will get creative to achieve perfect alignment:

github_close@2x.webp

But overall it’s a pretty hopeless game:

apple_id@2x.webp

The problem is, CSS doesn’t help us either. There are 13 possible values for the `vertical-align` property, but none would align the icon in a meaningful way:

text_align@2x.png

`text-align: middle` comes closest, but it aligns by x-height, not cap-height, which still looks unbalanced:

middle@2x.webp

That’s exactly why people love web programming so much. There’s always a challenge.

# Icon fonts

Aligning rectangles is relatively easy. Aligning text is hard. Icons are rectangles. So what if we put icons into a font file?

Now we can’t align anything:

icon_fonts@2x.webp

Neither can we set icon size! In the example above, all icons were set to the same font size and line height. As you can see, all of them come out different sizes, with different paddings, and none were properly aligned.

Despite many shortcomings and almost no upsides, companies rushed to add icon fonts everywhere. The result is this:

calculators@2x.png
macOS 10.14 → macOS 10.15

Notice how operators are not vertically aligned anymore and are also blurry. All because of switching to icon font.

Apple was so committed to icon fonts they even ruined the QuickTime record button:

quicktime@2x.webp

Just look at it:

quicktime_button@2x.webp

Yes, it actually looks like this to this day. As does the calculator.

But they are far from being the only ones. One:

icon_1@2x.webp

Two:

icon_3@2x.webp

Three:

icon_4@2x.webp

Four:

icon_5@2x.webp

Five:

icon_6@2x.webp

Six:

icon_7@2x.webp

Seven:

icon_8@2x.webp

Same as with text alignment, there’s an endless supply of poorly aligned icons.

# Skill issue

Not only programmers fail to center things. Designers do it, too:

things@2x.webp
[Current version](https://culturedcode.com/things/blog/2024/02/things-for-apple-vision-pro/) / my fix

The problem with icons is that sometimes you have to take their shape into account for things to look good:

apple_logo@2x.webp
Bad centering / good centering

Triangle is notably tricky:

triangle@2x.webp

Sometimes it is too far to the left:

triangle_left@2x.webp

Sometimes it’s too far to the right:

triangle_right@2x.webp

It can even be too high up (line-height strikes again):

triangle_up@2x.webp

# Horizontal centering

You might think that only centering things vertically is hard. Not only! Horizontal might be hard, too:

apple_sign_in_business@2x.webp

I don’t think there’s a deep reason for these, except for people just being sloppy:

twitter_horizontal@2x.webp

Just, come on!

android@2x.webp

Can this be a deliberate decision?

teams@2x.png

I don’t know. Icons can suffer from it, too:

drive@2x.webp

As can text:

steam_horizontal@2x.webp

# What can be done: designers

So what _is_ the problem?

It all starts with the font. Right now, the bounding box of a text block looks like this:

text_bounding_box@2x.png

The problem is, it can also look like this:

text_bounding_box_2@2x.png

or this:

text_bounding_box_3@2x.png

Now, what will happen if you try to center text by centering its bounding box?

text_bounding_box_4.png

The text will be off! Even though rectangles are perfectly centered.

But even if font _can_ have its metrics unbalanced, it doesn’t mean it does. What happens in reality?

In reality, _most_ of the popular fonts have metrics slightly off. Many have it _significantly_ off:

metrics@2x.png
Percentages are of cap-height

10% is not a small number. It’s a whole pixel in font size 13! Two, if you have 2× scaling! It’s easily noticeable.

Basically, Segoe UI is the reason why Github on Windows looks like this:

github@2x.webp

The solution is simple: make tight bounding boxes and centering will become trivial:

text_bounding_box_5.png

If you use Figma, it already can do this (although it’s not the default):

figma_vertical_trim@2x.png

# What can be done: font designers

If you are a font designer, make life easier for everybody by setting your metrics so that `ascender − cap-height = descender`:

font_metrics_numbers@2x.png

Or the same idea, visually:

font_metrics@2x.png

Important! You don’t have to _actually_ extend your ascenders/descenders to these boundaries. As you can see in the picture, my ascender space, for example, is way underutilized. Just make the numbers match.

For both web and native, to avoid headaches, choose a font that already follows this rule. SF Pro Text, Inter, and Martian Mono seem to do this already, so they will center perfectly with no extra effort.

See [Font size is useless; let’s fix it](https://tonsky.me/blog/font-size/) for more information.

# What can be done: web developers

From the developer side, it’s a bit more tricky.

The first thing to understand, you need to know which font you’ll be using. Unfortunately, this doesn’t work if you plan to substitute fonts.

We’ll use IBM Plex Sans, a font used on this very page. IBM Plex Sans has the following metrics:

ibm_plex_sans@2x.png

When you set `font-size`, what you set is UPM (this will also be equal to `1em`). However, the actual space occupied by the text block is the space between the ascender and descender.

ibm_plex_sans_notes@2x.png

With a few simple calculations, we get that extra `padding-bottom: 0.052em` should do the trick:

numi@2x.webp

Should work like this:

ibm_plex_sans_padding@2x.png

Or in actual CSS (select text to see default text bounding box):

<p>
    <span style="display: inline-block; font-size: 147px; background: #FFF; line-height: calc(2.094em - 0.052em); padding-bottom: 0.052em; width: 100%; text-align: center;">Andy</span>
</p>

You can get the required font metrics for your font from https://opentype.js.org/font-inspector.html (ascender, descender, sCapHeight).

Now that we have that sorted, aligning icons is not that hard too. You set `vertical-align: baseline` and then move them down by `(iconHeight - capHeight) / 2`:

ibm_plex_sans_icon@2x.png

This, unfortunately, requires you to know both font metrics and icon size. But hey, at least it works:

<p>
    <span style="display: inline-block; font-size: 147px; background: #FFF; line-height: calc(2.094em - 0.052em); padding-bottom: 0.052em; width: 100%; text-align: center;">
    <span style="display: inline-block; width: 147px; height: 147px; background: #FFB4FC; position: relative; top: calc((147px - 0.698em) / 2); "></span>
    Andy
    </span>
</p>

Again, select the text above to see how different the browser’s bounding box is from the correct position.

# What can be done: icons fonts

STOP.

USING.

FONTS.

FOR.

ICONS.

Use normal image format. The one with dimensions, you know? Width and height?

Here, I drew a diagram for you, to help you make a decision:

diagram@2x.png

Just look at how hard Apple tries to put the checkmark inside the rectangle, and the rectangle next to the text label:

apple_sign_in@2x.webp

And they still fail!

Nothing is easier than aligning two rectangles. Nothing is harder than trying to align text that has an arbitrary amount of empty space around it.

This is a game that can’t be won.

# What can be done: optical compensations

We, developers, can only mathematically align perfect rectangles. So for anything that requires manual compensation, please wrap it in a big enough rectangle and visually balance your icon inside:

icons_baked@2x.png

# What can be done: everyone

Please pay attention. Please care. Bad centering can ruin otherwise decent UI:

win@2x.webp

But a properly aligned text can make your UI sing:

win_fix@2x.webp

Even if it’s hard. Even if tools make it inconvenient. Even if you have to search for solutions. Together, I trust, we can find our way back to putting one rectangle inside another rectangle without messing it up.

I, for one, want to live in a world of beautiful well-balanced UIs. I trust that you do, too. 

It’s all worth it in the end.

# Honorable mention

Our article would be incomplete without this guy:

spinner.mp4

Take care!