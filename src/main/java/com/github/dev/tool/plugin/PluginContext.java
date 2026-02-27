package com.github.dev.tool.plugin;

import com.github.dev.tool.plugin.impl.DefaultPluginManager;

import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Context object passed to plugins during initialization.
 * Provides access to shared services: preferences, localization, theme management, etc.
 */
public class PluginContext {
    private final String pluginId;
    private final Preferences pluginPreferences;
    private final ResourceBundle resourceBundle;
    private final ThemeManager themeManager;
    private final LocalizationManager localizationManager;
    private final PluginManager pluginManager;

    public PluginContext(String pluginId,
                        Preferences pluginPreferences,
                        ResourceBundle resourceBundle,
                        ThemeManager themeManager,
                        LocalizationManager localizationManager,
                        PluginManager pluginManager) {
        this.pluginId = pluginId;
        this.pluginPreferences = pluginPreferences;
        this.resourceBundle = resourceBundle;
        this.themeManager = themeManager;
        this.localizationManager = localizationManager;
        this.pluginManager = pluginManager;
    }

    /**
     * Get the unique ID of this plugin.
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Get preferences specific to this plugin.
     */
    public Preferences getPreferences() {
        return pluginPreferences;
    }

    /**
     * Get localized strings for this plugin.
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Get the theme manager for UI consistency.
     */
    public ThemeManager getThemeManager() {
        return themeManager;
    }

    /**
     * Get the localization manager for runtime locale switching.
     */
    public LocalizationManager getLocalizationManager() {
        return localizationManager;
    }

    /**
     * Get the plugin manager for accessing other plugins.
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Convenience method to get a localized string.
     * Dynamically loads the resource bundle based on current locale.
     */
    public String getString(String key) {
        try {
            // Reload resource bundle every time to ensure current locale is used
            ResourceBundle currentBundle = loadResourceBundle();
            return currentBundle.getString(key);
        } catch (Exception e) {
            return key;  // Fallback to key name if translation not found
        }
    }

    private ResourceBundle loadResourceBundle() {
        try {
            java.util.Locale currentLocale = localizationManager.getCurrentLocale();
            // Clear cache to ensure locale switch takes effect
            ResourceBundle.clearCache();
            return ResourceBundle.getBundle("i18n.strings", currentLocale,
                new DefaultPluginManager.UTF8Control());
        } catch (Exception e) {
            // Return fallback bundle
            return new ResourceBundle() {
                @Override
                protected Object handleGetObject(String key) {
                    return key;
                }

                @Override
                public java.util.Enumeration<String> getKeys() {
                    return java.util.Collections.emptyEnumeration();
                }
            };
        }
    }

    /**
     * Convenience method to check if a resource key exists.
     */
    public boolean hasKey(String key) {
        try {
            ResourceBundle currentBundle = loadResourceBundle();
            return currentBundle.containsKey(key);
        } catch (Exception e) {
            return false;
        }
    }
}

