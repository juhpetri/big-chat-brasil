import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api')) {
    return next(req);
  }

  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  const request = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      // Token inválido/expirado numa chamada autenticada: a sessão local não serve mais pra nada,
      // então desloga e manda pro login em vez de deixar cada tela reinventar esse tratamento.
      // `inject()` só funciona no corpo síncrono do interceptor — por isso `router` já foi
      // resolvido acima, não pode ser chamado aqui dentro do callback assíncrono do catchError.
      if (error instanceof HttpErrorResponse && error.status === 401 && token) {
        authService.logout();
        router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
