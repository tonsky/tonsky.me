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

Unfortunately, most languages choose the easy way and let you iterate strings with 1-2-4 byte chunks, or sometimes codepoints, but not with grapheme clusters. And since it’s a default, people don’t think much and we get bugs like this:

error1.png

or this:

error2.png

or this:

error3@2x.png

What you should be doing is using a third-party library. For example:

1. Swift: nothing. Swift does the right thing by default.

1. C/C++/Java: use [ICU](https://github.com/unicode-org/icu). It’s a library from Unicode itself that encodes all the rules about text segmentation.

1. C#: use `TextElementEnumerator`, which is kept up to date with Unicode as far as I can tell.

1. For other languages, there’s probably a library or binding for ICU.

1. Roll your own. Unicode [publishes](https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries) rules and tables machine-readable format, and all the libraries above are based on them.

But whatever you choose, make sure it’s on recent enough version of Unicode (15.1 at the moment of writing), because definition of graphemes changes from version to version. For example, Java’s `java.text.BreakIterator` is no go: it’s based on very old version of Unicode and not updated.

IMO, the whole situation is a shame. Unicode should be in stdlib of every language by default. It’s lingua franca of the Internet! And it’s not new: we’ve been living with Unicode for 20 years now.

# Wait, rules are changing?



# What about regexps?

# Normalization

# Is UTF-16 still alive?

```
var bytes = "Analytics".getBytes("UTF-16");
var partial = Arrays.copyOfRange(bytes, 0, 11);
new String(partial, "UTF-16"); // => "Anal�"

var bytes = "Analytics".getBytes("UTF-16");
var partial = Arrays.copyOfRange(bytes, 1, 20);
new String(partial, "UTF-16"); // => "＀䄀渀愀氀礀琀椀挀�"
```

# What is surrogate pair?

# no reason not to use utf-8
# You only need two representations: grapheme clusters and bytes
# utf-16 is still in use
# only measures are byte length and extended grapheme clusters length
# Unicode is locale-dependent

Mixing C/J/K in a single document is impossible without metadata and special language specific fonts.

For example airlines can’t rely on Unicode to properly render passenger names

Oh, and yes that means that a single Unicode font that covers all language can never exist either.

# Unicode is updated once/twice a year
# Many ways to do it
# Regexps
# Conclusion

It’s not even hard there days, or complicated. It’s a matter of not doing random thing and doing the right thing.

# A quick note on UTF-16

I know, I know... I told you it’s not used anymore. Well, it is.

At the start of Unicode, all codepoints were planned to fit into 16 bit. That’s why Java, JavaScript and Windows were so keen to jump on that train. Fixed-width, 16-bit encoding is as convenient as ASCII. Unfortunately, 16 were not enough in the long run.

(We still pay price for this oversight, by the way. Each time Java or JavaScript receives a string from a disk or the network, it needs to be converted from UTF-8 to UTF-16. It’s not much, and you probably can’t do anything about it, so, you know, just another imperfection to feel sorry about.)

So, Unicode grew over 16 bits, and UTF-16 (called UCS-2 back then) had to be retrofitted.

They did that by blocking out two 16-bit ranges (U+D800..U+DBFF and U+DC00..U+DFFF) and assigning them no glyphs. These Unicode codepoints have no meaning on their own except for “we are used in UTF-16 to represent larger codepoints”. They are called “surrogate pairs”.

That’s a story of an encoding (technical detail) affecting Unicode table (a platonic ideal). Sorry, purists.

# So, how big is Unicode now?

Largest defined codepoint is 0x10FFFF. That’s about 11 million characters. This takes 21 bit to represent, but, surprisingly, does not cover all the space.

Technically, UTF-8 can go as high as 0x1FFFFF, but UTF-16 can’t, so, compromises. It’s plenty of space anyway, and most of it is unused.

This is the overview of the entire codepoint space:

overview@2x.png

On the picture, each large square is a Unicode _plane_. Plane fits 65,536 codepoints.

There are 17 planes total. Most are unallocated, i.e. reserved for future use. Plenty of space for new emojis.

Second plane contains mostly dead languages and some emoji.

Third and fourth are dedicated entirely for CJK (Chinese, Japanes, Korean).

Last two can be used freely by app developers. For example, icon fonts put assign their icons there.

The very first plane is called BMP, or Basic Multilingual Plane. It fits most of the languages in active use today, sans CJK. That’s what original UCS-2/UTF-16 was supposed to be. I guess one can feel the temptation of fitting _everything_ into these 65,536 characters and be done with it. Unfortunately, humanity invented more letters than it was convenient for the computer.

bmp@2x.png
A map of the Basic Multilingual Plane. Each numbered box represents 256 code points.

Each square is 256 characters. For example, the entirety of Latin alphabet fits into two small red squares in top left corner. ASCII is half of first square. Emoji take, what, about 5 squares at the bottom of second plane? Tiny compared to the entire thing. Unicode is HUGE.

# Unicode is variable-length

Unicode? Weren’t you supposed to say UTF-8?

Well, no.

So ASCII was fixed-length. It was so convenient that every other encoding since then was trying to bring back that property. All of them failed.

USC-2 was supposed to be fixed-length. Didn’t have enough codepoints.

UTF-32 had enough codepoints, but with 4× overhead. Yet we need grapheme clusters.

You see, Unicode codepoints sometimes come in pairs. Sometimes in groups, even. Something like `ö` is actually `o` (U+006F Latin Small Letter O) + `¨` (U+0308 Combining Diaeresis) in disguise. Two codepoints. Single _grapheme_. Under any circumstance, shouldn’t be broken.

Same goes for emoji, for example:

- `☹️` is `U+2639` + `U+FE0F`
- `🇺🇳` is `U+1F1FA` + `U+1F1F3`
- `🚵🏻‍♀️` is `U+1F6B5` + `U+1F3FB` + `U+200D` + `U+2640` + `U+FE0F`

These can get quite long.

What’s string lenght of `🤦🏼‍♂️`? If you ask human, a real human, someone not familiar with how computers work internally, naturally their answer would be `1`. Yet it’s 5 codepoints! 17 bytes, if encoded in UTF-8!



# Проблемы

- UTF-16
  - Byte order, BOM
  - Конвертация из-в UTF-8
- разбивать текст по байтам нельзя
  - �
  - да и по кодпоинтам нельзя
  - Extended Grapheme Cluster
  - «То, что воспринимается человеком как один символ»
- Искать по подстроке нельзя
  - Uppercase/lowercase
  - Номализация
  - NFD — все взорвать, é → e + ◌́
  - NFC — все слепить, e + ◌́ → é
  - macOS/Windows й
  - X и 𝕏 (U+1D54F, MATHEMATICAL DOUBLE-STRUCK CAPITAL X)
  - 1 и ¹ и ₁
- Локааали
  - uppercase/lowercase
    - I → ı, i → İ
  - Han unification
    - For Japanese, the kanji characters have been unified with Chinese; that is, a character considered to be the same in both Japanese and Chinese is given a single number, even if the appearance is actually somewhat different, with the precise appearance left to the use of a locale-appropriate font
  - Болгарица
    - https://twitter.com/nikitonsky/status/1171115067112398849
  - Text Segmentation
    - Границы букв, слогов (для переносов), слов, предложений
    - Работает через словарь
    - Поэтому ICU весит 10 Мб, а не 10 Кб :(
  - Новые версии каждый год
    - Результаты разбиения/переноса могут поменяться
