package com.github.dev.tool.plugin;

import java.util.List;

/**
 * Manages plugins: discovery, loading, initialization, and lifecycle.
 */
public interface PluginManager {

    /**
     * Load and discover all available plugins.
     * @throws Exception if plugin discovery or loading fails
     */
    void loadPlugins() throws Exception;

    /**
     * Get all loaded plugins.
     */
    List<Plugin> getLoadedPlugins();

    /**
     * Get a specific plugin by ID.
     */
    Plugin getPlugin(String pluginId);

    /**
     * Initialize a plugin.
     * @param plugin the plugin to initialize
     * @throws Exception if initialization fails
     */
    void initializePlugin(Plugin plugin) throws Exception;

    /**
     * Unload a plugin.
     * @param pluginId the ID of the plugin to unload
     */
    void unloadPlugin(String pluginId);

    /**
     * Enable or disable a plugin.
     */
    void setPluginEnabled(String pluginId, boolean enabled);

    /**
     * Check if a plugin is enabled.
     */
    boolean isPluginEnabled(String pluginId);

    /**
     * Register a listener for plugin lifecycle events.
     */
    void addPluginLifecycleListener(PluginLifecycleListener listener);

    /**
     * Remove a plugin lifecycle listener.
     */
    void removePluginLifecycleListener(PluginLifecycleListener listener);

    /**
     * Plugin lifecycle event listener.
     */
    interface PluginLifecycleListener {
        void onPluginLoaded(Plugin plugin);
        void onPluginInitialized(Plugin plugin);
        void onPluginUnloaded(Plugin plugin);
        void onPluginError(Plugin plugin, Throwable error);
    }
}

