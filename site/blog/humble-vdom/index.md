---
title: "Humble Chronicles: Managing State with VDOM"
summary: "An experiment of using VDOM for managing state in Humble UI"
published: 2023-12-21
---

This post follows the implementation of VDOM for Humble UI. We look at various aspects of the problem, weigh our options, and decide on the solution. In some cases, it’s not clear how to proceed, so we just state the problem.

Follow along if you are curious about how the sausage is made. This is not the final design yet, more of an exploration of possibilities.

## The idea

The main struggle of UI is that it’s inherently mutable, but we want to deal with it in as pure a way as possible.

The idea of virtual DOM is simple: you write a function `render(State) → UI` that always returns the entire UI as if it’s built from scratch, and the runtime does the necessary job to mutate the actual UI tree from the previous state to the one returned by your function.

vdom@2x.png

The upsides are:

- you always generate UI from scratch (easier),
- your function doesn’t deal with mutations or state transitions (also easier),
- you don’t have to manage component instances, runtime will manage them for you.

The downside are:

- Performance. The runtime has to do extra work comparing the old tree with the new one and trying to figure out the difference.
- It might be hard to get to component instances when you need one.

As crazy as it sounds, we know from practice this approach works. We need a couple of optimizations to make it practical, though.

## Lazy

Laziness is best described in [Elm documentation](https://guide.elm-lang.org/optimization/lazy.html). The idea: you don’t return the entire tree all at once. At some key points, you put special objects that say “Here will be the left panel component, and here’s how to generate it”. But you don’t generate it until it’s needed.

This works because UI rarely changes all at once. If I changed one part there’s no reason to re-generate another, especially if we know it’s going to be the same. Less work for diffing, too.

In React components themselves are lazy boundaries. You don’t create or return components — you only return the description, and React expands it for you later.

## O(N) diff

Good general-purpose diff is algorithmically expensive, so one of the insights of React was to use fast linear diff that does a good enough job at comparing sequences.

It works well in practice because most UIs are trees with very few siblings at each level.

## State management

Now, with the optimization above you can already have a working and useful VDOM implementation. I might be a bit off, but I think that’s all that Elm has, and it’s enough to build real-world apps.

But that forces you to store all state somewhere external, pass all of it down from the top, and always have full top-down rendering.

For the rest of us living in non-pure land, we want to have local state. If some text field needs to blink its cursor, or a collapsible panel needs to store its collapsed state, sometimes it’s just way more convenient not to put it in a global atom, but let the framework manage that state for you and keep things local.

Therefore, we need local state. But how?

Original React puts it as a class field:

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

Here `useState` would only work inside React, you can’t call `Counter()` on your own.

The beauty of the solution is that you get access to mutable fields while staying inside a simple function (they call it “pure” but I guess it only works for very frivolous interpretations of purity).

I guess one other option would be passing state as the first argument to render function:

```
function Counter(state) {
  return <button onClick = { () => state.set(state.value + 1); }>
}
```

but then it’s unclear where to put the default value.

## State persistence

The heavy lifting that React does for you is keeping your state in sync with your instances. Remember: you return lightweight “descriptions” from your render, there’s no notion of state or anything. It all comes later when `render` is called by React:

```
function Parent(num) {
  return <>
     [...Array(num).keys()].map((_) => Child())
  </>;
}
```

If we call it like this:

```
Parent(2)
```

we will get two instances of `Child`:

```
Child() // <= state1
Child() // <= state2
```

Even though both `Child`ren are essentially the same value, just the fact that they are positioned differently is enough for them to get two separate, independent instances of state.

If later we call `Parent` with 3:

```
Parent(3)
```

we will get three instances, but the first two will keep references to their states:

```
Child() // <= state1
Child() // <= state2
Child() // <= state3
```

Again. Entirely new render. Entirely new description. But the first two components keep reference to the very same state they had before.

That’s the main proposition of React. You do a lightweight description with no notion of state, and the framework does all the instance management for you.

## Keyed sequences

Now we start getting into the woods. What if you don’t want to keep the state of a component? E.g. you deleted one `Child` from the middle. You want it to drop its state, not for the next `Child` to pick it up.

For that, you have keys:

```
function Parent(num) {
  return <>
     children.map((it) => Child({key: it.id}))
  </>;
}
```

React claims that keys are here to make list diffing faster, but it’s even more important for instance management.

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

How do we keep the instance of `input` the same (and we want to keep it, because of cursor position etc)?

Flutter has a concept of [Global key](https://api.flutter.dev/flutter/widgets/GlobalKey-class.html). It’s similar to key but, you know, global. Again—not a performance optimization, but to keep instances and identity.

## Shape of component

How should arguments to component function look like? There are two approaches, React and Reagent.

In React, each component accepts a `props` map with an optional `children` key.

```
(defn MyComp [props]
  (let [{:keys [children color text]} props]
    ...))
```

This maps very nicely to HTML/JSX where each tag has, well, named props and a list of children. In Clojure/Hiccup, we have to rely on convention to pass named arguments at the second position:

```
[MyComp {:color c, :text t, ...}
 [Child]
 [Child]
 ...]
```

The alternative (Reagent) way is to have normal positional arguments:

```
(defn MyComp [color text & children]
  ...)
```

Calling the component stays largely the same, except now it’s up to you if you want named arguments or not:

```
[MyComp color text [Child] [Child] ...]
```

The problem here is that we no longer have a place to put stuff like `:key` and `:ref`. Reagent solves this by putting keys on metadata:

```
^{:key k} [MyComp color text ...]
```

which works, but feels a little bit inelegant.

The upside is that you can now have multiple children groups with semantic names:

```
(defn AppShell [header left-panel right-panel body]
 ...)

[AppShell
 [Header ...]
 [LeftPanel ...]
 [RightPanel ...]
 [AppBody
  ...]]
```

## Lifecycle callbacks

One thing that is not getting praised enough in React is lifecycle callbacks (onMount/onUnmount/onUpdate/onRender). I don’t think they were necessary for their model but they sure were a nice addition, and then very quickly became the norm.

If you don’t remember, kids, DOM nodes didn’t have any callbacks — if you deleted one, no one will be notified.

Why were callbacks important? Because you can now work with resources reliably! Set up timers, make requests, and then clean up after yourself.

Technically one can argue that such a thing belongs in the business logic layer or data fetching layer or in the model.

The thing is, you can’t put a callback on a certain key appearing or disappearing in an atom. At least not as easily. Maybe, if you could, nobody would need lifecycle callbacks and people would stop storing state in components. But until that happens, lifecycle is the best we’ve got.

## Exposing internal state

Imagine you have a text field (it’s the most complicated component I have in Humble UI, what do you want?). Most people only care about actual text, so you’d probably have a way to get that in and out, maybe by passing in an atom or something.

The rest is the local state, like cursor position, horizontal scroll, selection, etc. That’s all fine.

Now imagine someone needs to draw an autocomplete dropdown, and for that, it needs a cursor position. How do you expose it from the local state to something another component can use? Especially given that text inputs usually don’t have children.

```
[Column
 [TextField ...] <-- local state here
 [Autocomplete]] <-- need it here
```

I’m still thinking about this one.

## Hooks execution problem

So you use a couple of hooks, right?

```
function Comp() {
  useEffect(() => ..., []);
  useEffect(() => ..., [...]);
  return <>...</>;
}
```

It’s an elegant decision to bring mutability and state transitions to “pure” functions. Especially given that subsequent hooks can depend on local variables defined via earlier hooks.

The problem is, they execute on every render. Say, you use `useEffect` to do something when a component mounts. But for every subsequent render, `useEffect` will be called too! And the function you pass to `useEffect` will be created anew each time. Sounds like a waste, doesn't it?

What we want instead is something like:

```
function Comp() {
  let onMount = useEffect(() => ..., []);
  let onRender = useEffect(() => ...);
  return () => {
    return <>...</>;
  }
}
```

The cool thing about it is that you don’t even need anything special for the local state. Local state is just a variable captured by the render closure. That’s the way [Reagent does it](https://reagent-project.github.io/).

We can go even further and return a map instead of imperative `useEffect`s:

setup_render.png

The setup part is called once, on component initialization. The render will be re-called on each render.

Upside: it will let us get rid of hooks altogether, which as a Clojure programmer I like (less imperative code that depends on execution context).

## Hooks composition

Returning maps should work fine for a single component, but what about code composition? You see, the beauty of hooks is that you can collect them together and reuse them all at once. See this example from [Dan Abramov’s post](https://overreacted.io/making-setinterval-declarative-with-react-hooks/):

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

Which looks great. Can we do the same with maps? We can if we allow multiple maps to be returned. Like this:

```
(defn use-interval [cb]
  (let [*ref (atom nil)]
    {:after-mount #(...)
     :after-draw  #(...)}))

(defn timer []
  (let [state    (atom 0)
        interval (use-interval #(...))]
    [interval
     {:before-mount  #(...)
      :after-unmount #(...)
      :render        (fn []
                       [...])}]))
```

The upside of this approach? You don’t need any special “hook rules” to make it work. Use conditionals, loops, whatever you need.

Look, I even made a table trying to figure it out:

decision_matrix.png

## Instance reuse

This is kinda weird. The entirety of React is built on returning lightweight component descriptions and matching them with stateful component instances. But that problem can’t be solved in the general case. 

Consider this:

```
<Button text="Submit">
```

Due to some changes somewhere up the stack, after a new render, this button now becomes this:

```
<Button text="Cancel">
```

Should it be the same component or an entirely new one? Unclear. If I had to guess, it probably depends on the code that generated it. If it was:

```
return <Button text={props.text}>;
```

then probably identity should be the same. If it was this, though:

```
if (props.cond)
  return <Button text="Submit">;
else
  return <Button text="Cancel">;
```

then I’d say it’s an entirely new button.

But React can’t see that! It only operates on return values, so both cases look the same from React’s point of view.

The design decision here comes to this: sometimes you want your instances to be reused, and sometimes it can happen accidentally and you want to avoid that. React is going with “reuse as much as you can” by default, offering keys as an opt-out mechanism. But maybe there’s something better here?

One option we have in Clojure that JS people don’t have is using code position as the “default key”. Something like this:

```
(def *cnt
  (volatile! 0))

(defmacro defcomp [name args & body]
  `(defn ~name ~args
     ~@(clojure.walk/postwalk
         #(if (vector? %)
            (with-meta % {:implicit-key (vswap! *cnt inc)})
            %)
         body)))
```

This is how it works:

```
(defcomp comp []
  [column  ;; ← :implicit-key == 0
   [Child] ;; ← :implicit-key == 1
   [Child] ;; ← :implicit-key == 2
   (for [i (range 10)]
     [Child])]) ;; ← :implicit-key == 3
```

Now each vector returned from our component will have a unique `:implicit-key` assigned to it at compile time. It’ll still be a guess, but maybe a marginally better one.

## Props migration

Instance reuse can be a reason for subtle bugs. Consider:

```
function Comp(props) {
  let [state, setState] = useState(props.init);
  ...
}
```

The first render is fine:

```
{init: 1} -> new state (1)
```

Subsequent renders are also fine:

```
{init: 1} -> same stored state (1)
```

But if we render with new props, do we want to create a new instance? Or reuse the same one? Anyways, in this particular example, passing a new init value will not affect anything:

```
{init: 2} -> same stored state (1)
```

It’s weird because it creates a second path of how your component could be created: the first is from scratch and the second one is through reusing another instance. Might be tricky to spot, too.

Can something be done about it? It’s hard to tell. One option I was thinking about is using a custom macro to detect which props were used during setup and which were used during render:

```
(defcomp Comp [{:keys [init]}]
  (let [*state (atom init)]
    {:render
     (fn [{:keys [text]}]
       [Label (str text ": " @*state)])}))
```

In theory, we can statically figure out that if `text` is changing, we can reuse the instance, but if `init` is new then we have to re-run the setup phase, too.

The problem is, nothing is stopping Clojure people from writing it like this:

```
(defcomp Comp [props]
  (let [init (:init props)]
    ...))
```

and then we’ll completely fail on our guess.

Another option could be to specify dependencies manually, similar to how `memo` does it in React:

```
(defcomp Comp [props] [:init]
  (let [init (:init props)]
    ...))
```

which feels very unnatural.

Or maybe allowing components to redefine `shouldComponentUpdate`, like in the good old days?

```
(defcomp Comp [props]
  (let [init (:init props)]
    {:should-setup?
     (fn [props']
       (not= init (:init props')))
     :render
     (fn [props']
       ...)}))
```

Yes, I know that both `shouldComponentUpdate` and `memo` are for deciding whether we should re-run render, not set up, but React doesn’t have a setup phase, so they are the best analogy I can come up with.

We can skip re-renders too, btw:

```
(defcomp Comp [props]
  {:should-render?
   (fn [props']
     (not=
       (select-keys props [:a :b :c])
       (select-keys props' [:a :b :c])))
   :render
   (fn [props']
     ...)}))
```


## Callback identity problem

Imagine a simple component like this:

```
function Comp() {
  let [count, setCount] = useState(0);
  let onClick = () => setCount(count + 1);
  return <button onClick={ onClick }>Click me</button>;
}
```

The problem here is that every time `Comp()` is called, a new `onClick` instance will be created. Sometimes that’s alright (when `count` changes), but most of the time it’s a waste. It also breaks components down the line, because for them new `onClick` is being passed down, even if it does the same, and they can’t reuse previous results and have to go through diff.

React solution is to use `useCallback` which uses the local state to keep old `onClick` instances around and only generate new ones when its dependencies change:

```
function Comp() {
  let [count, setCount] = useState(0);
  let onClick = useCallback(() => setCount(count + 1), [count]);
  ...
}
```

It works fine, as long as you don’t mess up the dependencies (`[count]`) or don’t forget to do this at all. Sounds like a lot of conditions for such a common use-case. Needs an improvement.

The only upside here: even if you mess it up, it’ll still work, maybe, less performant. That’s a design principle I can get behind.

In our case, with a separate setup phase, it should be less of a problem, unless we (either me or you, Humble users) fuck up instance reuse:

```
(defn comp []
  (let [on-click (fn [_]
                   (println "Clicked"))]
    (fn []
      [button {:on-click on-click}
       "Click me"])))
```

## Point updates

The idea of having a local state is to allow you to update only a small portion of the UI without going all the way from the top. If I only need to blink a cursor, no need to diff App, LeftPanel, ContentArea, Center, Column, Padding, TextField, and finally Cursor. It’s just a waste of resources.

subtree_update.png

Now, if we combine it with Lazy and `:should-render?`, we do only the minimum amount of work needed: update one component directly, without touching its parents or children.

point_update.png

## Signals

React has been sleeping on these, but everyone else is pretty much on board. We’ve been talking about them in [Humble Chronicles: Managing State with Signals](/blog/humble-signals/).

The upside of signals (for me) is that components automatically subscribe to updates when they use them. So you can bring an external state and have point updates somewhere deep down in the tree without triggering full top-down re-render. Ultimately, signals solve props drilling.

The downside was the complexity of corner cases and subscription management. Now, with lifecycle callbacks, components can finally reliably unsubscribe when destroyed. With that solved, I think there are no downsides to having signals, at least as an option.

## Partial template updates

The selling point of Svelte/Solid is that they can only update _part_ of the returned template. So in

```
<div>Clicked {count} times</div>
```

only text node containing `{count}` will be updated, and `div` will always stay the same. This is nice performance-wise, but I think the complications that come with that outweigh the benefits.

solid.png

What’s important is that React operates on values, not templates. You can get the resulting tree any way you want: by DSL, programmatically, transformed with `clojure.walk`, chosen from multiple branches — whatever.

And it’s great. It’s clean. It’s simple. It bears no surprises. We value values here in Clojure land.

After all, templates returned from a single component are usually not that big anyway.

## Pluggable storages

That was a huge thing for me when writing Rum. Every other React framework was dictating how you should manage your state: single atom with Om, ratoms with Reagent. So when I wanted to use DataScript, there was simply no option.

I want to make sure it’s also possible with Humble UI. Even if we ship with atoms and signals, for example, I want to at least be able to roll out my storage too.

I think it’s possible with current React by implementing a custom hook or something, and then using a dummy `setState` to trigger re-rendering when needed. So something like that, but maybe a little more purpose-driven? Like `invalidate` or something. It might also be useful to be able to wrap render methods, too. Ultimately, if I can implement signals and DataScript both in user-land, I think we’re good.

## “DOM” access

Some stuff just isn’t declarative. React’s escape hatch for cases like this is refs. It’s a special mutable property that is filled with a component instance when React sees one. For example, focus on mount:

```
export default function Form() {
  const inputRef = useRef(null);

  useEffect(() => {
    inputRef.current.focus();
  }, []);

  return <input ref={inputRef} />;
}
```

Seems like an okay approach? In our case, we don’t even need to make it that special:

```
(defn comp []
  (let [*ref (atom nil)]
    ^{:ref *ref} [button {} "OK"]))
```

## Materialized components

Any sufficiently complex app will sooner or later need to “measure a DOM node” or do something bizarre like that. React lets you do that by giving you access to browser DOM, which, well, is materialized after the first render (that’s why `useEffect`s happen _after_ the render).

This is fine, but not clean enough for me because it gives you one frame of rendering where these constraints are not known, and what do you get then? Flickering!

An interesting example I think about is this:

win95.webp

What’s so interesting about it? All buttons are the same width! If you do it naively on the web, you’ll get this:

buttons_1.png

What if I want to make all three have the same width? And not just the fixed width (that would never fly, not in today’s world with localization etc), but the width of the widest button:

buttons_2.png

I imagine something like:

```
(defn row-of-buttons []
  (let [labels ["Ok" "Apply" "Cancel"]
        comps  (for [label labels]
                 (make [button {} label]))
        cs     (core/isize
                 Integer/MAX_VALUE
                 Integer/MAX_VALUE)
        width  (->> comps
                 (map #(measure % *ctx* cs))
                 (map :width)
                 (reduce max 0))]
  [row
   (for [comp comps]
    [width {:width width}
     comp])])})
```

The trick is, of course, to make `make` work outside of the normal render tree, but take into account all the styles nested up to this point.

Another trick is being able to _return_ a materialized component as part of an element (“description”) tree. If I’ve already created a component instance, might as well use it, right?

## TodoMVC example

With all of the above, let’s see what TodoMVC might look like. First, we start with the state:

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
(defn app [state]
  [column
   [text-field {:on-submit add-item}]
   (for [item (:items state)]
     ^{:key (:id item)} [item-comp item])])
```

Item:

```
(defn item-comp [item]
  (let [{:keys [id checked text]} item]
    [row
     [checkbox {:checked   checked
                :on-change #(toggle id)}]
     [label text]]))
```

One thing we want is an edit state: items could be edited. Let’s see if can keep this local:

```
(defn item-comp [item]
  (let [{:keys [id checked text]} item
        *edited (atom false)]
    {:render
     (fn []
       [row
        [checkbox {:checked checked
                   :on-change (fn [_]
                                (toggle id))}]
        (if @*edited
          [text-field
           {:text text
            :on-submit (fn [text']
                         (change-text id text')
                         (reset! *edited false))}]
          [clickable
           {:on-click (fn [_]
                        (reset! *edited true))}
           [label text]])])}))
```

We also want TextField to become focused once the user clicks on a Todo, so we extract it into a separate component:

```
(defn todo-edit [item]
  (let [{:keys [id checked text]} item
        *ref (atom nil)]
    {:after-unmount #(focus @*ref)
     :render
     (fn []
       ^{:ref *ref}
       [text-field
        {:text text
         :on-submit
         (fn [text']
           (change-text id text')
           (reset! *edited false))}])}))
```

And finally, we use watcher over state atom to trigger re-render:

```
(add-watch *state ::redraw
  (fn [_ _ _ _]
    (render app-root [app])))
```

That’s it. Seems doable and pretty straightforward. Should be familiar to anyone who’ve seen React, too, I hope.

We can simplify a little by replacing atom with signals:

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

## Conclusion

Overall I like both the idea of VDOM and the design we have arrived at. I also like that the approach itself is both well-understood and battle-proven, being implemented in frameworks like React, Flutter, SwiftUI, and Elm.

You can play with a toy version at [HumbleUI/dev/vdom_2.clj](https://github.com/HumbleUI/HumbleUI/blob/main/dev/vdom_2.clj)

Now to move it all to main HumbleUI 🙈