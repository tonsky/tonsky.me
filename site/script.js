function randInt(max) {
  return Math.floor(Math.random() * max);
}

/** HOVERABLE **/

window.addEventListener("load", (event) => {
  document.querySelectorAll('.hoverable').forEach((e) => {
    e.onclick = function() { e.classList.toggle('clicked'); }
  });
});

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

function updateFlashlight(e) {
  var style = document.body.style;
  style.backgroundPositionX = e.pageX - 250 + 'px';
  style.backgroundPositionY = e.pageY - 250 + 'px';
}

window.addEventListener("load", (event) => {
  document.querySelector('.dark_mode').onclick = function(e) {
    var body = document.body;
    body.classList.toggle('dark');
    if (body.classList.contains('dark')) {
      updateFlashlight(e);
      ['mousemove', 'touchstart', 'touchmove', 'touchend'].forEach(function(s) {
        document.documentElement.addEventListener(s, updateFlashlight, false);
      });
    } else {
      ['mousemove', 'touchstart', 'touchmove', 'touchend'].forEach(function(s) {
        document.documentElement.removeEventListener(s, updateFlashlight, false);
      });
    }
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
