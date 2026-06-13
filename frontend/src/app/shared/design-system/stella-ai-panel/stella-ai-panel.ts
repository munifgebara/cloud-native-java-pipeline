import { Component, input } from '@angular/core';

@Component({
  selector: 'app-stella-ai-panel',
  standalone: true,
  templateUrl: './stella-ai-panel.html',
  styleUrl: './stella-ai-panel.css',
})
export class StellaAiPanelComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>('');
  readonly kicker = input<string>('');
  readonly headingLevel = input<'h2' | 'h3'>('h3');
  readonly ariaLabel = input<string>('');
}
