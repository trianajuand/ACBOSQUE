import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Cotizacion, DetalleAccion, Orden, Perfil, Portafolio, ResumenComision, Saldo } from '../core/models';
import { ToastService } from '../core/toast.service';

type Panel = 'dashboard' | 'mercado' | 'ordenes' | 'portafolio' | 'propuestas' | 'perfil' | 'configuracion';

interface SymbolOption {
  simbolo: string;
  nombre: string;
  grupo: string;
  mercado: string;
}

const NOMBRES_EMPRESAS: Record<string, string> = {
  AAPL: 'Apple Inc.',
  MSFT: 'Microsoft Corp.',
  GOOGL: 'Alphabet Inc.',
  NVDA: 'NVIDIA Corp.',
  META: 'Meta Platforms',
  AMZN: 'Amazon.com Inc.',
  NFLX: 'Netflix Inc.',
  AMD: 'Advanced Micro Devices',
  INTC: 'Intel Corp.',
  CRM: 'Salesforce Inc.',
  JPM: 'JPMorgan Chase',
  BAC: 'Bank of America',
  GS: 'Goldman Sachs',
  MS: 'Morgan Stanley',
  V: 'Visa Inc.',
  MA: 'Mastercard Inc.',
  AXP: 'American Express',
  WFC: 'Wells Fargo',
  C: 'Citigroup',
  BLK: 'BlackRock',
  JNJ: 'Johnson & Johnson',
  UNH: 'UnitedHealth Group',
  PFE: 'Pfizer',
  ABBV: 'AbbVie',
  MRK: 'Merck & Co.',
  LLY: 'Eli Lilly',
  TMO: 'Thermo Fisher',
  ABT: 'Abbott Laboratories',
  BMY: 'Bristol Myers Squibb',
  AMGN: 'Amgen',
  XOM: 'Exxon Mobil',
  CVX: 'Chevron',
  COP: 'ConocoPhillips',
  SLB: 'Schlumberger',
  EOG: 'EOG Resources',
  PXD: 'Pioneer Natural Resources',
  MPC: 'Marathon Petroleum',
  VLO: 'Valero Energy',
  PSX: 'Phillips 66',
  HAL: 'Halliburton',
  TSLA: 'Tesla Inc.',
  NKE: 'Nike Inc.',
  MCD: "McDonald's",
  SBUX: 'Starbucks',
  TGT: 'Target',
  HD: 'Home Depot',
  LOW: "Lowe's",
  COST: 'Costco',
  WMT: 'Walmart',
  DIS: 'Walt Disney',
  'HSBA.L': 'HSBC Holdings',
  'BP.L': 'BP plc',
  'SHEL.L': 'Shell plc',
  'AZN.L': 'AstraZeneca',
  'GSK.L': 'GSK plc',
  'ULVR.L': 'Unilever',
  'RIO.L': 'Rio Tinto',
  'BT.L': 'BT Group',
  '7203.T': 'Toyota Motor',
  '6758.T': 'Sony Group',
  '9984.T': 'SoftBank Group',
  '8306.T': 'Mitsubishi UFJ',
  '6861.T': 'Keyence',
  '4502.T': 'Takeda Pharmaceutical',
  '6501.T': 'Hitachi',
  'BHP.AX': 'BHP Group',
  'CBA.AX': 'Commonwealth Bank',
  'CSL.AX': 'CSL Limited',
  'NAB.AX': 'National Australia Bank',
  'WBC.AX': 'Westpac',
  'ANZ.AX': 'ANZ Group',
  'RIO.AX': 'Rio Tinto',
};

@Component({
  selector: 'app-dashboard',
  imports: [ReactiveFormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private refrescoOperativoId?: number;
  private relojId?: number;

  readonly panel = signal<Panel>('dashboard');
  readonly perfil = signal<Perfil | null>(null);
  readonly cotizaciones = signal<Cotizacion[]>([]);
  readonly busqueda = signal<Cotizacion | null>(null);
  readonly detalleSeleccionado = signal<DetalleAccion | null>(null);
  readonly catalogo = signal<Record<string, string[]>>({});
  readonly ordenes = signal<Orden[]>([]);
  readonly activas = signal<Orden[]>([]);
  readonly propuestas = signal<Orden[]>([]);
  readonly portafolio = signal<Portafolio | null>(null);
  readonly saldo = signal<Saldo | null>(null);
  readonly resumen = signal<ResumenComision | null>(null);
  readonly cargando = signal(true);
  readonly reloj = signal('');
  readonly editandoPerfil = signal(false);

  readonly totalInvertido = computed(() =>
    (this.portafolio()?.valorTotalPortafolio || 0) - (this.portafolio()?.gananciaPerdidaTotal || 0),
  );

  readonly simbolos = computed<SymbolOption[]>(() =>
    Object.entries(this.catalogo()).flatMap(([grupo, simbolos]) =>
      simbolos.map((simbolo) => ({
        simbolo,
        nombre: this.nombreEmpresa(simbolo),
        grupo,
        mercado: this.mercadoInternacional(simbolo),
      })),
    ),
  );

  readonly simbolosPreferidos = computed<SymbolOption[]>(() => {
    const intereses = new Set((this.perfil()?.interesesMercado || []).map((s) => s.toUpperCase()));
    const porInteres = this.simbolos().filter((s) => intereses.has(s.simbolo.toUpperCase()));
    if (porInteres.length) return porInteres;
    return this.cotizaciones().map((c) => ({
      simbolo: c.simbolo,
      nombre: c.nombreEmpresa || this.nombreEmpresa(c.simbolo),
      grupo: 'Preferencias',
      mercado: c.mercado || this.mercadoInternacional(c.simbolo),
    }));
  });

  readonly otrosMercados = computed(() => {
    const preferidos = new Set(this.simbolosPreferidos().map((s) => s.simbolo));
    return Object.entries(this.catalogo()).map(([grupo, simbolos]) => ({
      grupo,
      simbolos: simbolos
        .filter((simbolo) => !preferidos.has(simbolo))
        .map((simbolo) => ({
          simbolo,
          nombre: this.nombreEmpresa(simbolo),
          grupo,
          mercado: this.mercadoInternacional(simbolo),
        })),
    })).filter((g) => g.simbolos.length > 0);
  });

  readonly puntosGrafica = computed(() => {
    const historico = this.detalleSeleccionado()?.historicoPrecios || [];
    const valores = historico
      .slice(0, 18)
      .reverse()
      .map((p) => Number(p['cierre'] || p['close'] || p['4. close'] || p['precioCierre'] || 0))
      .filter((v) => Number.isFinite(v) && v > 0);

    const fallback = [
      this.detalleSeleccionado()?.precioCierreAnterior,
      this.detalleSeleccionado()?.precioApertura,
      this.detalleSeleccionado()?.precioMinimo,
      this.detalleSeleccionado()?.precioActual,
      this.detalleSeleccionado()?.precioMaximo,
    ].map((v) => Number(v || 0)).filter((v) => v > 0);

    const data = valores.length >= 2 ? valores : fallback;
    if (data.length < 2) return '';
    const min = Math.min(...data);
    const max = Math.max(...data);
    const span = max - min || 1;
    return data.map((valor, i) => {
      const x = (i / (data.length - 1)) * 100;
      const y = 100 - ((valor - min) / span) * 82 - 9;
      return `${x.toFixed(2)},${y.toFixed(2)}`;
    }).join(' ');
  });

  readonly areaGrafica = computed(() => {
    const puntos = this.puntosGrafica();
    return puntos ? `0,100 ${puntos} 100,100` : '';
  });

  readonly graficaPositiva = computed(() =>
    Number(this.detalleSeleccionado()?.variacionPorcentual || this.busqueda()?.variacionPorcentual || 0) >= 0,
  );

  readonly statsDetalle = computed(() => {
    const d = this.detalleSeleccionado();
    const b = this.busqueda();
    return [
      ['Apertura', this.dinero(d?.precioApertura || b?.precioApertura)],
      ['Cierre ant.', this.dinero(d?.precioCierreAnterior || b?.precioCierreAnterior)],
      ['Maximo', this.dinero(d?.precioMaximo || b?.precioMaximo)],
      ['Minimo', this.dinero(d?.precioMinimo || b?.precioMinimo)],
      ['Volumen', String(d?.volumen || b?.volumen || 0)],
      ['Sector', d?.sector || '-'],
    ];
  });

  readonly orderForm = this.fb.nonNullable.group({
    simbolo: ['AAPL', [Validators.required]],
    tipoOrden: ['MARKET', [Validators.required]],
    lado: ['COMPRA', [Validators.required]],
    cantidad: [1, [Validators.required, Validators.min(0.000001)]],
    precioLimite: [0],
    precioStop: [0],
  });

  readonly depositoForm = this.fb.nonNullable.group({
    monto: [1000, [Validators.required, Validators.min(0.01)]],
  });

  readonly buscarForm = this.fb.nonNullable.group({
    simbolo: ['AAPL', [Validators.required]],
  });

  readonly perfilForm = this.fb.nonNullable.group({
    nombreCompleto: [''],
    nivelExperiencia: ['PRINCIPIANTE'],
    telefono: [''],
    interesesMercado: [''],
  });

  readonly preferenciasForm = this.fb.nonNullable.group({
    notificacionEmail: [true],
    notificacionSms: [false],
    notificacionWhatsapp: [false],
    tipoOrdenDefault: ['MARKET'],
    vistaPortafolio: ['LISTA'],
  });

  readonly historialFiltrosForm = this.fb.nonNullable.group({
    desde: [''],
    hasta: [''],
    tipoOrden: [''],
    simbolo: [''],
    estado: [''],
  });

  async ngOnInit(): Promise<void> {
    const rol = this.api.obtenerRolActual();
    if (rol === 'ADMINISTRADOR') {
      await this.router.navigateByUrl('/admin');
      return;
    }
    if (rol === 'COMISIONISTA') {
      await this.router.navigateByUrl('/comisionista');
      return;
    }
    this.tick();
    this.relojId = window.setInterval(() => this.tick(), 1000);
    await this.cargarTodo();
    this.refrescoOperativoId = window.setInterval(() => void this.refrescarOperativo(), 10000);
  }

  ngOnDestroy(): void {
    if (this.refrescoOperativoId) {
      window.clearInterval(this.refrescoOperativoId);
    }
    if (this.relojId) {
      window.clearInterval(this.relojId);
    }
  }

  async cargarTodo(): Promise<void> {
    this.cargando.set(true);
    await Promise.all([
      this.cargarPerfil(),
      this.cargarCatalogo(),
      this.cargarMercado(),
      this.cargarSaldo(),
      this.cargarPortafolio(),
      this.cargarOrdenes(),
      this.cargarPropuestas(),
    ]);
    this.cargando.set(false);
  }

  async refrescarOperativo(): Promise<void> {
    await Promise.all([
      this.cargarPropuestas(),
      this.cargarOrdenes(),
      this.cargarSaldo(),
      this.cargarPortafolio(),
    ]);
  }

  seleccionar(panel: Panel): void {
    this.panel.set(panel);
    if (panel === 'perfil' || panel === 'configuracion') {
      void this.cargarPerfil();
    }
  }

  async cargarPerfil(): Promise<void> {
    const res = await this.api.get<Perfil>('/api/perfil');
    if (!res.ok || !res.data) {
      this.toast.mostrar(res.error || 'No se pudo cargar el perfil', 'error');
      return;
    }
    const p = res.data;
    this.perfil.set(p);
    this.perfilForm.patchValue({
      nombreCompleto: p.nombreCompleto || '',
      nivelExperiencia: p.nivelExperiencia || 'PRINCIPIANTE',
      telefono: p.telefono || '',
      interesesMercado: (p.interesesMercado || []).join(', '),
    });
    this.preferenciasForm.patchValue({
      notificacionEmail: !!p.notificacionEmail,
      notificacionSms: !!p.notificacionSms,
      notificacionWhatsapp: !!p.notificacionWhatsapp,
      tipoOrdenDefault: p.tipoOrdenDefault || 'MARKET',
      vistaPortafolio: p.vistaPortafolio || 'LISTA',
    });
  }

  async cargarMercado(): Promise<void> {
    const res = await this.api.get<Cotizacion[]>('/api/mercado/dashboard');
    if (res.ok && res.data) {
      this.cotizaciones.set(res.data);
    }
  }

  async cargarCatalogo(): Promise<void> {
    const res = await this.api.get<Record<string, string[]>>('/api/mercado/simbolos', false);
    if (res.ok && res.data) {
      this.catalogo.set(res.data);
    }
  }

  async buscarCotizacion(): Promise<void> {
    const simbolo = this.buscarForm.controls.simbolo.value.trim().toUpperCase();
    await this.seleccionarSimboloMercado(simbolo);
  }

  async seleccionarSimboloMercado(simbolo: string): Promise<void> {
    this.buscarForm.patchValue({ simbolo });
    const res = await this.api.get<Cotizacion>(`/api/mercado/cotizacion/${encodeURIComponent(simbolo)}`);
    if (res.ok && res.data) {
      this.busqueda.set({
        ...res.data,
        nombreEmpresa: res.data.nombreEmpresa || this.nombreEmpresa(res.data.simbolo),
      });
      await this.cargarDetalle(simbolo);
      this.toast.mostrar(`Cotizacion ${simbolo} actualizada`, 'success');
    } else {
      this.toast.mostrar(res.error || 'No se pudo consultar el simbolo', 'error');
    }
  }

  async cargarDetalle(simbolo: string): Promise<void> {
    const res = await this.api.get<DetalleAccion>(`/api/mercado/detalle/${encodeURIComponent(simbolo)}`);
    if (res.ok && res.data) {
      this.detalleSeleccionado.set({
        ...res.data,
        nombreEmpresa: res.data.nombreEmpresa || this.nombreEmpresa(res.data.simbolo),
      });
    }
  }

  seleccionarSimboloOrden(simbolo: string): void {
    this.orderForm.patchValue({ simbolo });
    this.resumen.set(null);
  }

  async cargarSaldo(): Promise<void> {
    const res = await this.api.get<Saldo>('/api/portafolio/saldo');
    if (res.ok && res.data) {
      this.saldo.set(res.data);
    }
  }

  async cargarPortafolio(): Promise<void> {
    const res = await this.api.get<Portafolio>('/api/portafolio');
    if (res.ok && res.data) {
      this.portafolio.set(res.data);
    }
  }

  async cargarOrdenes(): Promise<void> {
    const historialUrl = this.historialUrl();
    const [historial, activas] = await Promise.all([
      this.api.get<Orden[]>(historialUrl),
      this.api.get<Orden[]>('/api/ordenes/activas'),
    ]);
    if (historial.ok && historial.data) {
      this.ordenes.set(historial.data);
    }
    if (activas.ok && activas.data) {
      this.activas.set(activas.data);
    }
  }

  async aplicarFiltrosHistorial(): Promise<void> {
    await this.cargarOrdenes();
  }

  async limpiarFiltrosHistorial(): Promise<void> {
    this.historialFiltrosForm.reset({
      desde: '',
      hasta: '',
      tipoOrden: '',
      simbolo: '',
      estado: '',
    });
    await this.cargarOrdenes();
  }

  async cargarPropuestas(): Promise<void> {
    const res = await this.api.get<Orden[]>('/api/propuestas');
    if (res.ok && res.data) {
      this.propuestas.set(res.data);
    }
  }

  private historialUrl(): string {
    const raw = this.historialFiltrosForm.getRawValue();
    const params = new URLSearchParams();
    if (raw.desde) params.set('desde', raw.desde);
    if (raw.hasta) params.set('hasta', raw.hasta);
    if (raw.tipoOrden) params.set('tipoOrden', raw.tipoOrden);
    if (raw.simbolo.trim()) params.set('simbolo', raw.simbolo.trim().toUpperCase());
    if (raw.estado) params.set('estado', raw.estado);
    const query = params.toString();
    return query ? `/api/ordenes/historial?${query}` : '/api/ordenes/historial';
  }

  setTipoOrden(tipo: string): void {
    let lado = this.orderForm.controls.lado.value;
    if (tipo === 'STOP_LOSS' || tipo === 'TAKE_PROFIT') {
      lado = 'VENTA';
    }
    this.orderForm.patchValue({ tipoOrden: tipo, lado });
    this.resumen.set(null);
  }

  setLado(lado: string): void {
    this.orderForm.patchValue({ lado });
    this.resumen.set(null);
  }

  async accionOrden(lado: 'COMPRA' | 'VENTA'): Promise<void> {
    const resumen = this.resumen();
    if (!resumen || resumen.lado !== lado) {
      this.setLado(lado);
      return;
    }
    await this.confirmarOrden(lado);
  }

  construirOrdenPayload(): Record<string, unknown> {
    const raw = this.orderForm.getRawValue();
    let lado = raw.lado;
    if (raw.tipoOrden === 'STOP_LOSS' || raw.tipoOrden === 'TAKE_PROFIT') {
      lado = 'VENTA';
    }
    const payload: Record<string, unknown> = {
      simbolo: raw.simbolo.trim().toUpperCase(),
      tipoOrden: raw.tipoOrden,
      lado,
      cantidad: raw.cantidad,
    };
    if (raw.tipoOrden === 'LIMIT' || raw.tipoOrden === 'TAKE_PROFIT') {
      payload['precioLimite'] = raw.precioLimite;
    }
    if (raw.tipoOrden === 'STOP_LOSS') {
      payload['precioStop'] = raw.precioStop;
    }
    return payload;
  }

  async previsualizar(): Promise<void> {
    if (this.orderForm.invalid || !this.ordenTieneCamposRequeridos()) {
      this.toast.mostrar('Completa los datos de la orden', 'error');
      return;
    }
    const res = await this.api.post<ResumenComision>('/api/ordenes/previsualizar', this.construirOrdenPayload());
    if (res.ok && res.data) {
      this.resumen.set(res.data);
      this.toast.mostrar('Comision previsualizada', 'success');
    } else {
      this.toast.mostrar(res.error || 'No se pudo previsualizar la orden', 'error');
    }
  }

  async confirmarOrden(lado: 'COMPRA' | 'VENTA'): Promise<void> {
    if (!this.resumen() || this.resumen()?.lado !== lado) {
      this.orderForm.patchValue({ lado });
      this.resumen.set(null);
      this.toast.mostrar(`Previsualiza la comision de ${lado === 'COMPRA' ? 'compra' : 'venta'} antes de enviar`, 'error');
      return;
    }

    const res = await this.api.post<Orden>('/api/ordenes', this.construirOrdenPayload());
    if (res.ok) {
      this.toast.mostrar('Orden enviada correctamente', 'success');
      this.resumen.set(null);
      await Promise.all([this.cargarOrdenes(), this.cargarSaldo(), this.cargarPortafolio()]);
    } else {
      this.toast.mostrar(res.error || 'No se pudo enviar la orden', 'error');
    }
  }

  async cancelarOrden(id?: number): Promise<void> {
    if (!id) return;
    const res = await this.api.delete(`/api/ordenes/${id}`);
    this.toast.mostrar(res.ok ? 'Orden cancelada' : res.error || 'No se pudo cancelar', res.ok ? 'success' : 'error');
    if (res.ok) {
      await this.cargarOrdenes();
    }
  }

  async aprobarPropuesta(id?: number): Promise<void> {
    if (!id) return;
    const res = await this.api.post<Orden>(`/api/propuestas/${id}/aprobar`, { comentario: 'Aprobada desde dashboard' });
    this.toast.mostrar(res.ok ? 'Propuesta aprobada' : res.error || 'No se pudo aprobar', res.ok ? 'success' : 'error');
    if (res.ok) {
      await Promise.all([this.cargarPropuestas(), this.cargarOrdenes()]);
    }
  }

  async rechazarPropuesta(id?: number): Promise<void> {
    if (!id) return;
    const res = await this.api.post<Orden>(`/api/propuestas/${id}/rechazar`, { comentario: 'Rechazada desde dashboard' });
    this.toast.mostrar(res.ok ? 'Propuesta rechazada' : res.error || 'No se pudo rechazar', res.ok ? 'success' : 'error');
    if (res.ok) {
      await Promise.all([this.cargarPropuestas(), this.cargarOrdenes()]);
    }
  }

  async depositar(): Promise<void> {
    const monto = this.depositoForm.controls.monto.value;
    const res = await this.api.post(`/api/portafolio/depositar?monto=${encodeURIComponent(monto)}`, undefined);
    this.toast.mostrar(res.ok ? 'Fondos depositados' : res.error || 'No se pudo depositar', res.ok ? 'success' : 'error');
    if (res.ok) {
      await this.cargarSaldo();
    }
  }

  async guardarPerfil(): Promise<void> {
    const raw = this.perfilForm.getRawValue();
    const res = await this.api.put('/api/perfil', {
      nombreCompleto: raw.nombreCompleto.trim(),
      nivelExperiencia: raw.nivelExperiencia,
      telefono: raw.telefono.trim() || null,
      interesesMercado: raw.interesesMercado.split(',').map((s) => s.trim()).filter(Boolean),
    });
    if (res.ok) {
      this.editandoPerfil.set(false);
      this.toast.mostrar('Perfil actualizado', 'success');
      await this.cargarPerfil();
      await this.cargarMercado();
    } else {
      this.toast.mostrar(res.error || 'No se pudo guardar el perfil', 'error');
    }
  }

  async toggleMfa(): Promise<void> {
    const activar = !this.perfil()?.mfaHabilitado;
    const res = await this.api.put(`/api/perfil/mfa?activar=${activar}`);
    if (res.ok) {
      this.toast.mostrar(activar ? 'MFA activado' : 'MFA desactivado', 'success');
      await this.cargarPerfil();
    } else {
      this.toast.mostrar(res.error || 'No se pudo cambiar MFA', 'error');
    }
  }

  async guardarPreferencias(): Promise<void> {
    const raw = this.preferenciasForm.getRawValue();
    const [notif, operacion] = await Promise.all([
      this.api.put('/api/perfil/preferencias/notificaciones', {
        notificacionEmail: raw.notificacionEmail,
        notificacionSms: raw.notificacionSms,
        notificacionWhatsapp: raw.notificacionWhatsapp,
        tiposNotificacion: ['ORDER_EXECUTED', 'LOGIN'],
      }),
      this.api.put('/api/perfil/preferencias/operacion', {
        tipoOrdenDefault: raw.tipoOrdenDefault,
        vistaPortafolio: raw.vistaPortafolio,
      }),
    ]);
    const ok = notif.ok && operacion.ok;
    this.toast.mostrar(ok ? 'Preferencias guardadas' : 'No se pudieron guardar todas las preferencias', ok ? 'success' : 'error');
    if (ok) {
      await this.cargarPerfil();
    }
  }

  async solicitarComisionista(): Promise<void> {
    const res = await this.api.put('/api/perfil/comisionista/solicitar');
    this.toast.mostrar(res.ok ? res.mensaje || 'Solicitud registrada' : res.error || 'No se pudo solicitar comisionista', res.ok ? 'success' : 'error');
    if (res.ok) {
      await this.cargarPerfil();
    }
  }

  mostrarLadoOrden(): boolean {
    const tipo = this.orderForm.controls.tipoOrden.value;
    return tipo === 'MARKET' || tipo === 'LIMIT';
  }

  mostrarVentaForzadaOrden(): boolean {
    const tipo = this.orderForm.controls.tipoOrden.value;
    return tipo === 'STOP_LOSS' || tipo === 'TAKE_PROFIT';
  }

  mostrarPrecioLimiteOrden(): boolean {
    return this.orderForm.controls.tipoOrden.value === 'LIMIT';
  }

  mostrarStopLossOrden(): boolean {
    return this.orderForm.controls.tipoOrden.value === 'STOP_LOSS';
  }

  mostrarTakeProfitOrden(): boolean {
    return this.orderForm.controls.tipoOrden.value === 'TAKE_PROFIT';
  }

  private ordenTieneCamposRequeridos(): boolean {
    const raw = this.orderForm.getRawValue();
    if ((raw.tipoOrden === 'LIMIT' || raw.tipoOrden === 'TAKE_PROFIT') && Number(raw.precioLimite || 0) <= 0) {
      return false;
    }
    if (raw.tipoOrden === 'STOP_LOSS' && Number(raw.precioStop || 0) <= 0) {
      return false;
    }
    return true;
  }

  async cerrarSesion(): Promise<void> {
    await this.api.post('/api/auth/logout').catch(() => undefined);
    this.api.clearToken();
    this.router.navigateByUrl('/login');
  }

  dinero(valor?: number | null): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(valor || 0));
  }

  pct(valor?: number | null): string {
    return `${Number(valor || 0).toFixed(2)}%`;
  }

  claseNumero(valor?: number | null): string {
    return Number(valor || 0) >= 0 ? 'up' : 'down';
  }

  mercadoInternacional(simbolo?: string): string {
    const s = (simbolo || '').toUpperCase();
    if (s.endsWith('.L')) return 'LSE';
    if (s.endsWith('.T')) return 'TSE';
    if (s.endsWith('.AX')) return 'ASX';
    return 'US';
  }

  nombreEmpresa(simbolo?: string): string {
    if (!simbolo) return 'Empresa';
    return NOMBRES_EMPRESAS[simbolo.toUpperCase()] || 'Empresa no disponible';
  }

  cotizacionConNombre(c: Cotizacion): Cotizacion {
    return { ...c, nombreEmpresa: c.nombreEmpresa || this.nombreEmpresa(c.simbolo) };
  }

  miniY(valor?: number | null, high?: number | null, low?: number | null): number {
    const h = Number(high || valor || 1);
    const l = Number(low || valor || 0);
    const v = Number(valor || 0);
    const rango = h - l || 1;
    return Math.round(2 + (1 - (v - l) / rango) * 22);
  }

  miniRectY(c: Cotizacion): number {
    return this.miniY(c.precioMaximo, c.precioMaximo, c.precioMinimo);
  }

  miniRectHeight(c: Cotizacion): number {
    const alto = this.miniY(c.precioMinimo, c.precioMaximo, c.precioMinimo) - this.miniRectY(c);
    return Math.max(2, alto);
  }

  variacionArrow(valor?: number | null): string {
    return Number(valor || 0) >= 0 ? '▲' : '▼';
  }

  private tick(): void {
    this.reloj.set(new Date().toLocaleTimeString('en-US', { hour12: false, timeZone: 'America/New_York' }) + ' NY');
  }
}
