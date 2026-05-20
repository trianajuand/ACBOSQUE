import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-recuperar',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './recuperar.component.html',
  styleUrl: './auth-card.scss',
})
export class RecuperarComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
  });

  async enviar(): Promise<void> {
    if (this.form.invalid) {
      this.toast.mostrar('Ingresa un correo valido', 'error');
      return;
    }

    const correo = this.form.controls.correo.value;
    const res = await this.api.post('/api/auth/forgot-password', { correo }, false);
    if (res.ok) {
      localStorage.setItem('reset_correo', correo);
      this.toast.mostrar('Codigo de recuperacion enviado', 'success');
      this.router.navigateByUrl('/reset-password');
    } else {
      this.toast.mostrar(res.error || 'No se pudo enviar el codigo', 'error');
    }
  }
}
