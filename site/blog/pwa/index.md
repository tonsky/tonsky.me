---
title: "You need neither PWA nor AMP to make your website load fast"
summary: "Good performance practices are still needed when developing fast web experience."
published: 2018-11-21
hackernews_id: 18624128
starred: true
---

_Translations: [Russian](https://habr.com/ru/company/timeweb/blog/555094/)_

There has been a trend of new “revolutionary” techniques on the Web that basically let you do stuff possible decades ago.

# AMP

First, AMP (Accelerated Mobile Pages). Think about it: web, in general, failed to be fast, so Google invents a parallel web where they simply don’t let you use JavaScript. Oh, and they let you use a couple of Google-approved AMP JS components. But wait, can’t regular web run without JavaScript? Of course it can. Can regular web include custom JS components? You bet. Can it be fast? Netflix [recently found out](https://medium.com/dev-channel/a-netflix-web-performance-case-study-c0bcde26a9d9) that if they remove 500 Kb of JavaScript from a static (!!!) webpage it will load WAY faster and users will generally be happier. Who would have thought, right?

So why was AMP needed? Well, basically Google needed to lock content providers to be served through Google Search. But they needed a good cover story for that. And they chose to promote it as a performance solution.

Thing is, web developers don’t believe in performance. They _say_ they do, but in reality, they don’t. What they believe in is hype. So if you hype old tricks under a new name, then developers can say “Now, finally, I can start writing fast apps. Thank you Google!”. Like if Google ever stopped you from doing so beforehand.

> “But AMP is new! `<amp-img>` does so much more than `<img>`!”

It might, but what stops Google, if it really has an intention to help, from releasing it as a regular JS library?

So hype worked, lots of developers bought the cover story and rushed creating a parallel version of every webpage they serve with “AMP-enabled” performance boost.

Before:

> “Hey boss, let’s rewrite our website to make it load fast!”
> “Fuck off!”
> “But studies show that every second of load time...”
> “I said fuck off!”

Now:

> “Hey boss, let’s rewrite our website with AMP. It’s a new tech by Google...”
> “Drop everything! Here, take $$$”
> “It also might improve...”
> “I don’t care. Get on this NOW!”

I’m not saying practices promoted by AMP are bad or useless. They are good practices. But nothing stops you from following them in the regular web. Nothing has ever stopped you from writing performant pages, from the very inception of the web. Google hardly invented CDNs and async script loading. But nobody cared because old tech and good practices are never as tempting as something branded as “new”.

# PWA

Enter PWA. Progressive Web Applications. Or Apps. Progressive Web Apps. Whatever.

So the idea was to be able to create a native-like experience but with web stack. What was the Web missing? Installing apps. Offline mode. Notifications (Ew). Working in the background. Yeah, that’s basically it. That’s it.

Again, I’m not going to say these things are wrong.[^1] They are not. If you want to create a native-like app using web technologies, you’ll have to use something like that. And it makes sense for apps like a shopping list or, I don’t know, alarm clock?

The problem with PWA is, well, there are two problems.

First is that most apps would be better off as websites rather than apps. Websites load each resource gradually, as it’s needed, unlike apps which have to fetch everything at install (that’s why app bundle sizes are usually way bigger than websites). Sites are more efficient but you can’t use them while offline.

But most “apps” today are online-only anyways! You can’t call Uber while being offline, and why would you open Uber app otherwise? Tinder is useless offline. You can’t date empty chat screens. You can’t join a meetup at Meetup.com without network connection. You can’t choose or book a hotel, you can’t transfer money or check your account balance offline. And nobody wants to re-read old cached tweets from Twitter or yesterday photos from Instagram. It just doesn’t make any sense.

So yeah, I would prefer those “apps” to be just websites. Believe it or not, there are benefits to that. I enjoy smaller download size, especially if I visit a site occasionally just for a quick look. I enjoy that websites do not consume my resources in the background.[^2] When I close it it unloads and does not constantly download new versions of its own libraries, which [developers frequently need to deploy](https://medium.com/@paularmstrong/d28a00e780a3#8255). I’m more than ready to sacrifice offline mode for that.

The second problem with PWA, and more relevant to our topic, is that it somehow [got associated with performance](https://www.thinkwithgoogle.com/intl/en-154/insights-inspiration/case-studies/trivago-embrace-progressive-web-apps-as-the-future-of-mobile/).

The thing is, it has nothing to do with performance. I mean, nothing _new_. You were always able to cache resources to make navigation between pages quick, and browsers are pretty good at doing so. With HTTP/2 you can efficiently fetch resources in bulk and even push resources from the server for a “more instant” experience.

So managing resource cache yourself, in a ServiceWorker, seems more like a burden than a blessing. HTTP caching is also declarative, well-tested and well understood at this point, in other words, hard to screw up. Which you can’t say about your ServiceWorker. Caching is one of two [hardest things in Computer Science](https://www.martinfowler.com/bliki/TwoHardThings.html). I personally [had a bad experience](https://twitter.com/nikitonsky/status/1064899552069722112) with Meetup.com PWA when an error in their cache code made the whole site unusable to the point where it wouldn’t open meetup pages. And unlike HTTP, it’s not that easy to reset. Nope, refresh didn’t help.

But it would’ve been ok if ServiceWorker was a tradeoff: you pay complexity fee but get exciting new capabilities. Except you don’t. Nothing useful that you can do with ServiceWorker you can’t do with HTTP cache/AJAX/REST/Local Storage. It’s just a complexity hole you'll sink countless workhours in.

PWA, as well as AMP, doesn’t even guarantee your website would be anywhere near “fast” or “instant”. It’s kind of funny how [Tinder case study](https://medium.com/@addyosmani/78919d98ece0) shows that login screen (one text input, one button, one SVG logo and a background gradient) takes 5 seconds to load on a 4G connection! I mean, they had to add loader for 2-5 seconds so users don’t close this bullshit immediately. And they call it fast.

This is fast:

wikipedia@2x.mp4

How did they do it? By fucking caring about performance. As simple as that.

Oh, also not serving a gazillion of JavaScript bundles and not rendering on a client with React served over GraphQL via fetch polyfill. That probably helped too.

airbnb.jpg

ServiceWorker or AMP, if your landing page is 170+ requests for 3.1 Mb for an image and four form fields, it can’t load fast no matter how many new frameworks you throw at it.


# Verdict

So what’s the verdict? To write fast websites with AMP and PWA you still need to understand performance optimization deeply. Without that, the only choice you have is to go with the hype.

But remember that neither AMP nor PWA would magically make your website any faster than say just a regular rewrite would.

Airbnb famous 800Kb index page. I would expect more care perf-wise from 900+ developers with average salary of $290,000/year:

index.jpg
Even SublimeText gives up highlighting this bullshit at some point.

Once you understand performance, though, you’ll notice you need neither AMP nor PWA. Just stop doing bullshit and web suddenly starts to work _instantly_. AMP didn’t invent CDN and `<noscript>`. PWA didn’t invent caching. Static web still runs circles around any modern-day much-hyped framework.

oldweb.png

“But the users! They want our fancy-schmancy interactivity. They DEMAND animations!”

I’ll tell you one thing. No one enjoys staring at the loading screen for 5 seconds. Loader being animated doesn’t make any difference. If you can’t into performance, at least don’t pretend it’s a feature.

[^1]: Although I don’t think we need more notifications in our life either. Especially <a href="https://grumpy.website/post/0PKEDf3JE">not from random web pages we visit</a>. Even not from native apps—I keep my phone in permanent Do Not Disturb mode with a short list of whitelisted apps.
[^2]: By the way, since you first opened this article my ServiceWorker has downloaded <code id="downloaded">0 Kb</code> of useless data in background. I hope you are on WiFi :)

<script>
if (!localStorage.getItem("first_opened"))
  localStorage.setItem("first_opened", "" + new Date().getTime());
var first_opened = parseInt(localStorage.getItem("first_opened"));

function update_downloaded() {
  var downloaded_kb = Math.ceil((new Date().getTime() - first_opened) / 200);
      text = downloaded_kb >= 1000000 ? Math.ceil(downloaded_kb / 1000) / 1000 + " Gb"
             : downloaded_kb >= 1000 ? downloaded_kb / 1000 + " Mb"
             : downloaded_kb + " Kb";
  document.getElementById("downloaded").innerHTML = text;
}
update_downloaded();
setInterval(update_downloaded, 1000);

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js', {scope: '/'}).then(function(registration) {
    console.log('Service worker registration succeeded:', registration);
  });
}
</script>