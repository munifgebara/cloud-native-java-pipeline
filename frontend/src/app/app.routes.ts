import { Routes } from '@angular/router';
import { AppLayoutComponent } from './layout/app-layout/app-layout';
import { authGuard } from './core/auth-guard';
import { adminGuard } from './core/role-guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'auth/callback',
    loadComponent: () => import('./pages/auth-callback/auth-callback').then((m) => m.AuthCallbackComponent),
  },
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.DashboardComponent),
      },
      {
        path: 'profile',
        loadComponent: () => import('./pages/profile/profile').then((m) => m.PerfilComponent),
      },
      {
        path: 'users',
        loadComponent: () => import('./pages/users/user-list/user-list').then((m) => m.UserListComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'users/create',
        loadComponent: () => import('./pages/users/user-form/user-form').then((m) => m.UserFormComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'users/:id/edit',
        loadComponent: () => import('./pages/users/user-form/user-form').then((m) => m.UserFormComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'people',
        loadComponent: () => import('./pages/people/person-list/person-list').then((m) => m.PersonListComponent),
      },
      {
        path: 'people/nova',
        loadComponent: () => import('./pages/people/person-form/person-form').then((m) => m.PersonFormComponent),
      },
      {
        path: 'people/:id/edit',
        loadComponent: () => import('./pages/people/person-form/person-form').then((m) => m.PersonFormComponent),
      },
      {
        path: 'categories',
        loadComponent: () => import('./pages/categories/category-list/category-list').then((m) => m.CategoryListComponent),
      },
      {
        path: 'categories/nova',
        loadComponent: () => import('./pages/categories/category-form/category-form').then((m) => m.CategoryFormComponent),
      },
      {
        path: 'categories/:id/edit',
        loadComponent: () => import('./pages/categories/category-form/category-form').then((m) => m.CategoryFormComponent),
      },
      {
        path: 'locations',
        loadComponent: () => import('./pages/locations/location-list/location-list').then((m) => m.LocationListComponent),
      },
      {
        path: 'locations/create',
        loadComponent: () => import('./pages/locations/location-form/location-form').then((m) => m.LocationFormComponent),
      },
      {
        path: 'locations/:id/edit',
        loadComponent: () => import('./pages/locations/location-form/location-form').then((m) => m.LocationFormComponent),
      },
      {
        path: 'main-items',
        loadComponent: () => import('./pages/main-items/main-item-list/main-item-list').then((m) => m.MainItemListComponent),
      },
      {
        path: 'photo-upload',
        loadComponent: () => import('./pages/photo-upload/photo-upload').then((m) => m.PhotoUploadComponent),
      },
      {
        path: 'main-items/create',
        loadComponent: () => import('./pages/main-items/main-item-form/main-item-form').then((m) => m.MainItemFormComponent),
      },
      {
        path: 'main-items/:id/edit',
        loadComponent: () => import('./pages/main-items/main-item-form/main-item-form').then((m) => m.MainItemFormComponent),
      },
      {
        path: 'item-instances/:id/history',
        loadComponent: () => import('./pages/item-instances/item-instance-history/item-instance-history').then((m) => m.ItemInstanceHistoricoComponent),
      },
      {
        path: 'item-instances/:id/edit',
        loadComponent: () => import('./pages/item-instances/item-instance-form/item-instance-form').then((m) => m.ItemInstanceFormComponent),
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
