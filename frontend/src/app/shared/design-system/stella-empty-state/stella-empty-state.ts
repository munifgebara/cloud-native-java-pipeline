import { Component, input } from '@angular/core';

@Component({
  selector: 'app-stella-empty-state',
  standalone: true,
  templateUrl: './stella-empty-state.html',
  styleUrl: './stella-empty-state.css',
})
export class StellaEmptyStateComponent {
  readonly icon = input<string>('');
  readonly title = input<string>('');
  readonly message = input.required<string>();
  readonly compact = input(false);
}
