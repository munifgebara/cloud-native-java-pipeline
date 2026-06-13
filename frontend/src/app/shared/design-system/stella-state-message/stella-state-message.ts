import { Component, input } from '@angular/core';

export type StellaStateMessageSeverity = 'info' | 'success' | 'warning' | 'error';

@Component({
  selector: 'app-stella-state-message',
  standalone: true,
  templateUrl: './stella-state-message.html',
  styleUrl: './stella-state-message.css',
})
export class StellaStateMessageComponent {
  readonly severity = input<StellaStateMessageSeverity>('info');
  readonly message = input.required<string>();
}
