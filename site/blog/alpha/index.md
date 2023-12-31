---
title: "Cognitect, please stop adding alpha to your namespaces"
summary: "Why putting alpha in your project name does more harm than good"
published: 2020-07-31
---

After [Spec-ulation talk](https://www.youtube.com/watch?v=oyLBGkS5ICk) Cognitect started putting `alpha` in the namespaces and library names they produce. Specifically, `spec.alpha`, `alpha.spec`, `core.specs.alpha` (yes, they are all different libraries) and `tools.deps.alpha`. I think this practice does not solve any problems AND introduces unnecessary complications. In this post I hope to show why it is, hoping that nobody will follow Cognitect on this.

# Why?

The idea, as I understand it, was simple: breaking changes are bad, we shouldn’t be doing them, at the same time, how are we supposed to be doing breaking changes now? The answer was to create a separate namespace while things are in flux, change it all you want, and when things are finally stable, rename both the library and the namespace, removing the `alpha` part, and ask everyone use a library as if it was something completely new.

# The good

The good part of this solution, I think, was the idea that as long as you change namespace and library names, you introduce no conflict. The biggest problem of dependency hell is not breaking changes per se (just don’t upgrade until you are ready, right?). The problem happens when two of your dependencies transitively depend on different versions of the same library and those are incompatible. Since they both want to be, for example, `clojure.spec`, and runtime can only load one under that name, you must pick one, effectively breaking expectations for one of your libraries.

conflict.jpg

BUT if one of them would call itself `clojure.spec` and other would be `clojure.spec.alpha`, there would be no problem of them coexisting. Voilà! Conflict avoided, at the expense of slight runtime code repetition.

no_conflict.jpg

Moreover, Clojure is probably one of the few languages on the planet that can really pull a trick like this off simply because library and module interactions in Clojure mostly happen via standard maps and vectors, not custom library-defined types. If you try to load the same library under two different names in Java, pretty soon you’ll be facing something like `ClassCastException: Expected Request, got: Request`.

# The bad

Now to the problems. I expected the idea of adding `alpha` was to discourage people from using the library until it’s final. Well, I don’t think it works as simple as that. If I need some functionality and your library provides it but has an `alpha` label, what am I supposed to do? Wait multiple years until it’s promoted? Clone it and put it on Clojars under a different name? Not write my software at all? I don’t think so. In my experience, you only bring in a library if you really, really, really (and I mean—REALLY) need some problem solved. At that point, you don’t care what version or status the library has, because the alternative is just to stop doing what you were doing completely. The bottom line is: you put software out, if people find it useful, they will use it. No matter the label. Remember Gmail Beta? Beta label was de facto a joke.

The next problem is that `alpha` status does not really solve any problems people might have with dependencies. It legitimizes them in the eyes of the maintainer, frees them from responsibility, but from the user’s point of view, the problems are still there. Someone might still depend on two different `alpha` versions and they still might conflict. The only thing that IS different is that maintainer now has saved themselves a right to say “it’s not my problem” if that happens.

The third problem is, there is usually no good point to graduate. How do you know that project is finalized and no future breaking changes will be needed? You don’t! Last time I checked, nobody can really tell the future. You might publish the perfect version the first time, everyone starts using it, you wait a couple of years and... what? Remove alpha, forcing a breaking change on everyone, without changing a line of code? Or you publish an alpha, work on it a little, everyone seems happy. How long are you supposed to wait before you make it final and remove the alpha? A month? A year? Three years? And what happens if you remove the alpha and then MUST make a breaking change?

The fourth problem is not really related to the `alpha` approach per se, but to where it is used. One place is `clojure.spec`, which Clojure itself depends on. So essentially Clojure _forces_ a particular version of alpha software on everyone programming in Clojure. Does it give Clojure itself an alpha status? I don’t know. The other project is `tools.deps.alpha`, which is a basis for the `clj` command-line utility and `deps.edn` dependency resolution, both being official ways to run Clojure projects and [promoted on the clojure.org](https://clojure.org/reference/deps_and_cli) site itself. If you insist on it being alpha and experimental, is it a good idea to teach it to newcomers?

# Alternative

What do I propose? Simple! Release the first version as e.g. `clojure.spec`. You can write ALPHA and DO NOT USE all over README if it makes you sleep better at night. Then, when you introduce a breaking change or decide to start over, just release a new namespace and a new artifact. For example, call it `clojure.spec2` (but please, not `clojure.spec.2`, more than two levels of nesting are... hard to work with). In the future, maybe even if it has been super-stable for years, a need for another breaking change might appear. Well, just bump it again to `clojure.spec3`! And so on.

Upsides:

- do not need to predict the future,
- avoid dependency conflict even for alpha users,
- don’t be stuck in alpha forever.

Downsides:

- Same as with the `alpha` approach.

I wish such an approach was incorporated deeply in Maven and Clojure itself, that two major versions could co-exist freely and you specify the major version on import. Until then, just giving them different names works too.

# Conclusion

I love Clojure, I really like Cognitect (even though it’s a division of Nubank these days) as the maintainers of it, I think they are doing a great job and I am eternally grateful for it. I mean no disrespect and hope that what I wrote up above sounds rational and calls to reason only, and could be incorporated into the future Clojure projects they release to a great effect. I believe we all can benefit from it.

And if you are not working on Clojure itself, I hope my post will warn you off from using this suboptimal practice. 

Thanks for reading, and see you next time!