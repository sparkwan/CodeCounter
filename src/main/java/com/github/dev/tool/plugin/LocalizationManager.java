package com.github.dev.tool.plugin;

import java.util.Locale;

/**
 * Manages application localization (i18n).
 * Provides support for runtime locale switching and language changes.
 */
public interface LocalizationManager {

    /**
     * Set the current locale and notify all registered listeners.
     * @param locale the new locale
     */
    void setLocale(Locale locale);

    /**
     * Get the current locale.
     */
    Locale getCurrentLocale();

    /**
     * Get supported locales.
     */
    Locale[] getSupportedLocales();

    /**
     * Register a listener to be notified when locale changes.
     */
    void addLocaleChangeListener(LocaleChangeListener listener);

    /**
     * Remove a locale change listener.
     */
    void removeLocaleChangeListener(LocaleChangeListener listener);

    /**
     * Locale change event listener.
     */
    interface LocaleChangeListener {
        void onLocaleChanged(Locale newLocale);
    }
}

