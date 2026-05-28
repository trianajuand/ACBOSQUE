import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ClienteAsignado, Orden, Portafolio } from '../core/models';
import { ToastService } from '../core/toast.service';

type PanelComisionista = 'clientes' | 'portafolio' | 'ordenes' | 'propuestas';

@Component({
  selector: 'app-comisionista-dashboard',
  imports: [ReactiveFormsModule],
  templateUrl: './comisionista-dashboard.component.html',
  styleUrl: './comisionista-dashboard.component.scss',
})
export class ComisionistaDashboardComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private refrescoOperativoId?: number;

  readonly panel = signal<PanelComisionista>('clientes');
  readonly clientes = signal<ClienteAsignado[]>([]);
  readonly clienteSeleccionado = signal<ClienteAsignado | null>(null);
  readonly portafolio = signal<Portafolio | null>(null);
  readonly activas = signal<Orden[]>([]);
  readonly historial = signal<Orden[]>([]);
  readonly aprobadas = signal<Orden[]>([]);
  readonly catalogo = signal<Record<string, string[]>>({});
  readonly cargando = signal(true);
  readonly firmandoId = signal<number | null>(null);

  readonly clientesActivos = computed(() => this.clientes().length);
  readonly valorCliente = computed(() => this.portafolio()?.valorTotalPortafolio || 0);
  readonly simbolosPropuesta = computed(() => {
    const catalogo = Object.values(this.catalogo()).flat();
    const intereses = this.clientes().flatMap((c) => c.interesesMercado || []);
    return Array.from(new Set([...intereses, ...catalogo, 'AAPL', 'MSFT', 'TSLA']))
      .map((s) => s.trim().toUpperCase())
      .filter(Boolean)
      .sort();
  });

  readonly propuestaForm = this.fb.nonNullable.group({
    clienteId: [0, [Validators.required, Validators.min(1)]],
    simbolo: ['AAPL', [Validators.required]],
    tipoOrden: ['MARKET', [Validators.required]],
    lado: ['COMPRA', [Validators.required]],
    cantidad: [1, [Validators.required, Validators.min(0.000001)]],
    precioLimite: [0],
    precioStop: [0],
    comentarioComisionista: [''],
  });

  async ngOnInit(): Promise<void> {
    await this.cargarCatalogo();
    await this.cargarClientes();
    await this.cargarAprobadas();
    this.cargando.set(false);
    this.refrescoOperativoId = window.setInterval(() => void this.refrescarOperativo(), 10000);
  }

  ngOnDestroy(): void {
    if (this.refrescoOperativoId) {
      window.clearInterval(this.refrescoOperativoId);
    }
  }

  seleccionar(panel: PanelComisionista): void {
    this.panel.set(panel);
    if (panel === 'propuestas') {
      void this.cargarAprobadas();
    }
  }

  async cargarClientes(): Promise<void> {
    const res = await this.api.get<ClienteAsignado[]>('/api/comisionista/clientes');
    if (res.ok && res.data) {
      this.clientes.set(res.data);
      if (res.data.length && !this.clienteSeleccionado()) {
        await this.seleccionarCliente(res.data[0]);
      }
    } else {
      this.toast.mostrar(res.error || 'No se pudieron cargar clientes', 'error');
    }
  }

  async cargarCatalogo(): Promise<void> {
    const res = await this.api.get<Record<string, string[]>>('/api/mercado/simbolos', false);
    if (res.ok && res.data) {
      this.catalogo.set(res.data);
    }
  }

  async verCliente(cliente: ClienteAsignado): Promise<void> {
    await this.seleccionarCliente(cliente, 'portafolio');
  }

  async seleccionarCliente(cliente: ClienteAsignado, destino?: PanelComisionista): Promise<void> {
    this.clienteSeleccionado.set(cliente);
    this.propuestaForm.patchValue({ clienteId: cliente.id });
    await Promise.all([
      this.cargarPortafolioCliente(cliente.id),
      this.cargarOrdenesCliente(cliente.id),
    ]);
    if (destino) {
      this.panel.set(destino);
    }
  }

  async seleccionarClientePorId(clienteIdRaw: string | number): Promise<void> {
    const clienteId = Number(clienteIdRaw);
    const cliente = this.clientes().find((c) => c.id === clienteId);
    if (cliente) {
      await this.seleccionarCliente(cliente);
    }
  }

  async cargarPortafolioCliente(clienteId: number): Promise<void> {
    const res = await this.api.get<Portafolio>(`/api/comisionista/clientes/${clienteId}/portafolio`);
    if (res.ok && res.data) this.portafolio.set(res.data);
  }

  async cargarOrdenesCliente(clienteId: number): Promise<void> {
    const [activas, historial] = await Promise.all([
      this.api.get<Orden[]>(`/api/comisionista/clientes/${clienteId}/ordenes/activas`),
      this.api.get<Orden[]>(`/api/comisionista/clientes/${clienteId}/ordenes/historial`),
    ]);
    if (activas.ok && activas.data) this.activas.set(activas.data);
    if (historial.ok && historial.data) this.historial.set(historial.data);
  }

  async refrescarOperativo(): Promise<void> {
    const cliente = this.clienteSeleccionado();
    await Promise.all([
      this.cargarAprobadas(),
      cliente ? this.cargarPortafolioCliente(cliente.id) : Promise.resolve(),
      cliente ? this.cargarOrdenesCliente(cliente.id) : Promise.resolve(),
    ]);
  }

  async crearPropuesta(): Promise<void> {
    if (this.propuestaForm.invalid || !this.propuestaTieneCamposRequeridos()) {
      this.toast.mostrar('Selecciona cliente y completa la propuesta', 'error');
      return;
    }
    const raw = this.propuestaForm.getRawValue();
    let lado = raw.lado;
    if (raw.tipoOrden === 'STOP_LOSS' || raw.tipoOrden === 'TAKE_PROFIT') {
      lado = 'VENTA';
    }
    const payload: Record<string, unknown> = {
      simbolo: raw.simbolo.trim().toUpperCase(),
      tipoOrden: raw.tipoOrden,
      lado,
      cantidad: raw.cantidad,
      comentarioComisionista: raw.comentarioComisionista,
    };
    if (raw.tipoOrden === 'LIMIT' || raw.tipoOrden === 'TAKE_PROFIT') payload['precioLimite'] = raw.precioLimite;
    if (raw.tipoOrden === 'STOP_LOSS') payload['precioStop'] = raw.precioStop;

    const res = await this.api.post<Orden>(`/api/comisionista/clientes/${raw.clienteId}/propuestas`, payload);
    if (res.ok) {
      this.toast.mostrar('Propuesta enviada al inversionista', 'success');
      await this.cargarOrdenesCliente(raw.clienteId);
    } else {
      this.toast.mostrar(res.error || 'No se pudo crear propuesta', 'error');
    }
  }

  async cargarAprobadas(): Promise<void> {
    const res = await this.api.get<Orden[]>('/api/comisionista/propuestas/aprobadas');
    if (res.ok && res.data) this.aprobadas.set(res.data);
  }

  setTipoPropuesta(tipo: string): void {
    let lado = this.propuestaForm.controls.lado.value;
    if (tipo === 'STOP_LOSS' || tipo === 'TAKE_PROFIT') {
      lado = 'VENTA';
    }
    this.propuestaForm.patchValue({ tipoOrden: tipo, lado });
  }

  mostrarLadoPropuesta(): boolean {
    const tipo = this.propuestaForm.controls.tipoOrden.value;
    return tipo === 'MARKET' || tipo === 'LIMIT';
  }

  mostrarVentaForzadaPropuesta(): boolean {
    const tipo = this.propuestaForm.controls.tipoOrden.value;
    return tipo === 'STOP_LOSS' || tipo === 'TAKE_PROFIT';
  }

  mostrarPrecioLimitePropuesta(): boolean {
    return this.propuestaForm.controls.tipoOrden.value === 'LIMIT';
  }

  mostrarStopLossPropuesta(): boolean {
    return this.propuestaForm.controls.tipoOrden.value === 'STOP_LOSS';
  }

  mostrarTakeProfitPropuesta(): boolean {
    return this.propuestaForm.controls.tipoOrden.value === 'TAKE_PROFIT';
  }

  private propuestaTieneCamposRequeridos(): boolean {
    const raw = this.propuestaForm.getRawValue();
    if ((raw.tipoOrden === 'LIMIT' || raw.tipoOrden === 'TAKE_PROFIT') && Number(raw.precioLimite || 0) <= 0) {
      return false;
    }
    if (raw.tipoOrden === 'STOP_LOSS' && Number(raw.precioStop || 0) <= 0) {
      return false;
    }
    return true;
  }

  async firmarEnviar(id?: number): Promise<void> {
    if (!id) return;
    this.firmandoId.set(id);
    try {
      const res = await this.api.post<Orden>(`/api/comisionista/propuestas/${id}/firmar-enviar`);
      this.toast.mostrar(res.ok ? 'Orden firmada y enviada' : res.error || 'No se pudo firmar', res.ok ? 'success' : 'error');
      if (res.ok) {
        await this.cargarAprobadas();
        const cliente = this.clienteSeleccionado();
        if (cliente) await this.cargarOrdenesCliente(cliente.id);
      }
    } finally {
      this.firmandoId.set(null);
    }
  }

  async cerrarSesion(): Promise<void> {
    await this.api.post('/api/auth/logout').catch(() => undefined);
    this.api.clearToken();
    this.router.navigateByUrl('/login');
  }

  dinero(valor?: number | null): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(valor || 0));
  }
}
