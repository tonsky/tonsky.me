---
title: "The Absolute Minimum Every Software Developer Must Know About Unicode in 2023 (Still No Excuses!)"
summary: "Modern extension to classic 2003 article by Joel Spolsky"
---

20 years ago [Joel Spolsky wrote](https://www.joelonsoftware.com/2003/10/08/the-absolute-minimum-every-software-developer-absolutely-positively-must-know-about-unicode-and-character-sets-no-excuses/):

> There Ain’t No Such Thing As Plain Text.
> It does not make sense to have a string without knowing what encoding it uses. You can no longer stick your head in the sand and pretend that “plain” text is ASCII.

A lot has changed in 20 years. In 2003, the main question was: what encoding is this? In 2023, it’s no longer a question: with 99,9% probability, it’s UTF-8.

Hooray? Can we, developers, finally stick our heads in the sand again?

Well, sure, but only after you learn to use UTF-8 properly. It’s not hard, but it has a few gotchas. Bear with me.

# What’s the difference between Unicode and UTF-8?

Because I reckon there might be some confusion.

Unicode assigns numbers to glyphs.

Letter A is 65. Infinity sign, ∞, is 8734. <span style="font-size: 150%">💩</span> is 128169.

These numbers are usually written in hexadecimal, like U+1F4A9, but they are still just numbers. A simple, plain platonic abstraction, unconcerned with the limitations of our physical world. Unicode calls them _codepoints_.

UTF-8 is _encoding_.

It’s a set of rules to convert these codepoints into a sequence of bytes that can be stored in computer.

That poo emoji? In Unicode it’s U+1F4A9, but encoded with UTF-8, it turns into `9F F0 A9 92`. Four bytes!

There are other encodings, too. In UTF-16, U+1F4A9 will turn into `D8 3D DC A9`. In UTF-32 it’ll be `00 01 F4 A9`. I doubt you’ll see these in the wild, but technically they do exist.

Important thing is, they all encode the same number. And that number is defined by Unicode.

# How big is Unicode?

21 bit
fits into an integer
codepoint

- How is UTF-8 laid out in memory
- no reason not to use utf-8
- Unicode is 21 bits
- Unicode is 10% filled
- utf-16 is still in use
- UTF-32 will not help you
- UTF-32 is useless
- only measures are byte length and extended grapheme clusters length
- Unicode is locale-dependent
- Unicode is updated once/twice a year
- Many ways to do it
- There is a library for it



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

overview.png

On the picture, each large square is a Unicode _plane_. Plane fits 65,536 codepoints.

There are 17 planes total. Most are unallocated, i.e. reserved for future use. Plenty of space for new emojis.

Second plane contains mostly dead languages and some emoji.

Third and fourth are dedicated entirely for CJK (Chinese, Japanes, Korean).

Last two can be used freely by app developers. For example, icon fonts put assign their icons there.

The very first plane is called BMP, or Basic Multilingual Plane. It fits most of the languages in active use today, sans CJK. That’s what original UCS-2/UTF-16 was supposed to be. I guess one can feel the temptation of fitting _everything_ into these 65,536 characters and be done with it. Unfortunately, humanity invented more letters than it was convenient for the computer.

bmp.png
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
