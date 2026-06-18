import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login';
import { AppLayoutComponent } from './layout/app-layout/app-layout';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { authGuard } from './core/auth-guard';
import { adminGuard } from './core/role-guard';
import { PersonListComponent } from './pages/people/person-list/person-list';
import { PersonFormComponent } from './pages/people/person-form/person-form';
import { CategoryListComponent } from './pages/categories/category-list/category-list';
import { CategoryFormComponent } from './pages/categories/category-form/category-form';
import { LocationListComponent } from './pages/locations/location-list/location-list';
import { LocationFormComponent } from './pages/locations/location-form/location-form';
import { MainItemListComponent } from './pages/main-items/main-item-list/main-item-list';
import { MainItemFormComponent } from './pages/main-items/main-item-form/main-item-form';
import { ItemInstanceFormComponent } from './pages/item-instances/item-instance-form/item-instance-form';
import { ItemInstanceHistoricoComponent } from './pages/item-instances/item-instance-history/item-instance-history';
import { UserListComponent } from './pages/users/user-list/user-list';
import { UserFormComponent } from './pages/users/user-form/user-form';
import { PerfilComponent } from './pages/profile/profile';
import { PhotoUploadComponent } from './pages/photo-upload/photo-upload';

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
        path: 'profile',
        component: PerfilComponent,
      },
      {
        path: 'users',
        component: UserListComponent,
        canActivate: [adminGuard],
      },
      {
        path: 'users/create',
        component: UserFormComponent,
        canActivate: [adminGuard],
      },
      {
        path: 'users/:id/edit',
        component: UserFormComponent,
        canActivate: [adminGuard],
      },
      {
        path: 'people',
        component: PersonListComponent,
      },
      {
        path: 'people/nova',
        component: PersonFormComponent,
      },
      {
        path: 'people/:id/edit',
        component: PersonFormComponent,
      },
      {
        path: 'categories',
        component: CategoryListComponent,
      },
      {
        path: 'categories/nova',
        component: CategoryFormComponent,
      },
      {
        path: 'categories/:id/edit',
        component: CategoryFormComponent,
      },
      {
        path: 'locations',
        component: LocationListComponent,
      },
      {
        path: 'locations/create',
        component: LocationFormComponent,
      },
      {
        path: 'locations/:id/edit',
        component: LocationFormComponent,
      },
      {
        path: 'main-items',
        component: MainItemListComponent,
      },
      {
        path: 'photo-upload',
        component: PhotoUploadComponent,
      },
      {
        path: 'main-items/create',
        component: MainItemFormComponent,
      },
      {
        path: 'main-items/:id/edit',
        component: MainItemFormComponent,
      },
      {
        path: 'item-instances/:id/history',
        component: ItemInstanceHistoricoComponent,
      },
      {
        path: 'item-instances/:id/edit',
        component: ItemInstanceFormComponent,
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
