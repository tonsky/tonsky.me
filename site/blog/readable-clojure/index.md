---
title: "Readable Clojure"
summary: "Some advices to writing Clojure code"
published: 2017-05-24
hackernews_id: 14413872
reddit_url: "https://www.reddit.com/r/Clojure/comments/6d3w8i/readable_clojure/"
starred: true
---

This is how you can make Clojure code more pleasant to work with:

* <a href="#dont-use-use">Don’t use “use”</a>
* <a href="#use-consistent-unique-namespace-aliases">Use consistent, unique namespace aliases</a>
* <a href="#use-long-namespace-aliases">Use long namespace aliases</a>
* <a href="#choose-readabilityover-compactness">Choose readability over compactness</a>
* <a href="#dont-rely-on-implicit-nil-to-false-coercion">Don’t rely on implicit nil-to-false coercion</a>
* <a href="#avoid-higher-order-functions">Avoid higher-order functions</a>
* <a href="#dont-spare-names">Don’t spare names</a>
* <a href="#dont-use-firstsecondnth-to-unpack-tuples">Don’t use first/second/nth to unpack tuples</a>
* <a href="#dont-fall-for-expanded-opts">Don’t fall for expanded opts</a>
* <a href="#use--as-prefix-for-references">Use * as prefix for references</a>
* <a href="#align-let-bindings-in-two-columns">Align let bindings in two columns</a>
* <a href="#use-two-empty-lines-between-top-level-forms">Use two empty lines between top-level forms</a>

## Don’t use “use”

And don’t `:refer` anything either. Every var you bring from another namespace should have a namespace qualifier. Makes it easier to track vars to their source.

use.gif

You’ll also save yourself from name collisions down the way.

## Use consistent, unique namespace aliases

If you gave namespace an alias, stick to it. Don’t require

```
[clojure.string :as str]
```

in one file but

```
[clojure.string :as string]
```

in another.

That way you’ll be able to actually remember your aliases. Oh, and `grep` starts to work.

aliases_1.gif

Keep aliases unique too. If `d/entity` could mean both `datomic.api/entity` or `datascript.core/entity`, you lose all the benefits of this rule.

aliases_2.gif

## Use long namespace aliases

Aliases should be readable on their own. Therefore

- don’t use single-letter aliases,
- don’t be clever or inventive,
- when shortening, only remove the most obvious parts (company prefix, project name, `clojure.*`, `core` suffix etc),
- leave everything else intact:

long_aliases.gif

Now you can read your code starting from any place and don’t have to remember what maps to what. Compare

short_aliases_1.gif

with

short_aliases_2.gif

The former looks terser, but it’s hard to tell what’s going on. The latter is a bit longer but immediately communicates which systems are in play.

Another benefit: you’ll naturally tend to use aliases less often if they are long and clumsy, so long aliases will force you to organize your code better.

## Choose readability over compactness

Clojure has a plethora of ways to write dense code. Don’t use them just because they are there. Always put readability and code clarity first. Sometimes it means even going against Clojure idioms.

An example. To understand this piece of code you need to know that `possible-states` is a set:

set_1.gif

By contrast, to understand following code you don’t need any context:

set_2.gif

Also, notice how the latter reads almost like plain English.

My (incomplete) set of personal rules:

- use `contains?` instead of using sets as functions,
- use `get` instead of using map as a function,
- prefer `(not (empty? coll))` over `(seq coll)`,
- explicitly check for `nil?`/`some?` (more on that below).


## Don’t rely on implicit nil-to-false coercion

Unfortunately, Clojure mixes two very different domains: nil/existence checking and boolean operations. As a result, you have to constantly guess author’s intents because they’re not expressed explicitly in the code. 

I advise using real boolean expressions and predicates in all boolean contexts. Explicit checks are easier to read and communicate intent better. Compare implicit

nil_1.gif

and explicit

nil_2.gif

The more serious reason is that nil-as-false idiom fails when you want `false` to be a possible value.

nil_3.gif

Problems like this are common when working with boolean attributes/parameters and default values.

Some advice to follow:

- wrap plain objects with `some?` in `when`/`if`,
- prefer `when-some`/`if-some` over `when-let`/`if-let`,
- be careful with `or` when choosing a first non-nil value,
- for `filter`/`remove` predicates, provide proper boolean values through `some?`/`nil?`.


## Avoid higher-order functions

I found code that builds functions with `comp`, `partial`, `complement`, `every-pred`, `some-fn` to be hard to read. Mainly because it _looks different_ from the normal function calls: no parens, application order is different, you can’t see arguments.

It requires effort to figure out what exactly will happen:

fn_1.gif

Even as experienced Clojure programmer I haven’t developed a skill to parse such structures easily.

What I find easy to read, though, is anonymous function syntax. It looks exactly like a normal function call, you can see where parameters go, what’s applied after what — it’s instantly familiar:

fn_2.gif


## Don’t spare names

Some facilities in Clojure (threading macros, anonymous functions, destructuring, higher-order functions) were designed to let you skip _names_:

names_1.gif

This is great but sometimes impedes readability. Without names, you are forced to keep all the intermediate results in your head.

To avoid that, add meaningful names where they could be omitted:

names_2.gif

You _can_ omit names in threading macros (`->`, `->>` etc) but only if object/objects passed through do not change their type. Most cases of filtering, removing, modifying elements in a collection are fine.

E.g. here because it’s still users all the way until the end, intermediate names can be omitted:

names_3.gif


## Don’t use first/second/nth to unpack tuples

Although this works:

tuples_1.gif

you’re missing an opportunity to use destructuring to

* improve readability,
* assign names to tuple elements
* and show the shape of the data:

tuples_2.gif


## Don’t fall for expanded opts

The expanded opts idiom does only two things:

- it is extremely cool,
- and it saves you two curly brackets at the call site.

opts_1.gif

The downsides are much more serious. `start` will be extremely painful to call if you construct map of options dynamically or if you need to do it through `apply`:

opts_2.gif

Because of that, I recommend to always accept options as a map:

opts_3.gif

## Use * as prefix for references

References and their content are different, so they need different names. At the same time, they are not _that_ different to invent unique combination of names each time.

I suggest simple convention: prepend `*` (star) to reference names.

refs.gif

The star was chosen because it resembles C/C++ pointers.


## Align let bindings in two columns

Compare this:

let_1.gif

to this:

let_2.gif

I do it by hand, which I consider to be a small price for readability boost that big. I hope your autoformatter can live with that.


## Use two empty lines between top-level forms

Put two empty lines between functions. It’ll give them more space to breathe.

space.gif

Seems unimportant, but trust me: once you try it, you’ll never want to go back.

