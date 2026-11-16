/**
 * Echter Test von Morni Wolkenkampf mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node wolkenkampf-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Carl), Bert (englisch), Carl, Dora, später Emil (schaut zu). Sprache und
 * Party kommen vom Test-Netzwerk (test/fake-network), das auch Statistik, Zustand und „zur Lobby“ in calls.log
 * mitschreibt.
 *
 * Runde 1: allein auf der Wolkenalm – Gondel, Landung, Truhen, Brücke, Kampf, Hinunterstoßen, Nachschub, Gewitter,
 * Nebel, Sieg, Statistik, Karte wieder frisch. Runde 2: zu zweit auf dem Kristallgipfel – Party im selben Team, kein
 * Schaden unter Teamkameraden, Zuschauen, Sieg zu zweit.
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '25810', RCON = '127.0.0.1:25910', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const HALL = { x: 0.5, y: 101, z: 11.5 };
const FLOOR = 100;

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const inWorld = (c) => `execute in minecraft:wolkenkampf run ${c}`;
const passed = async (c) => /passed/i.test((await cmd(inWorld(c)))[0]);
const playerFile = (name) => path.join(SERVER, 'plugins/MorniWolkenkampf/spieler', `${offlineUuid(name)}.yml`);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MoinMorni/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));

async function waitFor(cond, ms = 5000, step = 150) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

let lastJoin = 0;
async function join(name) {
  const wait = lastJoin + 3500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 25000);
  });
  await sleep(1500);
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const inHall = (bot) => near(bot.entity.position, HALL, 5) && bot.game.gameMode === 'adventure';
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const sawTitle = (bot, re) => bot.titles.some((t) => re.test(t));
const sawBar = (bot, re) => bot.bars.some((t) => re.test(t));
const clear = (...bots) =>
  bots.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
  });

/** /wolkenkampf status als Werte: {karte, phase, zeit, …, players: {Anna: {state, team, island, kills, chests, place}}, teams: [...]} */
async function status() {
  const lines = (await cmd('wolkenkampf status'))[0].split('\n');
  const out = { info: {} };
  for (const part of lines[0].split(' ')) {
    const [k, v] = part.split('=');
    if (v !== undefined) out.info[k] = v;
  }
  for (const line of lines.slice(1)) {
    if (!line.startsWith('karte=')) continue;
    const s = { players: {}, teams: [] };
    for (const part of line.split(' ')) {
      const [k, v] = part.split('=');
      if (v === undefined) continue;
      if (/^team\d+$/.test(k)) {
        const [island, x, z, top, gy] = v.split(':').map(Number);
        s.teams[Number(k.slice(4))] = { island, x, z, top, gy };
      } else if (/^[A-Z]/.test(k)) {
        if (v === 'SPECTATOR') {
          s.players[k] = { state: 'SPECTATOR' };
        } else {
          const [state, t, i, kk, c, p] = v.split(':');
          s.players[k] = { state, team: Number(t.slice(1)), island: Number(i.slice(1)), kills: Number(kk.slice(1)),
            chests: Number(c.slice(1)), place: Number(p.slice(1)) };
        }
      } else {
        s[k] = /^-?\d+(\.\d+)?$/.test(v) ? Number(v) : v;
      }
    }
    out[s.karte] = s;
  }
  return out;
}

async function openWithItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  bot.activateItem();
  return opened;
}

async function closeWindow(bot) {
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
  await sleep(250);
}

/** Bis ein Spieler im Ziel-Bereich ist (Teleport angekommen). */
async function tp(bot, x, y, z, yaw = 0, pitch = 0) {
  await cmd(inWorld(`tp ${bot.username} ${x} ${y} ${z} ${yaw} ${pitch}`));
  await waitFor(() => near(bot.entity.position, { x, y, z }, 1.5), 3000, 100);
  await sleep(300);
}

function invHas(bot, name) {
  return bot.inventory.items().some((i) => i.name === name);
}

async function attackUntil(attacker, victim, done, tries = 10) {
  for (let i = 0; i < tries; i++) {
    const target = attacker.players[victim.username]?.entity;
    if (target) {
      await attacker.lookAt(target.position.offset(0, 1.4, 0), true);
      attacker.attack(target);
    }
    await sleep(800);
    if (await done()) return true;
  }
  return false;
}

async function main() {
  // ------------------------------------------------------------------ Wolkenhafen
  await sleep(6000);
  const holos = String(await cmd(inWorld('execute positioned 0 101 0 if entity @e[type=text_display,distance=..40]')));
  check('Tafeln im Wolkenhafen stehen (6 Tafeln × 2 Sprachen)', /: 12\b/.test(holos), holos.trim());
  const anna = await join('Anna');
  const bert = await join('Bert');
  check('Ankunft im Wolkenhafen (Abenteuermodus)', inHall(anna), JSON.stringify(anna.entity.position));
  check('Schnellleiste: Kompass, Farbstoff, Kiste, Gold, Bett',
    slot(anna, 0)?.name === 'compass' && slot(anna, 1)?.name === 'gray_dye' && slot(anna, 2)?.name === 'chest' &&
      slot(anna, 4)?.name === 'gold_ingot' && slot(anna, 8)?.name === 'red_bed', [0, 1, 2, 4, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Abstimmen“', plain(slot(anna, 0)?.customName).includes('Abstimmen'), plain(slot(anna, 0)?.customName));
  check('Englisch (Sprache vom Netzwerk): „Vote“', plain(slot(bert, 0)?.customName).includes('Vote'), plain(slot(bert, 0)?.customName));
  check('Begrüßung + Erklärung im Chat', sees(anna, /Willkommen im Wolkenhafen/) && sees(anna, /Ballon-Gondel/) &&
    sees(bert, /Welcome to the Cloud Harbour/));
  check('Leiste rechts (Scoreboard) vorhanden', await waitFor(() => Object.keys(anna.scoreboards ?? {}).length > 0, 3000));
  check('Netzwerk: Zustand „waiting“', calls().some((c) => c.t === 'state' && c.state === 'waiting'));
  await tp(anna, 0.5, 101, 16.5, 0, 0);
  check('Unsichtbarer Rand: niemand fällt vom Wolkenhafen', await (async () => {
    anna.setControlState('forward', true);
    await sleep(2500);
    anna.setControlState('forward', false);
    return anna.entity.position.y > 99;
  })(), JSON.stringify(anna.entity.position));
  await tp(anna, 0.5, 101, 11.5, 180, 0);

  // ------------------------------------------------------------------ Abstimmen + Ausrüstung
  let win = await openWithItem(anna, 0);
  check('Kompass öffnet „Abstimmen“', win && plain(win.title).includes('Abstimmen'), win && plain(win.title));
  check('Karten und Spielarten im Menü', plain(win?.slots[10]?.customName).includes('Wolkenalm') &&
    plain(win?.slots[12]?.customName).includes('Kristallgipfel') && plain(win?.slots[14]?.customName).includes('Allein') &&
    plain(win?.slots[16]?.customName).includes('Zu zweit'),
    [10, 12, 14, 16].map((i) => plain(win?.slots[i]?.customName)).join(' / '));
  await anna.clickWindow(10, 0, 0);
  await sleep(600);
  check('Stimme für die Wolkenalm', sees(anna, /Deine Stimme: Wolkenalm/));
  win = await openWithItem(anna, 0);
  await anna.clickWindow(14, 0, 0);
  await sleep(600);
  check('Stimme für „Allein“', sees(anna, /Deine Stimme: Allein/));
  bert.chat('/karte wolkenalm');
  bert.chat('/spielart allein');
  await sleep(800);
  check('Bert stimmt per Befehl (englisch)', sees(bert, /Your vote: Cloud Alp/) && sees(bert, /Your vote: Solo/),
    bert.chats.slice(-2).join(' | '));
  win = await openWithItem(anna, 2);
  check('Kiste öffnet „Start-Ausrüstung (kostenlos)“ mit 7 Ausrüstungen', win && /Start-Ausrüstung/.test(plain(win.title)) &&
    [10, 11, 12, 13, 14, 15, 16].every((i) => win.slots[i]), win && plain(win.title));
  await anna.clickWindow(11, 0, 0);
  await sleep(600);
  check('Anna wählt „Baumeister“', sees(anna, /Deine Start-Ausrüstung: Baumeister/));
  bert.chat('/kit hunter');
  await sleep(600);
  check('Bert wählt per Befehl „Hunter“ (Jäger)', sees(bert, /Your starting kit: Hunter/), bert.chats.slice(-1)[0]);
  check('Ausrüstung in den Spielerdaten gemerkt', await waitFor(() => /ausruestung: baumeister/.test(readFile(playerFile('Anna'))), 3000));

  // ------------------------------------------------------------------ Countdown → Gondeln
  clear(anna, bert);
  await sleep(1500);
  check('Kein Countdown mit 2 von 3 nötigen Leuten', !anna.bars.some((b) => /Nächste Runde/.test(b)), anna.bars.slice(-1)[0]);
  const carl = await join('Carl');
  const dora = await join('Dora');
  check('Mit 4 Leuten läuft der Countdown', await waitFor(() => anna.bars.some((b) => /Nächste Runde in \d+ s · Wolkenalm · Allein/.test(b)), 4000),
    anna.bars.slice(-1)[0]);
  for (const b of [anna, bert, carl, dora]) {
    b.setQuickBarSlot(1);
    await sleep(150);
    b.activateItem();
  }
  check('Alle bereit (Farbstoff)', await waitFor(() => sees(anna, /Du bist bereit/) && sees(bert, /You're ready/), 3000));
  check('Start-Nachricht', await waitFor(() => sees(anna, /Auf geht's! Karte: Wolkenalm · Allein · 4 Leute/), 12000),
    anna.chats.slice(-2).join(' | '));
  let s = (await status()).wolkenalm;
  check('Runde in der Gondel: 4 Teams, 4 verschiedene Inseln', s?.phase === 'GONDOLA' && s.teams.length === 4 &&
    new Set(s.teams.map((t) => t.island)).size === 4, JSON.stringify(s?.teams));
  const team = (name) => s.teams[s.players[name].team];
  await sleep(1000);
  const annaT = team('Anna');
  check('Alle stehen in ihrer Gondel über der eigenen Insel', ['Anna', 'Bert', 'Carl', 'Dora'].every((n) => {
    const b = { Anna: anna, Bert: bert, Carl: carl, Dora: dora }[n];
    const t = team(n);
    return Math.hypot(b.entity.position.x - (t.x + 0.5), b.entity.position.z - (t.z + 0.5)) < 1.8 &&
      Math.abs(b.entity.position.y - (t.gy + 1)) < 1.2;
  }), JSON.stringify(anna.entity.position) + ' / ' + JSON.stringify(annaT));
  check('Abenteuermodus in der Gondel, Titel und Inselfarbe', anna.game.gameMode === 'adventure' &&
    sawTitle(anna, /Wolkenkampf/) && sees(anna, /Deine Insel: /) && sees(bert, /Your island: /),
    anna.chats.slice(-2).join(' | '));
  const gondolaFloor = await passed(`execute if block ${annaT.x} ${annaT.gy} ${annaT.z} spruce_trapdoor[open=false]`);
  check('Gondelboden aus geschlossenen Falltüren', gondolaFloor);
  anna.setControlState('forward', true);
  anna.setControlState('jump', true);
  await sleep(1500);
  anna.setControlState('forward', false);
  anna.setControlState('jump', false);
  await sleep(300);
  check('Die Gondel hält fest (niemand springt vorher hinaus)',
    Math.hypot(anna.entity.position.x - (annaT.x + 0.5), anna.entity.position.z - (annaT.z + 0.5)) < 1.9 &&
      anna.entity.position.y > annaT.gy, JSON.stringify(anna.entity.position));
  check('Countdown im Titel', await waitFor(() => sawTitle(anna, /^[1-5]$/), 6000), anna.titles.join(' | '));

  // ------------------------------------------------------------------ Öffnen + Landen
  check("Gondel öffnet sich: „Los geht's!“", await waitFor(() => sawTitle(anna, /Los geht's/) && sawTitle(bert, /Let's go/), 8000),
    anna.titles.slice(-2).join(' | '));
  s = (await status()).wolkenalm;
  check('Phase Kampf, Überlebensmodus', s.phase === 'FIGHT' && await waitFor(() => anna.game.gameMode === 'survival', 2000));
  const landed = await waitFor(() => [anna, bert, carl, dora].every((b) => b.entity.onGround &&
    Math.abs(b.entity.position.y - (team(b.username).top + 1)) < 1.6), 12000, 200);
  check('Alle sind auf ihrer Insel gelandet', landed, [anna, bert, carl, dora].map((b) =>
    `${b.username}:${b.entity.position.y.toFixed(1)}/${team(b.username).top + 1}`).join(' '));
  check('Kein Fallschaden bei der Landung', anna.health === 20 && bert.health === 20, `${anna.health} ${bert.health}`);
  check('Start-Ausrüstung: Baumeister (Bretter + Spitzhacke), Jäger (Bogen + Pfeile)',
    invHas(anna, 'spruce_planks') && invHas(anna, 'stone_pickaxe') && invHas(bert, 'bow') && invHas(bert, 'arrow') &&
      carl.inventory.items().length === 0, `Anna ${anna.inventory.items().map((i) => `${i.name}×${i.count}`)} / Bert ${bert.inventory.items().map((i) => i.name)}`);
  check('Die Ballons steigen auf und verschwinden', await waitFor(async () => (await status()).wolkenalm.gondel >= 16, 12000, 500) &&
    await passed(`execute if block ${annaT.x} ${annaT.gy} ${annaT.z} air`));
  check('Netzwerk: Zustand „running“', calls().some((c) => c.t === 'state' && c.state === 'running'));

  // ------------------------------------------------------------------ Truhen
  const chests = anna.findBlocks({ matching: (b) => b.name === 'chest', maxDistance: 7, count: 4 })
    .filter((p) => Math.hypot(p.x + 0.5 - (annaT.x + 0.5), p.z + 0.5 - (annaT.z + 0.5)) < 6);
  check('Zwei Truhen auf der Startinsel', chests.length === 2, JSON.stringify(chests));
  const contents = [];
  await tp(anna, annaT.x + 0.5, annaT.top + 1, annaT.z + 0.5);
  for (const c of chests) {
    const box = await anna.openContainer(anna.blockAt(c));
    contents.push(...box.containerItems().map((i) => ({ name: i.name, count: i.count })));
    await sleep(300);
    box.close();
    await sleep(300);
  }
  const blocksIn = contents.filter((i) => /planks|cobblestone/.test(i.name)).reduce((a, i) => a + i.count, 0);
  check('Gerecht gefüllt: Waffe, Rüstung, Essen, mindestens 40 Blöcke',
    contents.some((i) => /sword|_axe/.test(i.name)) && contents.some((i) => /helmet|chestplate|leggings|boots/.test(i.name)) &&
      contents.some((i) => /bread|beef|potato|apple/.test(i.name)) && blocksIn >= 40,
    `${blocksIn} Blöcke: ${contents.map((i) => `${i.name}×${i.count}`).join(', ')}`);
  s = (await status()).wolkenalm;
  check('Statistik zählt geöffnete Truhen (2)', s.players.Anna.chests === 2, JSON.stringify(s.players.Anna));

  // ------------------------------------------------------------------ Brücke
  const dirX = Math.abs(annaT.x - 1000) >= Math.abs(annaT.z) ? -Math.sign(annaT.x - 1000) : 0;
  const dirZ = dirX === 0 ? -Math.sign(annaT.z) : 0;
  let edge = { x: annaT.x, y: annaT.top, z: annaT.z };
  for (let i = 1; i < 12; i++) {
    const b = anna.blockAt(new Vec3(annaT.x + dirX * i, annaT.top, annaT.z + dirZ * i));
    if (!b || b.name === 'air') break;
    edge = { x: annaT.x + dirX * i, y: annaT.top, z: annaT.z + dirZ * i };
  }
  const placedAt = [];
  for (let i = 0; i < 4; i++) {
    const ref = { x: edge.x + dirX * i, y: edge.y, z: edge.z + dirZ * i };
    await tp(anna, ref.x + 0.5, ref.y + 1, ref.z + 0.5, Math.atan2(-dirX, dirZ) * 180 / Math.PI, 70);
    const planks = anna.inventory.items().find((it) => it.name === 'spruce_planks');
    await anna.equip(planks, 'hand');
    try {
      await anna.placeBlock(anna.blockAt(new Vec3(ref.x, ref.y, ref.z)), new Vec3(dirX, 0, dirZ));
      placedAt.push({ x: ref.x + dirX, y: ref.y, z: ref.z + dirZ });
    } catch (e) {
      console.log(`  (Brücke: ${e.message})`);
    }
    await sleep(300);
  }
  let bridgeOk = placedAt.length === 4;
  for (const p of placedAt) bridgeOk = bridgeOk && await passed(`execute if block ${p.x} ${p.y} ${p.z} spruce_planks`);
  check('Brücke gebaut: 4 Bretter über den Inselrand hinaus', bridgeOk, JSON.stringify(placedAt));
  const breakTarget = await (async () => {
    const pick = anna.inventory.items().find((it) => it.name === 'stone_pickaxe');
    await anna.equip(pick, 'hand');
    const last = placedAt[placedAt.length - 1];
    await tp(anna, placedAt[1].x + 0.5, placedAt[1].y + 1, placedAt[1].z + 0.5);
    try {
      await anna.dig(anna.blockAt(new Vec3(last.x, last.y, last.z)), true);
    } catch (e) {
      console.log(`  (Abbauen: ${e.message})`);
    }
    return passed(`execute if block ${last.x} ${last.y} ${last.z} air`);
  })();
  check('Blöcke lassen sich in der Runde abbauen', breakTarget);
  await tp(anna, annaT.x + 0.5, annaT.top + 1, annaT.z + 0.5);

  // ------------------------------------------------------------------ Kampf
  clear(anna, bert, carl, dora);
  await tp(bert, annaT.x + 2.5, annaT.top + 1, annaT.z + 0.5, 90, 0);
  await cmd('damage Bert 17');
  await sleep(400);
  const pick = anna.inventory.items().find((it) => it.name === 'stone_pickaxe');
  await anna.equip(pick, 'hand');
  const killed = await attackUntil(anna, bert, async () => (await status()).wolkenalm.players.Bert.state === 'OUT');
  s = (await status()).wolkenalm;
  check('Anna erledigt Bert im Kampf (Kill für Anna)', killed && s.players.Anna.kills === 1, JSON.stringify(s.players));
  check('Meldung an alle (de/en)', await waitFor(() => sees(anna, /Bert wurde von Anna erledigt. Noch 3 dabei/) &&
    sees(bert, /Bert was defeated by Anna/), 2000), anna.chats.slice(-1)[0]);
  check('Bert scheidet aus und schaut zu (Platz 4)', await waitFor(() => bert.game.gameMode === 'spectator', 2000) &&
    sawTitle(bert, /You're out/) && s.players.Bert.place === 4, `${bert.game.gameMode} ${s.players.Bert.place}`);
  check('Berts Sachen bleiben liegen', Object.values(anna.entities).some((e) => e.name === 'item'),
    Object.values(anna.entities).map((e) => e.name).join(','));
  check('Aktionsleiste „Bert erledigt“ für Anna', await waitFor(() => sawBar(anna, /Bert erledigt/), 2000), anna.bars.slice(-1)[0]);

  // Hinunterstoßen: Carl trifft Dora, Dora stürzt in die Tiefe → Kill für Carl.
  const carlT = team('Carl');
  await tp(dora, carlT.x + 1.5, carlT.top + 1, carlT.z + 0.5, 90, 0);
  await tp(carl, carlT.x - 0.5, carlT.top + 1, carlT.z + 0.5, -90, 0);
  await attackUntil(carl, dora, async () => true, 1);
  await sleep(300);
  await cmd(inWorld(`tp Dora ${carlT.x + 0.5} 50 ${carlT.z + 0.5}`));
  check('Dora stürzt in die Tiefe → von Carl hinuntergestoßen', await waitFor(() => sees(anna, /Dora wurde von Carl in die Tiefe gestoßen/), 3000),
    anna.chats.slice(-1)[0]);
  s = (await status()).wolkenalm;
  check('Kill für Carl, Dora schaut zu (über der Mitte)', s.players.Carl.kills === 1 && s.players.Dora.state === 'OUT' &&
    await waitFor(() => dora.game.gameMode === 'spectator' && dora.entity.position.y > 110, 3000), JSON.stringify(dora.entity.position));
  check('Noch 2 dabei', (await status()).wolkenalm.lebend === 2);

  // ------------------------------------------------------------------ Ereignisse
  const someChest = chests[0];
  await cmd(inWorld(`item replace block ${someChest.x} ${someChest.y} ${someChest.z} container.0 with air`));
  await cmd(inWorld(`data merge block ${someChest.x} ${someChest.y} ${someChest.z} {Items:[]}`));
  clear(anna, carl);
  await cmd('wolkenkampf vorspulen nachschub wolkenalm');
  check('Nachschub: Titel und Meldung', await waitFor(() => sawTitle(anna, /Nachschub/) && sees(anna, /Alle Truhen haben sich neu gefüllt/), 4000),
    anna.titles.slice(-1)[0]);
  const refilled = (await cmd(inWorld(`data get block ${someChest.x} ${someChest.y} ${someChest.z} Items`)))[0];
  check('Die geleerte Truhe ist wieder voll', /id:/.test(refilled) && (await status()).wolkenalm.nachschub === 1, refilled.slice(0, 120));

  for (const n of ['Anna', 'Carl']) await cmd(`effect give ${n} minecraft:resistance 600 4 true`);
  await cmd('wolkenkampf vorspulen gewitter wolkenalm');
  check('Gewitter: Titel und Meldung (de/en)', await waitFor(() => sawTitle(anna, /Gewitter/) && sees(anna, /Blitze schlagen ein/), 4000));
  check('Es regnet über der Karte (eigenes Wetter der Runde)', await waitFor(() => anna.isRaining, 4000));
  check('Blitze schlagen ein (mit Vorwarnung)', await waitFor(async () => (await status()).wolkenalm.blitze >= 2, 25000, 1000),
    `blitze=${(await status()).wolkenalm.blitze}`);
  check('Anna und Carl stehen noch (Widerstand)', (await status()).wolkenalm.lebend === 2);

  // Nebel: Carl ohne Schutz, Anna mit – der Nebel entscheidet.
  await cmd('effect clear Carl');
  await cmd('wolkenkampf vorspulen nebel wolkenalm');
  check('Nebel: Titel und Meldung', await waitFor(() => sawTitle(anna, /Der Nebel steigt/) && sees(anna, /Der Nebel steigt/), 4000));
  const fog1 = (await status()).wolkenalm.nebel;
  await sleep(4000);
  const fog2 = (await status()).wolkenalm.nebel;
  check('Der Nebel steigt von unten', fog2 > fog1, `${fog1} → ${fog2}`);
  await cmd(inWorld(`setblock ${carlT.x} ${Math.floor(fog2) - 4} ${carlT.z} spruce_planks`));
  await tp(carl, carlT.x + 0.5, Math.floor(fog2) - 3, carlT.z + 0.5);
  check('Wer im Nebel steht, nimmt Schaden (Hinweis)', await waitFor(() => sawBar(carl, /Du stehst im Nebel/), 4000) &&
    await waitFor(() => carl.health < 20, 3000), `Leben ${carl.health}`);

  // ------------------------------------------------------------------ Sieg
  clear(anna, bert, carl, dora);
  await cmd('wolkenkampf vorspulen ende wolkenalm');
  check('Der Nebel erwischt Carl', await waitFor(() => sees(anna, /Carl hat sich im Nebel verirrt|Carl ist im Nebel untergegangen/), 30000),
    anna.chats.slice(-2).join(' | '));
  check('Anna gewinnt: „Gewonnen!“', await waitFor(() => sawTitle(anna, /Gewonnen/), 4000), anna.titles.join(' | '));
  check('Alle anderen sehen den Sieger (englisch: „Anna wins“)', sawTitle(bert, /Anna wins/) && sawTitle(carl, /Anna gewinnt/));
  check('Sieg im Chat und eigene Runde', sees(anna, /Sieg für Anna/) && sees(anna, /Platz 1 · 1 Kills · 2 Truhen/) &&
    sees(bert, /Victory for Anna/), anna.chats.slice(-3).join(' | '));
  const stats = calls().filter((c) => c.t === 'stats');
  const st = (n) => stats.filter((c) => c.name === n).slice(-1)[0];
  check('Netzwerk-Statistik „wolkenkampf“: Anna games 1, wins 1, kills 1, deaths 0, chests 2, best_kills 1',
    st('Anna')?.mode === 'wolkenkampf' && st('Anna').add.games === 1 && st('Anna').add.wins === 1 && st('Anna').add.kills === 1 &&
      st('Anna').add.deaths === 0 && st('Anna').add.chests === 2 && st('Anna').best.best_kills === 1 && st('Anna').lower === false,
    JSON.stringify(st('Anna')));
  check('Statistik für alle vier (Bert: deaths 1, wins 0)', ['Anna', 'Bert', 'Carl', 'Dora'].every((n) => st(n)) &&
    st('Bert').add.deaths === 1 && st('Bert').add.wins === 0 && st('Carl').add.kills === 1, stats.map((c) => c.name).join(','));
  check('Zurück in den Wolkenhafen (alle, auch die Zuschauer)', await waitFor(() => [anna, bert, carl, dora].every(inHall), 15000),
    [anna, bert, carl, dora].map((b) => `${b.username}:${b.game.gameMode}`).join(' '));
  check('Wolkenhafen-Knöpfe wieder da, kein Regen', await waitFor(() => slot(anna, 0)?.name === 'compass' && !anna.isRaining, 9000),
    `${slot(anna, 0)?.name} Regen: ${anna.isRaining} (${anna.rainState})`);
  check('Karte wird wiederhergestellt', await waitFor(async () => (await status()).info.frei?.includes('wolkenalm'), 30000, 1000),
    (await status()).info.frei);
  const restored = Number((await status()).info.zurueckgesetzt);
  check('Karte frisch: Brücke weg, Gondel wieder da, Truhe wieder da',
    await passed(`execute if block ${placedAt[0].x} ${placedAt[0].y} ${placedAt[0].z} air`) &&
      await passed(`execute if block ${annaT.x} ${annaT.gy} ${annaT.z} spruce_trapdoor[open=false]`) &&
      await passed(`execute if block ${someChest.x} ${someChest.y} ${someChest.z} chest`) && restored > 100,
    `zurückgesetzt: ${restored}`);
  check('Keine liegengebliebenen Sachen auf der Karte',
    !(await passed('execute positioned 1000 100 0 if entity @e[type=item,distance=..120]')));
  const board = readFile(path.join(SERVER, 'plugins/MorniWolkenkampf/bestenliste.yml'));
  check('Bestenliste: Anna mit 1 Sieg', /name: Anna[\s\S]*?siege: 1/.test(board), board.trim().slice(0, 160));
  check('Spielerdatei: 1 Runde, 1 Sieg, 2 Truhen', /runden: 1/.test(readFile(playerFile('Anna'))) &&
    /siege: 1/.test(readFile(playerFile('Anna'))) && /truhen: 2/.test(readFile(playerFile('Anna'))),
  readFile(playerFile('Anna')).trim().replace(/\n/g, ' '));
  const top = (await cmd('top'))[0].replace(/§./g, '');
  check('/top zeigt Anna vorn', /1\. Anna – 1 Siege/.test(top), top.replace(/\n/g, ' | ').slice(0, 160));
  clear(anna);
  anna.chat('/stats');
  check('/stats zeigt die eigenen Werte', await waitFor(() => sees(anna, /Siege: 1/), 3000), anna.chats.slice(-3).join(' | '));
  check('Netzwerk: Zustand wieder „waiting“', await waitFor(() => calls().filter((c) => c.t === 'state').slice(-1)[0]?.state === 'waiting', 4000));

  // ------------------------------------------------------------------ Runde 2: zu zweit, Party zusammen
  clear(anna, bert, carl, dora);
  for (const b of [anna, bert, carl, dora]) {
    b.chat('/karte kristallgipfel');
    b.chat('/spielart zweier');
  }
  await sleep(800);
  for (const b of [anna, bert, carl, dora]) b.chat('/bereit');
  check('Zweite Runde: Kristallgipfel zu zweit', await waitFor(() => sees(anna, /Auf geht's! Karte: Kristallgipfel · Zu zweit · 4 Leute/), 15000),
    anna.chats.slice(-2).join(' | '));
  s = (await status()).kristallgipfel;
  check('Zwei Zweier-Teams', s?.teams.length === 2, JSON.stringify(s?.teams));
  check('Party Anna + Carl im selben Team, Bert + Dora im anderen', s.players.Anna.team === s.players.Carl.team &&
    s.players.Bert.team === s.players.Dora.team && s.players.Anna.team !== s.players.Bert.team, JSON.stringify(s.players));
  await sleep(800);
  const at2 = s.teams[s.players.Anna.team];
  check('Anna und Carl stehen zusammen in einer Gondel', [anna, carl].every((b) =>
    Math.hypot(b.entity.position.x - (at2.x + 0.5), b.entity.position.z - (at2.z + 0.5)) < 1.8 &&
      Math.abs(b.entity.position.y - (at2.gy + 1)) < 1.2), `${JSON.stringify(anna.entity.position)} ${JSON.stringify(carl.entity.position)}`);
  check('„Du kämpfst zusammen mit …“ (de/en)', sees(anna, /Du kämpfst zusammen mit Carl/) && sees(bert, /You're fighting together with Dora/));
  check('Gondel öffnet sich', await waitFor(async () => (await status()).kristallgipfel.phase === 'FIGHT', 10000));
  await waitFor(() => anna.entity.onGround && carl.entity.onGround, 10000);
  await sleep(500);
  await tp(carl, at2.x + 1.5, at2.top + 1, at2.z + 0.5, 90, 0);
  await tp(anna, at2.x - 0.5, at2.top + 1, at2.z + 0.5, -90, 0);
  const carlBefore = carl.health;
  await attackUntil(anna, carl, async () => false, 3);
  check('Kein Schaden unter Teamkameraden', carl.health === carlBefore && (await status()).kristallgipfel.players.Carl.state === 'ALIVE',
    `${carlBefore} → ${carl.health}`);

  // Zuschauen: Emil kommt dazu und schaut der Runde zu.
  const emil = await join('Emil');
  check('Emil landet im Wolkenhafen', inHall(emil));
  emil.chat('/zuschauen Anna');
  check('Emil schaut zu (Zuschauermodus über der Karte)', await waitFor(() => emil.game.gameMode === 'spectator' &&
    Math.abs(emil.entity.position.x - 2000) < 100, 5000), `${emil.game.gameMode} ${JSON.stringify(emil.entity.position)}`);
  check('Status kennt Emil als Zuschauer', (await status()).kristallgipfel.players.Emil?.state === 'SPECTATOR');

  // Bert und Dora scheiden aus → Team Anna + Carl gewinnt.
  clear(anna, bert, carl, dora, emil);
  const bt2 = s.teams[s.players.Bert.team];
  await tp(bert, at2.x + 0.5, at2.top + 1, at2.z + 2.5, 180, 0);
  await cmd('damage Bert 19');
  await sleep(300);
  const killed2 = await attackUntil(anna, bert, async () => (await status()).kristallgipfel.players.Bert.state === 'OUT');
  check('Anna erledigt Bert', killed2);
  check('Dora allein kämpft weiter („Dein Team kämpft weiter“ für Bert)', sawTitle(bert, /Your team fights on/) || sees(bert, /You're spectating/),
    bert.titles.join(' | '));
  await cmd(inWorld(`tp Dora ${bt2.x + 0.5} 40 ${bt2.z + 0.5}`));
  check('Team Anna + Carl gewinnt (beide „Gewonnen!“)', await waitFor(() => sawTitle(anna, /Gewonnen/) && sawTitle(carl, /Gewonnen/), 6000),
    anna.titles.slice(-2).join(' | '));
  check('Englisch: „Anna & Carl wins“', sawTitle(bert, /Anna & Carl wins/) || sawTitle(dora, /Anna & Carl gewinnt/),
    bert.titles.slice(-2).join(' | '));
  const stats2 = calls().filter((c) => c.t === 'stats');
  const carlWins = stats2.filter((c) => c.name === 'Carl' && c.add.wins === 1).length;
  check('Statistik: Carl hat jetzt einen Sieg (zu zweit)', carlWins === 1 && stats2.length === 8, `${stats2.length} Meldungen`);
  check('Alle zurück im Wolkenhafen (auch Emil)', await waitFor(() => [anna, bert, carl, dora, emil].every(inHall), 15000),
    [anna, bert, carl, dora, emil].map((b) => `${b.username}:${b.game.gameMode}`).join(' '));
  check('Kristallgipfel wird wiederhergestellt', await waitFor(async () => (await status()).info.frei?.includes('kristallgipfel'), 30000, 1000));
  const board2 = readFile(path.join(SERVER, 'plugins/MorniWolkenkampf/bestenliste.yml'));
  check('Bestenliste: Anna 2 Siege', /name: Anna[\s\S]*?siege: 2/.test(board2), board2.trim().slice(0, 200));

  // ------------------------------------------------------------------ Verlassen + Lobby
  clear(anna);
  anna.setQuickBarSlot(8);
  await sleep(200);
  anna.activateItem();
  check('Bett: zur Lobby (Netzwerk)', await waitFor(() => calls().some((c) => c.t === 'to-lobby' && c.name === 'Anna'), 4000));

  for (const b of [bert, carl, dora, emil]) b.quit();
  await sleep(800);
  const ok = results.filter((r) => r.ok).length;
  console.log(`\n${ok}/${results.length} Prüfungen bestanden`);
  process.exit(ok === results.length ? 0 : 1);
}

main().catch((e) => {
  console.error(e);
  const ok = results.filter((r) => r.ok).length;
  console.log(`\nAbgebrochen – ${ok}/${results.length} Prüfungen bestanden`);
  process.exit(1);
});
