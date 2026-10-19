package pregrafer.rir.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/** Strict checks for optional YAML values; absent keys still use the documented defaults. */
final class ConfigValidation {
    private ConfigValidation() {}

    static void section(ConfigurationSection config, String path) {
        if (config.contains(path) && !config.isConfigurationSection(path))
            throw new IllegalArgumentException(path + " must be a mapping");
    }

    static void string(ConfigurationSection config, String path) {
        if (config.contains(path) && !(config.get(path) instanceof String))
            throw new IllegalArgumentException(path + " must be a string");
    }

    static void bool(ConfigurationSection config, String path) {
        if (config.contains(path) && !(config.get(path) instanceof Boolean))
            throw new IllegalArgumentException(path + " must be true/false, without quotes");
    }

    static void integer(ConfigurationSection config, String path, int min, int max) {
        if (!config.contains(path)) return;
        Object raw = config.get(path);
        if (!(raw instanceof Number))
            throw new IllegalArgumentException(path + " must be an integer in " + min + ".." + max);
        double value = ((Number) raw).doubleValue();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < min || value > max)
            throw new IllegalArgumentException(path + " must be an integer in " + min + ".." + max);
    }

    static List<String> strings(ConfigurationSection config, String path) {
        if (config.contains(path) && !config.isList(path))
            throw new IllegalArgumentException(path + " must be a list");
        if (config.isList(path))
            for (Object item : config.getList(path))
                if (!(item instanceof String))
                    throw new IllegalArgumentException(path + " must contain only strings");
        return config.getStringList(path);
    }
}
