// Language Management System
class LanguageManager {
    constructor() {
        this.currentLanguage = localStorage.getItem('preferredLanguage') || 'en';
        this.translations = {};
        this.availableLanguages = ['en', 'fr', 'de', 'es'];
    }

    async loadLanguage(lang) {
        try {
            const response = await fetch(`assets/lang/${lang}.json`);
            if (!response.ok) throw new Error(`Failed to load language: ${lang}`);
            this.translations = await response.json();
            this.currentLanguage = lang;
            localStorage.setItem('preferredLanguage', lang);
            this.updatePageContent();
            this.updateLanguageSelector();
        } catch (error) {
            console.error('Error loading language file:', error);
            // Fallback to English if loading fails
            if (lang !== 'en') {
                await this.loadLanguage('en');
            }
        }
    }

    updatePageContent() {
        // Update Header Navigation
        this.updateElement('[data-i18n="header.nav.platform"]', this.translations.header.nav.platform);
        this.updateElement('[data-i18n="header.nav.technology"]', this.translations.header.nav.technology);
        this.updateElement('[data-i18n="header.nav.vision"]', this.translations.header.nav.vision);
        this.updateElement('[data-i18n="header.nav.company"]', this.translations.header.nav.company);
        this.updateElement('[data-i18n="header.nav.talkToUs"]', this.translations.header.nav.talkToUs);

        // Update Hero Section
        this.updateElement('[data-i18n="hero.headline"]', this.translations.hero.headline);
        this.updateElement('[data-i18n="hero.subheadline"]', this.translations.hero.subheadline);
        this.updateElement('[data-i18n="hero.ctaButton"]', this.translations.hero.ctaButton);

        // Update Digital Economy Section
        this.updateElement('[data-i18n="economy.eyebrow"]', this.translations.economy.eyebrow);
        this.updateElement('[data-i18n="economy.title"]', this.translations.economy.title);
        this.updateElement('[data-i18n="economy.description"]', this.translations.economy.description);
        this.updateElement('[data-i18n="economy.cta"]', this.translations.economy.cta);

        // Update Platform Section
        this.updateElement('[data-i18n="platform.eyebrow"]', this.translations.platform.eyebrow);
        this.updateElement('[data-i18n="platform.title"]', this.translations.platform.title);
        this.updateElement('[data-i18n="platform.subtitle"]', this.translations.platform.subtitle);
        for (let i = 1; i <= 4; i++) {
            this.updateElement(`[data-i18n="platform.item${i}.title"]`, this.translations.platform[`item${i}`].title);
            this.updateElement(`[data-i18n="platform.item${i}.description"]`, this.translations.platform[`item${i}`].description);
        }

        // Update AI Store Manager Section
        this.updateElement('[data-i18n="ai.eyebrow"]', this.translations.ai.eyebrow);
        this.updateElement('[data-i18n="ai.title"]', this.translations.ai.title);
        this.updateElement('[data-i18n="ai.subtitle"]', this.translations.ai.subtitle);
        this.updateElement('[data-i18n="ai.chat1.you"]', this.translations.ai.chat1.you);
        this.updateElement('[data-i18n="ai.chat1.ai"]', this.translations.ai.chat1.ai);
        this.updateElement('[data-i18n="ai.chat1.action"]', this.translations.ai.chat1.action);
        this.updateElement('[data-i18n="ai.chat2.you"]', this.translations.ai.chat2.you);
        this.updateElement('[data-i18n="ai.chat2.ai"]', this.translations.ai.chat2.ai);
        this.updateElement('[data-i18n="ai.chat2.stat1"]', this.translations.ai.chat2.stat1);
        this.updateElement('[data-i18n="ai.chat2.stat2"]', this.translations.ai.chat2.stat2);
        this.updateElement('[data-i18n="ai.chat2.stat3"]', this.translations.ai.chat2.stat3);
        this.updateElement('[data-i18n="ai.chat2.action"]', this.translations.ai.chat2.action);

        // Update Ecosystem Section
        this.updateElement('[data-i18n="ecosystem.eyebrow"]', this.translations.ecosystem.eyebrow);
        this.updateElement('[data-i18n="ecosystem.title"]', this.translations.ecosystem.title);
        this.updateElement('[data-i18n="ecosystem.description"]', this.translations.ecosystem.description);

        // Update Commerce Lifecycle Section
        this.updateElement('[data-i18n="lifecycle.eyebrow"]', this.translations.lifecycle.eyebrow);
        this.updateElement('[data-i18n="lifecycle.title"]', this.translations.lifecycle.title);
        this.updateElement('[data-i18n="lifecycle.description"]', this.translations.lifecycle.description);
        for (let i = 1; i <= 7; i++) {
            this.updateElement(`[data-i18n="lifecycle.stage${i}"]`, this.translations.lifecycle[`stage${i}`]);
        }

        // Update Final CTA Section
        this.updateElement('[data-i18n="finalCta.title"]', this.translations.finalCta.title);
        this.updateElement('[data-i18n="finalCta.subtitle"]', this.translations.finalCta.subtitle);
        this.updateElement('[data-i18n="finalCta.button"]', this.translations.finalCta.button);

        // Update Footer
        this.updateElement('[data-i18n="footer.nav.platform"]', this.translations.footer.nav.platform);
        this.updateElement('[data-i18n="footer.nav.technology"]', this.translations.footer.nav.technology);
        this.updateElement('[data-i18n="footer.nav.vision"]', this.translations.footer.nav.vision);
        this.updateElement('[data-i18n="footer.legal.privacy"]', this.translations.footer.legal.privacy);
        this.updateElement('[data-i18n="footer.legal.cookies"]', this.translations.footer.legal.cookies);
        this.updateElement('[data-i18n="footer.backToTop"]', this.translations.footer.backToTop);
        this.updateElement('[data-i18n="footer.copyright"]', this.translations.footer.copyright);

        // Notify other scripts that translated content has been refreshed.
        document.dispatchEvent(new CustomEvent('content:updated'));
    }

    updateElement(selector, content) {
        const elements = document.querySelectorAll(selector);
        elements.forEach(element => {
            if (element) {
                element.textContent = content;
            }
        });
    }

    updatePlaceholder(selector, placeholder) {
        const elements = document.querySelectorAll(selector);
        elements.forEach(element => {
            if (element) {
                element.placeholder = placeholder;
            }
        });
    }

    updateLanguageSelector() {
        const desktopSelector = document.getElementById('language-selector');
        const mobileSelector = document.getElementById('mobile-language-selector');
        
        if (desktopSelector) {
            desktopSelector.value = this.currentLanguage;
        }
        if (mobileSelector) {
            mobileSelector.value = this.currentLanguage;
        }
    }

    async init() {
        await this.loadLanguage(this.currentLanguage);
    }
}

// Initialize language manager when DOM is ready
let languageManager;
document.addEventListener('DOMContentLoaded', async () => {
    languageManager = new LanguageManager();
    await languageManager.init();

    // Add event listener for language selector (desktop)
    const languageSelector = document.getElementById('language-selector');
    if (languageSelector) {
        languageSelector.addEventListener('change', async (e) => {
            await languageManager.loadLanguage(e.target.value);
            // Sync mobile selector
            const mobileSelector = document.getElementById('mobile-language-selector');
            if (mobileSelector) mobileSelector.value = e.target.value;
        });
    }

    // Add event listener for mobile language selector
    const mobileLanguageSelector = document.getElementById('mobile-language-selector');
    if (mobileLanguageSelector) {
        mobileLanguageSelector.addEventListener('change', async (e) => {
            await languageManager.loadLanguage(e.target.value);
            // Sync desktop selector
            if (languageSelector) languageSelector.value = e.target.value;
        });
    }
});
