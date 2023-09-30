var lastX, lastY, newX, newY;

window.addEventListener("mousemove", (event) => {
  newX = event.clientX + window.scrollX - 3;
  newY = event.clientY + window.scrollY - 5;
});

window.addEventListener("load", (event) => {
  if (/iPhone|iPad|iPod|Android/i.test(navigator.userAgent))
    return;

  const pl = window.navigator.platform.toLowerCase();
  const platform = pl.indexOf("mac") >= 0 ? "m" : pl.indexOf("win") >= 0 ? "w" : pl.indexOf("linux") >= 0 ? "l" : pl.indexOf("x11") >= 0 ? "l" : undefined;

  if (!platform)
    return;

  const me = Math.floor(Math.random() * 1000000000);
  const url = (location.protocol === "http:" ? "ws://" : "wss://") + location.host + "/pointers";
  const socket = new WebSocket(url + "?id=" + me + "&page=" + location.pathname + "&platform=" + platform);

  const container = document.querySelector('.pointers');
  const pointers = new Map();
  let epoch = 0;

  socket.addEventListener("message", (event) => {
    const data = JSON.parse(event.data);
    
    for (const el of data) {
      const [id, x, y, platform] = el;
      if (id === me)
        continue;

      if (!pointers.has(id)) {
        const div = document.createElement("div");
        div.className = "pointer " + platform;
        div.dataset.id = id;
        container.appendChild(div);
        pointers.set(id, div);
      }
      const pointer = pointers.get(id);

      pointer.dataset.epoch = epoch;
      pointer.style.left = x + 'px';
      pointer.style.top = y + 'px';
    }

    for (const [id, pointer] of pointers) {
      if (pointer.dataset.epoch < epoch) {
        pointers.delete(id);
        container.removeChild(pointer);
      }
    }
    epoch++;
  });

  setInterval(() => {
    if (lastX != newX || lastY != newY) {
      lastX = newX;
      lastY = newY;
      socket.send(JSON.stringify([lastX, lastY]));
    }
  }, 1000);
});