import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppSidebarComponent } from '../app-sidebar/app-sidebar';
import { AppTopbarComponent } from '../app-topbar/app-topbar';
import { TranslatePipe } from '../../core/i18n/i18n';

@Component({
  selector: 'app-app-layout',
  imports: [RouterOutlet, AppSidebarComponent, AppTopbarComponent, TranslatePipe],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.css',
})
export class AppLayoutComponent {
  readonly currentYear = new Date().getFullYear();
}
