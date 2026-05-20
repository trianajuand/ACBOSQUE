import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { CorreoDisponibleResponse } from '../core/models';
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
  readonly validandoCorreo = signal(false);
  readonly pasoActual = signal(0);

  readonly pasos = [
    { titulo: 'Datos e identidad', descripcion: 'Acceso, documento y direccion de residencia.' },
    { titulo: 'Perfil financiero', descripcion: 'Mercados, ingresos, experiencia y estilo de trading.' },
    { titulo: 'Preferencias', descripcion: 'Ordenes, portafolio, notificaciones y comisionista.' },
    { titulo: 'Plan', descripcion: 'Selecciona como quieres empezar en Acciones ElBosque.' },
  ];

  readonly mercadoOpciones = [
    { simbolo: 'AAPL', nombre: 'Apple' },
    { simbolo: 'MSFT', nombre: 'Microsoft' },
    { simbolo: 'TSLA', nombre: 'Tesla' },
    { simbolo: 'AMZN', nombre: 'Amazon' },
    { simbolo: 'GOOGL', nombre: 'Alphabet' },
    { simbolo: 'NVDA', nombre: 'NVIDIA' },
    { simbolo: 'META', nombre: 'Meta' },
    { simbolo: 'JPM', nombre: 'JPMorgan' },
    { simbolo: 'SONY', nombre: 'Sony' },
    { simbolo: 'RIO.LON', nombre: 'Rio Tinto LSE' },
  ];

  readonly form = this.fb.nonNullable.group({
    nombreCompleto: ['', [Validators.required]],
    correo: ['', [Validators.required, Validators.email]],
    contrasenia: ['', [Validators.required, Validators.minLength(8)]],
    telefono: [''],
    tipoIdentificacion: ['CC', [Validators.required]],
    numeroIdentificacion: ['', [Validators.required]],
    fechaNacimiento: [''],
    direccion: ['', [Validators.required]],
    ciudad: ['', [Validators.required]],
    codigoPostal: ['', [Validators.required]],
    pais: ['CO'],
    interesesMercado: [['AAPL', 'MSFT', 'TSLA'], [Validators.required]],
    estiloTrading: ['INTRADIA', [Validators.required]],
    rangoIngresos: ['2M_5M', [Validators.required]],
    nivelExperiencia: ['PRINCIPIANTE', [Validators.required]],
    tipoOrdenDefault: ['MARKET', [Validators.required]],
    vistaPortafolio: ['LISTA', [Validators.required]],
    notificacionEmail: [true],
    notificacionSms: [false],
    notificacionWhatsapp: [false],
    solicitaComisionista: [true],
  });

  private readonly controlesPorPaso: Array<Array<keyof typeof this.form.controls>> = [
    [
      'nombreCompleto',
      'correo',
      'contrasenia',
      'tipoIdentificacion',
      'numeroIdentificacion',
      'direccion',
      'ciudad',
      'codigoPostal',
    ],
    ['interesesMercado', 'estiloTrading', 'rangoIngresos', 'nivelExperiencia'],
    ['tipoOrdenDefault', 'vistaPortafolio'],
    [],
  ];

  seleccionarPlan(plan: Plan): void {
    this.plan.set(plan);
  }

  interesSeleccionado(simbolo: string): boolean {
    return this.form.controls.interesesMercado.value.includes(simbolo);
  }

  alternarInteres(simbolo: string): void {
    const actuales = this.form.controls.interesesMercado.value;
    const intereses = actuales.includes(simbolo)
      ? actuales.filter((interes) => interes !== simbolo)
      : [...actuales, simbolo];

    this.form.controls.interesesMercado.setValue(intereses);
    this.form.controls.interesesMercado.markAsDirty();
  }

  async continuar(): Promise<void> {
    if (!this.pasoValido()) {
      this.toast.mostrar('Completa los campos de esta fase', 'error');
      return;
    }
    if (this.pasoActual() === 0 && !(await this.validarCorreoDisponible())) {
      return;
    }
    this.pasoActual.update((paso) => Math.min(paso + 1, this.pasos.length - 1));
  }

  regresar(): void {
    this.pasoActual.update((paso) => Math.max(paso - 1, 0));
  }

  async registrar(): Promise<void> {
    if (!this.pasoValido() || this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.mostrar('Revisa los datos antes de crear la cuenta', 'error');
      return;
    }

    const raw = this.form.getRawValue();
    this.cargando.set(true);
    const res = await this.api.post('/api/auth/register/investor', {
      ...raw,
      telefono: raw.telefono || null,
      fechaNacimiento: raw.fechaNacimiento || null,
      pais: raw.pais || 'CO',
      interesesMercado: raw.interesesMercado,
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

  private pasoValido(): boolean {
    const controles = this.controlesPorPaso[this.pasoActual()] || [];
    controles.forEach((nombre) => this.form.controls[nombre].markAsTouched());
    return controles.every((nombre) => this.form.controls[nombre].valid);
  }

  private async validarCorreoDisponible(): Promise<boolean> {
    const correoControl = this.form.controls.correo;
    const correo = correoControl.value.trim();
    if (correoControl.invalid) {
      return false;
    }

    this.validandoCorreo.set(true);
    const res = await this.api.get<CorreoDisponibleResponse>(
      `/api/auth/register/email-disponible?correo=${encodeURIComponent(correo)}`,
      false,
    );
    this.validandoCorreo.set(false);

    if (!res.ok || !res.data) {
      this.toast.mostrar(res.error || 'No se pudo validar el correo', 'error');
      return false;
    }

    if (!res.data.disponible) {
      correoControl.setErrors({ correoUsado: true });
      correoControl.markAsTouched();
      this.toast.mostrar('El correo ingresado ya esta en uso', 'error');
      return false;
    }

    return true;
  }
}
