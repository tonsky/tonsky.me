import { init, i, PresenceResponse, PresenceOf, RoomHandle, TopicsOf } from "@instantdb/core";
import './presence.css';

// ID for app: tonsky.me
const APP_ID = "4f95493d-2467-4b40-98a0-002e98022ece";

const _schema = i.schema({
  entities: {},
  rooms: {
    presence: {
      presence: i.entity({
        user_id: i.string(),
        countryCode: i.string(),
        country: i.string(),
        city: i.string(),
        visible: i.boolean(),
        self: i.boolean().optional(),
        time_joined: i.number(),
      })
    },
  },
});

// This helps Typescript display better intellisense
type _AppSchema = typeof _schema;
interface AppSchema extends _AppSchema {}
const schema: AppSchema = _schema;

const db = init({ appId: APP_ID, schema: schema, devtool: false });

const currentUrl = new URL(window.location.href);
const roomId = currentUrl.origin; // + currentUrl.pathname;
let regionNames: Intl.DisplayNames | undefined;
try {
  regionNames = new Intl.DisplayNames(['en'], { type: 'region' });
} catch (e) {
  regionNames = undefined;
}

let container: HTMLElement | null = null;

interface LocationData {
  countryCode: string;
  country: string;
  city: string;
}

type RoomData = LocationData & {
  user_id: string;
  visible: boolean;
  self?: boolean;
  time_joined?: number;
}

async function getLocationData(): Promise<LocationData> {
  try {
    const response = await fetch('/geoip');

    if (response.ok) {
      const data = await response.json();
      const countryCode = data.country || '??';
      let countryName = 'Unknown';

      // Use Intl.DisplayNames to get the country name from the code
      if (countryCode !== '??' && regionNames) {
        countryName = regionNames.of(countryCode) || 'Unknown';
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
function countryCodeToEmoji(code: string): string | null {
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
const animals: [string, string][] = [
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
function getAnimal(user_id: string): [string, string] {  
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
    
  const animalIndex = Math.abs(hash) % animals.length;
  return animals[animalIndex];
}

function compareRoomData(a: RoomData, b: RoomData): number {
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

function createPeerElement(peer: RoomData): HTMLElement | null {
  if (!peer.user_id) return null;

  const li = document.createElement('li');
  li.setAttribute('data-user-id', peer.user_id);
  // Store full peer data for comparison
  (li as any).__peerData = peer;

  // Create the peer content container
  const container = document.createElement('div');
  container.className = 'container';

  // Create span for animal emoji
  const animalSpan = document.createElement('span');
  animalSpan.className = 'animal-emoji';
  const [animalEmoji, animalName] = getAnimal(peer.user_id);
  animalSpan.textContent = animalEmoji;
  container.appendChild(animalSpan);

  let listItemTitle = `Anonymous ${animalName}`;

  // Create and append flag overlay span if flag exists
  if (peer.countryCode && peer.countryCode !== '??') {
    listItemTitle += ` from ${peer.city}, ${peer.country}`;

    const flagSpan = document.createElement('span');
    flagSpan.className = 'flag-overlay';
    flagSpan.textContent = countryCodeToEmoji(peer.countryCode);
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

function onPresenceChange(presence: PresenceResponse<PresenceOf<AppSchema, 'presence'>, keyof RoomData>) {
  if (!container) return;
  
  const { peers: _peers, user } = presence;

  const peers = { ..._peers };
  if (user?.user_id) {
    peers[user.user_id] = {...user, self: true};
  }

  // Filter for unique user_ids
  const uniquePeers = new Map<string, RoomData>();
  for (const peer of Object.values(peers)) {
    if (peer && peer.visible && peer.user_id && !uniquePeers.has(peer.user_id)) {
      uniquePeers.set(peer.user_id, peer);
    }
  }

  // Sort the unique peers
  const sortedPeers = [...uniquePeers.values()].sort(compareRoomData);
  
  // Two-pointer algorithm to sync DOM with sorted peers
  let domIndex = 0;
  let peerIndex = 0;
  const domChildren = Array.from(container.children) as HTMLElement[];
  
  while (domIndex < domChildren.length || peerIndex < sortedPeers.length) {
    const domElement = domIndex < domChildren.length ? domChildren[domIndex] : null;
    const domPeerData = domElement ? (domElement as any).__peerData : null;

    const peer = peerIndex < sortedPeers.length ? sortedPeers[peerIndex] : null;

    // Skip elements that are currently being removed
    if (domElement && domElement.classList.contains('removing')) {
      domIndex++;
      continue;
    }

    let comparison = 0;
    if (domPeerData && peer) {
      comparison = compareRoomData(domPeerData, peer);
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
      domElement!.classList.add('removing');
      setTimeout(function() {
        document.querySelectorAll('#presence > ul > li.removing').forEach(el => el.remove());
      }, 100);
      domIndex++;
    } else {
      // New peer should be inserted
      const li = createPeerElement(peer!);
      if (li) {
        li.classList.add('inserting');
        if (domElement) {
          container.insertBefore(li, domElement);
        } else {
          container.appendChild(li);
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

var room: RoomHandle<PresenceOf<AppSchema, 'presence'>, TopicsOf<AppSchema, 'presence'>> | undefined;
var presence: Partial<RoomData> | undefined;

function onVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    room?.publishPresence({...presence, visible: false});
  } else if (document.visibilityState === 'visible') {
    room?.publishPresence({...presence, visible: true});
  }
}

async function main() {
  container = document.querySelector('#presence ul');
  
  const user_id = await db.getLocalId('guest');
  const location = await getLocationData();
  presence = {
    user_id: user_id, 
    countryCode: location.countryCode!,
    country: location.country!,
    city: location.city!,
    time_joined: Date.now(),
  };
  room = db.joinRoom('presence', roomId);
  room.subscribePresence({}, onPresenceChange);
  onVisibilityChange();
  document.addEventListener('visibilitychange', onVisibilityChange);

  // setTimeout(() => onPresenceChange(generateTestPresences(100)), 2000);
}

main();

// @ts-ignore: TS6133
function generateTestPresences(amount: number) {
  if (!container) return;

  const countryCodes = ['US', 'GB', 'DE', 'FR', 'JP', 'CA', 'AU', 'BR', 'IN', 'CN', 'ES', 'IT', 'NL', 'SE', 'NO', 'DK', 'FI', 'BE', 'AT', 'CH', 'PT', 'PL', 'CZ', 'HU', 'RO', 'BG', 'HR', 'SI', 'SK', 'EE', 'LV', 'LT', 'IE', 'GR', 'CY', 'MT', 'LU', 'IS', 'MX', 'AR', 'CL', 'CO', 'PE', 'VE', 'UY', 'PY', 'BO', 'EC', 'GY', 'SR'];
  const cities = ['New York', 'London', 'Berlin', 'Paris', 'Tokyo', 'Toronto', 'Sydney', 'São Paulo', 'Mumbai', 'Beijing', 'Madrid', 'Rome', 'Amsterdam', 'Stockholm', 'Oslo', 'Copenhagen', 'Helsinki', 'Brussels', 'Vienna', 'Zurich'];

  const generateRandomUserId = () => {
    return Array.from({length: 32}, () => Math.floor(Math.random() * 16).toString(16)).join('');
  };

  const generateRandomPresence = (): RoomData => {
    const countryCode = countryCodes[Math.floor(Math.random() * countryCodes.length)];
    const city = cities[Math.floor(Math.random() * cities.length)];
    let countryName = 'Unknown';

    if (regionNames) {
      countryName = regionNames.of(countryCode) || 'Unknown';
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

  const testPeers: Record<string, any> = {};
  for (let i = 0; i < amount; i++) {
    const presence = generateRandomPresence();
    testPeers[presence.user_id] = presence;
  }

  const mockPresenceResponse: PresenceResponse<PresenceOf<AppSchema, 'presence'>, keyof RoomData> = {
    peers: testPeers,
    user: undefined,
    isLoading: false,
    error: undefined
  };

  return mockPresenceResponse;
}
