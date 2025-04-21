import { init, i } from "@instantdb/core";
import './presence.css';

// ID for app: tonsky.me
const APP_ID = "4f95493d-2467-4b40-98a0-002e98022ece";

const _schema = i.schema({
  entities: {},
  rooms: {
    presence: {
      presence: i.entity({
        id: i.string(),
        country_code: i.string(),
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
// const userId = await db.getLocalId('guest');
const userId = crypto.randomUUID();

// Function to get location data
interface LocationData {
  countryCode: string;
  country: string;
  city: string;
}

async function getLocationData(): Promise<Partial<LocationData>> {
  try {
    const response = await fetch('http://ip-api.com/json/?fields=country,countryCode,city');
    if (response.ok) {
      const data = await response.json();
      return {
        countryCode: data.countryCode || '??',
        country: data.country || 'Unknown',
        city: data.city || 'Unknown',
      };
    }
  } catch (error) {
    console.error("Error fetching location data:", error);
  }
  // Return default values if fetching fails
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
function getAnimal(id: string): [string, string] {  
  let hash = 0;
  for (let i = 0; i < id.length; i++) {
    const char = id.charCodeAt(i);
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
  // console.log(id, hash, animalIndex, animals.length);
  return animals[animalIndex]; // Always returns a tuple
}

// InstantCoreDatabase

const room = db.joinRoom('presence', roomId);

async function main() {
  const location = await getLocationData();
  room.publishPresence({
    id: userId, 
    country_code: location.countryCode!,
    country: location.country!,
    city: location.city!,
  });
}

room.subscribePresence({}, (presence) => {
  const { peers, user } = presence;
  const _user = user!;
  const _peers = { ...peers, [_user.id]: _user };

  let container = document.querySelector('#presence ul')!;
  container.innerHTML = ''; // Clear container before adding sorted items
  
  Object.values(_peers).sort((a, b) => {
    if (a.country_code < b.country_code) return -1;
    if (a.country_code > b.country_code) return 1;
    if (a.id < b.id) return -1;
    if (a.id > b.id) return 1;
    return 0;
  }).forEach((peer: { id: string, country_code: string, country: string, city: string }) => {
    if (!peer.id)
      return;
    
    const li = document.createElement('li');
    li.id = peer.id;

    // Create span for animal emoji
    const animalSpan = document.createElement('span');
    animalSpan.className = 'animal-emoji';
    const [animalEmoji, animalName] = getAnimal(peer.id);
    animalSpan.textContent = animalEmoji;
    li.appendChild(animalSpan);

    let listItemTitle = `Anonymous ${animalName}`;

    // Create and append flag overlay span if flag exists
    if (peer.country_code && peer.country_code !== '??') {
      listItemTitle += ` from ${peer.city}, ${peer.country}`;

      const flagSpan = document.createElement('span');
      flagSpan.className = 'flag-overlay';
      flagSpan.textContent = countryCodeToEmoji(peer.country_code);
      li.appendChild(flagSpan);
    }
    listItemTitle += ` (${peer.id.substring(0, 8)})`;

    li.title = listItemTitle;
    container.appendChild(li);
  });
});

main(); // Call the async main function

