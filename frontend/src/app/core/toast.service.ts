import { Injectable, signal } from '@angular/core';

export type ToastTipo = 'info' | 'success' | 'error';

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly mensaje = signal('');
  readonly tipo = signal<ToastTipo>('info');
  private timer: ReturnType<typeof setTimeout> | null = null;

  mostrar(mensaje: string, tipo: ToastTipo = 'info'): void {
    this.mensaje.set(mensaje);
    this.tipo.set(tipo);
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.timer = setTimeout(() => this.mensaje.set(''), 3600);
  }
}
