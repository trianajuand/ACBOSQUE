import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './auth-card.scss',
})
export class ResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly correo = signal(localStorage.getItem('reset_correo') || '');

  readonly form = this.fb.nonNullable.group({
    token: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
    nuevaContrasenia: ['', [Validators.required, Validators.minLength(8)]],
  });

  async cambiar(): Promise<void> {
    if (!this.correo()) {
      this.toast.mostrar('Solicita primero la recuperacion', 'error');
      this.router.navigateByUrl('/recuperar');
      return;
    }

    if (this.form.invalid) {
      this.toast.mostrar('Revisa el codigo y la nueva contrasenia', 'error');
      return;
    }

    const res = await this.api.post('/api/auth/reset-password', {
      correo: this.correo(),
      ...this.form.getRawValue(),
    }, false);

    if (res.ok) {
      this.toast.mostrar('Contrasenia actualizada', 'success');
      this.router.navigateByUrl('/login');
    } else {
      this.toast.mostrar(res.error || 'No se pudo cambiar la contrasenia', 'error');
    }
  }
}
