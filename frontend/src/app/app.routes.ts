import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login';
import { AppLayoutComponent } from './layout/app-layout/app-layout';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { authGuard } from './core/auth-guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        component: DashboardComponent,
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
