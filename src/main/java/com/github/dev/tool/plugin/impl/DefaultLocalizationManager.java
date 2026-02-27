package com.github.dev.tool.plugin.impl;

import com.github.dev.tool.plugin.LocalizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Default implementation of LocalizationManager.
 * Manages application-wide locale switching.
 */
public class DefaultLocalizationManager implements LocalizationManager {

    private Locale currentLocale = Locale.getDefault();
    private Locale[] supportedLocales = {
        Locale.ENGLISH,
        Locale.SIMPLIFIED_CHINESE,
        Locale.TRADITIONAL_CHINESE,
        Locale.JAPANESE,
        new Locale("es"),
        Locale.GERMAN,
        Locale.FRENCH,
        new Locale("pt")
    };
    private List<LocaleChangeListener> listeners = new ArrayList<>();

    @Override
    public void setLocale(Locale locale) {
        if (!currentLocale.equals(locale)) {
            this.currentLocale = locale;
            Locale.setDefault(locale);
            notifyListeners(locale);
        }
    }

    @Override
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    @Override
    public Locale[] getSupportedLocales() {
        return supportedLocales;
    }

    @Override
    public void addLocaleChangeListener(LocaleChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeLocaleChangeListener(LocaleChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Locale newLocale) {
        for (LocaleChangeListener listener : listeners) {
            try {
                listener.onLocaleChanged(newLocale);
            } catch (Exception e) {
                System.err.println("Error in locale change listener: " + e.getMessage());
            }
        }
    }
}

