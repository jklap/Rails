package net.sf.rails.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.util.Util;

/**
 * currently a proxy of ConfigManager similar to Config but destined as a replacement
 * for the singleton/static usage of Config & ConfigManager to support game specific configurations
 * (such as Discord)
 *
 */
public class GameConfig {
    private static final Logger log = LoggerFactory.getLogger(GameConfig.class);

    private final Map<String, String> transientConfig = new HashMap<>();

    /**
     * Configuration option (default value is empty string)
     */
    public String get(String key) {
        return get(key, "");
    }

    /**
     * Configuration option with default value
     */
    public String get(String key, String defaultValue) {
        if (transientConfig.containsKey(key)) {
            return transientConfig.get(key);
        }

        return ConfigManager.getInstance().getValue(key, defaultValue);
    }

    /**
     * Returns a boolean based on the config value (ie "yes", "no"). If the config value doesn't exist or is empty null is returned
     * @param key
     * @return
     */
    public Boolean getBoolean(String key) {
        String boolStr = get(key);
        if ( StringUtils.isBlank(boolStr) ) {
            return null;
        }
        return Util.parseBoolean(boolStr);
    }

    /**
     * Returns a boolean based on the config value (ie "yes", "no"). If the config value doesn't exist or is empty then the default value is returned
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean bool = getBoolean(key);
        return bool != null ? bool : defaultValue;
    }

    public Integer getInt(String key) {
        String intStr = get(key);
        if ( StringUtils.isBlank(intStr) ) {
            return null;
        }
        try {
            return Integer.valueOf(intStr);
        }
        catch (NumberFormatException e) {
            log.warn("Invalid value found in integer config {}: {}", key, intStr);
        }
        return null;
    }

    public Integer getInt(String key, int defaultValue) {
        Integer intValue = getInt(key);
        return intValue != null ? intValue : defaultValue;
    }

    public void set(String key, String value) {
        transientConfig.put(key, value);
    }

    public void setBoolean(String key, boolean value) {
        set(key, value ? "yes" : "no");
    }

    public void remove(String name) {
        transientConfig.remove(name);
    }

    public Map<String, String> getConfigMap() {
        return Collections.unmodifiableMap(transientConfig);
    }

    public void setConfig(Map<String, String> configItems) {
        clearTransientConfig();
        transientConfig.putAll(configItems);
    }

    public void clearTransientConfig() {
        transientConfig.clear();
    }

}
