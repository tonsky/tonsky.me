---
title: "When You Get to Be Smart Writing a Macro"
summary: "A story of writing Clojure’s println alternative that works inside threading-first/last macros"
published: 2025-05-01
---

Day-to-day programming isn’t always exciting. Most of the code we write is pretty straightforward: open a file, apply a function, commit a transaction, send JSON. Finding a problem that can be solved not the hard way, but smart way, is quite rare. I’m really happy I found this one.

I’ve been using [hashp](https://github.com/weavejester/hashp) for debugging for a long time. Think of it as a better `println`. Instead of writing

```
(println "x" x)
```

you write

```
#p x
```

It returns the original value, is shorter to write, and doesn’t add an extra level of parentheses. All good. It even prints original form, so you know which value came from where.

Under the hood, it’s basically:

```
(defn hashp [form]
  `(let [res# ~form]
     (println '~form res#)
     res#))
```

Nothing mind-blowing. It behaves like a macro but is substituted through a reader tag, so `defn` instead of `defmacro`.

Okay. Now for the fun stuff. What happens if I add it to a thread-first macro? Nothing good:

```
user=> (-> 1 inc inc #p (* 10) inc inc)
Syntax error macroexpanding clojure.core/let at (REPL:1:1).
(inc (inc 1)) - failed: vector? at: [:bindings] spec: :clojure.core.specs.alpha/bindings
```

Makes sense. Reader tags are expanded first, so it replaced `inc` with `(let [...] ...)` and _then_ tried to do threading. Wouldn’t fly.

We can invent a macro that would work, though:

```
(defn p->-impl [first-arg form fn & args]
  (let [res (apply fn first-arg args)]
    (println "#p->" form "=>" res)
    res))

(defn p-> [form]
  (list* 'p->-impl (list 'quote form) form))

(set! *data-readers* (assoc *data-readers* 'p-> #'p->))
```

Then it will expand to

```
user=> '(-> 1 inc inc #p-> (* 10) inc inc)

(-> 1
  inc
  inc
  (p->-impl '(* 10) * 10)
  inc
  inc)
```

and, ultimately, work:

```
user=> (-> 1 inc inc #p-> (* 10) inc inc)
#p-> (* 10) => 30
32
```

Problem? It’s a different macro. We’ll need another one for `->>`, too, so three in total. Can we make just one instead?

Turns out you can!

Trick is to use a probe. We produce an anonymous function with two arguments. Then we call it in place with _one_ argument (`::undef`) and see where other argument goes.

Inside, we check where `::undef` lands: first position means we’re inside `->>`, otherwise, `->`:


```
((fn [x y]
   (cond
     (= ::undef x) <thread-last>
     (= ::undef y) <thread-first>))
 ::undef)
```

Let’s see how it behaves:

```
(macroexpand-1
  '(-> "input"
     ((fn [x y]
        (cond
          (= ::undef x) <thread-last>
          (= ::undef y) <thread-first>))
      ::undef)))

((fn [x y]
   (cond
     (= ::undef x) <thread-last>
     (= ::undef y) <thread-first>))
   "input" ::undef)

(macroexpand-1
  '(->> "input"
     ((fn [x y]
        (cond
          (= ::undef x) <thread-last>
          (= ::undef y) <thread-first>))
      ::undef)))

((fn [x y]
   (cond
     (= ::undef x) <thread-last>
     (= ::undef y) <thread-first>))
   ::undef "input")
```

If we’re not inside any thread first/last macro, then no substitution will happen and our function will just be called with a single `::undef` argument. We handle this by providing an additional arity:

```
((fn
   ([_]
    <normal>)
   ([x y]
    (cond
      (= ::undef x) <thread-last>
      (= ::undef y) <thread-first>)))
   ::undef)
```

And boom:

```
user=> #p (- 10)
#p (- 10)
-10

user=> (-> 1 inc inc #p (- 10) inc inc)
#p (- 10)
-7

user=> (->> 1 inc inc #p (- 10) inc inc)
#p (- 10)
7
```

`#p` was already very good. Now it’s unstoppable.

You can get it as part of [Clojure+](https://github.com/tonsky/clojure-plus?tab=readme-ov-file#clojurehashp).