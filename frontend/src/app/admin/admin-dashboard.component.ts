import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import {
  DashboardEjecutivo,
  FeriadoMercado,
  MercadoAdmin,
  ParametroComision,
  UsuarioAdmin,
} from '../core/models';
import { ToastService } from '../core/toast.service';

type PanelAdmin = 'dashboard' | 'mercados' | 'comisiones' | 'usuarios';

@Component({
  selector: 'app-admin-dashboard',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly panel = signal<PanelAdmin>('dashboard');
  readonly cargando = signal(true);
  readonly metricas = signal<DashboardEjecutivo | null>(null);
  readonly mercados = signal<MercadoAdmin[]>([]);
  readonly mercadoSeleccionado = signal<MercadoAdmin | null>(null);
  readonly feriados = signal<FeriadoMercado[]>([]);
  readonly comisiones = signal<ParametroComision | null>(null);
  readonly usuarios = signal<UsuarioAdmin[]>([]);
  readonly errorCarga = signal('');

  private readonly correoActual = this.api.obtenerCorreoActual();

  readonly inversionistas = computed(() => this.usuarios().filter((u) => u.rol?.startsWith('INVERSIONISTA')));
  readonly comisionistas = computed(() => this.usuarios().filter((u) => u.rol === 'COMISIONISTA'));
  readonly usuariosCicloVida = computed(() => this.usuarios().filter((u) => u.correo !== this.correoActual));
  readonly totalMercadosHabilitados = computed(() => this.mercados().filter((m) => m.habilitado).length);
  readonly usuariosRestringidos = computed(() => this.usuarios().filter((u) => u.estadoCuenta === 'OPERACIONES_RESTRINGIDAS').length);

  readonly filtroMetricasForm = this.fb.nonNullable.group({
    desde: [''],
    hasta: [''],
    mercado: [''],
  });

  readonly mercadoForm = this.fb.nonNullable.group({
    codigo: ['', [Validators.required]],
    nombre: ['', [Validators.required]],
    zonaHoraria: ['', [Validators.required]],
    horaApertura: ['', [Validators.required]],
    horaCierre: ['', [Validators.required]],
    habilitado: [true],
    cierreAnticipado: [''],
  });

  readonly feriadoForm = this.fb.nonNullable.group({
    fecha: ['', [Validators.required]],
    descripcion: ['', [Validators.required]],
  });

  readonly comisionForm = this.fb.nonNullable.group({
    porcentajeComision: [2, [Validators.required, Validators.min(0), Validators.max(100)]],
    splitPlataforma: [60, [Validators.required, Validators.min(0), Validators.max(100)]],
    splitComisionista: [40, [Validators.required, Validators.min(0), Validators.max(100)]],
  });

  readonly comisionistaForm = this.fb.nonNullable.group({
    nombreCompleto: ['', [Validators.required]],
    correo: ['', [Validators.required, Validators.email]],
    contrasenia: ['', [Validators.required, Validators.minLength(8)]],
    especialidadesMercado: ['AAPL,MSFT,TSLA'],
  });

  async ngOnInit(): Promise<void> {
    const rol = this.api.obtenerRolActual();
    if (rol !== 'ADMINISTRADOR') {
      await this.router.navigateByUrl(rol === 'COMISIONISTA' ? '/comisionista' : '/dashboard');
      return;
    }

    try {
      await Promise.all([this.cargarMetricas(), this.cargarMercados(), this.cargarComisiones(), this.cargarUsuarios()]);
    } finally {
      this.cargando.set(false);
    }
  }

  seleccionar(panel: PanelAdmin): void {
    this.panel.set(panel);
  }

  async actualizar(): Promise<void> {
    await Promise.all([this.cargarMetricas(), this.cargarUsuarios()]);
  }

  async cargarMetricas(): Promise<void> {
    const raw = this.filtroMetricasForm.getRawValue();
    const params = new URLSearchParams();
    if (raw.desde) params.set('desde', raw.desde);
    if (raw.hasta) params.set('hasta', raw.hasta);
    if (raw.mercado) params.set('mercado', raw.mercado);
    const query = params.toString() ? `?${params.toString()}` : '';
    const res = await this.api.get<DashboardEjecutivo>(`/api/admin/dashboard${query}`);
    if (res.ok && res.data) {
      this.metricas.set(res.data);
      this.errorCarga.set('');
    } else {
      this.errorCarga.set(res.error || 'No se pudo cargar el dashboard administrativo');
      this.toast.mostrar(this.errorCarga(), 'error');
    }
  }

  async cargarMercados(): Promise<void> {
    const res = await this.api.get<MercadoAdmin[]>('/api/admin/mercados');
    if (res.ok && res.data) {
      this.mercados.set(res.data);
      if (!this.mercadoSeleccionado() && res.data.length) this.editarMercado(res.data[0]);
    } else if (!res.ok) {
      this.toast.mostrar(res.error || 'No se pudieron cargar mercados', 'error');
    }
  }

  editarMercado(mercado: MercadoAdmin): void {
    this.mercadoSeleccionado.set(mercado);
    this.mercadoForm.setValue({
      codigo: mercado.codigo,
      nombre: mercado.nombre,
      zonaHoraria: mercado.zonaHoraria,
      horaApertura: mercado.horaApertura,
      horaCierre: mercado.horaCierre,
      habilitado: mercado.habilitado,
      cierreAnticipado: mercado.cierreAnticipado || '',
    });
    void this.cargarFeriados(mercado.codigo);
  }

  async guardarMercado(): Promise<void> {
    if (this.mercadoForm.invalid) {
      this.toast.mostrar('Completa los datos del mercado', 'error');
      return;
    }
    const raw = this.mercadoForm.getRawValue();
    const payload = { ...raw, cierreAnticipado: raw.cierreAnticipado || null };
    const res = await this.api.put<MercadoAdmin>(`/api/admin/mercados/${encodeURIComponent(raw.codigo)}`, payload);
    this.toast.mostrar(res.ok ? 'Mercado actualizado' : res.error || 'No se pudo guardar', res.ok ? 'success' : 'error');
    if (res.ok) await this.cargarMercados();
  }

  async cargarFeriados(codigo: string): Promise<void> {
    const res = await this.api.get<FeriadoMercado[]>(`/api/admin/mercados/${encodeURIComponent(codigo)}/feriados`);
    if (res.ok && res.data) this.feriados.set(res.data);
  }

  async agregarFeriado(): Promise<void> {
    const mercado = this.mercadoSeleccionado();
    if (!mercado || this.feriadoForm.invalid) {
      this.toast.mostrar('Selecciona mercado y completa el feriado', 'error');
      return;
    }
    const res = await this.api.post<FeriadoMercado>(`/api/admin/mercados/${encodeURIComponent(mercado.codigo)}/feriados`, this.feriadoForm.getRawValue());
    this.toast.mostrar(res.ok ? 'Feriado agregado' : res.error || 'No se pudo agregar', res.ok ? 'success' : 'error');
    if (res.ok) {
      this.feriadoForm.reset();
      await this.cargarFeriados(mercado.codigo);
    }
  }

  async eliminarFeriado(id?: number): Promise<void> {
    const mercado = this.mercadoSeleccionado();
    if (!mercado || !id) return;
    const res = await this.api.delete(`/api/admin/mercados/${encodeURIComponent(mercado.codigo)}/feriados/${id}`);
    this.toast.mostrar(res.ok ? 'Feriado eliminado' : res.error || 'No se pudo eliminar', res.ok ? 'success' : 'error');
    if (res.ok) await this.cargarFeriados(mercado.codigo);
  }

  async cargarComisiones(): Promise<void> {
    const res = await this.api.get<ParametroComision>('/api/admin/comisiones');
    if (res.ok && res.data) {
      this.comisiones.set(res.data);
      this.comisionForm.setValue({
        porcentajeComision: Number(res.data.porcentajeComision),
        splitPlataforma: Number(res.data.splitPlataforma),
        splitComisionista: Number(res.data.splitComisionista),
      });
    } else if (!res.ok) {
      this.toast.mostrar(res.error || 'No se pudieron cargar comisiones', 'error');
    }
  }

  async guardarComisiones(): Promise<void> {
    if (this.comisionForm.invalid) return;
    const raw = this.comisionForm.getRawValue();
    if (Number(raw.splitPlataforma) + Number(raw.splitComisionista) !== 100) {
      this.toast.mostrar('El split debe sumar 100%', 'error');
      return;
    }
    const res = await this.api.put<ParametroComision>('/api/admin/comisiones', raw);
    this.toast.mostrar(res.ok ? 'Comisiones actualizadas' : res.error || 'No se pudo actualizar', res.ok ? 'success' : 'error');
    if (res.ok) await this.cargarComisiones();
  }

  async cargarUsuarios(): Promise<void> {
    const res = await this.api.get<UsuarioAdmin[]>('/api/admin/usuarios');
    if (res.ok && res.data) {
      this.usuarios.set(res.data);
    } else if (!res.ok) {
      this.toast.mostrar(res.error || 'No se pudieron cargar usuarios', 'error');
    }
  }

  async crearComisionista(): Promise<void> {
    if (this.comisionistaForm.invalid) {
      this.toast.mostrar('Completa el comisionista', 'error');
      return;
    }
    const res = await this.api.post<UsuarioAdmin>('/api/admin/comisionistas', this.comisionistaForm.getRawValue());
    this.toast.mostrar(res.ok ? 'Comisionista creado con MFA obligatorio' : res.error || 'No se pudo crear', res.ok ? 'success' : 'error');
    if (res.ok) {
      this.comisionistaForm.reset({ especialidadesMercado: 'AAPL,MSFT,TSLA' });
      await this.cargarUsuarios();
    }
  }

  async asignarComisionista(inversionistaId: number, comisionistaIdRaw: string): Promise<void> {
    const comisionistaId = Number(comisionistaIdRaw);
    if (!comisionistaId) return;
    const res = await this.api.put(`/api/admin/inversionistas/${inversionistaId}/comisionista/${comisionistaId}`, {});
    this.toast.mostrar(res.ok ? 'Comisionista asignado' : res.error || 'No se pudo asignar', res.ok ? 'success' : 'error');
    if (res.ok) await this.cargarUsuarios();
  }

  async cambiarEstado(usuario: UsuarioAdmin, estado: string): Promise<void> {
    const res = await this.api.put<UsuarioAdmin>(`/api/admin/usuarios/${usuario.id}/estado`, { estado });
    this.toast.mostrar(res.ok ? 'Estado actualizado' : res.error || 'No se pudo cambiar estado', res.ok ? 'success' : 'error');
    if (res.ok) await this.cargarUsuarios();
  }

  async eliminarUsuario(usuario: UsuarioAdmin): Promise<void> {
    const res = await this.api.delete(`/api/admin/usuarios/${usuario.id}`);
    this.toast.mostrar(res.ok ? 'Cuenta dada de baja' : res.error || 'No se pudo dar de baja', res.ok ? 'success' : 'error');
    if (res.ok) await this.cargarUsuarios();
  }

  async cerrarSesion(): Promise<void> {
    await this.api.post('/api/auth/logout').catch(() => undefined);
    this.api.clearToken();
    this.router.navigateByUrl('/login');
  }

  dinero(valor?: number | null): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(valor || 0));
  }

  tituloPanel(): string {
    const titulos: Record<PanelAdmin, string> = {
      dashboard: 'Dashboard ejecutivo',
      mercados: 'Mercados y calendario',
      comisiones: 'Comisiones futuras',
      usuarios: 'Usuarios y accesos',
    };
    return titulos[this.panel()];
  }

  estadoClase(estado?: string): string {
    if (estado === 'ACTIVA') return 'ok';
    if (estado === 'OPERACIONES_RESTRINGIDAS') return 'warn';
    return 'danger';
  }

  anchoTendencia(valor?: number): string {
    const total = Math.max(...(this.metricas()?.tendenciasPorMercado || []).map((t) => Number(t.volumen || 0)), 1);
    return `${Math.max(4, Math.round((Number(valor || 0) / total) * 100))}%`;
  }
}
