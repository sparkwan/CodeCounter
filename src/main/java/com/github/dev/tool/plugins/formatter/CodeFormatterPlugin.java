package com.github.dev.tool.plugins.formatter;

import com.github.dev.tool.plugin.Plugin;
import com.github.dev.tool.plugin.PluginContext;
import com.github.dev.tool.plugin.PluginMetadata;
import com.github.dev.tool.plugin.PluginPanel;
import com.github.dev.tool.plugin.*;

/**
 * Code Formatter Plugin - formats source code according to style rules.
 * This is a template plugin demonstrating how to implement other tools.
 */
public class CodeFormatterPlugin implements Plugin {

    private static final String PLUGIN_ID = "com.github.tools.code-formatter";
    private static final String PLUGIN_NAME = "Code Formatter";
    private static final String PLUGIN_VERSION = "1.0.0";

    private PluginMetadata metadata;
    private PluginContext context;
    private CodeFormatterPluginPanel pluginPanel;
    private boolean initialized = false;

    public CodeFormatterPlugin() {
        this.metadata = new PluginMetadata(
            PLUGIN_ID,
            PLUGIN_NAME,
            PLUGIN_VERSION,
            "Formats source code according to style rules (indent, spacing, line length, etc.)",
            "Spark Wan",
            "com.github.dev.tool.plugins.formatter.CodeFormatterPlugin",
            "1.0.0"
        );
    }

    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void initialize(PluginContext context) throws Exception {
        this.context = context;
        this.pluginPanel = new CodeFormatterPluginPanel(context);
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

