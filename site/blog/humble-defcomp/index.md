---
title: "Humble Chronicles: Shape of the Component"
summary: "Looking for an ergonomic way to define components"
published: 2024-04-30
---

Last time I ran a huge experiment trying to figure out how components should work in Humble UI. Since then, I’ve been trying to bring it to the main.

This was trickier than I anticipated — even with a working prototype, there are still lots of decisions to make, and each one takes time.

I discussed some ideas in [Humble Chronicles: Managing State with VDOM](https://tonsky.me/blog/humble-vdom/), but this is what we ultimately arrived at.

The simplest component:

```
(ui/defcomp my-comp []
  [ui/label "Hello, world!"])
```

Note the use of square brackets `[]`, it’s important. We are not creating nodes directly, we return a “description” of UI that will later be analyzed and instantiated for us by Humble UI.

Later if you want to use your component, you do the same:

```
(ui/defcomp other-comp []
  [my-comp])
```

You can pass arguments to it:

```
(ui/defcomp my-comp [text text2 text3]
  [ui/label (str text ", " text2 ", " text3)])
```

To use local state, return a function. In that case, the body itself will become the “setup” phase, and the returned function will become the “render” phase. Setup is called once, render is called many times:

```
(ui/defcomp my-comp [text]
  ;; setup
  (let [*cnt (signal/signal 0)]
    (fn [text]
      ;; render
      [ui/label (str text ": " @*cnt)])))
```

As you can see, we have our own signals implementation. They seem to fit very well with the rest of the VDOM paradigm.

Finally, the fullest form is a map with the `:render` key:

```
(ui/defcomp my-comp [text]
  (let [timer (timer/schedule #(println 123) 1000)]
    {:after-unmount
     (fn []
       (timer/cancel timer)) 
     :render
     (fn [text]
       [ui/label text])}))
```

Again, the body of the component itself becomes “setup”, and `:render` becomes “render”. As you can see, the map form is useful for specifying lifecycle callbacks.

# Code reuse

React has a notion of “hooks”: small reusable bits of code that have access to all the same state and lifecycle machinery that components have.

For example, a timer always needs to be cancelled in unmount, but I don’t want to write `after-unmount` every time I want to use a timer. I want to use a timer and have its lifecycle to be registered automatically.

Our alternative is `with` macro:

```
(defn use-timer []
  (let [*state (signal/signal 0)
        timer  (timer/schedule #(println @*state) 1000)
        cancel (fn []
                 (timer/cancel timer))]
    {:value         *state
     :after-unmount cancel}))

(ui/defcomp ui []
  (ui/with [*timer (use-timer)]
    (fn []
      [ui/label "Timer: " @*timer])))
```

Under the hood, `with` just takes a return map of its body and adds stuff it needs to it. Simple, no magic, no special “hooks rules”. 

Same as with hooks, `with` can be used inside `with` recursively. It just works.

Thanks [Kevin Lynagh](https://kevinlynagh.com/) for the idea.

# Shared state

One of the goals of Humble UI was to make component reuse trivial. Web, for example, has hundreds of properties to customize a button, and still, it’s often not enough.

I lack the resources to make hundreds of properties, so I wanted to take another route: make components out of simple reusable parts, and let end users recombine them.

So a button becomes `clickable` (behavior) and `button-look` (visual). Want a custom button? Implement your own look, and use the same behavior. Want to reuse the look in another component (e.g. a toggle button?). Write your own behavior, and reuse the visuals.

The look itself consists of simple parts that can be reused and recombined:

```
(ui/defcomp button-look [child]
  [clip-rrect {:radii [4]}
   [rect {:paint button-bg)}
    [padding {:padding 10}
     [center
      [label child]]]]])
```

And then the button becomes:

```
(ui/defcomp button [opts child]
  [ui/clickable opts
   [ui/button-look child]])
```

(this and a previous one are simplified for clarity)

Now, the problem. The button is, of course, interactive. It reacts to being hovered, pressed, etc. But the state that represents it lives in `clickable` (the behavior). How to share?

The first idea was to use signals. Like this:

```
(ui/defcomp button [opts child]
  (let [*state (signal/signal nil)]
    (fn [opts child]
      [ui/clickable {:*state *state}
       [ui/button-look @*state child]])))
```

Which does work, of course, but a little too verbose. It also forces you to define state outside, while logically `clickable` should be responsible for it.

So the current solution is this:

```
(ui/defcomp button [opts child]
  [ui/clickable opts
   (fn [state]
     [ui/button-look state child])])
```

Which is a bit tighter and doesn’t expose the state unnecessarily. The look component is also straightforward: it accepts the state as an argument, without any magic, so it can be reused anywhere.

# Where to try

Current development happens [in the “vdom” branch](https://github.com/HumbleUI/HumbleUI/tree/vdom). Components migrate slowly, but steadily, to the new model.

Current screenshot for history:

screenshot@2x.webp

Soon we will all live in a Virtual DOM world, I hope.
