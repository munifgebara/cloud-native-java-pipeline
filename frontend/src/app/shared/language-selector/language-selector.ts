import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';

@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './language-selector.html',
  styleUrl: './language-selector.css',
})
export class LanguageSelectorComponent {
  readonly i18n = inject(I18nService);

  changeLanguage(language: string): void {
    this.i18n.setLanguage(language);
  }
}
