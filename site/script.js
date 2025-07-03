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


/** PRESENCE **/

const presenceDB = instant.init({ appId: "4f95493d-2467-4b40-98a0-002e98022ece", devtool: false });

var presenceRegions;
try {
  presenceRegions = new Intl.DisplayNames(['en'], { type: 'region' });
} catch (e) {
  presenceRegions = undefined;
}

var presenceUL = null;

async function presenceMyLocation() {
  try {
    const response = await fetch('/geoip');

    if (response.ok) {
      const data = await response.json();
      const countryCode = data.country || '??';
      let countryName = 'Unknown';

      // Use Intl.DisplayNames to get the country name from the code
      if (countryCode !== '??' && presenceRegions) {
        countryName = presenceRegions.of(countryCode) || 'Unknown';
      }

      return {
        countryCode: countryCode,
        country: countryName, // Use the resolved country name
        city: data.city || 'Unknown',
      };
    } else {
      // Handle non-OK responses (e.g., 4xx, 5xx)
      const responseBody = await response.text();
      console.error(`ipinfo.io request failed with status: ${response.status}. Body: ${responseBody}`);
    }
  } catch (error) {
    console.error("Error fetching location data:", error);
  }

  return {
    countryCode: '??',
    country: 'Unknown',
    city: 'Unknown',
  };
}

// Helper function to convert country code to flag emoji
function presenceCountryCodeToFlag(code) {
  if (!code || code.length !== 2 || code === '??') {
    return null; // Invalid code
  }
  // Formula to convert letters (A=65) to Regional Indicator Symbols (A=1F1E6)
  const codePoints = code.toUpperCase()
                      .split('')
                      .map(char => 0x1F1E6 + (char.charCodeAt(0) - 65));
  // Ensure both characters are valid letters A-Z
  if (codePoints.some(cp => cp < 0x1F1E6 || cp > 0x1F1FF)) {
      return null;
  }
  return String.fromCodePoint(...codePoints);
}

// Data structure for animal emojis and names
const presenceAnimals = [
  ['🐶', 'Dog'], ['🐱', 'Cat'], ['🐭', 'Mouse'], ['🐹', 'Hamster'], ['🐰', 'Rabbit'], ['🦊', 'Fox'], ['🐻', 'Bear'],
  ['🐼', 'Panda'], ['🐨', 'Koala'], ['🐯', 'Tiger'], ['🦁', 'Lion'], ['🐮', 'Cow'], ['🐷', 'Pig'], ['🐸', 'Frog'],
  ['🐵', 'Monkey'], ['🐔', 'Chicken'], ['🐧', 'Penguin'], ['🐦', 'Bird'], ['🐤', 'Chick'], ['🦆', 'Duck'], ['🦅', 'Eagle'],
  ['🦉', 'Owl'], ['🦇', 'Bat'], ['🐺', 'Wolf'], ['🐗', 'Boar'], ['🐴', 'Horse'], ['🦄', 'Unicorn'], ['🐝', 'Bee'],
  ['🐛', 'Bug'], ['🦋', 'Butterfly'], ['🐌', 'Snail'], ['🐞', 'Lady Beetle'], ['🐜', 'Ant'], ['🦟', 'Mosquito'],
  ['🦗', 'Cricket'], ['🕷️', 'Spider'], ['🦂', 'Scorpion'], ['🐢', 'Turtle'], ['🐍', 'Snake'], ['🦎', 'Lizard'],
  ['🦖', 'T-Rex'], ['🦕', 'Sauropod'], ['🐙', 'Octopus'], ['🦑', 'Squid'], ['🦐', 'Shrimp'], ['🦞', 'Lobster'],
  ['🦀', 'Crab'], ['🐡', 'Blowfish'], ['🐠', 'Tropical Fish'], ['🐟', 'Fish'], ['🐬', 'Dolphin'], ['🐳', 'Whale'],
  ['🐋', 'Humpback Whale'], ['🦈', 'Shark'], ['🐊', 'Crocodile'], ['🐅', 'Leopard'], ['🦓', 'Zebra'], ['🦍', 'Gorilla'],
  ['🦧', 'Orangutan'], ['🐘', 'Elephant'], ['🦛', 'Hippopotamus'], ['🦏', 'Rhinoceros'], ['🐪', 'Camel'],
  ['🐫', 'Two-Hump Camel'], ['🦒', 'Giraffe'], ['🦘', 'Kangaroo'], ['🐃', 'Water Buffalo'], ['🐂', 'Ox'], ['🐄', 'Dairy Cow'],
  ['🐎', 'Racehorse'], ['🐖', 'Pig Face'], ['🐏', 'Ram'], ['🐑', 'Ewe'], ['🦙', 'Llama'], ['🐐', 'Goat'], ['🦌', 'Deer'],
  ['🐕', 'Guide Dog'], ['🐩', 'Poodle'], ['🦮', 'Service Dog'], ['🐕‍🦺', 'Safety Vest Dog'], ['🐈', 'Black Cat'],
  ['🐈‍⬛', 'Tomcat'], ['🐓', 'Rooster'], ['🦃', 'Turkey'], ['🦚', 'Peacock'], ['🦜', 'Parrot'], ['🦢', 'Swan'],
  ['🦩', 'Flamingo'], ['🕊️', 'Dove'], ['🐇', 'White Rabbit'], ['🦝', 'Raccoon'], ['🦨', 'Skunk'], ['🦡', 'Badger'],
  ['🦦', 'Otter'], ['🦥', 'Sloth'], ['🐁', 'White Mouse'], ['🐀', 'Rat'], ['🐿️', 'Chipmunk'], ['🦔', 'Hedgehog']
];

// Helper function to get a deterministic animal emoji and name tuple based on ID
function presenceUserAnimal(user_id) {
  let hash = 0;
  for (let i = 0; i < user_id.length; i++) {
    const char = user_id.charCodeAt(i);
    let value = 0;

    if (char >= 48 && char <= 57) { // ASCII '0' to '9'
        value = char - 48;
    } else if (char >= 97 && char <= 102) { // ASCII 'a' to 'f'
        value = char - 97 + 10;
    } else if (char >= 65 && char <= 70) { // ASCII 'A' to 'F'
        value = char - 65 + 10;
    } else {
        value = char;
    }

    hash = (hash << 4) + value;
    hash |= 0; // Convert to 32bit integer
  }

  const animalIndex = Math.abs(hash) % presenceAnimals.length;
  return presenceAnimals[animalIndex];
}

function presenceCompareRoomData(a, b) {
  // Handle undefined time_joined (older clients)
  const aTime = a.time_joined ?? 0;
  const bTime = b.time_joined ?? 0;

  if (aTime < bTime) return -1;
  if (aTime > bTime) return 1;
  if (a.countryCode < b.countryCode) return -1;
  if (a.countryCode > b.countryCode) return 1;
  if (a.user_id < b.user_id) return -1;
  if (a.user_id > b.user_id) return 1;
  return 0;
}

function presenceCreatePeerElement(peer) {
  if (!peer.user_id) return null;

  const li = document.createElement('li');
  li.setAttribute('data-user-id', peer.user_id);
  // Store full peer data for comparison
  li.__peerData = peer;

  // Create the peer content container
  const container = document.createElement('div');
  container.className = 'container';

  // Create span for animal emoji
  const animalSpan = document.createElement('span');
  animalSpan.className = 'animal-emoji';
  const [animalEmoji, animalName] = presenceUserAnimal(peer.user_id);
  animalSpan.textContent = animalEmoji;
  container.appendChild(animalSpan);

  let listItemTitle = `Anonymous ${animalName}`;

  // Create and append flag overlay span if flag exists
  if (peer.countryCode && peer.countryCode !== '??') {
    listItemTitle += ` from ${peer.city}, ${peer.country}`;

    const flagSpan = document.createElement('span');
    flagSpan.className = 'flag-overlay';
    flagSpan.textContent = presenceCountryCodeToFlag(peer.countryCode);
    container.appendChild(flagSpan);
  }

  if (peer.self) {
    listItemTitle += ` (you)`;
  }

  // listItemTitle += ` (${peer.user_id.substring(0, 8)})`;

  li.title = listItemTitle;
  li.appendChild(container);
  return li;
}

function presenceOnChange(presence) {
  if (!presenceUL) return;

  const { peers: _peers, user } = presence;

  const peers = { ..._peers };
  if (user?.user_id) {
    peers[user.user_id] = {...user, self: true};
  }

  // Filter for unique user_ids
  const uniquePeers = new Map();
  for (const peer of Object.values(peers)) {
    if (peer && peer.visible && peer.user_id && !uniquePeers.has(peer.user_id)) {
      uniquePeers.set(peer.user_id, peer);
    }
  }

  // Sort the unique peers
  const sortedPeers = [...uniquePeers.values()].sort(presenceCompareRoomData);

  // Two-pointer algorithm to sync DOM with sorted peers
  let domIndex = 0;
  let peerIndex = 0;
  const domChildren = Array.from(presenceUL.children);

  while (domIndex < domChildren.length || peerIndex < sortedPeers.length) {
    const domElement = domIndex < domChildren.length ? domChildren[domIndex] : null;
    const domPeerData = domElement?.__peerData;

    const peer = peerIndex < sortedPeers.length ? sortedPeers[peerIndex] : null;

    // Skip elements that are currently being removed
    if (domElement && domElement.classList.contains('removing')) {
      domIndex++;
      continue;
    }

    let comparison = 0;
    if (domPeerData && peer) {
      comparison = presenceCompareRoomData(domPeerData, peer);
    } else if (!peer) {
      comparison = -1; // DOM element should be removed
    } else if (!domPeerData) {
      comparison = 1; // New peer should be inserted
    }

    if (comparison === 0) {
      // Equal: advance both pointers
      domIndex++;
      peerIndex++;
    } else if (comparison < 0) {
      // DOM element should be deleted
      domElement.classList.add('removing');
      setTimeout(function() {
        document.querySelectorAll('#presence > ul > li.removing').forEach(el => el.remove());
      }, 100);
      domIndex++;
    } else {
      // New peer should be inserted
      const li = presenceCreatePeerElement(peer);
      if (li) {
        li.classList.add('inserting');
        if (domElement) {
          presenceUL.insertBefore(li, domElement);
        } else {
          presenceUL.appendChild(li);
        }

        // Trigger the expansion animation
        requestAnimationFrame(() => {
          requestAnimationFrame(() => {
            li.classList.remove('inserting');
          });
        });
      }
      peerIndex++;
    }
  }
}

var presenceRoom;
var presenceMyData;

function presenceOnVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    presenceRoom?.publishPresence({...presenceMyData, visible: false});
  } else if (document.visibilityState === 'visible') {
    presenceRoom?.publishPresence({...presenceMyData, visible: true});
  }
}

async function presenceOnLoad() {
  presenceUL = document.querySelector('#presence ul');

  const user_id = await presenceDB.getLocalId('guest');
  const location = await presenceMyLocation();
  presenceMyData = {
    user_id: user_id,
    countryCode: location.countryCode,
    country: location.country,
    city: location.city,
    time_joined: Date.now(),
  };

  const currentUrl = new URL(window.location.href);
  const roomId = currentUrl.origin; // + currentUrl.pathname;

  presenceRoom = presenceDB.joinRoom('presence', roomId);
  presenceRoom.subscribePresence({}, presenceOnChange);
  presenceOnVisibilityChange();
  document.addEventListener('visibilitychange', presenceOnVisibilityChange);

  // setTimeout(() => presenceOnChange(presenceFakeData(100)), 2000);
}

window.addEventListener("load", (event) => {
  presenceOnLoad();
});

function presenceFakeData(amount) {
  if (!presenceUL) return;

  const countryCodes = ['US', 'GB', 'DE', 'FR', 'JP', 'CA', 'AU', 'BR', 'IN', 'CN', 'ES', 'IT', 'NL', 'SE', 'NO', 'DK', 'FI', 'BE', 'AT', 'CH', 'PT', 'PL', 'CZ', 'HU', 'RO', 'BG', 'HR', 'SI', 'SK', 'EE', 'LV', 'LT', 'IE', 'GR', 'CY', 'MT', 'LU', 'IS', 'MX', 'AR', 'CL', 'CO', 'PE', 'VE', 'UY', 'PY', 'BO', 'EC', 'GY', 'SR'];
  const cities = ['New York', 'London', 'Berlin', 'Paris', 'Tokyo', 'Toronto', 'Sydney', 'São Paulo', 'Mumbai', 'Beijing', 'Madrid', 'Rome', 'Amsterdam', 'Stockholm', 'Oslo', 'Copenhagen', 'Helsinki', 'Brussels', 'Vienna', 'Zurich'];

  const generateRandomUserId = () => {
    return Array.from({length: 32}, () => Math.floor(Math.random() * 16).toString(16)).join('');
  };

  const generateRandomPresence = () => {
    const countryCode = countryCodes[Math.floor(Math.random() * countryCodes.length)];
    const city = cities[Math.floor(Math.random() * cities.length)];
    let countryName = 'Unknown';

    if (presenceRegions) {
      countryName = presenceRegions.of(countryCode) || 'Unknown';
    }

    return {
      user_id: generateRandomUserId(),
      countryCode,
      country: countryName,
      city,
      visible: true,
      time_joined: Date.now() - Math.floor(Math.random() * 300000), // Random time within last 5 minutes
    };
  };

  const testPeers = {};
  for (let i = 0; i < amount; i++) {
    const presence = generateRandomPresence();
    testPeers[presence.user_id] = presence;
  }

  const mockPresenceResponse = {
    peers: testPeers,
    user: undefined,
    isLoading: false,
    error: undefined
  };

  return mockPresenceResponse;
}
