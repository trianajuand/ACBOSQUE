import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ApiService } from './api.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const api = inject(ApiService);
  const router = inject(Router);

  const token = api.token;
  if (!token) {
    router.navigateByUrl('/login');
    return false;
  }

  const rol = obtenerRol(token);
  const url = state.url;
  if (url.startsWith('/comisionista') && rol !== 'COMISIONISTA') {
    router.navigateByUrl('/dashboard');
    return false;
  }
  if (url.startsWith('/dashboard') && rol === 'COMISIONISTA') {
    router.navigateByUrl('/comisionista');
    return false;
  }

  return true;
};

function obtenerRol(token: string): string | null {
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => `%${(`00${c.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join(''),
    );
    return JSON.parse(json)?.rol || null;
  } catch {
    return null;
  }
}
