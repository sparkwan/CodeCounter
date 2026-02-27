package com.github.dev.tool.plugins.counter;

import com.github.dev.tool.plugin.Plugin;
import com.github.dev.tool.plugin.PluginMetadata;
import com.github.dev.tool.plugin.PluginContext;
import com.github.dev.tool.plugin.PluginPanel;

/**
 * Code Counter Plugin - counts lines of code, comments, blank lines, etc.
 * This is the refactored version of the original CodeCounterGUI functionality.
 */
public class CodeCounterPlugin implements Plugin {

    private static final String PLUGIN_ID = "com.github.tools.code-counter";
    private static final String PLUGIN_VERSION = "1.0.0";

    // i18n keys for name and description
    private static final String NAME_KEY = "plugin.counter.name";
    private static final String DESC_KEY = "plugin.counter.description";

    private PluginMetadata metadata;
    private PluginContext context;
    private CodeCounterPluginPanel pluginPanel;
    private boolean initialized = false;

    public CodeCounterPlugin() {
        // Use English defaults before context is available
        this.metadata = buildMetadata("Code Counter",
            "Counts lines of code, comments, blank lines, and analyzes code metrics");
    }

    private PluginMetadata buildMetadata(String name, String description) {
        return new PluginMetadata(
            PLUGIN_ID,
            name,
            PLUGIN_VERSION,
            description,
            "Spark Wan",
            "com.github.dev.tool.plugins.counter.CodeCounterPlugin",
            "1.0.0"
        );
    }

    @Override
    public PluginMetadata getMetadata() {
        // If context is available, return localized metadata
        if (context != null) {
            return buildMetadata(context.getString(NAME_KEY), context.getString(DESC_KEY));
        }
        return metadata;
    }

    @Override
    public void initialize(PluginContext context) throws Exception {
        this.context = context;
        // Rebuild metadata with localized strings
        this.metadata = buildMetadata(context.getString(NAME_KEY), context.getString(DESC_KEY));
        // Create the plugin panel with the shared context
        this.pluginPanel = new CodeCounterPluginPanel(context);
        this.pluginPanel.initializeUI();
        this.initialized = true;
    }

    @Override
    public void shutdown() {
        if (pluginPanel != null) {
            pluginPanel.dispose();
        }
        initialized = false;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public PluginPanel getPluginPanel() {
        return pluginPanel;
    }
}
