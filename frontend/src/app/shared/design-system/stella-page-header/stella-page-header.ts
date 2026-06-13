import { Component, input } from '@angular/core';

@Component({
  selector: 'app-stella-page-header',
  standalone: true,
  templateUrl: './stella-page-header.html',
  styleUrl: './stella-page-header.css',
})
export class StellaPageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>('');
  readonly headingLevel = input<'h1' | 'h2'>('h2');
}
