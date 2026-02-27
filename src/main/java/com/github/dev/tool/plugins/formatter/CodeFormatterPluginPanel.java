package com.github.dev.tool.plugins.formatter;

import com.github.dev.tool.plugin.PluginContext;
import com.github.dev.tool.plugin.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/**
 * UI Panel for the Code Formatter Plugin.
 * Provides interface for selecting formatting rules and files to format.
 */
public class CodeFormatterPluginPanel extends PluginPanel {
    private static final long serialVersionUID = 1L;

    private JTextArea instructionArea;

    public CodeFormatterPluginPanel(PluginContext context) {
        super(context);
    }

    @Override
    public void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // For now, show a placeholder message
        instructionArea = new JTextArea();
        instructionArea.setText(
            "Code Formatter Plugin\n" +
            "====================\n\n" +
            "This plugin will provide:\n" +
            "- Batch code formatting\n" +
            "- Customizable style rules\n" +
            "- Support for Java, Python, JavaScript, and more\n\n" +
            "Development in progress...\n"
        );
        instructionArea.setEditable(false);
        instructionArea.setLineWrap(true);
        instructionArea.setWrapStyleWord(true);

        add(new JScrollPane(instructionArea), BorderLayout.CENTER);
    }

    @Override
    public void updateLocale(Locale newLocale) {
        // Update UI text
    }
}

