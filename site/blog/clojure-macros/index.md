---
title: "Clojure macros continue to surprise me"
published: 2024-07-15
---

Clojure macros have two modes: avoid them at all costs/do very basic stuff, or go absolutely crazy.

Here’s the problem: I’m working on [Humble UI’s](https://github.com/HumbleUI/HumbleUI) component library, and I wanted to document it. While at it, I figured it could serve as an integration test as well—since I showcase every possible option, why not test it at the same time?

This is what I came up with: I write component code, and in the application, I show a table with the running code on the left and the source on the right:

before@2x.png

It was important that code that I show is exactly the same code that I run (otherwise it wouldn’t be a very good test). Like a quine: hey program! Show us your source code!

Simple with Clojure macros, right? Indeed:

```
(defmacro table [& examples]
  (list 'ui/grid {:cols 2}
    (for [[_ code] (partition 2 examples)]
      (list 'list
        code (pr-str code)))))
```

This macro accepts code AST and emits a pair of AST (basically a no-op) back and a string that we serialize that AST to.

This is what I consider to be a “normal” macro usage. Nothing fancy, just another day at the office.

Unfortunately, this approach reformats code: while in the macro, all we have is an already parsed AST (data structures only, no whitespaces) and we have to pretty-print it from scratch, adding indents and newlines.

I tried a couple of existing formatters (clojure.pprint, zprint, cljfmt) but wasn’t happy with any of them. The problem is tricky—sometimes a vector is just a vector, but sometimes it’s a UI component and shows the structure of the UI.

And then I realized that I was thinking inside the box all the time. We already have the perfect formatting—it’s in the source file!

So what if... No, no, it’s too brittle. We shouldn’t even think about it... But what if...

What if our macro read the source file?

Like, actually went to the file system, opened a file, and read its content? We already have the file name conveniently stored in `*file*`, and luckily Clojure keeps sources around.

So this is what I ended up with:

```
(defn slurp-source [file key]
  (let [content      (slurp file)
        key-str      (pr-str key)
        idx          (str/index-of content key)
        content-tail (subs content (+ idx (count key-str)))
        reader       (clojure.lang.LineNumberingPushbackReader.
                       (java.io.StringReader.
                         content-tail))
        indent       (re-find #"\s+" content-tail)
        [_ form-str] (read+string reader)]
    (->> form-str
      str/split-lines
      (map #(if (str/starts-with? % indent)
              (subs % (count indent))
              %)))))
```

Go to a file. Find the string we are interested in. Read the first form after it _as a string_. Remove common indentation. Render. As a string.

Voilà!

after@2x.png

I know it’s bad. I know you shouldn’t do it. I know. I know.

But still. Clojure is the most fun I have ever had with any language. It lets you play with code like never before. Do the craziest, stupidest things. Read the source file of the code you are evaluating? [Fetch code from the internet and splice it into the currently running program?](github.com/tonsky/remote-require/)

In any other language, this would’ve been a project. You’d need a parser, a build step... Here—just ten lines of code, on vanilla language, no tooling or setup required.

Sometimes, a crazy thing is exactly what you need.