---
title: "The Absolute Minimum Every Software Developer Must Know About Unicode in 2023 (Still No Excuses!)"
summary: "Modern extension to classic 2003 article by Joel Spolsky"
published: 2023-10-02
hackernews_id: 37735801
starred: true
---

_Translations: [French](https://www.outofpluto.com/blog/nikita-prokopov-must-read-article-about-utf-8/) [Chinese](https://blog.xinshijiededa.men/unicode/) [Russian](https://habr.com/ru/companies/wunderfund/articles/777850/) [Spanish](es/)_

Twenty years ago, [Joel Spolsky wrote](https://www.joelonsoftware.com/2003/10/08/the-absolute-minimum-every-software-developer-absolutely-positively-must-know-about-unicode-and-character-sets-no-excuses/):

> There Ain’t No Such Thing As Plain Text.
> It does not make sense to have a string without knowing what encoding it uses. You can no longer stick your head in the sand and pretend that “plain” text is ASCII.

A lot has changed in 20 years. In 2003, the main question was: what encoding is this?

In 2023, it’s no longer a question: with a 98% probability, it’s UTF-8. Finally! We can stick our heads in the sand again!

utf8_trend@2x.png

The question now becomes: how do we use UTF-8 correctly? Let’s see!

# What is Unicode?

Unicode is a standard that aims to unify all human languages, both past and present, and make them work with computers.

In practice, Unicode is a table that assigns unique numbers to different characters. 

For example:

- The Latin letter `A` is assigned the number `65`.
- The Arabic Letter Seen `س` is `1587`.
- The Katakana Letter Tu `ツ` is `12484`
- The Musical Symbol G Clef `𝄞` is `119070`.
- <code class="emoji">💩</code> is `128169`.

Unicode refers to these numbers as _code points_.

Since everybody in the world agrees on which numbers correspond to which characters, and we all agree to use Unicode, we can read each other’s texts.

.loud Unicode == character ⟷ code point.

# How big is Unicode?

Currently, the largest defined code point is 0x10FFFF. That gives us a space of about 1.1 million code points.

About 170,000, or 15%, are currently defined. An additional 11% are reserved for private use. The rest, about 800,000 code points, are not allocated at the moment. They could become characters in the future.

Here’s roughly how it looks:

overview@2x.png

Large square == plane == 65,536 characters. Small one == 256 characters. The entire ASCII is half of a small red square in the top left corner.

# What’s Private Use?

These are code points reserved for app developers and will never be defined by Unicode itself.

For example, there’s no place for the Apple logo in Unicode, so Apple puts it at `U+F8FF` which is within the Private Use block. In any other font, it’ll render as missing glyph `􀣺`, but in fonts that ship with macOS, you’ll see ![](apple-logo@2x.png).

The Private Use Area is mostly used by icon fonts:

nerd_font@2x.png
Isn’t it a beauty? It’s all text!

# What does `U+1F4A9` mean?

It’s a convention for how to write code point values. The prefix `U+` means, well, Unicode, and `1F4A9` is a code point number in hexadecimal.

Oh, and `U+1F4A9` specifically is <code class="emoji">💩</code>.

# What’s UTF-8 then?

UTF-8 is an encoding. Encoding is how we store code points in memory.

The simplest possible encoding for Unicode is UTF-32. It simply stores code points as 32-bit integers. So `U+1F4A9` becomes `00 01 F4 A9`, taking up four bytes. Any other code point in UTF-32 will also occupy four bytes. Since the highest defined code point is `U+10FFFF`, any code point is guaranteed to fit.

UTF-16 and UTF-8 are less straightforward, but the ultimate goal is the same: to take a code point and encode it as bytes.

Encoding is what you’ll actually deal with as a programmer.

# How many bytes are in UTF-8?

UTF-8 is a variable-length encoding. A code point might be encoded as a sequence of one to four bytes.

This is how it works:

<table>
  <thead>
    <tr>
      <th>Code point</th>
      <th>Byte 1</th>
      <th>Byte 2</th>
      <th>Byte 3</th>
      <th>Byte 4</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>U+<code>0000</code>..<code>007F</code></td>
      <td><code>0xxxxxxx</code></td>
    </tr>
    <tr>
      <td>U+<code>0080</code>..<code>07FF</code></td>
      <td><code>110xxxxx</code></td>
      <td><code>10xxxxxx</code></td>
    </tr>
    <tr>
      <td>U+<code>0800</code>..<code>FFFF</code></td>
      <td><code>1110xxxx</code></td>
      <td><code>10xxxxxx</code></td>
      <td><code>10xxxxxx</code></td>
    </tr>
    <tr>
      <td>U+<code>10000</code>..<code>10FFFF</code></td>
      <td><code>11110xxx</code></td>
      <td><code>10xxxxxx</code></td>
      <td><code>10xxxxxx</code></td>
      <td><code>10xxxxxx</code></td>
    </tr>
  </tbody>
</table>

If you combine this with the Unicode table, you’ll see that English is encoded with 1 byte, Cyrillic, Latin European languages, Hebrew and Arabic need 2, and Chinese, Japanese, Korean, other Asian languages, and Emoji need 3 or 4.

A few important points here:

First, UTF-8 is byte-compatible with ASCII. The code points 0..127, the former ASCII, are encoded with one byte, and it’s the same exact byte. `U+0041` (`A`, Latin Capital Letter A) is just `41`, one byte.

Any pure ASCII text is also a valid UTF-8 text, and any UTF-8 text that only uses codepoints 0..127 can be read as ASCII directly.

Second, UTF-8 is space-efficient for basic Latin. That was one of its main selling points over UTF-16. It might not be fair for texts all over the world, but for technical strings like HTML tags or JSON keys, it makes sense.

On average, UTF-8 tends to be a pretty good deal, even for non-English computers. And for English, there’s no comparison.

Third, UTF-8 has error detection and recovery built-in. The first byte’s prefix always looks different from bytes 2-4. This way you can always tell if you are looking at a complete and valid sequence of UTF-8 bytes or if something is missing (for example, you jumped it the middle of the sequence). Then you can correct that by moving forward or backward until you find the beginning of the correct sequence.

And a couple of important consequences:

- You CAN’T determine the length of the string by counting bytes.
- You CAN’T randomly jump into the middle of the string and start reading.
- You CAN’T get a substring by cutting at arbitrary byte offsets. You might cut off part of the character.

Those who do will eventually meet this bad boy: �

# What’s �?

`U+FFFD`, the Replacement Character, is simply another code point in the Unicode table. Apps and libraries can use it when they detect Unicode errors.

If you cut half of the code point off, there’s not much left to do with the other half, except displaying an error. That’s when � is used.

```
var bytes = "Аналитика".getBytes("UTF-8");
var partial = Arrays.copyOfRange(bytes, 0, 11);
new String(partial, "UTF-8"); // => "Анал�"
```

# Wouldn’t UTF-32 be easier for everything?

NO.

UTF-32 is great for operating on code points. Indeed, if every code point is always 4 bytes, then `strlen(s) == sizeof(s) / 4`, `substring(0, 3) == bytes[0, 12]`, etc.

The problem is, you don’t want to operate on code points. A code point is not a unit of writing; one code point is not always a single character. What you should be iterating on is called “__extended grapheme clusters__”, or graphemes for short.

A grapheme is a minimally distinctive unit of writing in the context of a particular writing system. `ö` is one grapheme. `é` is one too. And `각`. Basically, grapheme is what the user thinks of as a single character.

The problem is, in Unicode, some graphemes are encoded with multiple code points!

graphemes@2x.png

For example, `é` (a single grapheme) is encoded in Unicode as `e` (U+0065 Latin Small Letter E) + `´` (U+0301 Combining Acute Accent). Two code points!

It can also be more than two:

- <code class="emoji">☹️</code> is `U+2639` + `U+FE0F`
- <code class="emoji">👨‍🏭</code> is `U+1F468` + `U+200D` + `U+1F3ED`
- <code class="emoji">🚵🏻‍♀️</code> is `U+1F6B5` + `U+1F3FB` + `U+200D` + `U+2640` + `U+FE0F`
- <code style="font-family: unset">y̖̠͍̘͇͗̏̽̎͞</code> is `U+0079` + `U+0316` + `U+0320` + `U+034D` + `U+0318` + `U+0347` + `U+0357` + `U+030F` + `U+033D` + `U+030E` + `U+035E`

There’s no limit, as far as I know.

Remember, we are talking about code points here. Even in the widest encoding, UTF-32, <code class="emoji">👨‍🏭</code> will still take three 4-byte units to encode. And it still needs to be treated as a single character.

If the analogy helps, we can think of the Unicode itself (without any encodings) as being variable-length.

.loud An Extended Grapheme Cluster is a sequence of one or more Unicode code points that must be treated as a single, unbreakable character.

Therefore, we get all the problems we have with variable-length encodings, but now on code point level: you can’t take only a part of the sequence, it always should be selected, copied, edited, or deleted as a whole.

Failure to respect grapheme clusters leads to bugs like this:

error1.png

or this:

intellij@2x.mp4
Just to be clear: this is NOT correct behavior

Using UTF-32 instead of UTF-8 will not make your life any easier in regards to extended grapheme clusters. And extended grapheme clusters is what you should care about.

.loud Code points — 🥱. Graphemes — 😍

# Is Unicode hard only because of emojis?

Not really. Extended Grapheme Clusters are also used for alive, actively used languages. For example:

- `ö` (German) is a single character, but multiple code points (`U+006F U+0308`).
- `ą́` (Lithuanian) is `U+00E1 U+0328`.
- `각` (Korean) is `U+1100 U+1161 U+11A8`.

So no, it’s not just about emojis.

# What’s "🤦🏼‍♂️".length?

The question is inspired by [this brilliant article](https://hsivonen.fi/string-length/).

Different programming languages will happily give you different answers.

Python 3:

```
>>> len("🤦🏼‍♂️")
5
```

JavaScript / Java / C#:

```
>> "🤦🏼‍♂️".length 
7
```

Rust:

```
println!("{}", "🤦🏼‍♂️".len());
// => 17
```

As you can guess, different languages use different internal string representations (UTF-32, UTF-16, UTF-8) and report length in whatever units they store characters in (ints, shorts, bytes).

BUT! If you ask any normal person, one that isn’t burdened with computer internals, they’ll give you a straight answer: 1. The length of <code class="emoji">🤦🏼‍♂️</code> string is 1.

That’s what extended grapheme clusters are all about: what _humans_ perceive as a single character. And in this case, <code class="emoji">🤦🏼‍♂️</code> is undoubtedly a single character.

The fact that <code class="emoji">🤦🏼‍♂️</code> consists of 5 code points (`U+1F926 U+1F3FB U+200D U+2642 U+FE0F`) is mere implementation detail. It should not be broken apart, it should not be counted as multiple characters, the text cursor should not be positioned inside it, it shouldn’t be partially selected, etc.

For all intents and purposes, this is an atomic unit of text. Internally, it could be encoded whatever, but for user-facing API, it should be treated as a whole.

The only two modern languages that get it right are Swift:

```
print("🤦🏼‍♂️".count)
// => 1
```

and Elixir:

```
String.length("🤦🏼‍♂️")
// => 1
```

Basically, there are two layers:

1. Internal, computer-oriented. How to copy strings, send them over the network, store on disk, etc. This is where you need encodings like UTF-8. Swift uses UTF-8 internally, but it might as well be UTF-16 or UTF-32. What's important is that you only use it to copy strings as a whole and never to analyze their content.

2. External, human-facing API. Character count in UI. Taking first 10 characters to generate preview. Searching in text. Methods like `.count` or `.substring`. Swift gives you _a view_ that pretends the string is a sequence of grapheme clusters. And that view behaves like any human would expect: it gives you 1 for `"🤦🏼‍♂️".count`.

I hope more languages adopt this design soon.

Question to the reader: what to you think <code style="font-family: unset">"ẇ͓̞͒͟͡ǫ̠̠̉̏͠͡ͅr̬̺͚̍͛̔͒͢d̠͎̗̳͇͆̋̊͂͐".length</code> should be?

# How do I detect extended grapheme clusters then?

Unfortunately, most languages choose the easy way out and let you iterate through strings with 1-2-4-byte chunks, but not with grapheme clusters.

It makes no sense and has no semantics, but since it’s the default, programmers don’t think twice, and we see corrupted strings as the result:

stdlib@2x.png

“I know, I’ll use a library to do strlen()!” — nobody, ever.

But that’s exactly what you should be doing! Use a proper Unicode library! Yes, for basic stuff like `strlen` or `indexOf` or `substring`!

For example:

1. C/C++/Java: use [ICU](https://github.com/unicode-org/icu). It’s a library from Unicode itself that encodes all the rules about text segmentation.

1. C#: use `TextElementEnumerator`, which is kept up to date with Unicode as far as I can tell.

1. Swift: just stdlib. Swift does the right thing by default.

1. UPD: Erlang/Elixir seem to be doing the right thing, too.

1. For other languages, there’s probably a library or binding for ICU.

1. Roll your own. Unicode [publishes](https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries) rules and tables in a machine-readable format, and all the libraries above are based on them.

But whatever you choose, make sure it’s on the recent enough version of Unicode (15.1 at the moment of writing), because the definition of graphemes changes from version to version. For example, Java’s `java.text.BreakIterator` is a no-go: it’s based on a very old version of Unicode and not updated.

.loud Use a library

IMO, the whole situation is a shame. Unicode should be in the stdlib of every language by default. It’s the lingua franca of the internet! It’s not even new: we’ve been living with Unicode for 20 years now.

# Wait, rules are changing?

Yes! Ain’t it cool?

(I know, it ain’t)

Starting roughly in 2014, Unicode has been releasing a major revision of their standard every year. This is where you get your new emojis from — Android and iOS updates in the Fall usually include the newest Unicode standard among other things.

versions@2x.png

What’s sad for us is that the rules defining grapheme clusters change every year as well. What is considered a sequence of two or three separate code points today might become a grapheme cluster tomorrow! There’s no way to know! Or prepare!

Even worse, different versions of your own app might be running on different Unicode standards and report different string lengths!

But that’s the reality we live in. You don’t really have a choice here. You can’t ignore Unicode or Unicode updates if you want to stay relevant and provide a decent user experience. So, buckle up, embrace, and update.

.loud Update yearly

# Why is "Å" !== "Å" !== "Å"?

spider_men@2x.jpg

Copy any of these to your JavaScript console:

```
"Å" === "Å"
"Å" === "Å"
"Å" === "Å"
```

What do you get? False? You should get false, and it’s not a mistake.

Remember earlier when I said that `ö` is two code points, `U+006F U+0308`? Basically, Unicode offers more than one way to write characters like `ö` or `Å`. You can:

1. Compose `Å` from normal Latin `A` + a combining character,
2. OR there’s also a pre-composed code point `U+00C5` that does that for you.

They will look the same (`Å` vs `Å`), they should work the same, and for all intents and purposes, they are considered exactly the same. The only difference is the byte representation.

That’s why we need normalization. There are four forms:

**NFD** tries to explode everything to the smallest possible pieces, and also sorts pieces in a canonical order if there is more than one.

**NFC**, on the other hand, tries to combine everything into pre-composed form if one exists.

normalization@2x.png

For some characters there are also multiple versions of them in Unicode. For example, there’s `U+00C5 Latin Capital Letter A with Ring Above`, but there’s also `U+212B Angstrom Sign` which looks the same. 

These are also replaced during normalization:

normalization_clones@2x.png

NFD and NFC are called “canonical normalization”. Another two forms are “compatibility normalization”:

**NFKD** tries to explode everything and replaces visual variants with default ones.

**NFKC** tries to combine everything while replacing visual variants with default ones.

normalization_compat@2x.png

Visual variants are separate Unicode code points that represent the same character but are supposed to render differently. Like, `①` or `⁹` or `𝕏`. We want to be able to find both `"x"` and `"2"` in a string like `"𝕏²"`, don’t we?

x_variants@2x.png
All of these have their own code points, but they are also all Xs

Why does the `ﬁ` ligature even have its own code point? No idea. A lot can happen in a million characters.

.loud Before comparing strings or searching for a substring, normalize!

# Unicode is locale-dependent

The Russian name Nikolay is written like this:

nikolay_ru.png

and encoded in Unicode as `U+041D 0438 043A 043E 043B 0430 0439`.

The Bulgarian name Nikolay is written:

nikolay_bg.png 

and encoded in Unicode as `U+041D 0438 043A 043E 043B 0430 0439`. Exactly the same!

Wait a second! How does the computer know when to render Bulgarian-style glyphs and when to use Russian ones?

Short answer: it doesn’t. Unfortunately, Unicode is not a perfect system, and it has many shortcomings. Among them is assigning the same code point to glyphs that are supposed to look differently, like Cyrillic Lowercase K and Bulgarian Lowercase K (both are `U+043A`).

From what I understand, Asian people [get it much worse](https://en.wikipedia.org/wiki/Han_unification): many Chinese, Japanese, and Korean logograms that are written very differently get assigned the same code point:

han.png
U+8FD4 in different locales

Unicode motivation is to save code points space (my guess). Information on how to render is supposed to be transferred outside of the string, as locale/language metadata.

Unfortunately, it fails the original goal of Unicode:

> [...] no escape sequence or control code is required to specify any character in any language.

In practice, dependency on locale brings a lot of problems:

1. Being metadata, locale often gets lost.

1. People are not limited to a single locale. For example, I can read and write English (USA), English (UK), German, and Russian. Which locale should I set my computer to?

1. It’s hard to mix and match. Like Russian names in Bulgarian text or vice versa. Why not? It’s the internet, people from all cultures hang out here.

1. There’s no place to specify the locale. Even making the two screenshots above was non-trivial because in most software, there’s no dropdown or text input to change locale.

1. When needed, it had to be guessed. For example, Twitter tries to guess the locale from the text of the tweet itself (because where else could it get it from?) and sometimes gets it wrong:

twitter_locale.jpg https://twitter.com/nikitonsky/status/1171115067112398849

# Why does `String::toLowerCase()` accepts Locale as an argument?

Another unfortunate example of locale dependence is the Unicode handling of dotless `i` in the Turkish language.

Unlike English, Turks have two `I` variants: dotted and dotless. Unicode decided to reuse `I` and `i` from ASCII and only add two new code points: `İ` and `ı`.

Unfortunately, that made `toLowerCase`/`toUpperCase` behave differently on the same input:

```
var en_US = Locale.of("en", "US");
var tr = Locale.of("tr");

"I".toLowerCase(en_US); // => "i"
"I".toLowerCase(tr);    // => "ı"

"i".toUpperCase(en_US); // => "I"
"i".toUpperCase(tr);    // => "İ"
```

So no, you can’t convert string to lowercase without knowing what language that string is written in.

# I live in the US/UK, should I even care?

english@2x.png

- quotation marks `“` `”` `‘` `’`, 
- apostrophe `’`,
- dashes `–` `—`,
- different variations of spaces (figure, hair, non-breaking),
- bullets `•` `■` `☞`,
- currency symbols other than the `$` (kind of tells you who invented computers, doesn’t it?): `€` `¢` `£`,
- mathematical signs—plus `+` and equals `=` are part of ASCII, but minus `−` and multiply `×` are not <nobr>¯\_(ツ)_/¯</nobr>,
- various other signs `©` `™` `¶` `†` `§`.

Hell, you can’t even spell `café`, `piñata`, or `naïve` without Unicode. So yes, we are all in it together, even Americans.

Touché.

# What are surrogate pairs?

That goes back to Unicode v1. The first version of Unicode was supposed to be fixed-width. A 16-bit fixed width, to be exact:

unicode1@2x.png
Version 1.0 of the Unicode Standard, October 1991

They believed 65,536 characters would be enough for all human languages. They were almost right!

When they realized they needed more code points, UCS-2 (an original version of UTF-16 without surrogates) was already used in many systems. 16 bit, fixed-width, it only gives you 65,536 characters. What can you do?

Unicode decided to allocate some of these 65,536 characters to encode higher code points, essentially converting fixed-width UCS-2 into variable-width UTF-16.

A surrogate pair is two UTF-16 units used to encode a single Unicode code point. For example, `D83D DCA9` (two 16-bit units) encodes _one_ code point, `U+1F4A9`.

The top 6 bits in surrogate pairs are used for the mask, leaving 2×10 free bits to spare:

```
   High Surrogate          Low Surrogate
        D800        ++          DC00
1101 10?? ???? ???? ++ 1101 11?? ???? ????
```

Technically, both halves of the surrogate pair can be seen as Unicode code points, too. In practice, the whole range from `U+D800` to `U+DFFF` is allocated as “for surrogate pairs only”. Code points from there are not even considered valid in any other encodings.

bmp@2x.png
This space on a very crammed Basic Multilingual Plane will never be used for anything good ever again

# Is UTF-16 still alive?

Yes!

The promise of a fixed-width encoding that covers all human languages was so compelling that many systems were eager to adopt it. Among them were Microsoft Windows, Objective-C, Java, JavaScript, .NET, Python 2, QT, SMS, and CD-ROM!

Since then, Python has moved on, CD-ROM has become obsolete, but the rest is stuck with UTF-16 or even UCS-2. So UTF-16 lives there as in-memory representation.

In practical terms today, UTF-16 has roughly the same usability as UTF-8. It’s also variable-length; counting UTF-16 units is as useless as counting bytes or code points, grapheme clusters are still a pain, etc. The only difference is memory requirements.

The only downside of UTF-16 is that everything else is UTF-8, so it requires conversion every time a string is read from the network or from disk.

Also, fun fact: the number of planes Unicode has (17) is defined by how much you can express with surrogate pairs in UTF-16.

# Conclusion

To sum it up:

- Unicode has won.
- UTF-8 is the most popular encoding for data in transfer and at rest.
- UTF-16 is still sometimes used as an in-memory representation.
- The two most important views for strings are bytes (allocate memory/copy/encode/decode) and extended grapheme clusters (all semantic operations).
- Using code points for iterating over a string is wrong. They are not the basic unit of writing. One grapheme could consist of multiple code points.
- To detect grapheme boundaries, you need Unicode tables.
- Use a Unicode library for everything Unicode, even boring stuff like `strlen`, `indexOf` and `substring`.
- Unicode updates every year, and rules sometimes change.
- Unicode strings need to be normalized before they can be compared.
- Unicode depends on locale for some operations and for rendering.
- All this is important even for pure English text.

Overall, yes, Unicode is not perfect, but the fact that

1. an encoding exists that covers all possible languages at once,
2. the entire world agrees to use it,
3. we can completely forget about encodings and conversions and all that stuff

is a miracle. Send this to your fellow programmers so they can learn about it, too.

.loud There’s such a thing as plain text, and it’s encoded with UTF-8.

Thanks Lev Walkin and my patrons for reading early drafts of this article.