import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ConfirmarRegistroResponse } from '../core/models';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-verificar-registro',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './verificar-registro.component.html',
  styleUrl: './auth-card.scss',
})
export class VerificarRegistroComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly correo = signal(localStorage.getItem('reg_correo') || '');
  readonly stripeCheckoutUrl = signal('');

  readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
  });

  async verificar(): Promise<void> {
    if (!this.correo()) {
      this.toast.mostrar('Primero completa el registro', 'error');
      this.router.navigateByUrl('/registro');
      return;
    }

    const res = await this.api.post<ConfirmarRegistroResponse>('/api/auth/register/confirm', {
      correo: this.correo(),
      codigo: this.form.controls.codigo.value,
    }, false);

    if (!res.ok || !res.data) {
      this.toast.mostrar(res.error || 'Codigo incorrecto', 'error');
      return;
    }

    if (res.data.requierePago && res.data.stripeCheckoutUrl) {
      this.stripeCheckoutUrl.set(res.data.stripeCheckoutUrl);
      this.toast.mostrar('Cuenta activada. Completa el pago premium.', 'success');
      return;
    }

    this.toast.mostrar('Cuenta activada. Ya puedes iniciar sesion.', 'success');
    this.router.navigateByUrl('/login');
  }

  pagarStripe(): void {
    if (this.stripeCheckoutUrl()) {
      window.location.href = this.stripeCheckoutUrl();
    }
  }
}
