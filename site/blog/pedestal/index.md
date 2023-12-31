---
title: "Grumpy chronicles: Pedestal and routing"
summary: "Migrating a web app from Ring to Pedestal"
published: 2019-06-13
---

As part of an ongoing experiment, I decided to update Grumpy to Pedestal. The main commit we’ll be discussing is [here](https://github.com/tonsky/grumpy/commit/141477477dd04d4a327208595b7344e3c4ed50ee).

# Long live middlewares

The biggest difference between Ring and Pedestal is interceptors instead of middlewares. I like the idea and sympathize with the reasoning behind it: more control over execution which enables multithreading and async request/responses. Not that I need those for a content site such as Grumpy but it was fun to play with something different for a change.

Interceptors are inherently less elegant than middlewares: they are records, so you have to rely on the interceptor library to build them. That is a bit annoying since now you have two types of things floating around: stuff that can be converted to interceptor and interceptors themselves. Those are not the same, and I used to mistake one for another a few times.

Unlike middlewares, which are simple functions, interceptors do not naturally compose. That means you’ll need something to run them for you, so you’ll have to bring Pedestal dependency whenever you need to play with those.

I was also happy to find out that most interceptors are just standard Ring middlewares converted to the new style. Most of my stuff “just worked” without too much conversion effort. E.g. parameters for Session middleware and Session interceptor [perfectly match](https://github.com/tonsky/grumpy/blob/ea3f6d1f2c227e36760de28dea649aed36bbe00e/src/grumpy/auth.clj#L115-L119), etc.

```
(def session
  (middlewares/session
    {:store (session.cookie/cookie-store
              {:key cookie-secret})
     :cookie-name "grumpy_session"
     :cookie-attrs session-cookie-attrs}))
```

Writing interceptors is [as easy as writing middleware](https://github.com/tonsky/grumpy/blob/ea3f6d1f2c227e36760de28dea649aed36bbe00e/src/grumpy/auth.clj#L100-L113). The biggest difference being that you now accept Context map and return Context map instead of request/response as in Ring. Context has both request/response as keys though.

```
(def force-user
  {:name ::force-user
   :enter
   (fn [ctx]
     (if-some [u grumpy/forced-user]
       (assoc-in ctx [:request :session :user] u)
       ctx))
   :leave
   (fn [ctx]
     (if-some [u grumpy/forced-user]
       (update ctx :response assoc
         :cookies {"grumpy_user"
                   (assoc user-cookie-attrs :value u)}
         :session {:user    u
                   :created (grumpy/now)})
       ctx))})
```

Being Clojure, Pedestal operates on loosely typed maps. This bestiary of map types was [pretty helpful](http://pedestal.io/reference/index#_handling_http).

One of the problems with Interceptors is that, despite being a great idea, they are not aiming at being a foundation for the next big Clojure web stack. For now, they are happy just being a part of Pedestal package. Others [are starting to build alternatives](https://github.com/metosin/sieppari#differences-to-pedestal) with slightly different semantics, which I’m not sure is a good thing. I mean, alternatives are great, but two version of the same thing with almost the same contract but not quite? Might lead to segmentation and confusion.

# Starting server

For some reason there’s no documentation on how to start a server:

deployment@2x.png
These are supposed to be links...

Even the Jetty page here is a placeholder.

jetty.png

From [one of the guides](http://pedestal.io/guides/hello-world) you can figure out that the method you need is `io.pedestal.http/create-server` which also has an empty API doc:

create_server.png

The only clue here is that argument is called “service-map” so you head onto [third documentation page](http://pedestal.io/reference/service-map) that’s easiest to find through Google and finally you may have your answers:

service_map.png

Luckily, the server I wanted to use (Immutant) was on the list:

http_type.png

Not sure how hard would it be to use a custom Servlet container for example. I have no idea what “server function” might be, and it doesn’t seem to be documented anywhere.

# Running the app

From there everything was smooth enough. The only annoyance was that production logs kept reporting “Broken pipe” error quite often:

broken_pipe_thumb.png
In full aligment with Clojure, stacktraces are enormous.

As far as I understand, this is an error that happens when the server tries to write to a socket that was effectively closed but not reported as closed yet? I’m not certain on details but enough to know that it repeats quite reliably during normal site usage from a browser.

My point here is: why the heck is it reported as an error at all? If you have a web server, a client disconnect is not something exceptional. It’s not even a warning! This is a normal operation that should not be reported to the logs at all unless specifically asked. Imagine if TCP stack logged each lost packet...

It was not easy to get rid of, too. Writing to the socket is something that happens after all user-defined interceptors have finished, so you can’t use that [elaborate error-handling routine](http://pedestal.io/reference/error-handling) to suppress this. My first attempt at catching this error in the last interceptor [undestandably failed](https://github.com/tonsky/grumpy/commit/fca324f142b1fd4076a2b834f2e0403103ceec0a).

```
(defn suppress-error [name class message-re]
   (interceptor/interceptor
     {:name name
      :error
      (fn [ctx ^Throwable e]
        (let [cause (stacktrace/root-cause e)
              message (.getMessage cause)]
          (if (and (instance? class cause) (re-matches message-re message))
            (do
              (println "Ignoring" (type cause) "-" message)
              ctx)
            (assoc ctx :io.pedestal.interceptor.chain/error e))))}))

...

(update ::http/interceptors
  #(cons (suppress-error ::suppress-broken-pipe
           java.io.IOException #"Broken pipe") %))
```

(last :leave/:error interceptor have to go first because logic)

In any other language that would be game over. The error should be handled inside the framework, so it’s either pull request with hopes of getting it merged in the next six months in the best case, or cloning and running your own version.

But thank God we are coding in Clojure! Which means we can redefine anything, anywhere at runtime at no cost. Which is exactly what I did to [monkey-patch Pedestal’s internals on the fly](https://github.com/tonsky/grumpy/commit/c6bbcc9590394243e37c2a72a4e569a7c506be7d)!

```
; Filtering out Broken pipe reporting
; io.pedestal.http.impl.servlet-interceptor/error-stylobate
(defn error-stylobate [{:keys [servlet-response] :as context} exception]
  (let [cause (stacktrace/root-cause exception)]
    (if (and (instance? IOException cause)
          (= "Broken pipe" (.getMessage cause)))
      (println "Ignoring java.io.IOException: Broken pipe")
      (io.pedestal.log/error
        :msg "error-stylobate triggered"
        :exception exception
        :context context))
    (@#'io.pedestal.http.impl.servlet-interceptor/leave-stylobate context)))


; io.pedestal.http.impl.servlet-interceptor/stylobate
(def stylobate
  (io.pedestal.interceptor/interceptor
    {:name ::stylobate
     :enter @#'io.pedestal.http.impl.servlet-interceptor/enter-stylobate
     :leave @#'io.pedestal.http.impl.servlet-interceptor/leave-stylobate
     :error error-stylobate}))

...

(with-redefs [io.pedestal.http.impl.servlet-interceptor/stylobate stylobate]
  (-> ...
    (http/create-server)
    (http/start)))
```

I also [filed it to Pedestal upstream](https://github.com/pedestal/pedestal/pull/621), we’ll see how it goes.

# Routing

Pedestal comes with routing built-in, which is a nice upgrade from Ring that required you to look for a separate library to handle those.

Another welcome change coming from Compojure: in Compojure, you wrap routes in middlewares, which means middlewares apply _before_ routing happens. That works fine unless you want a different set of middleware at different routes. It certainly can be made to work in Compojure as well, just not the default. In Pedestal, routing happens first and everything else is configured per-route.

One minor annoyance with Pedestal routes is that every route requires a unique name. I do agree that names, in general, are great, but not every little thing should have one. With routes, I believe, the method + path themselves make a great name. Forcing user to invent something else will just lead to obscure arbitrary names.

Another thing, a big one. Route composition and overlapping routes. Imagine the following routes:

```
/post/:id
/post/create
```

First one retrieves post by `id`, where `id` could be anything. BUT if that anything happens to be the word `"create"` then the second route should be triggered instead.

But is it even correct? Personally, I see no problem here. It’s pretty clear what should happen, the only downside is that you can’t have a post with `id == "create"` which is an acceptable tradeoff for beautiful URLs.

Compojure can make it work but only if you manually order your routes. That is because `/post/:id` also matches `/post/create`, but not the other way around.

Compojure matcher is a _linear matcher_. It examines routes one by one. That works fine as long as you don’t care about matching performance and as long as you can put your routes in the 
correct order.

But linear matching breaks encapsulation. Imagine that the final app is built from multiple namespaces each providing their own set of routes. If you have something like this:

```
(ns A)
(def routes
  ["/post/:id" ...]
  ["/draft/create" ...])

(ns B)
(def routes
  ["/post/create" ...]
  ["/draft/:id" ...])
```

Then no matter in which order you include `A/routes` and `B/routes` one of the routes will be shadowed. It also makes changes to routes non-local, as adding a new route to one namespace might accidentally break something else _in a completely different namespace_.

That’s the case against linear routes. Slow performance and bad isolation. Happily, Pedestal comes with two more advanced routes that perform better by using tries instead of the linear scan to match routes. This is faster but unfortunately does not support our use-case.

Map tree only supports static routes. If you need to pass any parameter do it in a query. Not sure if anybody does build applications that way, seems too limiting to me and not too beautiful. `"/post?id=123&action=create"` only because our matching algorithm happens to perform better? Nope. Not even once.

Prefix tree router documentation says

> Wildcard routes always win over explicit paths in the same subtree. E.g., /path/:wild will always match, even if /path/user is defined

which is basically to say that overlapping routes are not supported at all. Better throw an exception rather than “silently win” because if the user did supply a route that might be also matched as wildcard her intentions are pretty clear and those are not to “just ignore this for me will you?”.

The third router that comes with Pedestal is linear one so we are back at ordering routes ourselves.

I was under impression that [reitit](https://metosin.github.io/reitit/) was claiming to handle this exact problem. Turned out that the only thing they fixed was [error reporting](https://metosin.github.io/reitit/basics/route_conflicts.html): the conflicting routes are now reported to the user. Well, that’s much better that Pedestal approach but still leaves me at nothing.

So here’s [what I do](https://github.com/tonsky/grumpy/blob/ea3f6d1f2c227e36760de28dea649aed36bbe00e/src/grumpy/routes.clj): I collect all the routes and sort them myself, then pass the result to linear matcher. Still slow match performance (same to Compojure which I used before that, though) but at least encapsulation is respected.

```
(defn compare-parts [[p1 & ps1] [p2 & ps2]]
  (cond
    (and (nil? p1) (nil? p2)) 0
    (= p1 p2) (compare-parts ps1 ps2)
    (= (type p1) (type p2)) (compare p1 p2)
    (nil? p1) -1
    (nil? p2) 1
    (string? p1) -1
    (string? p2) 1
    :else (compare p1 p2)))

(defn sort [routes]
  (sort-by :path-parts compare-parts routes))

...

{::http/routes
 (routes/sort (concat routes auth/routes authors/routes))}
```


I also did a little DSL that auto-generates route names (required by Pedestal) from method and path and allows for nested collections of interceptors so that I can add multiple interceptors the same way I add one. Instead of this:

```
(io.pedestal.http.route/expand-routes
  #{{}
    ["/forbidden" :get :route-name :forbidden (vec (concat populate-session [route/query-params handle-forbidden]))]
    ...})
```

I can write

```
(grumpy.routes/expand
  [:get "/forbidden" populate-session route/query-params handle-forbidden]
  ...)
```

(in both cases `populate-session` is a vector of multiple interceptors that are convenient to use together).

I’m quite happy with the usability of this but would like the performance of trie matcher too. Maybe one day I’ll release my own opinionated router.

# Conclusion

Using Pedestal is no harder that Ring. Some included batteries are a welcome addition (router), some edges are rough (unnecessary exceptions) and documentation is scarce. Unfortunately, I didn’t get to the async requests where Pedestal model is supposed to shine, as the Grumpy website has no use of those. Overall an okay experience, but not a deal-breaker for me. I would love to see Clojure community gather around a one true interceptors model but it doesn’t seem to happen quite yet.