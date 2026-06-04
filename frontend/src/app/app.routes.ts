import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login';
import { AppLayoutComponent } from './layout/app-layout/app-layout';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { authGuard } from './core/auth-guard';
import { PessoaListComponent } from './pages/pessoas/pessoa-list/pessoa-list';
import { PessoaFormComponent } from './pages/pessoas/pessoa-form/pessoa-form';
import { CategoriaListComponent } from './pages/categorias/categoria-list/categoria-list';
import { CategoriaFormComponent } from './pages/categorias/categoria-form/categoria-form';
import { LocalListComponent } from './pages/locais/local-list/local-list';
import { LocalFormComponent } from './pages/locais/local-form/local-form';
import { ItemMestreListComponent } from './pages/itens-mestre/item-mestre-list/item-mestre-list';
import { ItemMestreFormComponent } from './pages/itens-mestre/item-mestre-form/item-mestre-form';
import { InstanciaItemListComponent } from './pages/instancias-item/instancia-item-list/instancia-item-list';
import { InstanciaItemFormComponent } from './pages/instancias-item/instancia-item-form/instancia-item-form';
import { InstanciaItemHistoricoComponent } from './pages/instancias-item/instancia-item-historico/instancia-item-historico';

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
      {
        path: 'locais',
        component: LocalListComponent,
      },
      {
        path: 'locais/novo',
        component: LocalFormComponent,
      },
      {
        path: 'locais/:id/editar',
        component: LocalFormComponent,
      },
      {
        path: 'itens-mestre',
        component: ItemMestreListComponent,
      },
      {
        path: 'itens-mestre/novo',
        component: ItemMestreFormComponent,
      },
      {
        path: 'itens-mestre/:id/editar',
        component: ItemMestreFormComponent,
      },
      {
        path: 'instancias-item',
        component: InstanciaItemListComponent,
      },
      {
        path: 'instancias-item/nova',
        component: InstanciaItemFormComponent,
      },
      {
        path: 'instancias-item/:id/historico',
        component: InstanciaItemHistoricoComponent,
      },
      {
        path: 'instancias-item/:id/editar',
        component: InstanciaItemFormComponent,
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
