function randInt(max) {
  return Math.floor(Math.random() * max);
}

/** THEME COLORS **/

const bodyCL = document.body.classList;
var defaultThemeColor = '#FDDB29';
var winterThemeColor = '#6ADCFF';
var darkThemeColor = '#000';

function maybeUpdateThemeColor() {
  const isWinter = bodyCL.contains('winter');
  const isDark = bodyCL.contains('dark');
  const currentColor = document.querySelector('meta[name=theme-color]').content;
  const targetColor = isDark ? darkThemeColor : isWinter ? winterThemeColor : defaultThemeColor;
  if (currentColor != targetColor) {
    document.querySelector('meta[name=theme-color]').content = targetColor;
  }
}

/** COOKIE HELPERS **/

function setCookie(name, value) {
  const days = 400;
  const expires = `; expires=${new Date(Date.now() + days * 864e5).toUTCString()}`;
  document.cookie = `${name}=${value}${expires}; path=/`;
}

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length !== 2) {
    return null;
  }
  return parts.pop().split(';').shift();
}

function toggleCookie(name) {
  setCookie(name, 'true' === getCookie(name) ? 'false' : 'true');
}


/** HOVERABLE **/

window.addEventListener("load", (event) => {
  document.querySelectorAll('.hoverable').forEach((e) => {
    e.onclick = function() { e.classList.toggle('clicked'); }
  });
});


/** BUTTON **/

function onButtonClick(button, text, callback) {
  button.disabled = true;
  button.innerHTML = '<img src=\"/i/spinner.svg\"> ' + text;
  setTimeout(callback, 2000);
}


/** ANIMATE **/

function animate(el) {
  if (el.animated) {
    el.offset = (el.offset || 0) - 250;  
    setTimeout(function() { animate(el); }, 700);
  }
  el.style.backgroundPosition = "0 " + el.offset + "px";
}

window.addEventListener("load", (event) => {
  for (let el of document.getElementsByClassName("anim")) {
    el.onmouseover = function() {
      if (!this.animated) {
        this.animated = true;
        animate(this);
      }
    }
    el.onmouseout = function() {
      this.animated = false;
      this.offset = 0;
      animate(this);
    }
  }
});


/** DARK MODE **/

var mousePos;

function updateFlashlight(e) {
  mousePos = {clientX: e.clientX,
              clientY: e.clientY,
              pageX: e.pageX,
              pageY: e.pageY};

  flashlight.style.left = mousePos.clientX - 250 + 'px';
  flashlight.style.top = mousePos.clientY - 250 + 'px';

  const centerX = darkModeGlow.offsetLeft + 38;
  const centerY = darkModeGlow.offsetTop + 12;
  const dist = Math.hypot(mousePos.pageX - centerX, mousePos.pageY - centerY);
  const opacity = Math.max(0, Math.min(1, (dist - 50) / (200 - 50)));
  darkModeGlow.style.opacity = opacity;
}

function maybeUpdateDarkMode(e) {
  const shouldBeDark = 'true' === getCookie('dark');
  const isDark = bodyCL.contains('dark');
  if (!isDark && shouldBeDark) {
    bodyCL.add('dark0');
    setTimeout(() => {
      bodyCL.remove('dark0');
      bodyCL.add('dark');
      maybeUpdateThemeColor();
      updateFlashlight(e);
      ['mousemove', 'touchstart', 'touchmove', 'touchend'].forEach(function(s) {
        document.documentElement.addEventListener(s, updateFlashlight, false);
      });
    }, 34);
  } else if (isDark && !shouldBeDark) {
    bodyCL.remove('dark');
    bodyCL.add('dark0');
    setTimeout(() => {
      bodyCL.remove('dark0');
      maybeUpdateThemeColor();
    }, 1000);
    ['mousemove', 'touchstart', 'touchmove', 'touchend'].forEach(function(s) {
      document.documentElement.removeEventListener(s, updateFlashlight, false);
    });
  }
}

window.addEventListener("load", (event) => {
  // localStorage -> cookie migration
  const isDark = localStorage.getItem('dark');
  if (null !== isDark) {
    setCookie('dark', isDark);
    localStorage.removeItem('dark');
  }

  const darkButton = document.querySelector('.dark_mode');
  if (darkButton) {
    darkButton.onclick = function(e) {
      toggleCookie('dark');
      maybeUpdateDarkMode(e);
    };
  }

  // after load initialization
  if ('true' === getCookie('dark')) {
    var mousePos = {clientX: 0, clientY: 0, pageX: 0, pageY: 0};
    const stored = localStorage.getItem('mousePos');
    if (stored) {
      mousePos = JSON.parse(stored);
    }
    updateFlashlight(mousePos);
    ['mousemove', 'touchstart', 'touchmove', 'touchend'].forEach(function(s) {
      document.documentElement.addEventListener(s, updateFlashlight, false);
    });
  }
});

window.addEventListener("beforeunload", (event) => {
  if (bodyCL.contains('dark') && mousePos) {
    localStorage.setItem('mousePos', JSON.stringify(mousePos));
  }
});


/** WINTER MODE **/

function maybeUpdateWinterMode() {
  const currentMonth = new Date().getMonth() + 1;
  const isWinterMonth = [12, 1, 2].includes(currentMonth);

  if (isWinterMonth && null === getCookie('winter')) {
    setCookie('winter', 'true');
  }
  const shouldBeWinter = isWinterMonth && 'true' === getCookie('winter');
  const isWinter = bodyCL.contains('winter');

  if (shouldBeWinter && !isWinter) {
    bodyCL.add('winter');
    flashlight.src = '/i/flashlight_winter.webp';
    maybeUpdateThemeColor();
  } else if (!shouldBeWinter && isWinter) {
    bodyCL.remove('winter');
    flashlight.src = '/i/flashlight.webp';
    maybeUpdateThemeColor();
  }
}

window.addEventListener("load", (event) => {
  // localStorage -> cookie migration
  const isWinter = localStorage.getItem('winter');
  if (null !== isWinter) {
    setCookie('winter', isWinter);
    localStorage.removeItem('winter');
  }

  maybeUpdateWinterMode();

  const winterButton = document.querySelector('div.winter');
  if (winterButton) {
      winterButton.onclick = function(e) {
        toggleCookie('winter');
        maybeUpdateWinterMode();
      }
  }
});
