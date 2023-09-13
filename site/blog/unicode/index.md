---
title: "The Absolute Minimum Every Software Developer Must Know About Unicode in 2023 (Still No Excuses!)"
category: blog
summary: "Modern extension to classic 2003 article by Joel Spolsky"
published: 2023-09-18
---

Exactly 20 years has passed since Joel Spolsky [wrote his version](https://www.joelonsoftware.com/2003/10/08/the-absolute-minimum-every-software-developer-absolutely-positively-must-know-about-unicode-and-character-sets-no-excuses/), so it’s about time we have an update.

In 2003, the question was: what encoding does this string/text/file use? Joel even went as far as to declare:

> There Ain’t No Such Thing As Plain Text.

Luckily for us, in the last 20 years Unicode has finally won: in 99,9%, your plain text it’s UTF-8. Hooray!

Hooray?

# What is Unicode

- 21-bit
- 17 planes
- Plane = 216 = 65 536 code points
- Plane 0
  - Basic Multilingual Plane
  - Все нужные языки
- Plane 1
  - Supplementary Multilingual Plane
  - Linear B, Egyptian hieroglyphs, cuneiform, Shavian, Deseret, Osage, Warang Citi, Adlam, Wancho and Toto.
  - musical notation; mathematical alphanumerics; shorthands; Emoji and other pictographic sets; and game symbols for playing cards, mahjong, and dominoes.
- Planes 2-3
  - CJK Unified Ideographs
- Planes 4-14
  - Unallocated or barely used
- Planes 15-16
  - Private use area

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
