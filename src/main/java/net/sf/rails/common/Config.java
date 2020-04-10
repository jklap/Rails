package net.sf.rails.common;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.Util;

/**
 * Proxy class to the ConfigManager
 */

public final class Config {

    /**
    * @return version id (including a "+" attached if development)
    */
    public static String getVersion() {
        return ConfigManager.getInstance().getVersion();
    }

    /**
     * @return true if development version
     */
    public static boolean isDevelop() {
        return ConfigManager.getInstance().isDevelop();
    }

    public static String getBuildDate() {
        return ConfigManager.getInstance().getBuildDate();
    }

    /**
     * Configuration option (default value is empty string)
     */
    public static String get(String key) {
        return ConfigManager.getInstance().getValue(key, "");
    }

    /**
     * Configuration option with default value
     */
    public static String get(String key, String defaultValue) {
        return ConfigManager.getInstance().getValue(key, defaultValue);
    }

    public static void set(String key, String value) {
        ConfigManager.getInstance().setValue(key, value);
    }

    /**
     * Configuration option: First tries to return {key}.{appendix}, if undefined returns {key}
     */
    public static String getSpecific(String key, String appendix) {
        String value = get(key + "." + appendix);
        if (Util.hasValue(value)) {
            return value;
        } else {
            return get(key);
        }
    }

    /**
     * Configuration option: First tries to return {key}.{gameName}, if undefined returns {key}
     */
    public static String getGameSpecific(String gameName, String key) {
        return getSpecific(key, gameName);
    }

    public static String getRecent(String key) {
        return ConfigManager.getInstance().getRecent(key);
    }

    public static boolean storeRecent(String key, String value) {
        return ConfigManager.getInstance().storeRecent(key, value);
    }

}


