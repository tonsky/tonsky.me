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
const roomId = currentUrl.origin + currentUrl.pathname;
let regionNames: Intl.DisplayNames | undefined;
try {
  regionNames = new Intl.DisplayNames(['en'], { type: 'region' });
} catch (e) {
  regionNames = undefined;
}

const container: HTMLElement = document.querySelector('#presence ul')!;

interface LocationData {
  countryCode: string;
  country: string;
  city: string;
}

type RoomData = LocationData & {
  user_id: string;
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
  if (a.countryCode < b.countryCode) return -1;
  if (a.countryCode > b.countryCode) return 1;
  if (a.user_id < b.user_id) return -1;
  if (a.user_id > b.user_id) return 1;
  return 0;
}

function appendPeerItem(peer: RoomData) {
  if (!peer.user_id) return;

  const li = document.createElement('li');

  // Create span for animal emoji
  const animalSpan = document.createElement('span');
  animalSpan.className = 'animal-emoji';
  const [animalEmoji, animalName] = getAnimal(peer.user_id);
  animalSpan.textContent = animalEmoji;
  li.appendChild(animalSpan);

  let listItemTitle = `Anonymous ${animalName}`;

  // Create and append flag overlay span if flag exists
  if (peer.countryCode && peer.countryCode !== '??') {
    listItemTitle += ` from ${peer.city}, ${peer.country}`;

    const flagSpan = document.createElement('span');
    flagSpan.className = 'flag-overlay';
    flagSpan.textContent = countryCodeToEmoji(peer.countryCode);
    li.appendChild(flagSpan);
  }
  // listItemTitle += ` (${peer.user_id.substring(0, 8)})`;

  li.title = listItemTitle;
  container.appendChild(li);
}

function onPresenceChange(presence: PresenceResponse<PresenceOf<AppSchema, 'presence'>, keyof RoomData>) {
  const { peers: _peers, user } = presence;
  const peers = { ..._peers };
  if (user?.user_id) {
    peers[user.user_id] = user;
  }
  container.innerHTML = '';

  // Filter for unique user_ids
  const uniquePeers = new Map<string, RoomData>();
  for (const peer of Object.values(peers)) {
    if (peer && peer.user_id && !uniquePeers.has(peer.user_id)) {
      uniquePeers.set(peer.user_id, peer);
    }
  }

  // Remove self from the list
  if (user?.user_id) {
    uniquePeers.delete(user.user_id);
  }
  
  // Sort and render the unique peers
  [...uniquePeers.values()]
    .sort(compareRoomData)
    .forEach(appendPeerItem);
}

var room: RoomHandle<PresenceOf<AppSchema, 'presence'>, TopicsOf<AppSchema, 'presence'>> | undefined;
var presence: RoomData | undefined;

function onVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    room?.leaveRoom();
    room = undefined;
  } else if (document.visibilityState === 'visible') {
    room = db.joinRoom('presence', roomId);
    room.publishPresence(presence!);
    room.subscribePresence({}, onPresenceChange);
  }
}

async function main() {
  const user_id = await db.getLocalId('guest');
  const location = await getLocationData();
  presence = {
    user_id: user_id, 
    countryCode: location.countryCode!,
    country: location.country!,
    city: location.city!,
  };
  document.addEventListener('visibilitychange', onVisibilityChange);
  onVisibilityChange();
}

main();

