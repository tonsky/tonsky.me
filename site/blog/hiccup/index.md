---
title: "Hiccup, Macros, API design, and magic"
summary: "Small changes in usage conditions could require full library redesign"
published: 2018-01-31
---

Every web developer ~~is unhappy in their own way~~ needs a way to generate HTML. 

In Clojure, the most popular approach is to use [Hiccup](http://github.com/weavejester/hiccup). Hiccup is a library that ditches this:

```
(str "<div id='timer'>"
       "Time is <a href='#' class='time'>"
       time
       "</a> sec"
     "</div>")
```

in favor of this:
 
```
(hiccup.core/html
  [:div#timer {}
    "Time is " [:a.time {:href "#"} time] " sec"])
```
    
Basically, it gives you convenience. It doesn’t change HTML semantics in any way. It’s an alternative, much more concise, Clojure-friendly syntax to write exactly the same thing.

_(Yes, on a grand scheme of things, more convenient syntax for HTML is like having three bullet holes in your back but taking an aspirin to treat a headache. It helps, sort of. But in practice, we all are stuck with HTML, so might at least take the aspirin. When I first tried it, I was surprised how much I actually like the thing, and I never wanted to go back to verbose HTML)_

## How Hiccup works

Naïve implementation would go over all those nested vectors, dispatch by children type, walk attributes map and convert all that to strings one-by-one, `clojure.string/join`-ing everything in the end.

Hiccup did something smarter. `hiccup.core/html` is a macro that analyzes the form inside and replaces everything that is known to be a constant at compile time with the compiled strings, at no runtime cost. `html` will convert most of your code inside into a huge string catenation:

```
(macroexpand 
 '(hiccup.core/html
    [:div#timer {}
      "Time is " [:a.time {:href "#"} time] " sec"]))

=> (clojure.core/str
     "<div"
     " id=\"timer\""
     ">"
     "Time is "
     "<a"
     " class=\"time\" href=\"#\""
     ">"
     ((var hiccup.compiler/render-html) time)
     "</a>"
     " sec"
     "</div>")
```

Great, right? We got almost constant performance at runtime almost for free (well, at the cost of James Reeves time). And it _is_ great. And smart. And it gives you a significant performance gain. The approach proved highly successful, and many libs copied it.

But let’s talk some limitations.

## Limitations

`hiccup.core/html` is a macro. It means it works with syntactic forms, not runtime values. It also means it cannot see inside function calls.

See that `render-html` call around `time`? It’s because the compiler has no idea what’s inside (string? more hiccup vectors?), so it has to _play safe_ by switching into interpreted (or runtime, or naïve) mode, losing all performance gains for that particular part of the code.

Another limitation comes from the fact that tags are just vectors, meaning there’s no way for Hiccup compiler to tell normal vectors from stuff we expect to be rendered as tags. It knows that end result should contain tags only, but it doesn’t mean every vector in a form is a tag. Consider this:

```
(hiccup.core/html
  (let [vec [:div "b" "c"]]
    [:div "Count " (count vec)]))
```

In that case, `[:div "Count " (count vec)]` should be converted to a tag, but `[:div "b" "c"]` shouldn’t. Again, the only option for compiler here is to play safe and only look at the actual return value at runtime, effectively falling back to the naïve algorithm.

Short summary. Pre-compilation in a macro could give you significant performance gains, but only in certain cases, and compiler _has to_ play it safe most of the time to stay correct. Not perfect, but still better than always taking the slow path, right?

## Going to the browser

Let’s look at another library: [Sablono](https://github.com/r0man/sablono).

Sablono is a compiler for Hiccup-style markup that produces React elements instead of HTML string. The idea is exactly the same, and as far as I can tell, initial compiler code was copied over from Hiccup and adjusted as needed.

The difference is, Sablono is supposed to be used in the browser, rendering React user interfaces. That means interpreting many potentially complex, highly nested markups with lots of components at 60 frames per second. Also, remember that JS is significantly slower than JVM (at least 2×), browsers run on consumer devices, not high-end servers, and people have low tolerance for lags and delays in their UIs.

What does it change? Well, if for Hiccup not getting every bit of performance was a minor inconvenience, in UI it’s critical to get every possible bit of performance from code and not waste it unless it’s absolutely unavoidable.

It means that tables have turned: the compiler is not a performance optimization anymore. The compiler is a baseline now, and runtime interpretation is a performance degradation and, essentially, a bug.

That makes `html` macro very inconvenient to use. You now have to somehow tell parts it could compile from parts that are opaque to it. And no, there’s no indication for it. Only your gut feeling and vague tribal knowledge passed from one frontend developer to the next. No guarantees either: next compiler version might “uncompile” some of your code if compiler author finds out that it was never safe to compile that particular form in the first place. That lead to a couple of “best practices” articles advising you where to wrap and where to double-wrap your tags in `html` macro to make sure everything will be as performant as possible.

Pretty fascinating how with a subtle context change the whole approach comes from totally fine to completely unacceptable. Both libraries _work the same_ and are required to _do exactly the same thing_, but conditions of their operation are slightly different, and that alone made entire API useless.

## Alternatives

Ok, what’s the solution?

One is to [remove runtime mode altogether](https://medium.com/@rauh/a-new-hiccup-compiler-for-clojurescript-8a7b63dc5128). It would work but still leaves you guessing where to put that `html` tag. Would the compiler be smart enough to compile that bit? Write, test, exception—nah, have to wrap it too.

It’s a working approach, but I personally don’t like the “guessing” part and the feeling of uncertainty it creates. Also: what if a new release of the compiler changes the formula and somewhere deep inside your app some rarely-seen dialogue will silently stop working? Seems like a time bomb waiting to explode.

Another one is to explicitly mark tags. For example, in Om:

```
(om.dom/div nil
  (om.dom/h3 nil (str "Props: " props))
  (om.dom/h3 nil (str "Shared: " shared))
  (om.dom/button #js {:onClick #(...)}
    "Increment!")))))
```

Each tag has to be wrapped in its own function or macro. This is the most straightforward, reliable, unsurprising approach there could be, and it only comes at the small cost of some extra typing, and at no performance cost at all.

## What have we learned?

- Macros aren’t magic.
- Small changes in usage conditions could lead to a full library redesign.
- There’s more to API than input, output and method names.
- Reliable first, predictable second, convenient last.


## One more thing

Since our topic today is subtle aspects of API design, let me suggest another questionable hypothesis.

The greatest gift of Hiccup syntax was its use of square brackets instead of parentheses. You see, Clojure is all about parentheses, it’s full of them, they are everywhere. Clojure code _is_ parentheses. Square brackets for tags moved markup into another, separate information layer.

Having tags look different helped. _A lot_. It would be great if next markup solution kept that. So far I’m thinking reader tags for Rum v.2:

```
#rum/tag [:div#timer
            "Time is " #rum/tag [:span.time time] " sec"]
```

M? What do you think?