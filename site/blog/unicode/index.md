---
title: "The Absolute Minimum Every Software Developer Must Know About Unicode in 2023 (Still No Excuses!)"
summary: "Modern extension to classic 2003 article by Joel Spolsky"
css: style2.css
---

20 years ago [Joel Spolsky wrote](https://www.joelonsoftware.com/2003/10/08/the-absolute-minimum-every-software-developer-absolutely-positively-must-know-about-unicode-and-character-sets-no-excuses/):

> There Ain’t No Such Thing As Plain Text.
> It does not make sense to have a string without knowing what encoding it uses. You can no longer stick your head in the sand and pretend that “plain” text is ASCII.

A lot has changed in 20 years. In 2003, the main question was: what encoding is this?

In 2023, it’s no longer a question: with 98% probability, it’s UTF-8. Finally! We can stick our heads in the sand again!

utf8_trend@2x.png

Unfortunatelly, there’re still a few more tricks you need to learn to work with Unicode/UTF-8 correctly.

# What is Unicode?

Unicode is a standard that tries to unify all human languages, past and present, and make them work with computers.

In practice, Unicode is a table that assigns unique numbers to different characters. 

For example,

- Latin letter `A` gets a number of `65`.
- Arabic Letter Seen `س` is `1587`.
- Katakana Letter Tu `ツ` is `12484`
- Musical Symbol G Clef `𝄞` is `119070`.
- <code class="emoji">💩</code> is `128169`.

Unicode calls these numbers _code points_.

Since everybody in the world agrees which numbers correspond to which characters, and we all agree to use Unicode, we can read eath other’s texts.

.loud Unicode == character ⟷ code point.

# How big is Unicode?

Currently, largest defined codepoint is 0x10FFFF. That gives us a space of about 11 million codepoints.

About 170K, or 15%, are currently defined. 11% more are reserved for private use. The rest, or about 800K codepoints, are currently not even allocated. Meaning, they can become characters in the future.

Here’s is roughly it looks:

overview@2x.png

Large square == plane == 65,536 characters. Small one == 256 characters. Entire ASCII is half of small red square in the top left corner.

# What’s Private Use?

These are codepoints that are reserved for app developers and will never be defined by Unicode itself.

For example, there’s no place for Apple logo in Unicode, so Apple puts it at `U+1008FA` which is in Private Use block. In any other font, it’ll render as missing glyph `􀣺`, but in Apple’s own San Francisco, you’ll see ![](apple-logo@2x.png).

The place it’s most used is probably icon fonts:

nerd_font@2x.png
Isn’t it a beauty?

# What does `U+1F4A9` mean?

It’s a convention how to write codepoint values. Prefix `U+` means, well, Unicode, and `1F4A9` is codepoint number in hexadecimal.

Oh, and `U+1F4A9` speficically is <code class="emoji">💩</code>.

# What’s UTF-8 then?

UTF-8 is an encoding. Encoding is how we store codepoints in memory.

The simplest possible encoding for Unicode is UTF-32. It just stores codepoints as 32-bit integers. So `U+1F4A9` becomes `00 01 F4 A9`, four bytes. Any other codepoint in UTF-32 will also take up four bytes. Since highest defined codepoint is 0x10FFFF, any codepoint is guaranteed to fit.

UTF-16 and UTF-8 are less straightforward, but the ultimate goal is the same: take a codepoint and encode it as bytes.

Encoding is what you’ll actually deal with as a programmer.

# So, how many bytes are in UTF-8?

UTF-8 is a variable-length encoding. Meaning, a codepoint might be encoded as a sequence of one to four bytes.

This is how it works:

<table>
  <thead>
    <tr>
      <th>Codepoint</th>
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

If you combine this with Unicode table, you’ll see that English is encoded with 1 byte, Cyrillic and Latin European languages, Hebrew and Arabic need 2, and Chinese, Japanese, Korean, other Asian and Emoji need 3 or 4.

Few important points there:

First, UTF-8 is byte-compatible with ASCII. Codepoints 0..127, former ASCII, are encoded with one byte, and it’s the same exact byte. `U+0041` (`A`, Latin Capital Letter A) is just `41`, one byte. Any pure ASCII text is also a valid UTF-8 text, and vice versa.

Second, UTF-8 is space-efficient for basic Latin. That was one of its main selling points over UTF-16. It might not be fair for texts all over the world, but for technical strings like e.g. HTML tags, it makes sense. So, on average, UTF-8 tends to be a pretty good deal, even for non-English computers. And for English there’s no comparison.

Third, UTF-8 has error detection built-in. As you can see, first byte prefix always looks different from bytes 2-4. So if can always tell if you are looking at complete and valid sequence of UTF-8 bytes or something is missing.

And a couple of important consequences:

- You CAN’t determine length of the string by counting bytes.
- You CAN’T randomly jump into the middle of the string and start reading.
- You CAN’T get substring by cutting at arbitrary byte offsets. You might cut off part of the character.

Those who do will eventually meet this bad boy: �

# What’s �?

� U+FFFD Replacement Character is just another codepoint in Unicode table. Apps and libraries can use it when they detect Unicode errors.

When you cut half of the codepoint off, there’s not much left to do with other half, except trying to display error. That’s when � is used.

```
var bytes = "Аналитика".getBytes("UTF-8");
var partial = Arrays.copyOfRange(bytes, 0, 11);
new String(partial, "UTF-8"); // => "Анал�"
```

# Wouldn’t UTF-32 be easier for everything?

It’s easy to think that if we allocate 4 bytes for every codepoint, some things become easier. Like, `strlen(s) == sizeof(s) / 4`, taking first 3 characters is just taking 12 bytes, etc.

Unfortunately, no. The problem is called “extended grapheme clusters”, or graphemes for short.

A grapheme is a minimally distinctive unit of writing in the context of a particular writing system. Basically, it’s what user thinks of as a character.

The problem is, in Unicode, some graphemes are encoded with multiple codepoints!

For example, `ö` (a single grapheme) is encoded in Unicode as `o` (U+006F Latin Small Letter O) + `¨` (U+0308 Combining Diaeresis). Two codepoints!

Same goes for emoji, for example:

- <code class="emoji">☹️</code> is `U+2639` + `U+FE0F`
- <code class="emoji">🇺🇳</code> is `U+1F1FA` + `U+1F1F3`
- <code class="emoji">🚵🏻‍♀️</code> is `U+1F6B5` + `U+1F3FB` + `U+200D` + `U+2640` + `U+FE0F`

These can get quite long. There’s no limit, as far as I know.

Anyways. The problem is, just like with UTF-8, you can’t just take part of that sequence and expect anything good happen. What is considered a single unit of writing by human should be added, copied, edited or deleted as a whole.

Otherwise, you get bugs like this:

error1.png

or this:

intellij@2x.mp4
Just to be clear: this is NOT a correct behavior

That’s why UTF-32 is not the answer. To correctly work with Unicode text, you have to be aware of extended grapheme clusters, and they span multiple codepoints anyway. So, fixed-length encoding wouldn’t help with anything.

.loud Code points alone are not enough!

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

BUT! If you ask any normal person, one that isn’t burdened with computers internals, they’ll give you a straight answer: 1. The length of <code class="emoji">🤦🏼‍♂️</code> string is 1.

That’s what extended grapheme clusters are all about: what _humans_ percieve as a single character. And in this case, <code class="emoji">🤦🏼‍♂️</code> is undoubtedly a single character.

The fact that <code class="emoji">🤦🏼‍♂️</code> consists of 5 codepoints (`U+1F926 U+1F3FB U+200D U+2642 U+FE0F`) is mere implementation detail. It should not be breaked apart, it should not be counted as multiple characters, text cursor should not be positioned inside it, it shouldn’t be partially selected, etc. For all intents and purposes, this is an atomic unit of text. Any other representations do more harm than good.

The only modern language that gets it right is Swift:

```
print("🤦🏼‍♂️".count)
// => 1
```

Hope more languages will get on board soon.

Oh, and by the way,

.loud "ẇ͓̞͒͟͡ǫ̠̠̉̏͠͡ͅr̬̺͚̍͛̔͒͢d̠͎̗̳͇͆̋̊͂͐".length === 4

# What if my app doesn’t use Emoji?

No matter. Extended Grapheme Clusters are for alive, actively used languages, too. For example:

- `ö` (German) is a single character, but multiple code points (`U+006F U+0308`).
- `ą́` (Lithuanian) is `U+00E1 U+0328`.
- `각` (Korean) is `U+1100 U+1161 U+11A8`.

So no, it’s not only about Emoji.

# I live in US/UK, should I even care?

english@2x.png

- proper quotation marks `“` `”` `‘` `’`, 
- proper apostrope `’`,
- proper dashes `–` `—`,
- different variations of spaces (figure, hair, non-breaking),
- bullets `•` `■` `☞`,
- currency symbols other than the `$` (kind of tells you who invented computers, doesn’t it?): `€` `¢` `£`,
- mathematical signs—plus `+` and equals `=` are part of ASCII, but minus `−` and multiply `×` are not <span>¯\_(ツ)_/¯</span>,
- various other signs `©` `™` `¶` `†` `§`.

Hell, you can’t even spell `café`, `piñata` or `naïve` without Unicode. So yes, we are all in it together, even Americans.

Touché.

# How do I detect extended grapheme clusters then?

Unfortunately, most languages choose the easy way out and let you iterate strings with 1-2-4 byte chunks, but not with grapheme clusters.

It makes no sense and has no semantic, but since it’s the default, programmers don’t think twice and we see corrupted strings as the result:

stdlib@2x.png

What you should be doing instead is using a proper Unicode library. For example:

1. C/C++/Java: use [ICU](https://github.com/unicode-org/icu). It’s a library from Unicode itself that encodes all the rules about text segmentation.

1. C#: use `TextElementEnumerator`, which is kept up to date with Unicode as far as I can tell.

1. Swift: just stdlib. Swift does the right thing by default.

1. For other languages, there’s probably a library or binding for ICU.

1. Roll your own. Unicode [publishes](https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries) rules and tables machine-readable format, and all the libraries above are based on them.

But whatever you choose, make sure it’s on recent enough version of Unicode (15.1 at the moment of writing), because definition of graphemes changes from version to version. For example, Java’s `java.text.BreakIterator` is no go: it’s based on very old version of Unicode and not updated.

IMO, the whole situation is a shame. Unicode should be in stdlib of every language by default. It’s lingua franca of the Internet! And it’s not new: we’ve been living with Unicode for 20 years now.

# Wait, rules are changing?

Yes! Ain’t it cool?

(I know, it ain’t)

Roughly starting from 2014, Unicode is releasing a major revision of their standard every year. This is where you get your new Emoji from — Android and iOS updates in the Fall usually include newest Unicode standard among other things. It also means that old systems can’t show new Emoji.

versions@2x.png

What’s sad for us, rules that define grapheme clusters change every year, too. What is considered a sequence of two or three separate codepoints today might become a grapheme cluster tomorrow! There’s no way to know! Or prepare!

Even worse, different versions of your own app might be running on different Unicode standards and report different string lengths!

But that’s the reality we live in. You don’t really have a choice here. You can’t ignore Unicode or Unicode updates if you want to stay relevant and you want decent user experience. So, buckle up, embrace, and update.

# Is UTF-16 still alive?

Yes!

A little bit of history. First version of Unicode was supposed to be fixed-width. 16-bit fixed width, to be exact:

unicode1@2x.png
Version 1.0 of the Unicode Standard, October 1991

They believed 65536 characterd would be enough for all human languages. They were almost right!

And that promise was so compelling many systems embedded it very deeply into their core. Among them were Microsoft Windows, Java, JavaScript, .NET, Python 2, QT, SMS and CD-ROM!

Since then, Python has moved on, but the rest is stuck with UTF-16 or with UCS-2 even. So UTF-16 lives there as memory representation.

It also warms my heart when I remember that every time I store, send or receive a string, there’s a conversion overhead from UTF-8 to UTF-16 to get it in memory. Well, nobody is perfect.

# What are surrogate pairs?

That goes back to Unicode v1 again. When they realized they need more codepoints, UCS-2 was already used in all these systems. 16 bit, fixed-width, it only gives you 65536 characters. What can you do?

Unicode decided to allocate some of these 65536 chars to encode higher codepoints, essentially converting fixed-width UCS-2 into variable-width UTF-16.

Surrogate pair is two UTF-16 units used to encode single Unicode codepoint. For example, `D83D DCA9` (two 16-bit units) encode _one_ codepoint, `U+1F4A9`.

Top 6 bits in surrogate pairs are used for mask, leaving 2×10 free bits to spare:

```
   High Surrogate          Low Surrogate
        D800        ++          DC00
1101 10?? ???? ???? ++ 1101 11?? ???? ????
```

Technically, both halves of surrogate pair can be seen as Unicode codepoints, too. In practice, the whole range from `U+D800` to `U+DFFF` is allocated as “for surrogate pairs only”. Codepoints from there are not even considered valid in any other encodings.

bmp@2x.png
This space on very crammed Basic Multilingual Plane will never be used for anything good ever again

# What about regexps?
# Normalization
# Unicode is locale-dependent

Russian name Nikolay is written like this:

nikolay_ru.png

and encoded in Unicode as `U+041D U+0438 U+043A U+043E U+043B U+0430 U+0439`.

Bulgarian name Nikolay is written:

nikolay_bg.png 

and encoded in Unicode as `U+041D U+0438 U+043A U+043E U+043B U+0430 U+0439`. Exactly the same!

Wait a second! How does computer know then when to render Bulgarian-style glyphs and when to use Russian ones?

Short answer: it doesn’t. Unfortunally, Unicode is not a perfect system and it has many shortcomings. Among them is assigning the same codepoint to glyphs that are supposed to look differently, like Cyrillic Lowercase K and Bulgarian Lowercase K (both are `U+043A`).

Unicode motivation is to save codepoints space. Information on how to render is supposed to be transferred outside of the string, as locale/language metadata.

Unfortunately, it fails the original goal of Unicode:

> [...] no escape sequence or control code is required to specify any character in any language.

In practice, dependency on locale brings a lot of problems:

1. Being metadata, locale often gets lost.

1. People are not limited to single locale. For example, I can read and write English (USA), English (UK), German, Russian. Which locale should I set my computer to?

1. It’s hard to mix and match. Like, Russian name in Bulgarian text or vice versa. Why not? It’s the internet, people from all cultures hang out here.

1. There’s no place to specify locale. Even making two screenshots above were non-trivial, because in most software there’s no dropdown or text input to change locale.

1. When needed, it had to be guessed. For example, Twitter tries to guess locale from text of the tweet itself (because where else could it get it from?) and sometimes gets it wrong:

twitter_locale.jpg https://twitter.com/nikitonsky/status/1171115067112398849

I’m using Bulgarian as an example because it’s what I personally experienced.

From what I understand, Asian people [get it much worse](https://en.wikipedia.org/wiki/Han_unification): many of Chinese, Japanese and Korean logograms that are written very differently get assigned the same codepoint:

han.png
U+8FD4 in different locales

Another unfortunate example of locale dependence is Unicode handling of dotless `i` in Turkish language.

Unlike English, Turks have two `I` variants: dotted and dotless. Unicode decided to reuse `I` and `i` from ASCII and only add two new codepoints: `İ` and `ı`.

Unfortunately, that made `toLowerCase`/`toUpperCase` behave differently on the same input:

```
var en_US = Locale.of("en", "US");
var tr = Locale.of("tr");

"I".toLowerCase(en_US); // => "i"
"I".toLowerCase(tr);    // => "ı"

"i".toUpperCase(en_US); // => "I"
"i".toUpperCase(tr);    // => "İ"
```

And that’s the main reason why there’s a `.toLowerCase` variant that accepts locale.

# Conclusion

It’s not even hard there days, or complicated. It’s a matter of not doing random thing and doing the right thing.

- Unicode is a miracle
- no reason not to use utf-8
- You only need two representations: grapheme clusters and bytes