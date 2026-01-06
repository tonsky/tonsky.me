---
title: "It’s hard to justify Tahoe icons"
summary: "Looking at the first principles of icon design—and how Apple failed to apply all of them in macOS Tahoe"
supercover: cover.webp
og_facebook: open_graph_facebook_smol.png
og_twitter: open_graph_twitter_smol.png
published: 2026-01-05
hackernews_id: 46497712
lobsters_url: https://lobste.rs/s/2gvk2r/it_s_hard_justify_tahoe_icons
starred: true
---

I was reading Macintosh Human Interface Guidelines [from 1992](https://dn721903.ca.archive.org/0/items/apple-hig/Macintosh_HIG_1992.pdf) and found this nice illustration:

hig_icons@2x.webp

accompanied by explanation:

hig_quote@2x.webp

Fast forward to 2025. Apple releases macOS Tahoe. Main attraction? Adding unpleasant, distracting, illegible, messy, cluttered, confusing, frustrating icons (their words, not mine!) to every menu item:

sequoia_tahoe_textedit@2x.webp
Sequoia → Tahoe

It’s bad. But why exactly is it bad? Let’s delve into it!

Disclaimer: screenshots are a mix from macOS 26.1 and 26.2, taken   from stock Apple apps only that come pre-installed with the system. No system settings were modified.

# Icons should differentiate

The main function of an icon is to help you find what you are looking for faster.

Perhaps counter-intuitively, adding an icon to everything is exactly the wrong thing to do. To stand out, things need to be different. But if everything has an icon, nothing stands out.

The same applies to color: black-and-white icons look clean, but they don’t help you find things faster!

Microsoft used to know this:

.noround word@2x.webp https://tonsky.me

Look how much faster you can find Save or Share in the right variant:

menu_cleanup@2x.webp

It also looks cleaner. Less cluttered.

A colored version would be even better (clearer separation of text from icon, faster to find):

menu_cleanup_color@2x.webp

I know you won’t like how it looks. I don’t like it either. These icons are hard to work with. You’ll have to actually design for color to look nice. But the principle stands: it is way easier to use.

# Consistency between apps

If you want icons to work, they need to be _consistent_. I need to be able to learn what to look for.

For example, I see a “Cut” command and ![](scissors.svg) next to it. Okay, I think. Next time I’m looking for “Cut,” I might save some time and start looking for ![](scissors.svg) instead.

How is Tahoe doing on that front? I present to you: Fifty Shades of “New”:

menu_new@2x.webp

I even collected them all together, so the absurdity of the situation is more obvious.

icons_new@2x.webp

Granted, some of them are different operations, so they have different icons. I guess creating a smart folder is different from creating a journal entry. But this?

menu_new_object@2x.webp

Or this:

icons_new_smart_folder@2x.webp

Or this:

icons_new_window@2x.webp

There is no excuse.

Same deal with open:

menu_open@2x.webp

Save:

menu_save@2x.webp

Yes. One of them is a checkmark. And they can’t even agree on the direction of an arrow!

Close:

menu_close@2x.webp

Find (which is sometimes called Search, and sometimes Filter):

menu_find@2x.webp

Delete (from Cut-Copy-Paste-Delete fame):

menu_delete@2x.webp

Minimize window.

menu_minimize@2x.webp

These are not some obscure, unique operations. These are OS basics, these are foundational. Every app has them, and they are always in the same place. They shouldn’t look different!

# Consistency inside the same app

Icons are also used in toolbars. Conceptually, operations in a toolbar are identical to operations called through the menu, and thus should use the same icons. That’s the simplest case to implement: inside the same app, often on the same screen. How hard can it be to stay consistent?

Preview:

preview@2x.webp

Photos: same ![](info_circle.svg) and ![](info.svg) mismatch, but reversed ¯\\\_(ツ)\_/¯

photos@2x.webp

Maps and others often use different symbols for zoom:

consistency_maps@2x.webp

# Icon reuse

Another cardinal sin is to use the same icon for different actions. Imagine: I have learned that ![](square_and_pencil.svg) means “New”:

new_note@2x.webp

Then I open an app and see![](square_and_pencil.svg). “Cool”, I think, “I already know what it means”:

edit_address@2x.webp

Gotcha!

You’d think: okay, ![](eye.svg) means quick look:

quick_look@2x.webp

Sometimes, sure. Some other times, ![](eye.svg) means “Show completed”:

show_completed@2x.webp

Sometimes ![](square_and_arrow_down.svg) is “Import”:

import@2x.webp

Sometimes ![](square_and_arrow_down.svg) is “Updates”:

update@2x.webp

Same as with consistency, icon reuse doesn’t only happen between apps. Sometimes you see ![](rectangle_pencil_ellipsis.svg) in a toolbar:

form_filling_toolbar@2x.webp

Then go to the menu _in the same app_ and see ![](rectangle_pencil_ellipsis.svg) means something else:

autofill@2x.webp

Sometimes identical icons meet in the same menu.

save_export@2x.webp

Sometimes next to each other.

passwords@2x.webp

Sometimes they put an entire barrage of identical icons in a row:

photos_export@2x.webp

This doesn’t help anyone. No user will find a menu item faster or will understand the function better if all icons are the same.

The worst case of icon reuse so far has been the Photos app:

photos_copy@2x.webp

It feels like the person tasked with choosing a unique icon for every menu item just ran out of ideas.

Understandable.

# Too much nuance

When looking at icons, we usually allow for slight differences in execution. That lets us, for example, understand that these _technically different_ road signs mean the same thing:

pedestrians.webp

Same applies for icons: if you draw an arrow going out of the box in one place and also an arrow and the box but at a slightly different angle, or with different stroke width, or make one filled, we will understand them as meaning the same thing.

Like, ![](info_circle.svg) is supposed to mean something else from ![](info_circle_fill.svg)? Come on!

similar_i@2x.webp

Or two letters A that only slightly differ in the font size:

similar_font_size@2x.webp

A pencil is “Rename” but a slightly thicker pencil is “Highlight”?

similar_pencil@2x.webp

Arrows that use different diagonals?

similar_actual_size@2x.webp

Three dots occupying ⅔ of space vs three dots occupying everything. Seriously?

similar_sidebar@2x.webp

Slightly darker dots?

similar_quality@2x.webp

The sheet of paper that changes meaning depending on if its corner is folded or if there are lines inside?

similar_sheet@2x.webp

But the final boss are arrows. They are all different:

similar_arrows@2x.webp

Supposedly, a user must become an expert at noticing how squished the circle is, if it starts top to right or bottom to right, and how far the arrow’s end goes.

Do I care? Honestly, no. I could’ve given it a shot, maybe, if Apple applied these consistently. But Apple considers ![](square_and_pencil.svg) and ![](plus.svg) to mean the same thing in one place, and expects me to notice minute details like this in another?

Sorry, I can’t trust you. Not after everything I’ve seen.

# Detalization

Icons are supposed to be easily recognizable from a distance. Every icon designer knows: small details are no-go. You can have them sometimes, maybe, for aesthetic purposes, but you can’t _rely_ on them.

And icons in Tahoe menus are _tiny_. Most of them fit in a 12×12 pixel square (actual resolution is 24×24 because of Retina), and because many of them are not square, one dimension is usually even less than 12.

It’s not a lot of space to work with! Even Windows 95 had 16×16 icons. If we take the typical DPI of that era at 72 dots per inch, we get a physical icon size of 0.22 inches (5.6 mm). On a modern MacBook Pro with 254 DPI, Tahoe’s 24×24 icons are 0.09 inches (2.4 mm). Sure, 24 is bigger than 16, but in reality, these icons’ area is 4 times as small!

dpi_comparison@2x.webp
Simulated physical size comparison between 16×16 at 72 DPI (left) and 24×24 at 254 DPI (right)

So when I see this:

details_zoom@2x.webp

I struggle. I can tell they are different. But I definitely struggle to tell what’s being drawn.

Even zoomed in 20×, it’s still a mess:

details_zoomed@2x.webp

Or here. These are three different icons:

details_lists@2x.webp

Am I supposed to tell plus sign from sparkle here?

details_sparkle@2x.webp

Some of these lines are half the pixel thicker than the other lines, and that’s supposed to be the main point:

details_redact@2x.webp

Is this supposed to be an arrow?

details_original@2x.webp

A paintbrush?

details_paste@2x.webp

Look, a tiny camera.

details_screenshot@2x.webp

It even got an even tinier viewfinder, which you can almost see if you zoom in 20×:

details_screenshot_zoomed@2x.webp

Or here. There is a box, inside that box is a circle, and inside it is a tiny letter `i` with a total height of 2 pixels:

details_properties@2x.webp

Don’t see it?

details_properties_zoomed@2x.webp

I don’t. But it’s there...

And this is a window! It even has traffic lights! How adorable:

details_window@2x.webp

Remember: these are retina pixels, ¼ of a real pixel. Steve Jobs himself claimed they were invisible.

> It turns out there’s a magic number right around 300 pixels per inch, that when you hold something around to 10 to 12 inches away from your eyes, is the limit of the human retina to differentiate the pixels.

And yet, Tahoe icons rely on you being able to see them.

# Pixel grid

When you have so little space to work with, every pixel matters. You can make a good icon, but you have to choose your pixels very carefully.

For Tahoe icons, Apple decided to use vector fonts instead of good old-fashioned bitmaps. It saves Apple resources—draw once, use everywhere. Any size, any display resolution, any font width.

But there’re downsides: fonts are hard to position vertically, their size [doesn’t map directly to pixels](https://tonsky.me/blog/font-size/), stroke width doesn’t map 1-to-1 to pixel grid, etc. So, they work everywhere, but they also look blurry and mediocre everywhere:

details_clean_up@2x.webp
Tahoe icon (left) and its pixel-aligned version (right).

They certainly start to work better once you give them more pixels.

ipad_comparison@2x.webp
iPad OS 26 vs macOS 26

or make graphics simpler. But the combination of small details and tiny icon size is deadly. So, until Apple releases MacBooks with 380+ DPI, unfortunately, we still have to care about the pixel grid.

# Confusing metaphors

Icons might serve another function: to help users understand the meaning of the command.

For example, once you know the context (move window), these icons explain what’s going on faster than words:

window@2x.webp

But for this to work, the user must understand what’s drawn on the icon. It must be a familiar object with a clear translation to computer action (like Trash can → Delete), a widely used symbol, or an easy-to-understand diagram. HIG:

hig_metaphor@2x.webp

A rookie mistake would be to misrepresent the object. For example, this is how selection looks like:

metaphor_selection@2x.webp

But its icon looks like this:

metaphor_select@2x.webp

Honestly, I’ve been writing this essay for a week, and I still have zero ideas why it looks like that. There’s an object that looks like this, but it’s a text block in Freeform/Preview:

metaphor_text_block@2x.webp

It’s called `character.textbox` in SF Symbols:

character_textbox@2x.webp

Why did it become a metaphor for “Select all”? My best guess is it’s a mistake.

Another place uses text selection from iOS as a metaphor. On a Mac!

metaphor_text_selection@2x.webp

Some concepts have obvious or well-established metaphors. In that case, it’s a mistake not to use them. For example, bookmarks: ![](bookmark.svg). Apple, for some reason, went with a book:

metaphor_bookmarks@2x.webp

Sometimes you already have an interface element and can use it for an icon. However, try not to confuse your users. Dots in a rectangle look like password input, not permissions:

metaphor_permissions@2x.webp

Icon here says “Check” but the action is “Uncheck”.

metaphor_mark_incomplete@2x.webp

Terrible mistake: icon doesn’t help, it actively confuses the user.

It’s also tempting to construct a two-level icon: an object and some sort of indicator. Like, a checkbox and a cross, meaning “Delete checkbox”:

metaphor_mark_unchecked@2x.webp

Or a user and a checkmark, like “Check the user”:

metaphor_manage@2x.webp

Unfortunately, constructs like this rarely work. Users don’t build sentences from building blocks you provide; they have no desire to solve these puzzles.

Finding metaphors is hard. Nouns are easier than verbs, and menu items are mostly verbs. How does open look? Like an arrow pointing to the top right? Why?

metaphor_open@2x.webp

I’m not saying there’s an obvious metaphor for “Open” Apple missed. There isn’t. But that’s the point: if you can’t find a good metaphor, using no icon is better than using a bad, confusing, or nonsensical icon.

There’s a game I like to play to test the quality of the metaphor. Remove the labels and try to guess the meaning. Give it a try:

metaphor_guess@2x.webp

It’s delusional to think that there’s a good icon for every action if you think hard enough. There isn’t. It’s a lost battle from the start. No amount of money or “management decisions” is going to change that. The problems are 100% self-inflicted.

All this being said, I gotta give Apple credit where credit is due. When they are good at choosing metaphors, they are good:

metaphor_up_down@2x.webp

# Symmetrical actions

A special case of a confusing metaphor is using different metaphors for actions that are direct opposites of one another. Like Undo/Redo, Open/Close, Left/Right.

It’s good when their icons use the same metaphor:

symmetry_import_export_right@2x.webp

Because it saves you time and cognitive resources. Learn one, get another one for free.

Because of that, it’s a mistake not to use common metaphors for related actions:

symmetry_select@2x.webp

Or here:

symmetry_clipboard@2x.webp

Another mistake is to create symmetry where there is none. “Back” and “See all”?

symmetry_app_store@2x.webp

Some menus in Tahoe make both mistakes. E.g. lack of symmetry between Show/Hide and false symmetry between completed/subtasks:

symmetry_eye@2x.webp

Import not mirrored by Export but by Share:

symmetry_import_export@2x.webp

# Text in icons

HIG again:

hig_text_icons@2x.webp

Authors of HIG are arguing against including text as a part of an icon. So something like this:

metaphor_select@2x.webp

or this:

similar_i@2x.webp

would not fly in 1992.

I agree, but Tahoe has more serious problems: icons consisting _only_ of text. Like this:

text_font@2x.webp

It’s unclear where “metaphorical, abstract icon text that is not supposed to be read literally” ends and actual text starts. They use the same font, the same color, so how am I supposed to differentiate? Icons just get in a way: A...Complete? AaFont? What does it mean?

I can maybe understand ![](textformat_characters_dottedunderline.svg) and ![](a_ellipsis.svg). Dots are supposed to represent something. I can imagine thinking that led to ![](aa.svg). But ![](textformat_characters.svg)? No decorations. No effects. Just plain Abc. Really?

# Text transformations

One might think that using icons to illustrate text transformations is a better idea.

Like, you look at this:

text_transformations@2x.webp

or this:

text_size@2x.webp

or this:

text_styles@2x.webp

and just from the icon alone understand what will happen with the text. Icon _illustrates_ the action.

Also, BIU are well-established in word processing, so all upside?

Not exactly. The problem is the same—text icon looks like text, not icon. Plus, these icons are _excessive_. What’s the point of taking the first letter and repeating it? The word “Bold” already starts with a letter “B”, it reads just as easily, so why double it? Look at it again:

text_styles@2x.webp

It’s also repeated once more as a shortcut...

There is a better way to design this menu:

text_styles_inline@2x.webp

And it was known to Apple for at least 33 years.

hig_style@2x.webp

# System elements in icons

Operating system, of course, uses some visual elements for its own purposes. Like window controls, resize handles, cursors, shortcuts, etc. It would be a mistake to use those in icons.

hig_standard_elements@2x.webp

Unfortunately, Apple fell into this trap, too. They reused arrows.

text_arrow@2x.webp

Key shortcuts:

text_encoding@2x.webp

HIG has an entire section on ellipsis specifically and how dangerous it is to use it anywhere else in the menu.

hig_ellipsis@2x.webp

And this exact problem is in Tahoe, too.

text_ellipsis@2x.webp

# Icons break scanning

Without icons, you can just scan the menu from top to bottom, reading only the first letters. Because they all align:

align_sequoia@2x.webp
macOS Sequoia

In Tahoe, though, some menu items have icons, some don’t, and they are aligned differently:

align_tahoe@2x.webp

Some items can have both checkmarks _and_ icons, or have only one of them, or have neither, so we get situations like this:

align_holes@2x.webp

Ugh.

# Special mention

This menu deserves its own category:

writing_direction@2x.webp

Same icon for different actions. Missing the obvious metaphor. Somehow making the first one slightly smaller than the second and third. Congratulations! It got it all.

# Is HIG still relevant?

I’ve been mentioning HIG a lot, and you might be wondering: is an interface manual from 1992 still relevant today? Haven’t computers changed so much that entirely new principles, designs, and idioms apply?

Yes and no. Of course, advice on how to adapt your icons to black-and-white displays is obsolete. But the principles—as long as they are good principles—still apply, because they are based on how humans work, not how computers work.

Humans don’t get a new release every year. Our memory doesn’t double. Our eyesight doesn’t become sharper. Attention works the same way it always has. Visual recognition, motor skills—all of this is exactly as it was in 1992.

So yeah, until we get a direct chip-to-brain interface, HIG will stay relevant.

# Conclusion

In my opinion, Apple took on an impossible task: to add an icon to every menu item. There are just not enough good metaphors to do something like that.

But even if there were, the premise itself is questionable: if everything has an icon, it doesn’t mean users will find what they are looking for faster.

And even if the premise was solid, I still wish I could say: they did the best they could, given the goal. But that’s not true either: they did a poor job consistently applying the metaphors and designing the icons themselves.

I hope this article would be helpful in avoiding common mistakes in icon design, which Apple managed to collect all in one OS release. I love computers, I love interfaces, I love visual communication. It makes me sad seeing perfectly good knowledge already accessible 30 years ago being completely ignored or thrown away today.

On the upside: it’s not that hard anymore to design better than Apple! Let’s drink to that. Happy New year!

smiley@2x.webp
From SF Symbols: a smiley face calling somebody on the phone

# Notes

During review of this post I was made familiar with [Jim Nielsen’s article](https://blog.jim-nielsen.com/2025/icons-in-menus/), which hits a lot of the same points as I do. I take that as a sign there’s some common truth behind our reasoning.

Also note: Safari → File menu got worse since 26.0. Used to have only 4 icons, now it’s 18!

Thanks Kevin, Ryan, and Nicki for reading drafts of this post.