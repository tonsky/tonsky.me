---
title: "SwiftUI defaults considered harmful"
summary: "A few notes on SwiftUI and why UI frameworks should not try to be “smart”"
published: 2020-01-20
---

iphone.png

A few notes on SwiftUI and why UI frameworks should not try to be “smart”.

First, the general approach SwiftUI is taking (reactive declarative data-driven UI framework) is really solid and considered state-of-the-art as of the current day of the year. No complaints here, great job, we all needed that, thank you Apple for releasing it. No, seriously. It’s a great tool and I’m looking forward to using it.

But, a few things I noticed in SwiftUI concern me. I think they could illustrate points in the UI framework design that future systems could handle better. Without further ado, let’s start with the biggest problem in API design: commas! 

## War on commas

I’m very much blown away with how far people are willing to go to achieve really slick looking DSL. I mean, SwiftUI could’ve been:

```
VStack(
    Image(uiImage: image),
    Text(title),
    Text(subtitle)
)
```

but instead, they decided to get rid of those annoying commas between elements. To do so they altered the language so that lambdas could have multiple implicit return values. Like this:

```
VStack {
    Image(uiImage: image)
    Text(title)
    Text(subtitle)
}
```

I mean, the only difference before the two is whether you have to put a comma between elements or not. And trust me, making this happen wasn’t easy. This is a solution [Apple came up with](https://developer.apple.com/documentation/swiftui/viewbuilder):

```
static func buildBlock() -> EmptyView

static func buildBlock<Content>(Content) -> Content

static func buildBlock<C0, C1>(C0, C1) -> TupleView<(C0, C1)>

static func buildBlock<C0, C1, C2>(C0, C1, C2) -> TupleView<(C0, C1, C2)>

static func buildBlock<C0, C1, C2, C3>(C0, C1, C2, C3) -> TupleView<(C0, C1, C2, C3)>

static func buildBlock<C0, C1, C2, C3, C4>(C0, C1, C2, C3, C4) -> TupleView<(C0, C1, C2, C3, C4)>

static func buildBlock<C0, C1, C2, C3, C4, C5>(C0, C1, C2, C3, C4, C5) -> TupleView<(C0, C1, C2, C3, C4, C5)>

static func buildBlock<C0, C1, C2, C3, C4, C5, C6>(C0, C1, C2, C3, C4, C5, C6) -> TupleView<(C0, C1, C2, C3, C4, C5, C6)>

static func buildBlock<C0, C1, C2, C3, C4, C5, C6, C7>(C0, C1, C2, C3, C4, C5, C6, C7) -> TupleView<(C0, C1, C2, C3, C4, C5, C6, C7)>

static func buildBlock<C0, C1, C2, C3, C4, C5, C6, C7, C8>(C0, C1, C2, C3, C4, C5, C6, C7, C8) -> TupleView<(C0, C1, C2, C3, C4, C5, C6, C7, C8)>

static func buildBlock<C0, C1, C2, C3, C4, C5, C6, C7, C8, C9>(C0, C1, C2, C3, C4, C5, C6, C7, C8, C9) -> TupleView<(C0, C1, C2, C3, C4, C5, C6, C7, C8, C9)>
```

I hope you have a wide enough monitor to read this. Not only does this seem ad-hoc and unpretty, but it also doesn’t allow you to put more than 10 elements in a container! All because someone in charge of API design was afraid of lists and had more power than someone in charge of Swift language.

These three methods look worrisome too:

```
static func buildEither<TrueContent, FalseContent>(first: TrueContent) -> _ConditionalContent<TrueContent, FalseContent>

static func buildEither<TrueContent, FalseContent>(second: FalseContent) -> _ConditionalContent<TrueContent, FalseContent>

static func buildIf<Content>(Content?) -> Content?
```

Not because you don’t need `if` — you obviously do — but because what about every other language construct? Can’t I use them inside those special lambdas without special support? I mean, `for` is [supported separately](https://developer.apple.com/documentation/swiftui/foreach). But what about while, repeat, switch, continue, break, throw and others then? Isn’t the point of programming in a language to be able to use that language?

## Implicit wraps

Component design raises some questions too. E.g. in

```
VStack {
    Text("abc")
        .bold()
        .padding(.all)
}
```

`.bold()` alters Text, but `.padding()` wraps it in another view, changing the return type of the whole expression along the way. Compare that with `VStack`, which wraps its children explicitly. Why make the distinction? If you sit down to design a slick DSL, shouldn’t it also convey some semantics? A tiny bit? You know, to help the reader understand code quicker? Why hide wrapping views inside method call chains if you already established another visual way of view wrapping? Not saying it’s completely wrong, but wouldn’t this look more consistent?

```
VStack {
    Padding {
        Text("abc").bold()
    }
}
```

## Child privacy invasion

Some things are probably just plain mistakes (very funny though). E.g. `NavigationView` takes its properties not from its constructor or via modifiers, but instead from the properties of its first child. WHY?

```
NavigationView {
    List {...}
        .navigationBarTitle(Text("Rooms"))
}
```

## Smart defaults

Ok, but these very mere annoyances. I do not understand why they exist, but their existence does not bring up anything horrible either. Unlike SwiftUI defaults:

```
Text("abc").padding()
```

This will surround Text with a padding of... I don’t know! Nobody knows, exactly. SwiftUI decides what that padding will be, according to some internal logic. Maybe it is some hardcoded value (god I hope so!). It might be multiple values, depending on a device. Or on an orientation. Or on a screen size. Or language, day/night cycle, moon phase – my point is, nobody knows or CAN know for sure.

Ok, but if you see padding with no arguments, you can guess something fishy is going on. Well, how about this?

```
HStack {
    Text("★★★★★")
    Text("Avocado Toast).font(.title)
    ...
}
```

See the padding here? No? But look at the picture!

hstack.png

Let me quote [David Abrahams](https://youtu.be/u6ImPjD8dT4?t=720):

> SwiftUI didn’t slam all of the stack’s children against each other. It left some space between these two because Adaptive Spacing™ is in effect.

So SwiftUI decided that, even though we didn’t ask it, it would be good to have some spacing between these two elements. Another default, you say. Well, not exactly. As far as I understand, SwiftUI will look inside your view hierarchy and if it recognizes some of the views it might make pretty non-trivial calls about how much space it should add. For example, if you have Text somewhere deep down in your component, it might use its baseline instead of container’s bounding box:

vstack.png

Another example of “smart” (or “magic”) behavior. A button might look black if you add it to a list:

```
List {
    Button { Text("Add room") }
}
```

button_black.png

but blue if you change `listStyle` (not Button style! you don’t need to touch Button at all!):

```
List {
   Button { Text("Add room") }
}.listStyle(.grouped) 
```

button_blue.png

And here comes my concern. As a person who has extensive web programming experience, starting back when the web was neither fun nor pretty, I have particular doubts about smart magic rules kindly provided by the platform.

First, sometimes defaults just get in a way. That means you need to undo them. It might not be as easy as it seems. Sure, changing padding is easy if you see it in your code:

```
Text().padding() -> Text().padding(5)
```

But what if there’s nothing? No code? How to undo that?

```
HStack { Text() Text() Text() } -> ?
```

When fixing broken layout, it is always easier to add stuff that you forgot than removing stuff that your framework did for you and you that can’t see. With written code, you can find it, read it, understand it, debug it, alter it. Framework code is completely opaque to you.

Second, smart behavior might be a nightmare when it doesn’t work your way. In web many people wasted millions of hours on StackOverflow trying to figure out how to remove extra space around `img`, how to align icon with text on a button, how to get rid of an unwanted extra scroll, undo mobile text boosting, remove link tap delay, make floats align properly, fix spacing around inline elements, etc. All these became problems because HTML has seemingly simple components that incapsulate very complex behaviors. Sometimes it does what you need and you are happy, but sometimes it does not and you have no idea what to do.

align.jpeg
They do not do that intentionally.

Quoting [Rob Napier](https://www.patreon.com/posts/33291031?cid=31419370):

> IMO the most difficult thing about SwiftUI is it is very non-discoverable and the documentation is incredibly sparse, and so you can only really know that by digging into the system quite a lot (that one is pretty easy, but alignments are incredibly subtle beasts). In many ways, it's quite like CSS in this regard. There are many twiddle knobs, and it's not obvious where you would look for them, again like CSS.

The third problem with defaults is that sometimes they create variations that you are just not aware of. You might be happy with your layout in a simulator, but somewhere on some weird iPad model in a particular orientation SwiftUI kindly sets padding to a bigger value and breaks your layout. Viola! In the web we used to have [CSS reset](https://cssreset.com/what-is-a-css-reset/) just for that reason: you can never be sure.

Fourth, even if you’ve made a perfect application and tested it thoroughly in all possible variations, who guarantees that tomorrow Apple will not get bored with the current design language and release SwiftUI 5.6.7 with completely different defaults? Or, worse, with _slightly_ different defaults?

## The solution

...is to be dumb and explicit! A framework should do what it was told and not do what it wasn’t. The simpler the better. If I forgot to put padding between HStack elements, well, shame on me, there should be no padding! All mistakes are mine.

Sure, some nuances might not be as good as they should in hands of an inexperienced developer. That’s the gap one can cover by learning. Unlike the current situation, where things look good at first but turn into a maintenance nightmare later. In professional tools, predictability beats first-timer convenience. I wish more frameworks follow that.
