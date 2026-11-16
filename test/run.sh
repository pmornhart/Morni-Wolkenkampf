#!/usr/bin/env bash
# Echter Test von Morni Wolkenkampf: baut das Plugin und das Test-Netzwerk, richtet einen Paper-Server ein (offline,
# eigener Port, RCON an), startet ihn, schickt Test-Spieler (mineflayer) durch alle Abläufe und hält ihn wieder an.
#
#   test/run.sh                 # alles
#   KEEP=1 test/run.sh          # Server danach laufen lassen (zum Nachschauen, z. B. für Bilder)
#   KEEP=1 SKIP_TEST=1 test/run.sh   # nur Server starten
#   FRESH=1 test/run.sh         # Welt vorher löschen (alles wird neu gebaut)
#
# Erwartet (Vorgaben für die MoinMorni-Sandbox, alles per Umgebungsvariable änderbar):
#   WK_JAVA       Java 25 (Ordner; sonst die JDK 25 aus dem Cache, sonst JAVA_HOME)
#   PAPER_JAR     Paper 26.1.2 (mineflayer kann bis 26.1)
#   TEMPLATE      ein schon einmal gestarteter Paper-26.1.2-Ordner (libraries/, cache/, versions/ werden kopiert,
#                 damit nichts heruntergeladen werden muss) – optional
#   WK_NODE_MODULES  node_modules mit mineflayer
#   EXTRA_PLUGINS weitere Jars für den Server (z. B. ViaVersion + ViaBackwards, damit ein Client 26.3 hineinkommt)
#   PORT / RCON_PORT  Vorgabe 25810 / 25910
#
# Verfasst mit Claude 🤖
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(dirname "$HERE")"
CACHE="${MM_CACHE:-/home/user/mm-cache}"
CACHED_JDK="$(ls -d "$CACHE"/jdk/jdk-25* 2>/dev/null | head -1 || true)"
JAVA_HOME="${WK_JAVA:-${CACHED_JDK:-${JAVA_HOME:-}}}"
export JAVA_HOME
JAVA="$JAVA_HOME/bin/java"
PAPER_JAR="${PAPER_JAR:-$CACHE/jars/paper-26.1.2.jar}"
TEMPLATE="${TEMPLATE:-$CACHE/servers/t1}"
SERVER="${SERVER_DIR:-$CACHE/servers/wolkenkampf-test}"
PORT="${PORT:-25810}"
RCON_PORT="${RCON_PORT:-25910}"
export NODE_PATH="${WK_NODE_MODULES:-$CACHE/node/node_modules}${NODE_PATH:+:$NODE_PATH}"
NICE="nice -n ${NICE_LEVEL:-15}"

echo "== Bauen"
(cd "$REPO" && $NICE mvn -q -B -o -DskipTests package)
(cd "$REPO" && $NICE mvn -q -B -o -f test/fake-network/pom.xml package)

echo "== Server einrichten: $SERVER"
if [ "${FRESH:-0}" = "1" ]; then
  # Welt und Bau-Stand vergessen: alles wird neu gebaut.
  rm -rf "$SERVER/wolkenkampf" "$SERVER/plugins/MorniWolkenkampf/bau.yml"
fi
mkdir -p "$SERVER/plugins/MorniWolkenkampf" "$SERVER/plugins/MoinMorni"
if [ -d "$TEMPLATE" ] && [ ! -d "$SERVER/libraries" ]; then
  cp -r "$TEMPLATE/libraries" "$TEMPLATE/cache" "$TEMPLATE/versions" "$SERVER/" 2>/dev/null || true
fi
cp "$PAPER_JAR" "$SERVER/paper.jar"
echo "eula=true" > "$SERVER/eula.txt"
RCON_PASS="$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)"
printf '%s' "$RCON_PASS" > "$SERVER/.rcon-pass"
chmod 600 "$SERVER/.rcon-pass"
cat > "$SERVER/server.properties" <<EOF
online-mode=false
enforce-secure-profile=false
server-port=$PORT
enable-rcon=true
rcon.port=$RCON_PORT
rcon.password=$RCON_PASS
level-name=world
level-type=minecraft\:flat
generator-settings={"layers"\:[{"block"\:"minecraft\:bedrock","height"\:1}],"biome"\:"minecraft\:plains"}
generate-structures=false
spawn-protection=0
view-distance=8
simulation-distance=6
max-players=20
white-list=false
enforce-whitelist=false
motd=Morni Wolkenkampf Test
allow-flight=true
EOF
# Testwerte: kurzer Countdown, kurze Gondelzeit, Ereignisse weit weg (der Test spult vor).
cat > "$SERVER/plugins/MorniWolkenkampf/config.yml" <<'EOF'
welt: wolkenkampf
wolkenhafen:
  countdown-sekunden: 8
  mindestens-spieler: 3
runde:
  hoechstens-spieler: 12
  gondel-sekunden: 6
  nachspiel-sekunden: 4
ereignisse:
  nachschub: 600
  gewitter: 700
  nebel: 800
  nebel-dauer: 30
EOF
# Test-Netzwerk: Bert spricht Englisch, Anna und Carl sind eine Party (Anna führt).
cat > "$SERVER/plugins/MoinMorni/fake.properties" <<'EOF'
lang.Bert=en
party.Anna=Anna,Carl
EOF
rm -f "$SERVER/plugins/MoinMorni/calls.log"
rm -rf "$SERVER/plugins/MorniWolkenkampf/spieler" "$SERVER/plugins/MorniWolkenkampf/bestenliste.yml"
rm -f "$SERVER/plugins/"Morni-Wolkenkampf*.jar
cp "$REPO/target/Morni-Wolkenkampf.jar" "$SERVER/plugins/"
cp "$REPO/test/fake-network/target/MoinMorni-FakeNetwork.jar" "$SERVER/plugins/"
for jar in ${EXTRA_PLUGINS:-}; do
  cp "$jar" "$SERVER/plugins/"
done

echo "== Server starten (Port $PORT, RCON $RCON_PORT)"
cd "$SERVER"
: > console.log
$NICE "$JAVA" -Xms1G -Xmx2G ${JAVA_OPTS:-} -jar paper.jar --nogui < /dev/null > console.log 2>&1 &
PID=$!
echo "$PID" > "$SERVER/.pid"
stop_server() {
  if kill -0 "$PID" 2>/dev/null; then
    node "$HERE/rcon.cjs" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" stop >/dev/null 2>&1 || kill "$PID" || true
    for _ in $(seq 1 60); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$PID" 2>/dev/null || true
  fi
}
if [ "${KEEP:-0}" != "1" ]; then
  trap stop_server EXIT
fi

for _ in $(seq 1 400); do
  if grep -q "Wolkenkampf ist bereit" console.log; then break; fi
  if ! kill -0 "$PID" 2>/dev/null; then echo "Server ist abgestürzt:"; tail -40 console.log; exit 1; fi
  sleep 1
done
grep -q "Wolkenkampf ist bereit" console.log || { echo "Wolkenkampf wurde nicht bereit:"; tail -40 console.log; exit 1; }
grep -E "Fertig gebaut|wiederhergestellt|Wolkenkampf ist bereit|Netzwerk" console.log || true

if [ "${SKIP_TEST:-0}" = "1" ]; then
  echo "Server läuft (PID $PID) – anhalten mit: node $HERE/rcon.cjs 127.0.0.1:$RCON_PORT $SERVER/.rcon-pass stop"
  exit 0
fi

echo "== Test-Spieler"
STATUS=0
node "$HERE/wolkenkampf-test.cjs" 127.0.0.1 "$PORT" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" "$SERVER" || STATUS=$?

echo "== Fehler im Server-Log:"
if grep -qE "ERROR|Exception" console.log; then
  grep -E "ERROR|Exception" console.log | head -20
else
  echo "keine"
fi
exit $STATUS
