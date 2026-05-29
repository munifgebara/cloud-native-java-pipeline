import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login';
import { AppLayoutComponent } from './layout/app-layout/app-layout';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { authGuard } from './core/auth-guard';
import { PessoaListComponent } from './pages/pessoas/pessoa-list/pessoa-list';
import { PessoaFormComponent } from './pages/pessoas/pessoa-form/pessoa-form';
import { CategoriaListComponent } from './pages/categorias/categoria-list/categoria-list';
import { CategoriaFormComponent } from './pages/categorias/categoria-form/categoria-form';

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
      {
        path: 'pessoas',
        component: PessoaListComponent,
      },
      {
        path: 'pessoas/nova',
        component: PessoaFormComponent,
      },
      {
        path: 'pessoas/:id/editar',
        component: PessoaFormComponent,
      },
      {
        path: 'categorias',
        component: CategoriaListComponent,
      },
      {
        path: 'categorias/nova',
        component: CategoriaFormComponent,
      },
      {
        path: 'categorias/:id/editar',
        component: CategoriaFormComponent,
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
