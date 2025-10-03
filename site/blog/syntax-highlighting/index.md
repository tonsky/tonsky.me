---
title: "I am sorry, but everyone is getting syntax highlighting wrong"
summary: "Applying human ergonomics and design principles to syntax highlighting"
---

Syntax highlighting is a tool. It can help you read code faster. Find things quicker. Orient yourself in a large file.

Like any tool, it can be used correctly or incorrectly. Let’s see how to use syntax highlighting to help you work.

# Christmas Lights Diarrhea

Most color schemes highlight as much as they can. They literally have a unique bright color for everything: variables, language keywords, constants, punctuation, functions, classes, calls, comments.

Sometimes it gets so bad one can’t identify what the base text color is: everything is highlighted. What’s the base text color here?

diarrhea.webp

The problem with that is, well, if everything is highlighted, nothing stands out. Your eye adapts and considers it a new norm: everything is bright and shiny, and instead of getting separated, it all blends together.

Here’s a quick test. Try to find function definition here:

definitions_bad.webp

and here:

definitions_good.webp

I bet second one was faster, right? Even though color scheme is unfamiliar to you.

So yeah, unfortunatelly, you can’t just highlight everything. You have to make decisions: what’s most important, what’s less. What should stand out, what shouldn’t.

Highlighting everything is like assigning “top priority” to every task in Linear. It only works if most of the tasks have lesser priorities.

If everything is highlighted, nothing is highlighted.

# Enough colors to remember

There are two main use-cased you want your color scheme to address:

1. Look at something and tell what it is. By its color, not by its text. Text you can read without syntax highlighting.
2. Search for something. You want to know what to look for (which color).

1 is direct index lookup: color → type of thing.

2 is reverse lookup: type of thing → color.

Truth is, most people don’t do these lookups at all. They might think they do, but in reality they don’t.

Let me illustrate. Before:

change_before.webp

After:

change_after.webp

Can you see it? I misspelled `return` for `retunr` and its color switched from red to purple.

I can’t.

Here’s another test. Close your eyes (not yet! finish this sentence first) and try to remember, what color does your color scheme uses for class names?

Can you?

If the answer for both questions is “no”, then your color scheme is _not functional_. It might give you comfort (as in—I feel safe. If it’s highlighted, it’s probably code) but you can’t use it as a tool. You might as well use all black text on all white background.

What’s the solution? Have an absolute minimum of colors that can all fit in your head at once. For example, my color scheme, Alabaster, only uses four:

- Green for strings
- Purple for constants
- Yellow for comments
- Light blue for top-level definitions

That’s it! And I was able to type it all from memory, too. This minimalism allows me to actually do lookups: if I’m looking for a string, I know it will be green. If I’m looking at something yellow, I know it’s a comment.

.loud Limit number of different colors to what you can remember.

If you swap green and purple in my editor, it’ll be a catastrophe. If somebody swapped colors in yours, would you even notice?

# What should you highlight?

Something there isn’t a lot of.  Remember—we want highlights to stand out. That’s why I don’t highlight variables or function calls—they are everywhere, your code basically consist of them.

I do highlight constants (numbers, strings). These are usually used more sparingly and often are reference points—a lot of logic paths start from constants.

Punctuation: it helps to separate names from syntax a little bit, and you care about names first, especially when quickly scanning code.

Please, please don’t highlight language keywords. `class`, `function`, `if`, `else`, stuff like this. You rarely look for them: “where’s that if” is a valid question, but you will be looking not at the `if` the keyword, but at the condition after it. The condition is the important, distinguishing part. Keyword is not.

.loud Highlight names and constants. Grey out punctuation. Don’t highlight language keywords.

# Comments are important

The tradition of using grey for comments comes from the times when people were paid by line. If you have something like

javadoc.webp

of course you would want to grey it out! This is bullshit text that doesn’t add anything and was written to be ignored.

But for good comments, situation is opposite. Good comments ADD to the code. They explain something that couldn’t be expressed directly. They are _important_.

yellow_comments.webp

So here’s another controversial idea:

.loud Comments should be highlighted, not hidden away.

Use bold colors, draw attention to them. Don’t shy away. If somebody took time to tell you something, you might want to read it.

# Two types of comments

Another secret nobody is talking about, is that there are two types of comment:

1. Explanations
2. Temporarily disabled code

Most languages don’t distinguish between those, so there’s not much you can do syntax-wise. Sometimes there’s a convention (e.g. `--` vs `/* */` in SQL), then use it!

Here’s a real example from Clojure codebase that makes perfect use of two types of comments:

two_types_of_comments.webp

# Light or dark?

Per statistics, 70% of developers prefer dark schemes. Not me, so that question always puzzled me. Why?

And I think I have an answer. Here’s a typical dark scheme:

vscode_default_dark@2x.webp

and here’s a light one:

vscode_default_light@2x.webp

On the latter one, colors are way less vibrant. Here, I picked them out for you:

vscode_colors@2x.png
Notice how many colors there are. No one can remember that many.

This is because dark colors are in general less distinguashable and more muddy. Look at Hue scale as we move brightness down:

brightness_hue@2x.webp

Basically, in dark part of the spectrum, you just get less colors to play with. There’s no “dark yellow” or good looking “dark teal”.

Nothing can be done here. There are no magic colors hiding somewhere that have both good contrast on white background and look good at the same time. By choosing light scheme, you are dooming yourself to a very limited, bad-looking, barely distinguishable set of dark colors.

So it makes sense. Dark schemes do look better. Or rather: light ones can’t look good. Science ¯\\\_(ツ)\_/¯

But!

But.

There is one thing you can do. Use background colors! Compare:

bg_highlight@2x.png

First one has nice colors, but contrast is too low: letters become hard to read.

Second one has good contrast but you can barely see colors.

Last one has _both_: high contrast and clean, vibrant colors. Ligher colors are readable even on white background since they fill a lot more area. It’s all upside, really.

If your editor supports choosing background color, try it. It’s looks good.

# Bold and italics

Don’t use. This goes into the same category as too many colors. It’s just another way to highlight something, and you don’t need too many, because you can’t highlight everything.

In theory, you might try to _replace_ colors with typography. Would that work? I don’t know. I haven’t seen any examples.

typography.png
Using italics and bold instead of colors

# Myth of number-based perfection

Some schemes pay too much attention to be scientifically uniform. Like, all colors have same exact lightness, and hues are distributed evenly on a circle.

This could be nice (to know if you have OCR), but in practice, I find that it hurts more than helps. If you make your colors to 

# Let’s design color scheme together