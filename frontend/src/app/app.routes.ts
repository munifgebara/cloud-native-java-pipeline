import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login';
import { AppLayoutComponent } from './layout/app-layout/app-layout';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { authGuard } from './core/auth-guard';
import { adminGuard } from './core/role-guard';
import { PersonListComponent } from './pages/pessoas/pessoa-list/pessoa-list';
import { PersonFormComponent } from './pages/pessoas/pessoa-form/pessoa-form';
import { CategoryListComponent } from './pages/categorias/categoria-list/categoria-list';
import { CategoryFormComponent } from './pages/categorias/categoria-form/categoria-form';
import { LocationListComponent } from './pages/locais/local-list/local-list';
import { LocationFormComponent } from './pages/locais/local-form/local-form';
import { MainItemListComponent } from './pages/itens-mestre/item-mestre-list/item-mestre-list';
import { MainItemFormComponent } from './pages/itens-mestre/item-mestre-form/item-mestre-form';
import { ItemInstanceFormComponent } from './pages/instancias-item/instancia-item-form/instancia-item-form';
import { ItemInstanceHistoricoComponent } from './pages/instancias-item/instancia-item-historico/instancia-item-historico';
import { UserListComponent } from './pages/usuarios/usuario-list/usuario-list';
import { UserFormComponent } from './pages/usuarios/usuario-form/usuario-form';
import { PerfilComponent } from './pages/perfil/perfil';
import { PhotoUploadComponent } from './pages/cadastro-foto/cadastro-foto';

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
        path: 'perfil',
        component: PerfilComponent,
      },
      {
        path: 'usuarios',
        component: UserListComponent,
        canActivate: [adminGuard],
      },
      {
        path: 'usuarios/create',
        component: UserFormComponent,
        canActivate: [adminGuard],
      },
      {
        path: 'usuarios/:id/edit',
        component: UserFormComponent,
        canActivate: [adminGuard],
      },
      {
        path: 'pessoas',
        component: PersonListComponent,
      },
      {
        path: 'pessoas/nova',
        component: PersonFormComponent,
      },
      {
        path: 'pessoas/:id/edit',
        component: PersonFormComponent,
      },
      {
        path: 'categorias',
        component: CategoryListComponent,
      },
      {
        path: 'categorias/nova',
        component: CategoryFormComponent,
      },
      {
        path: 'categorias/:id/edit',
        component: CategoryFormComponent,
      },
      {
        path: 'locais',
        component: LocationListComponent,
      },
      {
        path: 'locais/create',
        component: LocationFormComponent,
      },
      {
        path: 'locais/:id/edit',
        component: LocationFormComponent,
      },
      {
        path: 'itens-mestre',
        component: MainItemListComponent,
      },
      {
        path: 'cadastro-foto',
        component: PhotoUploadComponent,
      },
      {
        path: 'itens-mestre/create',
        component: MainItemFormComponent,
      },
      {
        path: 'itens-mestre/:id/edit',
        component: MainItemFormComponent,
      },
      {
        path: 'instancias-item/:id/historico',
        component: ItemInstanceHistoricoComponent,
      },
      {
        path: 'instancias-item/:id/edit',
        component: ItemInstanceFormComponent,
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
