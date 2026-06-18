import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

type ApiErrorBody = {
  erro?: string;
  message?: string;
  campos?: Record<string, string>;
};

export function mensagemErroHttp(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  const userMessage = (error as HttpErrorResponse & { userMessage?: string }).userMessage;
  if (userMessage) {
    return userMessage;
  }

  return montarMensagemUser(error, fallback);
}

export const errorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const userMessage = montarMensagemUser(error, 'Não foi possível concluir a operação.');
        Object.defineProperty(error, 'userMessage', {
          configurable: true,
          enumerable: false,
          value: userMessage,
        });
        console.error('Erro HTTP', {
          method: req.method,
          url: req.urlWithParams,
          status: error.status,
          message: userMessage,
        });
      }

      return throwError(() => error);
    }),
  );

function montarMensagemUser(error: HttpErrorResponse, fallback: string): string {
  if (error.status === 0) {
    return 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.';
  }

  const body = error.error as ApiErrorBody | null;
  const campos = body?.campos ? Object.values(body.campos).filter(Boolean) : [];

  if (campos.length) {
    return campos.join(' ');
  }

  if (body?.erro) {
    return body.erro;
  }

  if (body?.message) {
    return body.message;
  }

  if (error.status === 401) {
    return 'Sua sessão expirou. Entre novamente para continuar.';
  }

  if (error.status === 403) {
    return 'Você não tem permissão para executar esta ação.';
  }

  if (error.status >= 500) {
    return 'Erro interno ao processar a solicitação. Tente novamente em instantes.';
  }

  return fallback;
}
