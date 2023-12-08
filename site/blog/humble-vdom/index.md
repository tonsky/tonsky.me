---
title: "Humble Chronicles: Managing State with VDOM"
summary: "An experiment of using VDOM for managing state in Humble UI"
---

The main struggle of UI is that it’s inherently mutable, but we want to deal with it in as pure way as possible.

The idea of virtual DOM is simple: you write a function `render(State) → UI` that always returns the entire UI as if it’s build from scratch, and the runtime does the necessary job to mutate actual UI tree from previous state to the one returned by your function.

vdom@2x.png

The upsides are:

- you always generate UI from scratch (easier),
- your function doesn´t deal with mutations or state transisions (also easier),
- you don’t have to manage component instances, runtime will manage them for you.

The downside are:

- Performance. The runtime has to do extra work comparing old tree with the new one and trying to figure out the difference.
- Might be hard to get to component instances when you need one.

As crazy as it sounds, we know from practice this approach works. We need couple of optimizations to make it practical, though.

## Lazy

Laziness is best described in [Elm documentation](https://guide.elm-lang.org/optimization/lazy.html). The idea is: you don’t return the entire tree all at once. At some key points you put special objects that say “here will be left panel component and here’s how to generate it”. But you don’t generate it until it’s really needed.

This works because UI rarely changes all at once. If I changed one part there’s no reason to re-generate another, especially if we know it’s going to be exactly the same. Less work for diffing, too.

In React components themselves are lazy boundaries. You don’t create or return components — you only return the description, and React expands it for you if it thinks it should be diffed.

## O(N) diff

Good general-purpose diff is algorithmically expensive, so one of the insights of React was to use fast linear diff that does good enough job at comparing sequences.

It works well in practice because most of our UIs are trees with very few siblings at each level.

## State management

Now, with the optimization above you can already have a working and useful VDOM implementation. I might be a bit off, but I think that’s exactly what Elm has, and it’s enough to build real-world apps.

But that forces you to store all state somewhere external, pass all of it down from the top and always have top-down rendering.

For the rest of us living in non-pure land, we want to have local state. If some text field needs to blink its cursor, or collapsible panel needs to store its collapsed state, sometimes it’s just way more convenient not to put it in a global atom, but let framework manage that state for you and keep things local.

Therefore, we need local state. But how?

Original React put it as class field:

```
class Counter extends Component {
  state = 0;
  render() {
    return <button onClick = { () => this.setState(this.state + 1); }>
  }
}
```

New React invented another paradigm compatible with pure functional components:

```
function Counter() {
  let [state, setState] = useState(0);
  return <button onClick = { () => setState(state + 1); }>
}
```

Here `useState` would only work inside React, you can’t really call `Counter()` on your own.

The beauty of the solution is that you get access to mutable field while staying inside a simple function (they call it “pure” but I guess it only works for very frivolous interpretation of purity).

I guess one other option would be passing state as first argument to render function:

```
function Counter(state) {
  return <button onClick = { () => state.set(state.value + 1); }>
}
```

but then it’s unclear where to put default value.

## State persistence

The heavy lifting that React does for you is keeping your state in sync with your instances. Remember: you return lightweight “descriptions” from your render, there’s no notion of state or anything. It comes later when render is actually called by React:

```
function Comp(num) {
  return <>
     [...Array(num).keys()].map((_) => Child())
  </>;
}
```

If we call it like that:

```
Comp(2)
```

We will get two intances of Child:

```
Child() // <= state1
Child() // <= state2
```

Even though both Children are basically equal, just the fact that they are positioned differently is enough for them to get two separate, independent instances of state.

If later we call Comp with 3:

```
Comp(3)
```

We will get three intances, but the first two will keep references to their states:

```
Child() // <= state1
Child() // <= state2
Child() // <= state3
```

Again. Entirely new render. Entirely new description. But first two components keep reference to the exactly the same state reference they had before.

That’s the main proposition of React. You do lightweight description with no notion to references or state, and framework does all the instance management for you.

## Keyed sequences

Now we start getting into the woods. What if you don’t want to keep state of a component? E.g. you deleted one Child from the middle. You want it to drop its state, not for next Child to pick it up.

For that, you have keys:

```
function Comp(num) {
  return <>
     children.map((it) => Child({key: it.id}))
  </>;
}
```

React claims that keys are here to make list diffing faster, but in fact it’s even more important for instance management.

## Global keys

This is where React breaks a little. What if you have a component like this:

```
function Input(outlined) {
  if (outlined)
    return <div class="outline">
             <input type="text">
           </div>;
  else
    return <input type="text">;
}
```

How do we keep instance of `input` the same (and we want to keep it, because cursor position etc)?

Flutter has a concept of [Global key](https://api.flutter.dev/flutter/widgets/GlobalKey-class.html). It’s similar to key but, you know, global. Again—not a performance optimization, but to keep instances and identity.

## Props migration and instance reuse

This is kinda weird. So components in React have props. Props are like function arguments for a function. The weird part is, when props change, React will reuse component instance anyway. So this:

```
<Button text="Submit">
```

when changed to this:

```
<Button text="Cancel">
```

will keep the identity, state and everything. In old class-based components, you could even write `componentWillUpdate` method that will be called when instance of your component is reused with new props. In a functional component you’ll get new `text` prop but old state.

It’s weird because it creates a second path of how you component could be created: first is from scratch and second one is through reusing another instance.

create-update-destroy.png
Illustration from [acko.net](https://acko.net/blog/climbing-mt-effect/)

You can, of course, give your component a key to prevent this from happening, but then, I guess, you’ll have to hand out a lot of keys.

So the design decision here comes to this: sometimes you clearly want your instances to be reused, and sometimes it can happen accidentally and you want to avoid that. React goes with “reuse as much as you can” route but maybe there’s a better option here?

## Exposing internal state

Imagine you have a text field (that’s the most complicated component I have in Humble UI, what do you want?). Most people only care about actual text, so you’d probably have a way to get that in and out, maybe by passing atom in or something.

The rest is local state, like cursor position, horizontal scroll, selection etc. That’s all fine.

Now imagine someone needs to draw autocomplete and for that it needs cursor position. How do you expose it from local state to something another components can use? Especially given that text inputs usually don’t have children.

```
[Column
 [TextField ...] <-- local state here
 [Autocomplete]] <-- need it here
```

## Hooks execution problem

So you use couple of hooks, right?

```
function Comp() {
  useEffect(() => ..., []);
  useEffect(() => ..., [...]);
  return <>...</>;
}
```

It’s an elegant decision to bring mutability and state transitions to “pure” functions. Especially given that later hooks can depend on local variables defined via earlier hooks.

The problem is, they execute on every render. Say, you use `useEffect` to do something on component mount. But for every subsequent render, `useEffect` will be called too! And the function you pass to `useEffect` will be created anew each time. Sounds like a waste, no?

What we actually want is something like:

```
function Comp() {
  let onMount = useEffect(() => ..., []);
  let onRender = useEffect(() => ...);
  return () => {
    return <>...</>;
  }
}
```

I think that’s the way [Reagent does local state](https://reagent-project.github.io/).

We can go even further and return map instead of imperative `useEffect`s:

```
function Comp() {
  let state = {};
  return {
    mount:   () => { ... },
    unmount: () => { ... },
    render:  () => {
      return <>...</>;
    }
}
```

That will let us get rid of hooks altogether, which as a Clojure programmer I like (less imperative code that depends on execution context).

## Hooks composition

Returning maps should work fine for a single component, but what about code composition? You see, beauty of hooks is that you can collect them together and reuse all at once. See this example from [Dan Abramov’s post](https://overreacted.io/making-setinterval-declarative-with-react-hooks/):

```
function useInterval(callback, delay) {
  const savedCallback = useRef();

  useEffect(() => {
    savedCallback.current = callback;
  }, [callback]);

  useEffect(() => {
    function tick() { savedCallback.current(); }
    if (delay !== null) {
      let id = setInterval(tick, delay);
      return () => clearInterval(id);
    }
  }, [delay]);
}
```

His custom hook uses three built-in hooks to set up everything it needs and then clean it up correctly. You use it like this:

```
function Counter() {
  ...
  useInterval(() => { ... }, 1000);
  ...
}
```

Which looks great. Can we do the same with maps? We can, in fact, if we allow _multiple_ maps to be returned. Like this:

```
(defn use-interval [cb]
  (let [*ref (atom nil)]
    {:on-mount  #(...)
     :on-render #(...)}))

(defn timer []
  (let [state    (atom 0)
        interval (use-interval #(...))]
    [interval
     {:on-mount   #(...)
      :on-unmount #(...)
      :render     (fn []
                    [...])}]))
```

The upside of this approach? You don’t need any special “hook rules” to make it work. Use conditionals, loops, whatever you need.

## Callback identity problem

Imagine a simple component like this:

```
function Comp() {
  let [count, setCount] = useState(0);
  let onClick = () => setCount(count + 1);
  return <button onClick={ onClick }>Click me</button>;
}
```

The problem here is that every time `Comp()` will be called, new `onClick` instance will be created. Sometimes that’s alright (when `count` changes), but most of the time it’s a waste. It also breaks components down the line, because for them new `onClick` is being passed down, even if it does exactly the same, and they can’t reuse previous results and have to go through diff.

React solution is to use `useCallback` which uses local state to keep old `onClick` instances around and only generate new one when it dependencies change:

```
function Comp() {
  let [count, setCount] = useState(0);
  let onClick = useCallback(() => setCount(count + 1), [count]);
  ...
}
```

It works fine, as long as you don’t mess up dependencies (`[count]`) or don’t forget to do this at all. Sounds like a lot of conditions for such a common use-case. Definitely needs an improvement.

The only upside here is: if you mess it up, it’ll still work, maybe, less performant. That’s a design principle I can get behind.

## Lifecycle callbacks

One thing that is not getting praised enough in React is lifecycle callbacks (onMount/onUnmount/onUpdate/onRender). I don’t think they were necessary for their model but sure were a nice addition, and then very quickly became the norm.

If you don’t remember, kids, DOM nodes didn’t have any callbacks — if you deleted one, no one will be notified.

Why were callbacks important? Because you can now work with resouces reliably! Set up timers, make requests, and then clean up after yourself.

Technically one can argue that such a thing belongs in business logic layer or data fetching layer or in the model. The thing is, you can’t put a callback on a certain key appearing or disappearing in an atom. At least not as easily. Maybe, if you could, nobody would need lifecycle callbacks and people would stop storing state in components. But until that happens, lifecycle is the best we’ve got.

## Point updates

The idea of having local state is actually to allow you to update only a small portion of UI without going all the way down from the top. If I only need to blink a cursor, no need to diff App, LeftPanel, ContentArea, Center, Column, Padding, TextField and finally Cursor. It’s just a waste of resources.

subtree_update.png

Now, if we combine it with Lazy, we do only the minumum amount of work needed: update one component directly, without touching its parents or children.

point_update.png

## Signals

React has been sleeping on these, but everyone else are pretty much on board. We’be been talking about them in [Humble Chronicles: Managing State with Signals](/blog/humble-signals/).

The upside of signal (for me) is that components automatically subscribe to updates when they use them. So you can bring external state and have point updates somewhere deep down in the tree without triggering full top-down re-render. Ultimately, signals solve props drilling.

The downside was complexity of corner cases and subscription management. Now, with lifecycle callbacks, components can finally reliably unsubscribe when destroyed. With that solved, I think there’re no downsides to having signals, at least as an option.

## Partial template updates

Selling point of Svelte/Solid is that they can only update _part_ of returned template. So in

```
<div>Clicked {count} times</div>
```

only text node containing `{count}` will be updated, and `div` will always stay the same. This is nice performance-wise, but I think complications that come with that outweight the benefits.

solid.png

What’s important is that React operates on values, not templates. You can get resulting tree any way you want: by DSL, programmatically, transformed with `clojure.walk`, choosen from multiple branches — whatever.

And it’s great. It’s clean. It’s simple. We value values here in Clojure land.

After all, templates returned from single component are usually not that big anyway.

## Pluggable storages

That was a huge thing for me when writing Rum. Every other React framework was dictating how you should manage your state: single atom with Om, ratoms with Reagent. So when I wanted to use DataScript, there was simply no option.

I want to make sure it’s also possible with Humble UI. Even if we ship with atoms and signals, for example, I want to at least be able to roll out my own storage too.

I think it’s possible with current React by implementing custom hook or something, and then using dummy `setState` to trigger re-rendering when needed. So something like that, but maybe a little more purpose-driven? Like `invalidate` or something. It might also be useful to be able to wrap render methods, too. Ultimately, if I can implement signals and DataScript both in user-land, I think we’re good.

## “DOM” access

Some stuff just isn’t declarative. React’s escape hatch for cases like this are refs. It’s a special mutable property that is filled with component instance when React sees one. For example, focus on mount:

```
export default function Form() {
  const inputRef = useRef(null);

  useEffect(() => {
    inputRef.current.focus();
  }, []);

  return <input ref={inputRef} />;
}
```

Seems like okay approach?

## Materialized components

Any sufficiently complex app will sooner or later need to “measure a DOM node” or do something bizzare like that. React let you do that by giving you access to browser DOM, which, well, is materialized after first render (that’s why `useEffect`s happen _after_ the render).

This is fine, but not clean enough for me because it gives you one frame of rendering where these constraints are not know, and what do you do then?

An example I’m thinking about is this:

```
(defn Comp []
  [Row
   [Button "Ok"]
   [Button "Apply"]
   [Button "Cancel"]])
```

Which looks like

buttons_1.png

What if I want to make all three have the same width? And not just the fixed width (that would never fly, not in today’s world with localizataion etc), but the width of the widest button:

buttons_2.png

How do I do that? I imagine something like

```
(defn Comp []
  (let [comps  (for [text ["Ok" "Apply" "Cancel"]]
                 (hui/make-comp
                   [Button text]))
        cs     (hui/make-size
                 Float/MAX_VALUE
                 Float/MAX_VALUE)
        width  (->> comps
                 (map #(hui/measure % cs))
                 (map :width)
                 (reduce max 0))]
  {:render
   (fn []
    [Row
     (for [comp comps]
      [Width {:width width}
       comp])])}))
```

The trick is, of course, to make `hui/make-comp` work outside of the normal render tree, but take into account all the styles etc nested up to this point.

Another trick is being able to _return_ materialized component as part of element (“description”) tree. If I’ve already created a component instance, might as well use it, right?

## TodoMVC example

With all of the above, let’s see how TodoMVC might look like. First, we start with the state:

```
(def *state
  (atom
    {:items
     [{:id 0, :checked false, :text "Item 1"}
      {:id 1, :checked true,  :text "Item 2"}
      {:id 2, :checked false, :text "Item 3"}]}))
```

Now the app UI. Main app:

```
(defn App [state]
  [Column
   [TextField {:on-submit add-item}]
   (for [item (:items state)]
     [Item {:key  (:id item)
            :item item}])])
```

Item:

```
(defn Item [{{:keys [id checked text]} :item}]
  [Row
   [Checkbox {:checked   checked
              :on-change #(toggle id)}]
   [Label {:text text}]])
```

One thing we want is edit state: items could be edited. Let’s see if can keep this local:

```
(defn Item [props]
  (let [item (:item props)
        {:keys [id checked text]} item
        *edited (atom false)]
    {:render
     (fn []
       [Row
        [Checkbox
         {:checked checked
          :on-change (fn [_] (toggle id))}]
        (if @*edited
          [TextField
           {:text text
            :on-submit
            (fn [text']
              (change-text id text')
              (reset! *edited false))}]
          [Clickable
           {:on-click (fn [_] (reset! *edited true))}
           [Label {:text text}]])])}))
```

We also want TextField to become focused once user clicks on a Todo, so we extract it into its own component I guess:

```
(defn todo-edit [props]
  (let [item (:item props)
        {:keys [id checked text]} item
        *ref (hui/make-ref)]
    {:unmount #(focus ref)
     :render
     (fn []
       [TextField
        {:ref  *ref
         :text text
         :on-submit
         (fn [text']
           (change-text id text')
           (reset! *edited false))}])}))
```

And finally, we use watcher over state atom to trigger re-render:

```
(add-watch *state ::redraw
  (fn [_ _ _ _]
    (render app-root [App])))
```

If we replace state with signals:

```
(def *state
  (hui/signal
    {:items
     [(hui/signal {:id 0, :checked false, :text "Item 1"})
      (hui/signal {:id 1, :checked true,  :text "Item 2"})
      (hui/signal {:id 2, :checked false, :text "Item 3"})
     ]}))
```

The re-rendering will happen automatically and when e.g. toggling an item it won’t even do full top-down, which is nice.
