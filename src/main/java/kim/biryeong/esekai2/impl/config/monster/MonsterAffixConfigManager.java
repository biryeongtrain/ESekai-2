package kim.biryeong.esekai2.impl.config.monster;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.Esekai2;
import kim.biryeong.esekai2.api.config.monster.MonsterAffixConfig;
import kim.biryeong.esekai2.api.config.monster.MonsterAffixCountConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads and exposes the server-side monster affix config.
 */
public final class MonsterAffixConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("esekai2-server.json");
    private static volatile MonsterAffixConfig current = MonsterAffixConfig.DEFAULT;

    private MonsterAffixConfigManager() {
    }

    public static void bootstrap() {
        current = load(CONFIG_PATH);
    }

    public static MonsterAffixConfig current() {
        return current;
    }

    public static MonsterAffixCountConfig currentCounts() {
        return current.monsterAffixCounts();
    }

    public static MonsterAffixConfig reload(Path path) {
        current = load(path);
        return current;
    }

    /**
     * Testing hook that replaces the currently active server config in memory.
     *
     * @param config config to expose until the next reload
     */
    public static void setCurrentForTesting(MonsterAffixConfig config) {
        current = config;
    }

    public static MonsterAffixConfig load(Path path) {
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                writeDefault(path);
                return MonsterAffixConfig.DEFAULT;
            }

            try (Reader reader = Files.newBufferedReader(path)) {
                JsonElement json = GSON.fromJson(reader, JsonElement.class);
                if (json == null) {
                    Esekai2.LOGGER.warn("Monster affix config was empty, using defaults");
                    writeDefault(path);
                    return MonsterAffixConfig.DEFAULT;
                }

                try {
                    Optional<MonsterAffixConfig> parsed = MonsterAffixConfig.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(message ->
                            Esekai2.LOGGER.warn("Invalid monster affix config at {}: {}; using defaults", path, message)
                    );
                    return parsed.orElseGet(() -> {
                        writeDefault(path);
                        return MonsterAffixConfig.DEFAULT;
                    });
                } catch (RuntimeException exception) {
                    Esekai2.LOGGER.warn("Monster affix config parse failed at {}; using defaults", path, exception);
                    writeDefault(path);
                    return MonsterAffixConfig.DEFAULT;
                }
            }
        } catch (IOException exception) {
            Esekai2.LOGGER.warn("Failed to load monster affix config, using defaults", exception);
            return MonsterAffixConfig.DEFAULT;
        }
    }

    private static void writeDefault(Path path) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            JsonElement encoded = MonsterAffixConfig.CODEC.encodeStart(JsonOps.INSTANCE, MonsterAffixConfig.DEFAULT)
                    .getOrThrow(message -> new IllegalStateException("Failed to encode default monster affix config: " + message));
            GSON.toJson(encoded, writer);
        } catch (IOException exception) {
            Esekai2.LOGGER.warn("Failed to write default monster affix config", exception);
        }
    }
}
