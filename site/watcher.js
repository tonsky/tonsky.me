var watcher;
var watcherWasConnected = false;

function watcherConnect() {
  console.log("Connecting to ws://" + location.host + "/watcher...");
  watcher = new WebSocket("ws://" + location.host + "/watcher");
  watcher.addEventListener("open", (event) => {
    console.log("Connected to ws://" + location.host + "/watcher");
    watcherWasConnected = true;
  });
  watcher.addEventListener("message", (event) => {
    console.log("Refreshing because of", event.data);
    document.location.reload();
  });
  watcher.addEventListener("close", (event) => {
    watcher = undefined;
    if (watcherWasConnected) {
      setTimeout(() => {
        watcherConnect();
        console.log("Refreshing because of disconnect");
        document.location.reload();
      }, 1000);
    }
  });
}

window.addEventListener("load", (event) => {
  watcherConnect();
});