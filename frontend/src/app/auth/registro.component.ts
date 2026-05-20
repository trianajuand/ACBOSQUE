import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ToastService } from '../core/toast.service';

type Plan = 'BASICO' | 'PREMIUM_MENSUAL' | 'PREMIUM_ANUAL';

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrl: './auth-card.scss',
})
export class RegistroComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly plan = signal<Plan>('BASICO');
  readonly cargando = signal(false);

  readonly form = this.fb.nonNullable.group({
    nombreCompleto: ['', [Validators.required]],
    correo: ['', [Validators.required, Validators.email]],
    contrasenia: ['', [Validators.required, Validators.minLength(8)]],
    telefono: [''],
    nivelExperiencia: ['PRINCIPIANTE'],
    interesesMercado: ['AAPL, MSFT, TSLA'],
  });

  seleccionarPlan(plan: Plan): void {
    this.plan.set(plan);
  }

  async registrar(): Promise<void> {
    if (this.form.invalid) {
      this.toast.mostrar('Completa los datos obligatorios', 'error');
      return;
    }

    const raw = this.form.getRawValue();
    this.cargando.set(true);
    const res = await this.api.post('/api/auth/register/investor', {
      ...raw,
      telefono: raw.telefono || null,
      interesesMercado: raw.interesesMercado.split(',').map((s) => s.trim()).filter(Boolean),
      planSuscripcion: this.plan(),
    }, false);
    this.cargando.set(false);

    if (res.ok) {
      localStorage.setItem('reg_correo', raw.correo);
      this.toast.mostrar('Codigo de verificacion enviado', 'success');
      this.router.navigateByUrl('/verificar-registro');
    } else {
      this.toast.mostrar(res.error || 'No se pudo registrar la cuenta', 'error');
    }
  }
}
