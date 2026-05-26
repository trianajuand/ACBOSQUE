export interface LoginResponse {
  token?: string;
  requiereMfa?: boolean;
  mfaToken?: string;
  rol?: string;
  mensaje?: string;
}

export interface ConfirmarRegistroResponse {
  mensaje: string;
  requierePago: boolean;
  stripeCheckoutUrl?: string;
}

export interface CorreoDisponibleResponse {
  correo: string;
  disponible: boolean;
}

export interface Perfil {
  nombreCompleto?: string;
  correo?: string;
  nivelExperiencia?: string;
  interesesMercado?: string[];
  telefono?: string | null;
  tipoIdentificacion?: string;
  numeroIdentificacion?: string;
  fechaNacimiento?: string;
  direccion?: string;
  ciudad?: string;
  codigoPostal?: string;
  pais?: string;
  estiloTrading?: string;
  solicitaComisionista?: boolean;
  comisionistaAsignado?: ComisionistaAsignado | null;
  mfaHabilitado?: boolean;
  planSuscripcion?: string;
  esPremium?: boolean;
  notificacionesActivas?: boolean;
  notificacionEmail?: boolean;
  notificacionSms?: boolean;
  notificacionWhatsapp?: boolean;
  tiposNotificacion?: string[];
  tipoOrdenDefault?: string;
  vistaPortafolio?: string;
}

export interface ComisionistaAsignado {
  id: number;
  nombreCompleto?: string;
  correo?: string;
  especialidadesMercado?: string[];
  fechaAsignacion?: string;
}

export interface Cotizacion {
  simbolo: string;
  nombreEmpresa?: string;
  precioActual?: number;
  precioApertura?: number;
  precioCierreAnterior?: number;
  precioMaximo?: number;
  precioMinimo?: number;
  variacionPorcentual?: number;
  volumen?: number;
  mercado?: string;
  mercadoAbierto?: boolean;
  actualizadoEn?: string;
}

export interface DetalleAccion extends Cotizacion {
  sector?: string;
  industria?: string;
  descripcion?: string;
  capitalizacionMercado?: number;
  historicoPrecios?: Array<Record<string, unknown>>;
}

export interface Orden {
  id?: number;
  simbolo: string;
  tipoOrden?: string;
  lado?: string;
  estado?: string;
  cantidad?: number;
  precioLimite?: number;
  precioStop?: number;
  precioEjecucion?: number;
  montoTotal?: number;
  comision?: number;
  montoNeto?: number;
  comisionistaId?: number;
  comentarioComisionista?: string;
  creadaEn?: string;
  aprobadaEn?: string;
  rechazadaEn?: string;
  firmadaEn?: string;
}

export interface ClienteAsignado {
  id: number;
  nombreCompleto?: string;
  correo?: string;
  nivelExperiencia?: string;
  interesesMercado?: string[];
  fechaAsignacion?: string;
}

export interface Holding {
  simbolo: string;
  nombreEmpresa?: string;
  cantidad?: number;
  precioPromedio?: number;
  precioActual?: number;
  valorTotal?: number;
  gananciaPerdida?: number;
  gananciaPerdidaPct?: number;
}

export interface Portafolio {
  holdings?: Holding[];
  valorTotalPortafolio?: number;
  gananciaPerdidaTotal?: number;
  gananciaPerdidaTotalPct?: number;
  vistaPreferida?: string;
}

export interface Saldo {
  saldoDisponible?: number;
  fondosReservados?: number;
  totalComisionesPagadas?: number;
}

export interface ResumenComision {
  simbolo?: string;
  tipoOrden?: string;
  lado?: string;
  cantidad?: number;
  precioEstimado?: number;
  montoBase?: number;
  porcentajeComision?: number;
  montoComision?: number;
  montoPlataforma?: number;
  montoComisionista?: number;
  totalADebitar?: number;
  totalARecibir?: number;
  mercadoAbierto?: boolean;
  advertencia?: string;
}

export interface MercadoAdmin {
  id?: number;
  codigo: string;
  nombre: string;
  zonaHoraria: string;
  horaApertura: string;
  horaCierre: string;
  habilitado: boolean;
  cierreAnticipado?: string | null;
}

export interface FeriadoMercado {
  id?: number;
  mercadoCodigo?: string;
  fecha: string;
  descripcion: string;
}

export interface ParametroComision {
  porcentajeComision: number;
  splitPlataforma: number;
  splitComisionista: number;
}

export interface UsuarioAdmin {
  id: number;
  nombreCompleto?: string;
  correo?: string;
  rol?: string;
  estadoCuenta?: string;
  mfaHabilitado?: boolean;
  comisionistaAsignado?: string;
  fechaCreacion?: string;
}

export interface DashboardEjecutivo {
  usuariosActivos?: number;
  crecimientoUsuarios?: number;
  transacciones?: number;
  volumenTransacciones?: number;
  comisionesGeneradas?: number;
  tendenciasPorMercado?: TendenciaMercado[];
}

export interface TendenciaMercado {
  mercado?: string;
  operaciones?: number;
  volumen?: number;
  comisiones?: number;
}
