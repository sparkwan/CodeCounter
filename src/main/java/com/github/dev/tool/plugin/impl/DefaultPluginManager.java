package com.github.dev.tool.plugin.impl;

import com.github.dev.tool.plugin.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

/**
 * Default implementation of PluginManager.
 * Handles plugin discovery, loading, initialization, and lifecycle management.
 */
public class DefaultPluginManager implements PluginManager {

    private List<Plugin> loadedPlugins = new ArrayList<>();
    private Map<String, Boolean> pluginEnabledMap = new HashMap<>();
    private List<PluginLifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();

    private ThemeManager themeManager;
    private LocalizationManager localizationManager;
    private Preferences appPreferences;

    public DefaultPluginManager(ThemeManager themeManager, LocalizationManager localizationManager, Preferences appPreferences) {
        this.themeManager = themeManager;
        this.localizationManager = localizationManager;
        this.appPreferences = appPreferences;
    }

    @Override
    public void loadPlugins() throws Exception {
        // For now, manually load built-in plugins
        // In future, this can be extended to load plugins from filesystem/classpath

        // Load Code Counter Plugin
        try {
            Class<?> clazz = Class.forName("com.github.dev.tool.plugins.counter.CodeCounterPlugin");
            Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
            loadedPlugins.add(plugin);
            pluginEnabledMap.put(plugin.getMetadata().getId(), true);

            notifyPluginLoaded(plugin);
        } catch (Exception e) {
            System.err.println("Failed to load Code Counter Plugin: " + e.getMessage());
            e.printStackTrace();
        }

        // Additional built-in plugins can be loaded here
    }

    @Override
    public List<Plugin> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins);
    }

    @Override
    public Plugin getPlugin(String pluginId) {
        for (Plugin plugin : loadedPlugins) {
            if (plugin.getMetadata().getId().equals(pluginId)) {
                return plugin;
            }
        }
        return null;
    }

    @Override
    public void initializePlugin(Plugin plugin) throws Exception {
        if (plugin.isInitialized()) {
            return;  // Already initialized
        }

        // Create plugin-specific preferences
        String prefsKey = "plugin." + plugin.getMetadata().getId();
        Preferences pluginPrefs = appPreferences.node(prefsKey);

        // Load plugin's resource bundle
        ResourceBundle resourceBundle = loadPluginResources(plugin);

        // Create plugin context
        PluginContext context = new PluginContext(
            plugin.getMetadata().getId(),
            pluginPrefs,
            resourceBundle,
            themeManager,
            localizationManager,
            this
        );

        // Initialize the plugin
        plugin.initialize(context);
        notifyPluginInitialized(plugin);
    }

    @Override
    public void unloadPlugin(String pluginId) {
        Plugin plugin = getPlugin(pluginId);
        if (plugin != null) {
            plugin.shutdown();
            loadedPlugins.remove(plugin);
            notifyPluginUnloaded(plugin);
        }
    }

    @Override
    public void setPluginEnabled(String pluginId, boolean enabled) {
        pluginEnabledMap.put(pluginId, enabled);
        // Persist preference
        appPreferences.putBoolean("plugin." + pluginId + ".enabled", enabled);
    }

    @Override
    public boolean isPluginEnabled(String pluginId) {
        Boolean enabled = pluginEnabledMap.get(pluginId);
        return enabled != null ? enabled : true;  // Default to enabled
    }

    @Override
    public void addPluginLifecycleListener(PluginLifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    @Override
    public void removePluginLifecycleListener(PluginLifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    // Helper methods

    private ResourceBundle loadPluginResources(Plugin plugin) {
        // Use application-level i18n resources for all plugins
        // This allows all plugins to share the same translation files
        try {
            ResourceBundle.clearCache();
            return ResourceBundle.getBundle("i18n.strings", Locale.getDefault(),
                new UTF8Control());
        } catch (MissingResourceException e) {
            System.err.println("Could not load application i18n resources: " + e.getMessage());
            // Return fallback bundle that returns keys as values
            return createFallbackBundle();
        }
    }

    private ResourceBundle createFallbackBundle() {
        return new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return key;  // Return key as fallback
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.emptyEnumeration();
            }
        };
    }

    // UTF-8 Resource Bundle Control for proper internationalization
    public static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, java.io.IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            try (java.io.InputStream is = loader.getResourceAsStream(resourceName)) {
                if (is == null) return null;
                // Skip UTF-8 BOM (EF BB BF) if present
                java.io.InputStream bomSafe = new java.io.BufferedInputStream(is);
                bomSafe.mark(3);
                byte[] bom = new byte[3];
                int read = bomSafe.read(bom);
                if (read < 3 || bom[0] != (byte) 0xEF || bom[1] != (byte) 0xBB || bom[2] != (byte) 0xBF) {
                    bomSafe.reset(); // No BOM found, reset to start
                }
                try (java.io.InputStreamReader isr = new java.io.InputStreamReader(bomSafe,
                    java.nio.charset.StandardCharsets.UTF_8)) {
                    java.util.Properties props = new java.util.Properties();
                    props.load(isr);
                    return new ResourceBundle() {
                        @Override
                        protected Object handleGetObject(String key) {
                            return props.getProperty(key);
                        }

                        @Override
                        public Enumeration<String> getKeys() {
                            return java.util.Collections.enumeration(
                                new java.util.ArrayList<>(props.stringPropertyNames()));
                        }
                    };
                }
            }
        }
    }

    private void notifyPluginLoaded(Plugin plugin) {
        for (PluginLifecycleListener listener : lifecycleListeners) {
            listener.onPluginLoaded(plugin);
        }
    }

    private void notifyPluginInitialized(Plugin plugin) {
        for (PluginLifecycleListener listener : lifecycleListeners) {
            listener.onPluginInitialized(plugin);
        }
    }

    private void notifyPluginUnloaded(Plugin plugin) {
        for (PluginLifecycleListener listener : lifecycleListeners) {
            listener.onPluginUnloaded(plugin);
        }
    }
}

