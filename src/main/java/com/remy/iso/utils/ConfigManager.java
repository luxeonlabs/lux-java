package com.remy.iso.utils;

import com.remy.iso.networking.incoming.generic.ClientConfig.ClientConfigItem;

public class ConfigManager {
    private ClientConfigItem[] configs = new ClientConfigItem[0];

    public void setConfigs(ClientConfigItem[] configs) {
        this.configs = configs;
    }

    private String getRaw(String key) {
        for (ClientConfigItem config : configs) {
            if (config.key.equals(key))
                return config.value;
        }
        return null;
    }

    public String getString(String key, String defaultValue) {
        String val = getRaw(key);
        return val != null ? val : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String val = getRaw(key);
        if (val == null)
            return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public float getFloat(String key, float defaultValue) {
        String val = getRaw(key);
        if (val == null)
            return defaultValue;
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = getRaw(key);
        if (val == null)
            return defaultValue;
        return val.equals("1") || val.equalsIgnoreCase("true");
    }
}
