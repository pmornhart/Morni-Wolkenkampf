# Morni Wolkenkampf

@STAND.md

## Regeln für dieses Projekt

- Repo ist **öffentlich**: keine Geheimnisse, Tokens oder privaten Daten committen (Geheimes nur in `.env`, nie im Repo).
  Auch keine Test-Server-Ordner, Welten, Spielerdaten oder RCON-Passwörter.
- Der Modus heißt **Wolkenkampf** (englisch „Cloud Clash“), das Repo `pmornhart/Morni-Wolkenkampf`, die Jar
  `Morni-Wolkenkampf.jar`, das Plugin `MorniWolkenkampf`, Kennung im Netzwerk `wolkenkampf`.
- Texte im Spiel auf Deutsch (Standard) und Englisch, freundlich, keine Fachbegriffe ohne Erklärung. Alle Texte stehen
  in `Texts.java` (MiniMessage, beide Sprachen; `TextsTest` prüft Platzhalter, Zeichen, dass jeder im Code benutzte
  Schlüssel existiert und dass jede Karte, Spielart, Ausrüstung und Inselfarbe einen Namen hat).
- **„Verfasst mit Claude 🤖“** als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commits mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Keine fremden Marken, Figuren, Namen, Texte oder Grafiken (nichts von Mojang, Nintendo, Hypixel, Gomme, Cytooxien
  oder anderen Servern und Spielen): nur die Spielidee „Kampf auf schwebenden Inseln“, eigene Namen (Ballon-Gondeln,
  Nachschub, Gewitter, Nebel, Inselfarben nach Alpenblumen), eigene Karten, eigene Bauten.
- Die Netzwerk-Schnittstelle `src/api/java/de/moinmorni/api/MoinMorni.java` ist eine Kopie aus `pmornhart/moinmorni.net`
  (`plugins/moinmorni-network/src/main/java/de/moinmorni/api/MoinMorni.java`) und muss **genau gleich** bleiben. Sie wird
  nur mitübersetzt und kommt **nicht** in die Jar. Nur `net/Connected` darf sie benutzen.
- Wolkenkampf muss auch ohne Netzwerk laufen (`softdepend: [MoinMorni]`; Sprache dann aus dem Client).
- Paper 26.x, Java 25. Der Code muss auch gegen Paper 26.1 übersetzen (`mvn -Ppaper-26.1 compile`, läuft in der CI).
  Blocknamen von 26.x (z. B. `iron_chain`, nicht `chain`) – beim Start werden alle Pläne gebaut, ein falscher Name fällt
  sofort auf (und `PlanTest` prüft die Blocknamen ohne Server).
- Regeln ohne Server (Truhen-Inhalt, Teams, Abstimmung, Ablauf der Ereignisse, Nebel, Startplätze, Ausrüstungen,
  Bestenliste, Karten-Aufbau) gehören in `logic/` bzw. `data/` – ohne Bukkit, mit Tests. Werte dort nur zusammen mit den
  Tests ändern.
- Ändern sich Wolkenhafen oder Karten: `Construction.VERSION` erhöhen – dann baut der Server beim nächsten Start neu.
  Nach jeder Runde stellt `Restorer` die Karte Block für Block nach dem Bauplan wieder her.
- Neue Version: `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; ein Push auf `main` baut das
  Release mit `Morni-Wolkenkampf.jar` – das Netzwerk holt es sich von dort.
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Nach jeder sichtbaren Änderung Bilder wie im echten Minecraft (echter Client aus `moinmorni.net/tools/mc-client`,
  Texturen und Bilder nur lokal, nie ins Repo).
- „Alles im Panel“: jede Einstellung, die per Befehl geht, muss auch im Panel gehen. Darum hat Wolkenkampf nur die
  `config.yml` und Verwaltungs-Befehle zum Testen (`/wolkenkampf …`); die Anbindung an den Einstellungs-Katalog gehört
  ins Netzwerk-Repo.
- Bedrock-Spieler (Geyser) spielen mit: alle Menüs sind Truhen-Fenster, nichts braucht eine Steuerung, die nur Java hat.
- Echter Test nach jeder Änderung am Spielablauf: `test/run.sh` (siehe README).
- `STAND.md` nach jedem größeren Schritt aktualisieren.
