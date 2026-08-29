import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: 'conversations',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/conversations-shell/conversations-shell.component').then(
        (m) => m.ConversationsShellComponent,
      ),
    children: [
      {
        path: ':id',
        loadComponent: () =>
          import('./features/chat/chat-page.component').then((m) => m.ChatPageComponent),
      },
    ],
  },
  {
    path: 'account',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/account/account-page.component').then((m) => m.AccountPageComponent),
  },
];
