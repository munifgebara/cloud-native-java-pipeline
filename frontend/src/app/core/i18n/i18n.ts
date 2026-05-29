import { Injectable, Pipe, PipeTransform, computed, signal } from '@angular/core';
import { EN_TRANSLATIONS } from './translations/en';
import { ES_TRANSLATIONS } from './translations/es';
import { PT_BR_TRANSLATIONS } from './translations/pt-br';

export type Language = 'pt-BR' | 'en' | 'es';
export type TranslationKey = keyof typeof PT_BR_TRANSLATIONS;

type TranslationParams = Record<string, string | number>;
type TranslationMap = Partial<Record<TranslationKey, string>>;

const STORAGE_KEY = 'stella.language';
const DEFAULT_LANGUAGE: Language = 'pt-BR';
const TRANSLATIONS: Record<Language, TranslationMap> = {
  'pt-BR': PT_BR_TRANSLATIONS,
  en: EN_TRANSLATIONS,
  es: ES_TRANSLATIONS,
};

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly selectedLanguage = signal<Language>(this.loadInitialLanguage());

  readonly language = this.selectedLanguage.asReadonly();
  readonly languages: ReadonlyArray<Language> = ['pt-BR', 'en', 'es'];
  readonly languageOptions = computed(() =>
    this.languages.map((language) => ({
      value: language,
      label: this.translate(`language.${language}` as TranslationKey),
    })),
  );

  setLanguage(language: string): void {
    const selectedLanguage = this.isLanguage(language) ? language : DEFAULT_LANGUAGE;

    this.selectedLanguage.set(selectedLanguage);
    localStorage.setItem(STORAGE_KEY, selectedLanguage);
    document.documentElement.lang = selectedLanguage;
  }

  translate(key: TranslationKey, params: TranslationParams = {}): string {
    const value = TRANSLATIONS[this.selectedLanguage()][key] ?? PT_BR_TRANSLATIONS[key] ?? key;
    return this.interpolate(value, params);
  }

  private loadInitialLanguage(): Language {
    const savedLanguage = localStorage.getItem(STORAGE_KEY);
    const language = this.isLanguage(savedLanguage) ? savedLanguage : DEFAULT_LANGUAGE;
    document.documentElement.lang = language;
    return language;
  }

  private isLanguage(language: string | null): language is Language {
    return language === 'pt-BR' || language === 'en' || language === 'es';
  }

  private interpolate(value: string, params: TranslationParams): string {
    return value.replace(/\{\{\s*(\w+)\s*\}\}/g, (_, key: string) => String(params[key] ?? ''));
  }
}

@Pipe({
  name: 'translate',
  standalone: true,
  pure: false,
})
export class TranslatePipe implements PipeTransform {
  constructor(private readonly i18n: I18nService) {}

  transform(key: TranslationKey, params?: TranslationParams): string {
    return this.i18n.translate(key, params);
  }
}
