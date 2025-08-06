function randInt(max) {
  return Math.floor(Math.random() * max);
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

/** FLASHLIGHT **/

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

function updateDarkMode(e) {
  const theme = document.querySelector("meta[name=theme-color]");
  if (document.body.classList.contains('dark')) {
    localStorage.setItem('dark', 'true');
    theme.content = "#000";
    updateFlashlight(e);
    ['mousemove', 'touchstart', 'touchmove', 'touchend'].forEach(function(s) {
      document.documentElement.addEventListener(s, updateFlashlight, false);
    });
  } else {
    localStorage.removeItem('dark');
    localStorage.removeItem('mousePos');
    theme.content = "#FDDB29";
    ['mousemove', 'touchstart', 'touchmove', 'touchend'].forEach(function(s) {
      document.documentElement.removeEventListener(s, updateFlashlight, false);
    });
  }
}

window.addEventListener("load", (event) => {
  let cl = document.body.classList;
  document.querySelector('.dark_mode').onclick = function(e) {
    if (cl.contains('dark')) {
      cl.remove('dark');
      cl.add('dark0');
      updateDarkMode(e);
      setTimeout(() => {
        cl.remove('dark0');
      }, 1000);
    } else {
      cl.add('dark0');
      setTimeout(() => {
        cl.remove('dark0');
        cl.add('dark');
        updateDarkMode(e);
      }, 34);
    }
  };

  // first time initialization
  if (localStorage.getItem('dark')) {
    cl.toggle('dark');
    var mousePos = {clientX: 0, clientY: 0, pageX: 0, pageY: 0};
    const stored = localStorage.getItem('mousePos');
    if (stored) {
      mousePos = JSON.parse(stored);
    }
    updateDarkMode(mousePos);
  }
});

window.addEventListener("beforeunload", (event) => {
  if (document.body.classList.contains('dark') && mousePos) {
    localStorage.setItem('mousePos', JSON.stringify(mousePos));
  }
});

/** Snow **/

let snowflakeTimer;

function createSnowflake() {
  const snowflake = document.createElement('div');
  snowflake.classList.add('snowflake');
  const randomLeft = Math.random() * window.innerWidth;
  const dist = Math.random();
  const randomSize = 20 - dist * 15;
  const randomDuration = 5 + dist * 10;
  snowflake.style.position = 'fixed';
  snowflake.style.left = `${randomLeft}px`;
  snowflake.style.top = '-50px';
  snowflake.style.width = `${randomSize}px`;
  snowflake.style.height = `${randomSize}px`;
  snowflake.style.fontSize = `${50 - dist * 30}px`;
  snowflake.style.opacity = Math.random() * 0.5 + 0.25;
  snowflake.style.pointerEvents = 'none';
  snowflake.style.animation = `snow-fall ${randomDuration}s linear,
    ${Math.random() > 0.5 ? "snow-left" : "snow-right"} ${50 + Math.random() * 50}s linear`;
  document.body.appendChild(snowflake);
  setTimeout(() => { snowflake.remove() }, randomDuration * 1000);
}

function updateWinterMode() {
  if (document.body.classList.contains('winter')) {
    localStorage.setItem('winter', 'true');
    snowflakeTimer = setInterval(createSnowflake, Math.random() * 25 + 25);
    flashlight.src = '/i/flashlight_winter.webp';
    document.querySelector("meta[name=theme-color]").content = "#6ADCFF";
  } else {
    localStorage.setItem('winter', 'false');
    clearInterval(snowflakeTimer);
    snowflakeTimer = undefined;
    flashlight.src = '/i/flashlight.webp';
    document.querySelector("meta[name=theme-color]").content = "#FDDB29";
  }
}

window.addEventListener("load", (event) => {
  const currentMonth = new Date().getMonth() + 1;
  const isWinter = [12, 1, 2].includes(currentMonth);
  if (isWinter) {
    const winter = localStorage.getItem('winter');
    if (winter === 'true' || winter === null) {
      document.body.classList.toggle('winter');
      updateWinterMode();
    }
  }

  const winter = document.querySelector('div.winter');
  if (winter) {
      winter.onclick = function(e) {
      document.body.classList.toggle('winter');
      updateWinterMode();
    }
  }
});
