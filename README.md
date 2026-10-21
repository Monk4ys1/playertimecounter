# PlayerTimeCounter

Paper/Spigot-Plugin (1.21+), das **pro Spieler und Tag** ein Spielzeit-Limit setzt.  
Ist das Limit aufgebraucht, wird der Spieler gekickt und bis zum nächsten Tageswechsel am Login gehindert.

## Features

- Konfigurierbares Tageslimit (`config.yml`)
- Kick + Login-Sperre bei Limitüberschreitung
- **TAB-Liste:** Restzeit rechts (Minuten-Score) + kompakter Suffix hinter jedem Namen
- TAB Header/Footer mit Tages-Restzeit
- Sporadische Erinnerungen in **Chat** und **Actionbar** (kein Dauer-Spam)
- Zusätzliche Warnungen bei konfigurierbaren Restzeit-Schwellen (z. B. 30 / 10 / 5 / 1 Min.)
- `/playtime` zum manuellen Abfragen der Restzeit
- `/playtimetest` zum Testen (setzen, addieren, resetten, Status)
- Bypass-Permission für Admins
- Persistenz in `playtimes.yml` (überlebt Restarts)

## Installation

1. Mit Maven bauen: `mvn clean package`
2. JAR aus `target/` in den `plugins/`-Ordner des Servers legen
3. Server starten, `plugins/PlayerTimeCounter/config.yml` anpassen
4. Optional: `/playtimereload`

## Befehle

| Befehl | Beschreibung | Permission |
|--------|--------------|------------|
| `/playtime` | Eigene Restzeit anzeigen (Chat + Actionbar) | `playertimecounter.playtime` |
| `/playtimetest set <Min> [Spieler]` | Verbrauchte Minuten setzen | `playertimecounter.test` |
| `/playtimetest add <Min> [Spieler]` | Minuten hinzufügen | `playertimecounter.test` |
| `/playtimetest reset [Spieler]` | Tageszeit zurücksetzen | `playertimecounter.test` |
| `/playtimetest status [Spieler]` | Status anzeigen | `playertimecounter.test` |
| `/playtimereload` | Config neu laden | `playertimecounter.reload` |

Aliases: `/zeit`, `/spielzeit`, `/pttest`, `/ptreload`

## Wichtige Config-Werte

```yaml
daily-limit-minutes: 120          # Tageslimit
reminder-interval-seconds: 300    # Erinnerung alle 5 Minuten (0 = aus)
warning-thresholds-minutes: [30, 10, 5, 1]
reset-hour: 0                     # Tageswechsel um 00:00 Serverzeit

tab:
  enabled: true
  score-enabled: true             # gelbe Minuten-Zahl rechts
  suffix-enabled: true            # Text hinter dem Namen (z. B. • 1h 20m)
  header-footer-enabled: true
```

Alle Chat-/Kick-/TAB-Texte sind unter `messages:` bzw. `tab:` anpassbar (`&`-Farbcodes).

## Schnelltest

1. Limit in der Config kurz setzen (z. B. `5` Minuten) und `/playtimereload`
2. `/playtimetest set 4` → fast am Limit
3. `/playtime` → Restzeit prüfen
4. `/playtimetest add 2` → Kick wegen Limit
5. Erneut joinen → Login wird verweigert
6. `/playtimetest reset <Spieler>` (als anderer OP / Konsole nach erneutem Join mit Bypass) bzw. Limit/Reset-Stunde anpassen

Zum Testen ohne Kick: Permission `playertimecounter.bypass` (Standard: OP).

## Technik

- Java 21, Paper API 1.21.4
- Adventure-Komponenten für Kick/Actionbar
- Periodischer Tracker-Task schreibt Online-Zeit fort
- Tages-Schlüssel berücksichtigt `reset-hour`
