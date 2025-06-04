---
title: "El mínimo absoluto que todo desarrollador de software debe de saber sobre Unicode en 2023 (¡Sigue sin haber excusas!)"
summary: "Modern extension to classic 2003 article by Joel Spolsky"
published: 2023-10-02
hackernews_id: 37735801
starred: true
---

_Translated from [English](../) by Juan Carlos Pérez Domínguez._

Hace veinte años, [Joel Spolsky escribió](https://www.joelonsoftware.com/2003/10/08/the-absolute-minimum-every-software-developer-absolutely-positively-must-know-about-unicode-and-character-sets-no-excuses/):

> El texto plano no existe.
> No tiene sentido tener una cadena sin saber cómo está codificada. Ya no se puede seguir escondiendo la cabeza en el suelo y pretender que "plano" significa ASCII.

En veinte años han cambiado muchas cosas. En 2003, la principal duda era: ¿cómo está codificado esto?

En 2023, esa ya no es la cuestión: hay un 98% de probabilidades de que esté codificado en UTF-8. ¡Por fin! ¡Podemos esconder la cabeza en el suelo de nuevo!

../utf8_trend@2x.png

La cuestión ahora es: ¿cómo usar correctamente UTF-8 ? ¡Veamos!

# ¿Que es Unicode?

Unicode es un estándar que pretende unificar todos los lenguajes humanos, tanto pasados como actuales, y hacer que puedan usarse en los ordenadores.

En la práctica, Unicode es una tabla que asigna un número único a cada carácter.

Por ejemplo:

- La letra `A` del alfabeto latino tiene asignado el número `65`.
- La letra Seen `س` del alfabeto árabe es `1587`.
- La letra Tu `ツ` del alfabeto Katakana es `12484`
- El símbolo musical de clave de sol `𝄞` es `119070`.
- <code class="emoji">💩</code> es `128169`.

Unicode llama a estos números _puntos de código_.

Como todo el mundo está de acuerdo en qué números corresponden a cada carácter y todos estamos de acuerdo en usar Unicode, todos podemos leer los textos de todos los demás.

.loud Unicode == carácter ⟷ punto de código.

# ¿Cómo de grande es Unicode?

Actualmente el punto de código más alto posible es el 0x10FFFF. Esto nos da un espacio de aproximadamente 1.1 millones de puntos de código.

Aproximadamente 170.000, o un 15%, están actualmente asignados a caracteres. Un 11% adicional están reservados para uso privado. El resto, unos 800.000 puntos de código no están todavía asignados. Podrían asociarse a nuevos caracteres en el futuro.

Así es cómo se ve en líneas generales:

../overview@2x.png

Cuadrado grande == plano == 65.536 caracteres. Cuadrado pequeño == 256 caracteres. Todo ASCII cabe en la mitad del cuadrado pequeño rojo de la esquina superior izquierda.

# ¿Qué es el uso privado?

Corresponde a puntos de código reservados para desarrolladores de aplicaciones y nunca serán asignados directamente por el propio Unicode.

Por ejemplo, no hay ningún lugar para el logo de Apple en Unicode. Apple lo pone en `U+F8FF` que está en el bloque de uso privado. En cualquiera otra fuente se mostrará como glifo vacío `􀣺`, pero en las fuentes que vienen con macOS, se verá así: ![](../apple-logo@2x.png).

El área para uso privado se usa principalmente en fuentes de iconos:

../nerd_font@2x.png
¿No es una belleza? ¡Todo es texto!

# ¿Qué significa `U+1F4A9`?

Es una convención para representar valores de puntos de código. El prefijo `U+` significa, bueno, Unicode, y `1F4A9` es un número de punto de código en hexadecimal.

Oh, y `U+1F4A9` en particular es <code class="emoji">💩</code>.

# ¿Qué es entonces UTF-8?

UTF-8 es una codificación. La codificación es la forma de almacenar los puntos de código en memoria.

La forma más sencilla de codificar Unicode es UTF-32. Simplemente se almacenan los puntos de código como enteros de 32 bits. Así que `U+1F4A9` se convierte en `00 01 F4 A9`, ocupando cuatro bytes. Cualquier otro punto de código en UTF-32 también ocupará cuatro bytes. Dado que el punto de código más alto definido es `U+10FFFF`, está garantizado que puede almacenarse cualquier punto de código.

UTF-16 y UTF-8 son menos directos pero el objetivo final es el mismo: tomar un punto de código y representarlo por una serie de bytes.

La codificación es con lo que realmente trabaja el programador.

# ¿Cuantos bytes hay en UTF-8?

UTF-8 es una codificación de longitud variable. Un punto de código puede ser codificado por una secuencia de uno a cuatro bytes.

Así es como funciona:

<table>
  <thead>
    <tr>
      <th>Punto de código</th>
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

Si se coteja esto con la tabla Unicode, se verá que el inglés se codifica con un byte, los abecedarios de idiomas europeos como el cirílico, el latino, el hebreo y el árabe necesitan dos bytes, y los de idiomas asiáticos como el chino, japonés, coreano y los emojis necesitan tres o cuatro bytes.

Unas cuantas notas importantes aquí:

Primero, UTF-8 es compatible con ASCII. Los puntos de código 0..127, el antiguo ASCII, se codifican con un byte, y con el mismo exacto byte. `U+0041` (`A`, Letra mayúscula latina A) es simplemente `41`, un byte.

Cualquier texto ASCII puro es también un texto UTF-8 válido, y cualquier texto UTF-8 que solo use puntos de código 0..127 puede leerse directamente como ASCII.

Segundo, UTF-8 es eficiente en espacio para el alfabeto latino básico. Esa fue una de sus principales ventajas sobre UTF-16. Puede que no sea suficiente para textos de todo el mundo, pero para cadenas técnicas como etiquetas HTML o claves JSON, tiene sentido.

En promedio, UTF-8 es una buena opción, incluso para ordenadores que no son de habla inglesa. Y para inglés, no hay comparación.

Tercero, UTF-8 tiene detección y recuperación de errores incorporada. El prefijo de primer byte siempre es diferente de los bytes 2-4. De esta forma, siempre se puede saber si se está viendo una secuencia completa y válida de bytes UTF-8 o si falta algo (por ejemplo, por un salto al medio de una secuencia). Entonces se puede corregir el error moviéndose hacia adelante o hacia atrás hasta encontrar el comienzo de la secuencia correcta.

Y algunas consecuencias importantes:

- NO SE PUEDE conocer la longitud de la cadena contando bytes.
- NO SE PUEDE saltar a un punto al azar de la cadena y comenzar a leer.
- NO SE PUEDE obtener una subcadena seleccionando un número arbitrario de bytes. Podría estarse eliminando parte de algún carácter.

Aquellos que lo hagan eventualmente se encontrarán con este chico malo: �

# ¿Que es �?

`U+FFFD`, el _carácter de reemplazo_, es simplemente otro punto de código en la tabla de Unicode. Las aplicaciones y bibliotecas de software pueden usarlo cuando detectan errores Unicode.

Si se corta por la mitad un punto de código, no hay mucho que hacer con la otra mitad, excepto mostrar un error. Es entonces cuando se usa �.

```
var bytes = "Аналитика".getBytes("UTF-8");
var partial = Arrays.copyOfRange(bytes, 0, 11);
new String(partial, "UTF-8"); // => "Анал�"
```

# ¿No sería más fácil usar UTF-32 para todo?

NO.

UTF-32 es estupenda para operar con puntos de código. De hecho, si cada punto de código ocupa siempre 4 bytes, entonces `strlen(s) == sizeof(s) / 4`, `substring(0, 3) == bytes[0, 12]`, etc.

El problema es que no queremos operar con puntos de código. Un punto de código no es una unidad de escritura; un punto de código no siempre es un solo carácter. Lo que se debería iterar son las llamadas __agrupaciones extendidas de grafemas__, o simplemente grafemas.

Un grafema es una unidad mínima distintiva de escritura en un sistema de escritura particular. `ö` es un grafema. `é` también lo es. Y `각`. Básicamente, un grafema es lo que el usuario considera un carácter simple.

El problema es que en Unicode, algunos grafemas están codificados con múltiples puntos de código.

../graphemes@2x.png

Por ejemplo, `ö` (un solo grafema) está codificado en Unicode como `o` (U+006F O Minúscula del alfabeto latino) + `¨` (U+0308 Diéresis de combinación). ¡Dos puntos de código!

También pueden ser más de dos:

- <code class="emoji">☹️</code> es `U+2639` + `U+FE0F`
- <code class="emoji">👨‍🏭</code> es `U+1F468` + `U+200D` + `U+1F3ED`
- <code class="emoji">🚵🏻‍♀️</code> es `U+1F6B5` + `U+1F3FB` + `U+200D` + `U+2640` + `U+FE0F`
- <code style="font-family: unset">y̖̠͍̘͇͗̏̽̎͞</code> es `U+0079` + `U+0316` + `U+0320` + `U+034D` + `U+0318` + `U+0347` + `U+0357` + `U+030F` + `U+033D` + `U+030E` + `U+035E`

Hasta donde yo se, no hay límite.

Recuerde que aquí estamos hablando de puntos de código. Incluso en la codificación más amplia, UTF-32, <code class="emoji">👨‍🏭</code> seguirá ocupando tres unidades de 4 bytes y tendrá que seguir siendo tratado como un solo carácter.

Si una analogía ayuda, podemos pensar que Unicode mismo (sin codificaciones) es de longitud variable.

.loud Una agrupación de grafemas es una secuencia de uno o más puntos de código Unicode que deben ser tratados como un solo carácter indivisible.

Por tanto, tenemos todos los problemas que tenemos con las codificaciones de longitud variable, pero ahora a nivel de puntos de código: no se puede tomar solo una parte de la secuencia, siempre debe seleccionarse, copiarse, editarse o eliminarse en su totalidad.

No respetar las agrupaciones de grafemas lleva a errores como este:

../error1.png

o este:

../intellij@2x.mp4
Solamente para que quede claro: este NO es el comportamiento correcto.

Utilizar UTF-32 no va a hacer que su vida sea más fácil con las agrupaciones de grafemas. Y las agrupaciones de grafemas son lo que realmente debería importarle.

.loud Puntos de código — 🥱. Grafemas — 😍

# ¿Es difícil Unicode a causa solo de los emojis?

En realidad, no. Las agrupaciones de grafemas también se utilizan para lenguajes vivos y activos. Por ejemplo:

- `ö` (alemán) es un solo carácter, pero varios puntos de código (`U+006F U+0308`).
- `ą́` (lituano) es `U+00E1 U+0328`.
- `각` (koreano) es `U+1100 U+1161 U+11A8`.

Asi que no solamente es por los emojis.

# ¿Qué es "🤦🏼‍♂️".length?

La pregunta está inspirada por [este brillante artículo](https://hsivonen.fi/string-length/).

Diferentes lenguajes de programación te darán alegremente diferentes respuestas.

Python 3:

```
>>> len("🤦🏼‍♂️")
5
```

JavaScript / Java / C#:

```
>>> "🤦🏼‍♂️".length
7
```

Rust:

```
println!("{}", "🤦🏼‍♂️".len());
// => 17
```

Como puede suponerse, diferentes lenguajes usan diferentes representaciones internas de cadenas (UTF-32, UTF-16, UTF-8) e informan de la longitud en las unidades que almacenan los caracteres (enteros, entero cortos, bytes).

Pero si le preguntas a cualquier persona normal, una que no esté ofuscada con los entresijos de los ordenadores, te dará una respuesta directa: 1. La longitud de la cadena <code class="emoji">🤦🏼‍♂️</code> es 1.

Esto es para lo que son las agrupaciones de grafemas: para lo que los humanos perciben como caracteres individuales. Y en este caso, <code class="emoji">🤦🏼‍♂️</code> es sin duda un solo carácter.

El hecho de que <code class="emoji">🤦🏼‍♂️</code> consista en 5 puntos de código (`U+1F926 U+1F3FB U+200D U+2642 U+FE0F`) es un mero detalle de implementación. No debería descomponerse, no debería contarse como múltiples caracteres, el cursor de texto no debería posicionarse dentro de él, no debería ser seleccionado parcialmente, etc.

Para todos los efectos, esto es una unidad atómica de texto. Internamente, podría codificarse de cualquier manera, pero para la API orientada al usuario, debería tratarse como un todo.

Los dos únicos lenguajes modernos que lo hacen bien son Swift:

```
print("🤦🏼‍♂️".count)

// => 1
```

y Elixir:

```
String.length("🤦🏼‍♂️")
// => 1
```

Básicamente hay dos capas:

1. Interna, orientada a los ordenadores. Como copiar cadenas, enviarlas por la red, almacenarlas en disco, etc. Aquí es donde se necesitan codificaciones como UTF-8. Swift usa UTF-8 internamente, pero podría ser UTF-16 o UTF-32. Lo importante es que solo se use para copiar cadenas enteras y nunca para analizar su contenido.

2. Externa, orientada a los humanos. Como mostrar cadenas en la pantalla, como contar caracteres, como buscar en el texto, etc. Métodos como `.count` or `.substring`. Aquí es donde se necesitan las agrupaciones de grafemas. Swift te da una vista que presenta la cadena como una secuencia de clústeres de grafemas. Y esa vista se comporta como cualquier humano esperaría: te da 1 para `"🤦🏼‍♂️".count`.

Espero que pronto más lenguajes adopten este diseño.

Pregunta al lector: ¿qué cree que debería ser <code style="font-family: unset">"ẇ͓̞͒͟͡ǫ̠̠̉̏͠͡ͅr̬̺͚̍͛̔͒͢d̠͎̗̳͇͆̋̊͂͐".length</code>?

# Entonces, ¿cómo detectar las agrupaciones de grafemas?

Desafortunadamente, la mayoría de los lenguajes eligen la forma fácil y te permiten iterar a través de cadenas por trozos de 1-2-4 bytes, pero no por agrupaciones de grafemas.

No tiene sentido, pero como es el valor por defecto, los programadores no se lo piensan dos veces, y vemos cadenas corruptas como resultado:

../stdlib@2x.png

“Vale, usaré una biblioteca para hacer strlen()!” — dijo nadie, nunca.

Pero eso es exactamente lo que se debería hacer. ¡Hay que usar una biblioteca Unicode adecuada! ¡Sí, para cosas básicas como `strlen` o `indexOf` o `substring`!

Por ejemplo:

1. C/C++/Java: usar [ICU](https://github.com/unicode-org/icu). Es una biblioteca de la propia Unicode que respeta todas las reglas sobre segmentación de texto.

2. C#: usar `TextElementEnumerator`, que se mantiene actualizado con Unicode hasta donde yo puedo decir.

3. Swift: simplemente `stdlib`. Swift lo hace bien por defecto.

4. UPD: Erlang/Elixir parece que lo hacen bien también.

5. Para otros lenguajes, probablemente haya una biblioteca o adaptación de ICU.

6. Hágalo Vd. mismo. Unicode [publica](https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries) reglas y tablas en formato legible por máquina. Todas las bibliotecas anteriores se basan en ellas.

Pero, sea lo que sea que elija, asegúrese de que está en una versión reciente de Unicode (15.1 en el momento de escribir esto), porque la definición de las agrupaciones de grafemas cambia de versión a versión. Por ejemplo, `java.text.BreakIterator` no debe de usarse ya: se basa en una versión muy antigua de Unicode y no se actualiza.

.loud Utilice una biblioteca

En mi opinión, todo el asunto es una vergüenza. Unicode debería estar en la biblioteca estándar de todos los idiomas por defecto. ¡Es la lengua franca de internet! Ni siquiera es nuevo: llevamos 20 años conviviendo con Unicode.

# Un momento, ¿están cambiando las reglas?

Si, ¿no es genial?

(Lo sé, no lo es)

Desde aproximadamente 2014, Unicode ha estado lanzando una revisión importante de su estándar cada año. De ahí es de donde salen los nuevos emojis: las actualizaciones de Android e iOS en otoño suelen incluir el estándar Unicode más reciente, entre otras cosas.

../versions@2x.png

Lo que es triste para nosotros es que las reglas que definen las agrupaciones de grafemas cambian cada año también. Lo que hoy se considera una secuencia de dos o tres puntos de código separados puede convertirse en una agrupación de grafemas mañana. ¡No hay forma de saberlo! ¡O de prepararse!

¡Peor aún!, ¡diferentes versiones de tu propia aplicación podrían estar ejecutándose en diferentes estándares Unicode y reportar diferentes longitudes de cadena!

Pero esta es la realidad en la que vivimos. Realmente no hay elección. No se pueden ignorar ni Unicode ni sus actualizaciones si se quiere seguir siendo relevante y proporcionar una experiencia de usuario decente. Así que, ¡a agarrarse los machos, aceptarlo y a actualizarse!

.loud Actualice cada año

# Por qué es "Å" !== "Å" !== "Å"?

../spider_men@2x.jpg

Copie cualquiera de estos a la consola de JavaScript:

```
"Å" === "Å"
"Å" === "Å"
"Å" === "Å"
```

¿Qué obtiene? ¿False? Debería obtener False. No es un error.

Recuerde cuando antes dije que `ö` es dos puntos de código, `U+006F U+0308`? Básicamente, Unicode ofrece más de una forma de escribir caracteres como `ö` o `Å`. Puedes:

1. Componer `Å` a partir del carácter `A` del alfabeto latino + un carácter combinado,
2. O usar un carácter pre-compuesto `U+00C5` que ya lo contiene.

Se verán exactamente iguales (`Å` vs `Å`), funcionarán igual, y para todos los efectos prácticos, se consideran exactamente iguales. La única diferencia es la representación interna en bytes.

Por eso necesitamos la normalización. Hay cuatro formas:

**NFD** intenta separar todo en las piezas más pequeñas posibles, y ordena las piezas en un orden canónico si hay más de una.

**NFC** por su parte intenta combinar todo en una forma pre-compuesta si existe.

../normalization@2x.png

Para algunos caracteres, hay múltiples versiones de ellos en Unicode. Por ejemplo, hay una `U+00C5 Letra mayúscula A del alfabeto latino con un anillo encima`, pero también hay un `U+212B Símbolo de Angstrom` que tiene el mismo aspecto.

Estos también se reemplazan durante la normalización:

../normalization_clones@2x.png

NFD y NFC se denominan “normalizaciones canónicas”. Otras dos formas son “normalizaciones de compatibilidad”:

**NFKD** intenta separar todo en las piezas más pequeñas y reemplaza las variantes visuales con las predeterminadas.

**NFKC** intenta combinar todo mientras reemplaza las variantes visuales con las predeterminadas.

../normalization_compat@2x.png

Las variantes visuales son puntos de código Unicode separados que representan el mismo carácter, pero se supone que se representan de distinta manera. Como, `①` o `⁹` o `𝕏`. Queremos poder encontrar tanto `"x"` y `"2"` en una cadena como `"𝕏²"`, ¿no?

../x_variants@2x.png
Todas tienen sus puntos de código propios pero todas son equis.

¿Porqué el símbolo de ligadura `ﬁ` tiene su propio punto de código? Ni idea. Pueden pasar muchas cosas en un millón de caracteres.

.loud ¡Antes de comparar cadenas o buscar un subcadena, normalice!

# Unicode depende de la configuración regional

El nombre ruso Nikolay se escribe así:

../nikolay_ru.png

y se codifica en Unicode como: `U+041D 0438 043A 043E 043B 0430 0439`.

El nombre búlgaro Nikolay se escribe así:

../nikolay_bg.png

y se codifica en Unicode como: `U+041D 0438 043A 043E 043B 0430 0439`. ¡Exactamente lo mismo!

¡Un momento! ¿Cómo sabe el ordenador cuando representar los glifos al estilo búlgaro y cuando al estilo ruso?

Respuesta corta: no lo sabe. Desafortunadamente, Unicode no es un sistema perfecto y tiene muchas deficiencias. Entre ellas está el hecho de asignar el mismo punto de código a glifos que deberían verse diferentes, como la letra K minúscula cirílica y la letra K minúscula búlgara (ambas son `U+043A`).

Según tengo entendido, los asiáticos [lo tienen mucho peor](https://en.wikipedia.org/wiki/Han_unification): muchos logogramas chinos, japoneses y coreanos que se escriben de forma muy diferente tienen el mismo punto de código en Unicode:

../han.png
U+8FD4 en diferentes configuraciones locales

La razón de Unicode para esto (creo) es ahorrar espacio de puntos de código. La información sobre como representar se supone que debe de estar fuera de la cadena, en forma de metadatos de configuración regional/idioma.

Desafortunadamente, esto hace fallar uno de los objetivos originales de Unicode:

> [...] no se requerirá ninguna secuencia de control para especificar ningún carácter de ningún lenguaje.

En la práctica, la dependencia de la configuración regional trae muchos problemas:


1. Al ser metadatos, las configuraciones locales frecuentemente se pierden.

2. La gente no está limitada a una sola configuración regional. Por ejemplo, yo puedo leer y escribir en inglés (EE.UU.), inglés (Reino Unido), alemán y ruso. ¿Qué configuración regional debería establecer en mi ordenador?

3. Es difícil mezclar correctamente. Por ejemplo, nombres rusos en texto búlgaro o viceversa. ¿Pero por qué no? Es internet, donde se reúne gente de todas las culturas.

4. No hay lugar para especificar la configuración regional. Incluso hacer las dos capturas de pantalla anteriores fue complicado porque en la mayoría del software no hay un menú desplegable o campo de texto para cambiar la configuración regional.

5. Cuando se necesita, tiene que adivinarse. Por ejemplo, Twitter intenta deducir la configuración regional del texto del tweet (porque, ¿de dónde más podría obtenerla?) y, claro, a veces se equivoca:

../twitter_locale.jpg https://twitter.com/nikitonsky/status/1171115067112398849

# ¿Por qué `String::toLowerCase()` acepta Locale (para la configuración regional) como argumento?

Otro desafortunado ejemplo de dependencia de la configuración regional es el manejo de Unicode de la letra `i` sin punto en el idioma turco.

A diferencia del inglés, los turcos tienen dos variantes de `I`: con punto y sin punto. Unicode decidió reutilizar `I` e `i` de ASCII y solo añadir dos nuevos puntos de código: `İ` y `ı`.

Desafortunadamente, esto significa que `toLowerCase` y `toUpperCase` funcionan de forma diferente con la misma entrada:

```
var en_US = Locale.of("en", "US");
var tr = Locale.of("tr");

"I".toLowerCase(en_US); // =&gt; "i"
"I".toLowerCase(tr);    // =&gt; "ı"

"i".toUpperCase(en_US); // =&gt; "I"
"i".toUpperCase(tr);    // =&gt; "İ"
```

Así que no, no puedes convertir una cadena a minúsculas sin saber en qué idioma está escrita esa cadena.

# Yo vivo en EE.UU./Reino Unido, ¿debería importarme todo esto?

../english@2x.png
Incluso el inglés puro utiliza muchos caracteres tipográficos que no están en ASCII.

- comillas `“` `”` `‘` `’`,
- apóstrofos `’`,
- barras `–` `—`,
- diferentes variantes de espacios (entre cifras, de ajuste fino, irrompible),
- marcas de lista `•` `■` `☞`,
- símbolos de moneda distintos de `$``€` `¢` `£`,
- los signos de suma `+` y de igual `=` están en ASCII, pero el de resta `−` y multiplicación `×` no <nobr>¯\_(ツ)_/¯</nobr>,
- varios otros símbolos `©` `™` `¶` `†` `§`.

Vaya, ni siquiera puedes escribir correctamente `café`, `piñata`, or `naïve` sin Unicode. Así que si, estamos todos en esto, incluso los americanos.

Touché.

# ¿Que son los pares subrogados?

Esto se remonta al Unicode v1. La primera versión de Unicode se suponía que sería de ancho fijo. Un ancho fijo de 16 bits, para ser exactos:

../unicode1@2x.png
Versión 1.0 del estándar Unicode, octubre de 1991

Pensaban que 65.536 caracteres serían suficientes para todos los lenguajes humanos. ¡Casi aciertan!

Cuando se dieron cuenta de que necesitaban más puntos de código, UCS-2 (una versión original de UTF-16 sin subrogados) ya estaba siendo utilizada en muchos sistemas. Un ancho fijo de 16 bits, solamente da para 65.536 caracteres. ¿Qué se podía hacer?

Unicode decidió asignar algunos de estos 65.536 caracteres para codificar puntos de código más altos, convirtiendo esencialmente UCS-2 de ancho fijo en UTF-16 de ancho variable.

Un par subrogado se forma de dos unidades UTF-16 para codificar un único punto de código Unicode. Por ejemplo, `D83D DCA9` (dos unidades de 16 bits) codifica _un_ punto de código, `U+1F4A9`.

Los 6 bits más altos de un par subrogado se utilizan para la máscara, dejando 2×10 bits libres:

```
Alto del Subrogado     Bajo del subrogado
       D800         ++        DC00
1101 10?? ???? ???? ++ 1101 11?? ???? ????
```

Técnicamente, las dos mitades del par subrogado también pueden verse como puntos de código Unicode. En la práctica, todo el rango de `U+D800` a `U+DFFF` está reservado "exclusivamente para pares subrogados". Los puntos de código de esa parte no se consideran válidos en ninguna otra codificación.

../bmp@2x.png
Este espacio del muy abarrotado Plano Multilingüe Básico nunca se utilizará jamás será para nada bueno.

# ¿Está aún vivo UTF-16?

¡Si!

La promesa de una codificación de ancho fijo que cubriera todos los lenguajes humanos era tan atractiva que muchos sistemas estaban deseosos de adoptarla. Entre ellos estaban Microsoft Windows, Objective-C, Java, JavaScript, .NET, Python 2, QT, SMS, ¡incluso CD-ROM!

Desde entonces, Python ha evolucionado, el CD-ROM está obsoleto, pero el resto se ha quedado estancado en UTF-16 o incluso UCS-2. Por lo tanto, UTF-16 sigue usándose para la representación en memoria.

En términos prácticos, UTF-16 es tan usable como UTF-8. También es de longitud variable; contar unidades UTF-16 es tan inútil como contar bytes o puntos de código, las agrupaciones de grafemas siguen siendo un dolor de cabeza, etc. La única diferencia son los requisitos de memoria.

La única desventaja de UTF-16 es que todo lo demás está en UTF-8, por lo que requiere conversión cada vez que se lee una cadena desde la red o desde el disco.

Y, dato curioso: el número de planos que tiene Unicode (17) está definido por cuánto se puede expresar con pares subrogados en UTF-16.

# Conclusión

Resumiendo:

- Unicode ha ganado.
- UTF-8 es la codificación más popular para datos en movimiento y en reposo.
- UTF-16 se utiliza aún algunas veces para la representación en memoria.
- Las dos formas más importantes de ver las cadanas son como una secuencia de bytes (para asignar memoria, copiar, decodificar) y las agrupaciones de grafemas (para todas las operaciones semánticas).
- Utilizar puntos de código para iterar sobre una cadena es incorrecto. No son la unidad básica de escritura. Un solo grafema puede consistir en múltiples puntos de código.
- Para detectar agrupaciones de grafemas, se necesitan las tablas de Unicode.
- Utilice una biblioteca Unicode para todo lo relacionado con Unicode, incluso para cosas aburridas como `strlen`, `indexOf` y `substring`.
- Unicode se actualiza cada año y algunas veces las reglas cambian.
- Las cadenas Unicode deben de normalizarse antes de poder ser comparadas.
- Unicode depende de la configuración regional para algunas operaciones y para la presentación.
- Todo esto es importante incluso para texto en inglés puro.

En resumen, Unicode no es perfecto pero el hecho de que

1. exista un código que cubre todos los lenguajes posibles a la vez,
2. todo el mundo esté de acuerdo en usarlo,
3. podamos olvidarnos completamente de codificaciones y conversiones y de todas esas cosas

es un milagro. Envíe esto a sus compañeros programadores para que puedan aprender sobre ello también.

.loud Si que existe el texto plano, y está codificado con UTF-8.

Gracias a Lev Walkin y a mis patrocinadores por leer los primeros borradores de este artículo.
