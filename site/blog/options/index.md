---
title: "You don’t want many options"
summary: "Don’t get excited about libraries that offer many options"
published: 2017-09-17
---

A nice little library came up the other day: [`cprop`](https://github.com/tolitius/cprop). Simply put, it’s a swiss army knife of configuration management: it can load configs from classpath, files, DB or environment, it merges configs, provides defaults, does type coercion etc.

Should you use it? It’s easy to say yes, but I say no.

## “Yes” path

Choosing something like `cprop` is usually a no-brainer. After all, you need _something_ to manage configs, and library like that is obviously future-proof. If you don’t need everything at the start, you might decide to use it _just_ for simple things like port numbers and _only_ load them from the environment and nothing more. Using existing library is still better than rolling your own solution.

But projects are long endeavors. Somewhere down the road, you might have a situation: a demand will come up that is most easily addressed by using some other part of `cprop`. What would you do?

Let’s see: `cprop` is already in the project, it has the perfect solution, its authors already made all the design decisions and implemented all the code which is just sitting there, waiting, ready to be used. Using more of `cprop` _at that point_ will be the absolutely rational, most effective decision you can make.

The downside though is that eventually you’ll end up with a really complex configuration system. It’s nobody’s fault, it’s just how project dynamics work.

## “No” path

Alternatively, if you choose a much more focused library or decided to roll your own minimal solution, when that demand comes up there’ll be nothing to address it.

This moment is important: you’ll realize that making config more complicated is _not free_ anymore. Because of that, you might decide not to extend your configuration system but build a workaround or introduce a convention. Your config might, as a result, stay simple.

This is backpressure: your home-grown configuration system _resists_ being extended, and _it’s a good thing_. It keeps you from turning everything into a complicated mess or at least postpones that moment. Value it. Look for ways to restrain complexity creep and don’t get excited about the plethora of options.
