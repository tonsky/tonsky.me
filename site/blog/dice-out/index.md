---
layout: post
title: "GMTK Game Jam 2022: Dice Out"
category: blog
summary: "Experience report from participating in latest GMTK Game Jam"
published: 2022-07-26
---

I’ve made a game! Well, a small one and in just 48 hours, but still a game.

You can play it in the browser:

cover.png https://tonsky.itch.io/dice-out

Here are my observations.

## Game engine

Using a game engine helps a lot. It solves a lot of problems that you don’t want to deal with in 48 hours or, probably, never.

I chose Godot and liked it a lot: Python-like language seems like a better fit for casual scripting than C#, and apparently it has much better HTML export than Unity.

A game engine also gives you a visual editor for many resources you’ll need for a game: tilesets, sprites, animations, levels, and state machines. You don’t want to tune all that in JSON.

## Game programming

Game programming is quite different from standard web- or whatever type of programming we do.

In games they prefer ECS over OOP (data and behavior separation, components instead of inheritance), encourage global mutable state, like simple solutions, and dislike abstractions.

## Art pipeline

Figma feels a great fit for 2D sprites because of the components. In the case of our dice, I designed dots and faces separately and then combined them into dice. When I wanted to tune colors or shapes, I changed them in one place and they updated everywhere.

figma.png

Feels like programming!

## Characters

Any game is better with a character. We made a pretty abstract roll-the-dice-in-a-labyrinth game which by itself would be inherently boring:

boring.png

That’s why we introduced Gnargle! Look how he follows the dice with his eyes:

fun.mp4

Much more lively, isn’t it?

## Juice

Juice are small effects that add the feeling of “impact” to the actions. In the example above, it’s a puff of smoke after the dice, a little jump, screen shake, and dynamic camera position.

Compare it to the version without all that:

no_juice.mp4

Still functional, but much less fun.

## Level design (spoilers!)

I’m pretty proud of our level design. Each level tries to explore an interesting property that’s inherent to the dice as a mathematical object.

I’m going to go through each level explaining its purpose, so if you want first to experience it yourself, open our <a href="https://tonsky.itch.io/dice-out" target="_blank">Itch.io page</a> and play the game. It shouldn’t take you more than 10-15 minutes.

### Level 1

A pretty standard tutorial level that explains how the game works and what your goal is:

level1.png

On top of that, it shows that returning dice exactly the same way changes nothing, or that any path is completely reversible.

### Level 2

Still tutorial, it demonstrates that if you take another path you might get a different result. It also shows that the solution from level 1 won’t work, so you have to adapt:

level2.png

### Level 3

This teaches an interesting property: rotation around one vertex.

level3.mp4

Notice how 1 travels from the top left to bottom left to bottom right. That’s because only three faces are touching one vertex which makes a cycle of three (1, 2, and 3 in that case). But there are four tiles on the board, so using this rotation you can move any top-facing face to any of the four squares you want.

### Level 4

This is pretty much an extended version of level 3, but now, given more than four tiles, you can get faces that weren’t previously possible:

level4.png

### Level 5

This was supposed to demonstrate that now that you can get any face with 3-by-3 tiles, you can get them anywhere. This is the level how it went to the game:

level5.png

But after the release I thought of a better way to demonstrate the point:

level5_better.png

### Level 6

This is a tiny aha moment (hopefully). You thought your goal is to move your dice somewhere, but here’s no goal. How come?

level6.png

Of course, because the target tile is under your starting position!

### Level 7

To be honest, designing puzzles for this wasn’t easy because you can get absolutely anything given 3x3 and almost anything given 2x2, as we’ve explored before.

level7.png

So in this case you don’t get a single 2x2 configuration and instead get a cycle that you need to run multiple times to get the dice in the correct position.

### Level 8

This is in some sense a re-iteration of level 5, again illustrating that you can pretty much get anything anywhere.

level8.png

The gotcha in this one is, I guess, that an easy-looking tile configuration doesn’t mean the solution will be equally straightforward.

### Level 9

Finally, level 9, which could be best described with the video:

level9.mp4

Again, this is a non-verbal joke that is communicated entirely through level design.

But beyond that, the missing tile is located so inconveniently that players will (hopefully) figure out a way to predict the value they will get there (which is quite simple, because going in a straight line is a simple 4-cycle).

## Conclusion

Thanks for reading and I hope you liked the game! I for sure had a lot of fun!
