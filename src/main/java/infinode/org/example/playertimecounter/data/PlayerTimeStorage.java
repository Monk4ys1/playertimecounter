package infinode.org.example.playertimecounter.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Persistiert Tages-Spielzeiten in {@code playtimes.yml}.
 * <p>
 * Daten liegen im Speicher ({@link ConcurrentHashMap}) und werden
 * bei Quit, Disable und periodisch geschrieben.
 */
public final class PlayerTimeStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerDayRecord> records = new ConcurrentHashMap<>();

    public PlayerTimeStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playtimes.yml");
        load();
    }

    /** Lädt alle Einträge von Disk in den Speicher. */
    public void load() {
        records.clear();
        if (!file.exists()) {
            return;
        }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String dayKey = players.getString(key + ".day", "");
                long usedMs = players.getLong(key + ".used-ms", 0L);
                PlayerDayRecord record = new PlayerDayRecord(uuid, dayKey, usedMs);

                Set<Long> warnings = new HashSet<>();
                for (long threshold : players.getLongList(key + ".fired-warnings")) {
                    warnings.add(threshold);
                }
                record.setFiredWarningsMs(warnings);
                records.put(uuid, record);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Ungültige UUID in playtimes.yml: " + key);
            }
        }
    }

    /** Schreibt alle Records atomar auf Disk. */
    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerDayRecord> entry : records.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerDayRecord record = entry.getValue();
            yaml.set(path + ".day", record.getDayKey());
            yaml.set(path + ".used-ms", record.getUsedMs());
            yaml.set(path + ".fired-warnings", record.getFiredWarningsMs().stream().sorted().toList());
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Konnte Plugin-Ordner nicht anlegen.");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Speichern von playtimes.yml", ex);
        }
    }

    /**
     * Liefert den Record eines Spielers oder legt einen neuen an.
     */
    public PlayerDayRecord getOrCreate(UUID uuid, String dayKey) {
        return records.computeIfAbsent(uuid, id -> new PlayerDayRecord(id, dayKey, 0L));
    }

    public PlayerDayRecord get(UUID uuid) {
        return records.get(uuid);
    }

    public Map<UUID, PlayerDayRecord> getAll() {
        return records;
    }
}
