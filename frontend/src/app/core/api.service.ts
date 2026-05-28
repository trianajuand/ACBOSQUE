import { Injectable } from '@angular/core';

export interface ApiResult<T> {
  ok: boolean;
  status: number;
  data: T | null;
  error?: string;
  mensaje?: string;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  readonly baseUrl = 'http://localhost:8080';
  readonly tokenKey = 'acbosque_jwt';

  get token(): string | null {
    return sessionStorage.getItem(this.tokenKey) || localStorage.getItem(this.tokenKey);
  }

  setToken(token: string): void {
    sessionStorage.setItem(this.tokenKey, token);
    localStorage.removeItem(this.tokenKey);
  }

  clearToken(): void {
    sessionStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.tokenKey);
  }

  obtenerRolActual(): string | null {
    const token = this.token;
    if (!token) return null;
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

  obtenerCorreoActual(): string | null {
    const token = this.token;
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => `%${(`00${c.charCodeAt(0).toString(16)}`).slice(-2)}`)
          .join(''),
      );
      return JSON.parse(json)?.sub || null;
    } catch {
      return null;
    }
  }

  async get<T>(endpoint: string, auth = true): Promise<ApiResult<T>> {
    return this.request<T>(endpoint, { method: 'GET' }, auth);
  }

  async post<T>(endpoint: string, body?: unknown, auth = true): Promise<ApiResult<T>> {
    return this.request<T>(endpoint, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) }, auth);
  }

  async put<T>(endpoint: string, body?: unknown): Promise<ApiResult<T>> {
    return this.request<T>(endpoint, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) }, true);
  }

  async delete<T>(endpoint: string): Promise<ApiResult<T>> {
    return this.request<T>(endpoint, { method: 'DELETE' }, true);
  }

  async getBlob(endpoint: string): Promise<Blob | null> {
    const headers: Record<string, string> = {};
    if (this.token) headers['Authorization'] = `Bearer ${this.token}`;
    try {
      const res = await fetch(this.baseUrl + endpoint, { method: 'GET', headers });
      if (!res.ok) return null;
      return await res.blob();
    } catch {
      return null;
    }
  }

  private async request<T>(endpoint: string, init: RequestInit, auth: boolean): Promise<ApiResult<T>> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (auth && this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    try {
      const res = await fetch(this.baseUrl + endpoint, { ...init, headers: { ...headers, ...(init.headers ?? {}) } });

      if (res.status === 401 && auth) {
        this.clearToken();
        window.location.replace('/login');
        return { ok: false, status: 401, data: null, error: 'Sesión terminada' };
      }

      const payload = await res.json().catch(() => ({}));
      const hasWrappedData = payload && Object.prototype.hasOwnProperty.call(payload, 'data');
      const data = (hasWrappedData ? payload.data : payload) as T;
      return {
        ok: res.ok,
        status: res.status,
        data: res.ok ? data : null,
        error: payload?.error || payload?.mensaje || payload?.message,
        mensaje: payload?.mensaje,
      };
    } catch {
      return { ok: false, status: 0, data: null, error: 'No se pudo conectar con el backend' };
    }
  }
}
