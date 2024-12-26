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


/** POINTERS **/

const ptr = {
  lastX: 0,
  lastY: 0,
  newX: 0,
  newY: 0,
  url: undefined,
  socket: undefined,
  me: 1000 + randInt(9000),
  container: undefined,
  pointers: new Map(),
  epoch: 0,
  timer: undefined
};

function ptrOnMessage(event) {
  if (document.visibilityState != 'visible') {
    ptr.socket.close();
    return;
  }
  
  const data = JSON.parse(event.data);
  for (const el of data) {
    const [id, x, y, platform] = el;
    if (id === ptr.me)
      continue;

    if (!ptr.pointers.has(id)) {
      const div = document.createElement("div");
      div.className = "pointer " + platform;
      div.dataset.id = id;
      ptr.container.appendChild(div);
      ptr.pointers.set(id, div);
    }
    const pointer = ptr.pointers.get(id);

    pointer.dataset.epoch = ptr.epoch;
    setTimeout(() => {
      pointer.style.left = Math.floor(Math.min(x, 10000) / 10000 * (document.body.clientWidth - 16)) + 'px';
      pointer.style.top =  Math.floor(Math.min(y, 10000) / 10000 * (document.body.clientHeight - 32)) + 'px';
    }, randInt(1000));
  }

  for (const [id, pointer] of ptr.pointers) {
    if (pointer.dataset.epoch < ptr.epoch) {
      ptr.pointers.delete(id);
      ptr.container.removeChild(pointer);
    }
  }
  ptr.epoch++;
}

function ptrOnTimer() {
  if (document.visibilityState != 'visible' && ptr.socket) {
    ptr.socket.close();
  } else if (ptr.lastX != ptr.newX || ptr.lastY != ptr.newY) {
    ptr.lastX = ptr.newX;
    ptr.lastY = ptr.newY;
    const x = Math.floor(ptr.lastX / document.body.clientWidth * 10000);
    const y = Math.floor(ptr.lastY / document.body.clientHeight * 10000);
    ptr.socket.send(JSON.stringify([x, y]));
  }
}

function ptrOnOpen(event) {
  ptr.timer = setInterval(ptrOnTimer, 1000);
}

function ptrOnClose(event) {
  ptr.socket = undefined;
  if (ptr.timer) {
    clearInterval(ptr.timer);
    ptr.timer = undefined;
  }
}

function ptrConnect() {
  if (document.visibilityState === 'visible') {
    ptr.socket = new WebSocket(ptr.url);
    ptr.socket.addEventListener("open", ptrOnOpen);
    ptr.socket.addEventListener("message", ptrOnMessage);
    ptr.socket.addEventListener("close", ptrOnClose);
  }
}

window.addEventListener("load", (event) => {
  if (/iPhone|iPad|iPod|Android/i.test(navigator.userAgent))
    return;

  const pl = window.navigator.platform.toLowerCase();
  const platform = pl.indexOf("mac") >= 0 ? "m" : pl.indexOf("win") >= 0 ? "w" : pl.indexOf("linux") >= 0 ? "l" : pl.indexOf("x11") >= 0 ? "l" : undefined;

  if (!platform)
    return;

  ptr.container = document.querySelector('.pointers');
  ptr.url = (location.protocol === "http:" ? "ws://" : "wss://") + location.host + "/ptrs" + "?id=" + ptr.me + "&page=" + location.pathname + "&platform=" + platform;

  window.addEventListener("mousemove", (event) => {
    ptr.newX = event.clientX + window.scrollX - 3;
    ptr.newY = event.clientY + window.scrollY - 5;
    if (!ptr.socket)
      ptrConnect();
  });
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
  const winter = localStorage.getItem('winter');
  console.log(winter, winter === 'true', winter === null);
  if (winter === 'true' || winter === null) {
    document.body.classList.toggle('winter');
    updateWinterMode();
  }
  document.querySelector('div.winter').onclick = function(e) {
    document.body.classList.toggle('winter');
    updateWinterMode();
  }
});
