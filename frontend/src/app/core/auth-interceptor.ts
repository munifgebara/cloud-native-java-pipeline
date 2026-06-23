import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const isRelativeApiRequest = req.url.startsWith('/api/');
  const isAbsoluteApiRequest = !!environment.apiBaseUrl && req.url.startsWith(`${environment.apiBaseUrl}/api/`);
  const shouldAttachToken = isRelativeApiRequest || isAbsoluteApiRequest;

  if (!shouldAttachToken || req.url.includes('/api/public/')) {
    return next(req);
  }

  return authService.accessTokenForRequest().pipe(
    switchMap((token) => {
      if (!token) {
        authService.redirectToLogin('session-expired', req.urlWithParams);
        return throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized', url: req.urlWithParams }));
      }

      return next(withBearer(req, token)).pipe(
        catchError((error: unknown) => {
          if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
            return throwError(() => error);
          }

          return authService.refreshAccessToken().pipe(
            catchError((refreshError) => {
              authService.redirectToLogin('session-expired', req.urlWithParams);
              return throwError(() => refreshError);
            }),
            switchMap((response) => next(withBearer(req, response.accessToken)).pipe(
              catchError((retryError: unknown) => {
                if (retryError instanceof HttpErrorResponse && retryError.status === 401) {
                  authService.redirectToLogin('session-expired', req.urlWithParams);
                }
                return throwError(() => retryError);
              })
            ))
          );
        })
      );
    })
  );
};

function withBearer(req: HttpRequest<unknown>, token: string) {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}
