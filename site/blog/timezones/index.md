---
title: "It’s time: The Absolute Minimum Every Software Developer Must Know About Time"
---

Unix timestamp
    Одинаков по всей земле
    Однозначен

Wall clock time
    Не однозначно!

Таймзоны
    UTC
        Почему такая аббервиатура?
            Coordinated Universal Time / Temps Universel Coordonne
        GMT vs UTC
            https://www.timeanddate.com/time/gmt-utc-time.html
    Меняются (летнее/зимнее)
        Поэтому Asia/Novosibirsk, а не UTC+7
        Меняются по-разному в разных странах
        Меняются в другую сторону в южном полушарии
    Меняются (исторически)
        Отказ от летнего времени в России
        Новосибирск
            1957–1993 МСК+4 (UTC+7)
            1993–2011 МСК+3 (UTC+6)
            2011–2014 МСК+3 (UTC+7)
            2014–2016 МСК+3 (UTC+6)
            2016–now МСК+4 (UTC+7)
        Бывают странные (+30min, +15min)
            from 1880 to 1916, Ireland was 25 minutes and 21 seconds behind UTC
            В 1924—1956 Новосибирск разные берега Оби жили в МСК+3/+4

tz database
    https://en.wikipedia.org/wiki/Tz_database
    Поддерживается одним человеком
    Обновляется вместе с ОС
    Я как-то год ходил с другой таймзоной потому что Гугл не выпустил апдейт для телефона

Календари
    Юлианский vs Григорианский
    Високосные года
    В прошлом не особо хорошо определены
    Октябрьская революция 7-го ноября
    Иногда бывают дырки
        Аляска, Самоа, Токелау прыгали через линию перемены дат

Local date
    День рождения
    Бизнес-дни

Local time
    Митинг в 9 утра
    Митинг между США и Германией в 9 утра
        Летнее время в США в 2024: March 10–November 3
        Летнее время в Европе в 2024: March 31–October 27
Почему людям кажется, что работать со временем сложно?
    Интеграция с другими системами
    Время без таймзон
    Плохие библиотеки
        Месяцы в JS начинаются с нуля.
        Nullember
    Использование wall clock time
        cron job at 2:30 am
    Сделать UI непросто, пользователи не хотят думать про даты правильно
        “Use timezones” в Эпл-календаре
    https://mastodon.online/@nikitonsky/111772105787706714
    https://twitter.com/nikitonsky/status/1747649041238745144
    https://yourcalendricalfallacyis.com/
    https://gist.github.com/timvisee/fcda9bbdff88d45cc9061606b4b923ca

Как компьютеры считают время
    System time
        Windows vs Linux
    NTP
    Проблема 2000
    Проблема 2038
    Високосная секунда
        Время в будущем тоже не особо определено!
    UT1 vs UTC
        https://en.wikipedia.org/wiki/Universal_Time
    Монотонные часы
        https://www.erlang.org/doc/apps/erts/time_correction.html
        TAI (UTC + 37 sec)
Атомные часы / спутники + квазары
GPS
Как считается время на Марсе
    https://en.wikipedia.org/wiki/Timekeeping_on_Mars
Надо ли всему миру перейти на UTC?
