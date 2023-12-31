---
title: "Designing good DSL"
summary: "A look at common mistakes in DSL designs and how to fix them"
published: 2018-07-16
---

DSLs are great tool to reduce complexity and define problems in a compact and succinct way. In case you need to design your own, these are a few common traps to avoid.

## Make it verbose

Most well-known example of non-verbose DSL is regular expressions:

```
([A-Z][a-z]*[,.!?]\s+)*
```

Even though regular expressions are quite popular, it’s not a good design to copy.

First, most of its syntax beyond the very basics like `"X+"` or `"[^X]"` is impossible to remember. It’d be nice to know what `"(?<!X)"` does without having to look it up first.

Second: how do I google something like that?

And third: it’s really hard to read. There are no clear boundaries, so it’s hard to understand what is part of what. I always struggle even on simple cases like `"https?"` (it’s either `"http"` or `"https"` but it’s really hard to see that at first sight). And when things get complex—and they do get complex quick—you’ll spend more time parsing regex in your head than you would spend writing it anew. I mean, try reading this:

```
[a-z]{3,10}://([^/?#]*)([^?#]*)(?:\?([^#]*))?(?:#(.*))?
```

There’s nothing tricky going on, but it’s way too hard to read than it needs to be. That’s why they call Perl write-only language.

Another non-verbose DSL example is Java date and time format string:

```
"YYYY-MM-DD'T'HH:mm:ss.SSSZ"
```

More readable (the domain is way simpler) but also less widespread, so you still have to look it up every time. Not a problem while writing, but consider this: you just walking by that code and want to make simple modification (change short month to full month) or even fool-proof that it uses padded 24-hour for hours. Now, is `"HH"` 24-hour format or 12-hour? Is `"MM"` the format of month you really want here? Is it even a month, or minutes? It’s all still write-only and ungooglable.

Things get worse if we move to Clojure land. Clojure started as a language that assigned contextual meaning to a few language-defined syntax structures: vector might be a let-binding in `let` expression, function arguments list in `fn` and just a data structure. Again, more or less fine in a language because language is the same for all its users, finite and fixed. Learn it once and move along (still, mature developers keep discovering that e.g. `case` expression treats lists specially etc).

It got worse when library authors treated it as a design guideline. 
E.g. Datomic rules are specified like this:

```
[[(descendant? ?p ?c)
  [?p :parent ?c]]
 [(descendant? ?p ?c)
  [?p :parent ?x]
  (descendant? ?x ?c)]]
```

No words at all, just three-level deep mix of lists and vectors. If you don’t a priori know what’s going on it’s hard to even formulate a question about THAT. Also, as a user, I have to admit it’s really annoying placing all these brackets and parents right every time.

Core.async has the same problem. Here’s how you specify get operation in `alt!`:

```
(alt! [c t] ...)
```

And this is put (value to channel):

```
(alt! [[c v]] ...)
```

I mean, this doesn’t correspond to anything in core language or even in core.async itself! It’s just a structure-based syntax for the sake of structure-basedness. Strange that the very same `alt!` has named options—compare how clearer those are!

```
(alt! [c t]     ...
      [[c v]]   ...
      :default  nil
      :priority true) 
```

How to solve? Simple:

> Always give alternative options/behaviors/branches long, descriptive names.

E.g. core.match does this right:

```
(match [x]
  ([1 2] :seq)    ...
  (:or 1 2 3)     ...
  (_ :guard odd?) ...
  :else           ...)
```

Notice those `:seq` `:or` and `:guard`. Those are the explicit words marking different behaviour, not some implicit shape-defined structure.

Clojure.spec does good job too. It still uses short non-descriptive `+` and `?` (those are borrowed from regexes) but rest is perfectly readable and googlable words: `alt, cat, keys, req, coll-of, map-of, every, tuple`:

```
(spec/alt
 :arity-1 ::args+body
 :arity-n (spec/cat
           :bodies (spec/+ (spec/spec ::args+body))
           :attr   (spec/? map?)))
```

This is how datetime formatting could be done, aided by this principle (example from [Tongue](https://github.com/tonsky/tongue)):

```
{hour12}:{minutes-padded}{dayperiod} {month-numeric}/{day}/ {year-2digit}
```

Verbose, yes, but why would you care about just a few more characters here, when readability is at stake? By making it verbose, even without any prior experience with Tongue, anyone can fool-proof that this code will output something like `"1:05PM 7/13/18"` and not `"13:5PM 07/13/2018"`. Or even add 0-padding to hours, day and month and maybe even change 12-hour to 24-hour format if needed. Without even looking at the docs! Can you say the same about `"H:mm a d/M/y"`?

As for regexes, there’re [VerbalExpressions](https://github.com/VerbalExpressions):

```
var tester = VerEx()
    .startOfLine()
    .then('http')
    .maybe('s')
    .then('://')
    .maybe('www.')
    .anythingBut(' ')
    .endOfLine();
```

## Don’t invent second syntax

Sometimes your DSL starts simple and then you figure there’s no way to handle complex cases. So you extend it with advanced ways to do stuff. E.g. Datomic Pull syntax let you simply list attributes you need, attribute = keyword:

```
[:artist/name :artist/gid ...]
```

But that way there’s no way to specify additional options: nesting, limits, etc. So even though attributes started as keywords in a list, sometimes they might be specified as a map:

```
{:artist/tracks [:track/name :track/gid ...]}
```

Or a list can be used instead of keyword:

```
(:artist/endYear :default "N/A")
```

Now even I am confused how to combine default and nesting in a single expression.

Sometimes you start with all-powerful solution and then figure it’s really too much writing for simple cases. So you add shortcuts. E.g. Datomic query let you shorthand this:

```
(d/q {:find  [?a]
      :where [[?a :artist/name "Beatles"]]})
```

to this (less brackets):

```
(d/q [:find  ?a
      :where [?a :artist/name "Beatles"]])
```

Both ways, now your language has two+ ways of saying the same thing.

Having more than one way to do something is not a virtue, it’s a curse. Your users now have to learn two syntaxes. They need twice as much examples. Answers found on internet might not work because they use different syntax, etc.

My suggestion:

> One way to do it + an escape hatch.

You cover most of your users’ needs (that’s the primary value of your DSL anyway) and let users figure the rest in plain old code—best of both worlds.

## Don’t be too liberal

Almost the same as the previous one, but on a smaller scale. Sometimes DSLs let you get away with small variations: e.g. in Datomic you can specify `default` and `limit` as either keyword, symbol or a string—all are fine (apparently, you can even change order):

```
(:artist/tracks :limit 10)
(:artist/tracks "limit" 10)
(limit :artist/tracks 10)
```

Hiccup lets you drop empty attributes map:

```
[:div {} "Hello, world"]
[:div "Hello, world"]
```

Core.match lets you drop vector in case you’re matching a single value:

```
(match [x]
  [:a] ...
  :a   ...) // the same
```

The thing with special cases, same as with multiple syntaxes, is that you have to learn them, remember about them and recognize them. That’s a problem: it eats up cognitive resources better spent elsewhere.

Also, people in your/other teams will have _preferences_ or would simply not care about consistency. Hence:

> The only way to force everybody to always do the same thing is to ban any variations.

Being DSL designer, it’s your responsibility alone.

## Keep it small

Rule of thumb:

> Your DSL should be entirely memorizable.

That makes using it quick (once you’ve learned it), and this is where the value comes from. If you don’t use it too often or there’s too much syntax, you’ll be required to look at documentation each time before using or reading it. That slows things down, making using DSL an effort and eventually leading to DSL being replaced with a simpler solution.

## Don’t try to help too much

Many DSLs were designed to reduce amount of non-DSL code to the absolute zero. They try to help too much. Your stance here should be:

> Do in DSL what you know how to do well, leave rest for the users to figure out.

E.g. routing libraries might extract parameters but shouldn’t try to coerce them to other types—it’s really simple to do in your own code. Your users will have full programming language to handle those tricky/rare/exceptional cases.

Sometimes not doing stuff in DSL and leaving specific cases to users leads to less overall complexity.

## Conclusion

That’s more or less all I can think of right now. Designing DSLs is a fun and rewarding activity, but as with any design, it’s really hard to get right. I wish you luck on that tricky path, and let me know if it helps.