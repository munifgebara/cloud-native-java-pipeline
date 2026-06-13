import { Component, input } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';

export type StellaMetricCardTone = 'info' | 'warning' | 'success' | 'accent';

@Component({
  selector: 'app-stella-metric-card',
  standalone: true,
  imports: [NgTemplateOutlet, RouterLink],
  templateUrl: './stella-metric-card.html',
  styleUrl: './stella-metric-card.css',
})
export class StellaMetricCardComponent {
  readonly title = input.required<string>();
  readonly value = input.required<number | string>();
  readonly caption = input<string>('');
  readonly tone = input<StellaMetricCardTone>('info');
  readonly link = input<string | readonly unknown[] | null>(null);
  readonly href = input<string>('');
  readonly ariaLabel = input<string>('');
}
