import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { LoginResponse } from '../core/models';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './auth-card.scss',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  readonly cargando = signal(false);
  readonly requiereMfa = signal(false);
  readonly mfaToken = signal('');

  readonly form = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasenia: ['', [Validators.required]],
  });

  readonly mfaForm = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
  });

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    if (params.get('stripe_success') === 'true') {
      void this.confirmarPagoStripe(params.get('session_id'));
    } else if (params.get('stripe_cancel') === 'true') {
      this.toast.mostrar('Pago cancelado. Puedes activar premium mas tarde.');
    }
  }

  async login(): Promise<void> {
    if (this.form.invalid) {
      this.toast.mostrar('Revisa el correo y la contrasenia', 'error');
      return;
    }

    this.cargando.set(true);
    const res = await this.api.post<LoginResponse>('/api/auth/login', this.form.getRawValue(), false);
    this.cargando.set(false);

    if (!res.ok || !res.data) {
      this.toast.mostrar(res.error || 'Credenciales incorrectas', 'error');
      return;
    }

    if (res.data.token) {
      this.api.setToken(res.data.token);
      this.toast.mostrar('Sesion iniciada', 'success');
      this.router.navigateByUrl(this.rutaPorRol(res.data.rol));
      return;
    }

    if (res.data.requiereMfa) {
      this.mfaToken.set(res.data.mfaToken || '');
      this.requiereMfa.set(true);
      this.toast.mostrar('Ingresa el codigo MFA enviado al correo');
    }
  }

  async verificarMfa(): Promise<void> {
    if (this.mfaForm.invalid) {
      this.toast.mostrar('El codigo MFA debe tener 6 digitos', 'error');
      return;
    }

    const res = await this.api.post<LoginResponse>('/api/auth/mfa/verify', {
      mfaToken: this.mfaToken(),
      codigo: this.mfaForm.controls.codigo.value,
    }, false);

    if (res.ok && res.data?.token) {
      this.api.setToken(res.data.token);
      this.toast.mostrar('MFA verificado', 'success');
      this.router.navigateByUrl(this.rutaPorRol(res.data.rol));
    } else {
      this.toast.mostrar(res.error || 'Codigo MFA incorrecto', 'error');
    }
  }

  private async confirmarPagoStripe(sessionId: string | null): Promise<void> {
    if (!sessionId) {
      this.toast.mostrar('Stripe retorno sin session_id', 'error');
      return;
    }

    const res = await this.api.get(`/api/suscripciones/confirmar-checkout?session_id=${encodeURIComponent(sessionId)}`, false);
    this.toast.mostrar(res.ok ? 'Pago premium confirmado' : res.error || 'No se pudo confirmar Stripe', res.ok ? 'success' : 'error');
  }

  private rutaPorRol(rol?: string): string {
    if (rol === 'COMISIONISTA') return '/comisionista';
    if (rol === 'ADMINISTRADOR') return '/admin';
    return '/dashboard';
  }
}
