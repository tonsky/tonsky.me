---
title: "Humble Chronicles: The Inescapable Objects"
summary: "Why we need OOP, even in Clojure"
published: 2024-04-30
---

In [HumbleUI](https://github.com/HumbleUI/HumbleUI), there is a full-fledged OOP system that powers lower-level component instances. Sacrilegious, I know, in Clojure we are not supposed to talk about it. But...

Look. Components (we call them Nodes in Humble UI because they serve the same purpose as DOM nodes) have state. Plain and simple. No way around it. So we need something stateful to store them.

They also have behaviors. Again, pretty unavoidable. State and behavior work together.

Still not a case for OOP yet: could’ve been maps and functions. One can just

```
(def node []
  {:state   (volatile! state)
   :measure (fn [...] ...)
   :draw    (fn [...] ...)})
```

But there’s more to consider.

# Code reuse

Many nodes share the same pattern: e.g. a wrapper is a node that “wraps” another node. `padding` is a wrapper:

```
[ui/padding {:padding 10}
 [ui/button "Click me"]]
```

So is `center`:

```
[ui/center
 [ui/button "Click me"]]
```

So is `rect` (it draws a rectangle behind its child):

```
[ui/rect {:paint ...}
 [ui/button "Click me"]]
```

The first two are different in how they position their child but identical in drawing and event handling. The third one has a different paint function, but the layout and event handling are the same.

I want to write `AWrapperNode` once and let the rest of the nodes reuse that.

Now — you might think — still not a case for OOP. Just extract a bunch of functions and then pick and choose!

```
;; shared library code
(defn wrapper-measure [...] ...)

(defn wrapper-draw [...] ...)

;; a node
(defn padding [...]
  {:measure (fn [...]
              <custom measure fn>)
   :draw    wrapper-draw}) ;; reused
```

This has an added benefit of free choice: you can mix and match implementations from different parents, e.g. measure from wrapper and draw from container.

# Partial code replacement

Some functions call other functions! What a surprise.

One direction is easy. E.g. Rect node can first draw itself and then call a parent. We solve this by wrapping one function into another:

```
(defn rect [opts child]
  {:draw (fn [...]
           (canvas/draw-rect ...)
           ;; reuse by wrapping
           (wrapper-draw ...))})
```

But now I want to do it the other way: the parent defines wrapping behavior and the child only replaces one part of it.

E.g., for Wrapper nodes we always want to save and restore the canvas state around the drawing, but the drawing itself can be redefined by children:

```
(defn wrapper-draw [callback]
  (fn [...]
    (let [layer (canvas/save canvas)]
      (callback ...)
      (canvas/restore canvas layer))))

(defn rect [opts child]
  {:draw (wrapper-draw ;; reuse by inverse wrapping
           (fn [...]
             (canvas/draw-rect ...)
             ((:draw child) child ...)}))})
```

I am not sure about you, but to me, it starts to feel a little too high-ordery.

Another option would be to pass “this” around and make shared functions lookup implementations in it:

```
(defn wrapper-draw [this ...]
  (let [layer (canvas/save canvas)]
    ((:draw-impl this) ...) ;; lookup in a child
    (canvas/restore canvas layer))))

(defn rect [opts child]
  {:draw      wrapper-draw   ;; reused
   :draw-impl (fn [this ...] ;; except for this part
                (canvas/draw-rect ...)
                ((:draw child) child ...)}))
```

Starts to feel like OOP, doesn’t it?

# Future-proofing

Final problem: I want Humble UI users to write their own nodes. This is not the default interface, mind you, but if somebody wants/needs to go low-level, why not? I want them to have all the tools that I have.

The problem is, what if in the future I add another method? E.g. when it all started, I only had:

- `-measure`
- `-draw`
- `-event`

Eventually, I added `-context`, `-iterate`, and `-*-impl` versions of these. Nobody guarantees I won’t need another one in the future.

Now, with the map approach, the problem is that there will be none. A node is written as:

```
{:draw    ...
 :measure ...
 :event   ...}
```

will not suddenly have a `context` method when I add one.

That’s what OOP solves! If I control the root implementation and add more stuff to it, everybody will get it no matter when they write their nodes.

# How does it look

We still have normal protocols:

```
(defprotocol IComponent
  (-context              [_ ctx])
  (-measure      ^IPoint [_ ctx ^IPoint cs])
  (-measure-impl ^IPoint [_ ctx ^IPoint cs])
  (-draw                 [_ ctx ^IRect rect canvas])
  (-draw-impl            [_ ctx ^IRect rect canvas])
  (-event                [_ ctx event])
  (-event-impl           [_ ctx event])
  (-iterate              [_ ctx cb])
  (-child-elements       [_ ctx new-el])
  (-reconcile            [_ ctx new-el])
  (-reconcile-impl       [_ ctx new-el])
  (-should-reconcile?    [_ ctx new-el])
  (-unmount              [_])
  (-unmount-impl         [_]))
```

Then we have base (abstract) classes:

```
(core/defparent ANode
  [^:mut element
   ^:mut mounted?
   ^:mut rect
   ^:mut key
   ^:mut dirty?]
  
  protocols/IComponent
  (-context [_ ctx]
    ctx)

  (-measure [this ctx cs]
    (binding [ui/*node* this
              ui/*ctx*  ctx]
      (ui/maybe-render this ctx)
      (protocols/-measure-impl this ctx cs)))

  ...)
```

Note that parents can also have fields! Admit it: We all came to Clojure to write better Java.

Then we have intermediate abstract classes that, on one hand, reuse parent behavior, but also redefine it where needed. E.g.

```
(core/defparent AWrapperNode [^:mut child] :extends ANode
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (when-some [ctx' (protocols/-context this ctx)]
      (measure (:child this) ctx' cs)))

  (-draw-impl [this ctx rect canvas]
    (when-some [ctx' (protocols/-context this ctx)]
      (draw-child (:child this) ctx' rect canvas)))
  
  (-event-impl [this ctx event]
    (event-child (:child this) ctx event))
  
  ...)
```

Finally, leaves are almost normal `deftype`s but they pull basic implementations from their parents.

```
(core/deftype+ Padding [] :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ ctx cs] ...)
  (-draw-impl [_ ctx rect canvas] ...))
```

Underneath, there’s almost no magic. Parent implementations are just copied into children, fields are concatenated to child’s fields, etc.

Again, this is not the interface that the end-user will use. End-user will write components like this:

```
(ui/defcomp button [opts child]
  [clickable opts
   [clip-rrect {:radii [4]}
    [rect {:paint button-bg)}
     [padding {:padding 10}
      [center
       [label child]]]]]])
```

But underneath all these `rect/padding/center/label` will eventually be instantiated into nodes. Heck, even your `button` will become `FnNode`. But you are not required to know this.

Also, a reminder: all these solutions, just like Humble UI itself, are a work in progress at the moment. No promises it’ll stay that way.

# Conclusion

I’ve heard a rumor that OOP was originally invented for UIs specifically. Mutable objects with mostly shared but sometimes different behaviors were a perfect match for the object paradigm.

Well, now I know: even today, no matter how you start, eventually you will arrive at the same conclusion.

I hope you find this interesting. If you have a better idea — let me know.
