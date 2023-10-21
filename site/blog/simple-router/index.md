---
title: New Library: Simple Router
summary: "Announcing clj-simple-router, a HTTP router for Clojure that allows overlapping routes and order-independent route declarations"
published: 2023-10-21
---

Simple story, really. I wanted an HTTP router for Clojure that

1. is order-independent
2. and allows for overlapping routes.

I didn’t find one, so I had to write my own.

# Order-Independence

If your app is any big, it probably has modules. Each module defines its own routes. So how do you bring them together?

In traditional Compojure model, the order in which you define your routes matters. This:

```
(GET "/user/create" [] ...)
(GET "/user/*" [] ...)
```

would work just fine, while in this case:

```
(GET "/user/*" [] ...)
(GET "/user/create" [] ...)
```

second route will never trigger.

When you bring your routes from multiple modules, the order in which you import and merge them still matters, but dependencies become opaque and implicit:

```
(routes
  finance-routes
  billing-routes
  api-routes)
```

I don’t like it! In an app with hundrets of routes, it’s too easy to forget about something and make a mistake. I simply don’t want to think about it, the same way I don’t have think about the order in which I define Java classes (sorry, Clojure, I can’t make an example out of you here).

# Overlapping routes

Basically, I want to do this:

```
(GET "/user/*" [] ...)
(GET "/user/create" [] ...)
```

Pedestal allows for map syntax (order-independence), but then forbids cases like this. Reitit makes those a special case that requires exceptional handling.

But I don’t think there’s anything is wrong with it or that it’s dangerous. It’s a pretty common case, really. Yes, you can’t create a user named `"create"`, but so what?

# Small details

I also wanted my route to be simple to use and work with. Routes are just maps. Keys are plain strings (not even vectors!):

```
(def user-routes
  {"GET /article/*"       (fn [req] ...)
   "GET /article/create"" (fn [req] ...)})
```

I don’t think complex stuff like regexp validation or coercing should be solved in the router:

```
(GET ["/user/:id" :id #"[0-9]+"] [id :<< as-int]
   ...)
```

It makes router a much more complex tool while saving almost nothing—same thing could be done by normal Clojure code immediately after declaration.

Do one thing well, you know. Ken Thompson’s philosophy.

Also, personally, I don’t like how many times `:id` is repeated in the previous example. Yes, it allows you to match `/:x/:y` to `[y x]`, but why would anyone want that?

In my router, you specify bindings only once:

```
(router/routes
  "GET /article/*/*" [x y] ...)
```

# That’s it!

That’s about everything you need to know. As the name implies, simple. 

Next time you’re writing a web app and need a router, something super-basic and well-made, give it a shot.

Link here:

banner@2x.webp https://github.com/tonsky/clj-simple-router

