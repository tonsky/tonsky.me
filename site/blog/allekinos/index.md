---
title: Going to the cinema is a data visualization problem
summary: "How I build a website for choosing movies"
published: 2024-05-14
---

Do you like going to the cinema? I do. But I also like to know where I am going and which movie I am going to see. But how do you choose?

You can’t go to the cinema’s website. There are just too many. Of course, you might have a favorite one and always go to it, but you won’t know what you are missing out.

Then, there are aggregators. The idea is good: gather everything that’s playing in cinemas right now in one place. Flight aggregators, but for movies.

Implementation, unfortunately, is not that good. As with any other website, the aggregator’s goal is to make you go through as many web pages as possible, do as many clicks as possible, and show you as many ads as possible.

kinode.mp4
Please use an ad blocker, this is unbearable

They even play a freaking TV ad in place of a movie trailer!

Information architecture can be weird too:

kinode_menu.png
[kino.de](https://kino.de/), auto-translated from German

Should I go to “Movies” or “Cinema Programme”? Should I select “Currently in Cinema” or “New in Cinema”?

So I decided to take matters into my own hands and build a cinema selection website I always dreamed of.

Meet [allekinos.de](https://allekinos.de/):

allekinos@2x.webp

# So what is it?

It’s a website that shows every movie screening in every cinema across the entire Germany.

And when I say EVERY screening, I mean it:

everything.mp4

Every screening, every cinema, every movie. All in one long HTML table.

# What else can it do?

Just filter. You can filter:

- by city,
- by city district (don’t want to travel too far),
- by a particular cinema (maybe you have a favorite one),
- by genre (want to see something with your kid but don’t know what),
- or by movie (which cities does it still play?).

That’s it. That’s the site.

Oh, we also have a list of premieres so you would know what’s coming. But that’s it.

# What about the interface?

There isn’t one. I mean, there is, of course, but I tried to make it as invisible as possible. There’s no logo. No menu. No footer. No pagination. No “See more”. No cookie banners (because no cookies). No ChatGPT/SEO generated bullshit. No ads, of course.

Why? Because people don’t care about that stuff. They care about function. And our UI is a pure function.

# But how do I search?

Well, Ctrl+F, of course. We are too humble, too lazy, and too smart to try to compete with in-browser implementation.

# Wait, what about page size?

It’s totally fine. I mean, for Berlin, for example, we serve 1.4 MB of HTML. 3 MB with posters. It’s fine. 

Slack loads 50 MB (yes, MEGA bytes) to show you a list of 10 chats. AirBnB loads 15 MB, including 500 KB HTML, just to show 20 images. LinkedIn loads 1.5 MB of just HTML (37 MB total) for a fraction of the data we’re showing. So we are fine.

It’s kind of refreshing, actually. What kind of speed do you get from a table with a thousand rows. Feels like a lot, but still feels faster than anything on the modern web.

# What about mobile?

That is a good question. I am still thinking about it.

The table trick won’t work on mobile. So layout needs to be different, but I also want it to have the same information density as the desktop, which is tricky.

If you just make the table vertical, it’ll be too much to scroll even for people with the strongest fingers. Maybe I’ll figure something out one day.

# What’s under the hood?

[DataScript](https://github.com/tonsky/datascript).

When I looked at the data, I realized it’s multidimensional: there are movies, they have genres, years, countries, languages, there are cinemas, which are located in districts, which are located in cities, then there are showings, which have day and time, and very possibly something else will come up later, too.

Now, I had no idea how that data would be accessed. Is the cinema part of the movie or is the movie part of the cinema? So I decided to make it all flat and put it into the database.

And it worked! It worked remarkably well. Now I can utilize DataScript queries being data to build them on the fly:

```
(defn search [{:keys [city cinema district movie genre]}]
  (let [inputs   
        (cond-> [['$ db]]
          city     (conj ['?city     city])
          cinema   (conj ['?cinema   cinema])
          district (conj ['?district district])
          movie    (conj ['?movie    movie])
          genre    (conj ['?genre    genre]))
      
        where
        (cond-> [:where]
          city     (conj '(or
                            [?cinema :cinema/city ?city]
                            [?cinema :cinema/area ?city]))
          cinema   (conj '[?cinema :cinema/title ?cinema-title])
          district (conj '[?cinema :cinema/district ?district])
          movie    (conj '[?movie :movie/title ?movie-title])
          genre    (conj '[?movie :movie/genre ?genre]))]

    (apply ds/q
      (concat
        '[:find ?show ?date ?time ?url ?cinema ?version ?movie
          :keys  id    date  time  url  cinema  version  movie
          :in]
        (map first inputs)
        where 
        '[[?show    :show/cinema         ?cinema]
          [?show    :show/date           ?date]
          [?show    :show/time           ?time]
          [?show    :show/url            ?url]
          [?show    :show/movie-version  ?version]
          [?version :movie-version/movie ?movie]])
      (map second inputs))))
```

The whole database is around 11 MB, basically nothing. I don’t even bother with proper storage, I just serialize the whole thing to a single JSON file every time it updates.

# The hosting

I have been building websites for a while. I have two ([Grumpy](https://grumpy.website) and this blog) running right now on my own server. I already spent my time, I have figured this all out. I have all the templates at my fingertips.

But for [allekinos.de](https://allekinos.de/) I decided to try something different: [application.garden](https://application.garden/).

It’s a hosting for small Clojure web apps (still in private beta) that’s supposed to take care of insignificant details for you and let you focus on your app first and foremost.

And it works! It’s refreshingly simple: you download a single binary that operates as a command-line tool, create `garden.edn` file with your project’s name, and call `garden deploy`. That’s it! Your app is live!

No, seriously. You tend to forget how many annoying small details there are before other people can use your app. But when something like Garden takes them away, you remember and get blown away again! If that’s what Heroku used to feel like back in the day, I’m all in for it.

The beauty Garden is that it helps you start fast, but it’s not a toy. It easily scales all the way up to production. Custom domain, HTTPS, auth, cron, logs, persistent storage: they take care of all of this for you.

And a cherry on top: they even provide nREPL to production! Again, no setup, just `garden repl` and you are in! Perfect for debugging weird performance issues or running one-off jobs.

An example: when I implemented premieres and committed the code, I still needed to run it for the first time. Instead of making a special flag or endpoint or adding and then immediately removing the startup code, I just connected to remote nREPL and invoked the function in the code. It doesn’t get easier than that!

Uncharacteristic of me, but I kind of enjoy building web apps again, when it’s that simple. Might build more in the future.

# Conclusion

In the beginning, I wanted a simple website that solved my problem. I wanted a website that I’d enjoy using.

But I don’t want to make _a product_ out of it. We have enough products already. It’s time someone took a user’s side. And I am one of the users.

Magic things happen when you trust your users and just show them everything you’ve got. 

For example, I found some rare films playing that I had no idea about. Matrix in German (!), but once a week and only in one cinema. Or Mars Express, they play it in three cities only, excluding mine. How do you find out about stuff like this?

Here, I _discovered_ it. I looked at the data and you started seeing stuff that otherwise is completely invisible.

Anyway, enjoy. If this becomes a trend, I’m all in for it. Wouldn’t mind seeing more sites like this in the future.