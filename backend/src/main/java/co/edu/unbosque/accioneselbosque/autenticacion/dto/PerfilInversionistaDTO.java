package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import java.util.List;

public class PerfilInversionistaDTO {

    private String nombreCompleto;
    private String correo;
    private String nivelExperiencia;
    private List<String> interesesMercado;
    private String telefono;
    private String tipoIdentificacion;
    private String numeroIdentificacion;
    private String fechaNacimiento;
    private String direccion;
    private String ciudad;
    private String codigoPostal;
    private String pais;
    private String estiloTrading;
    private String rangoIngresos;
    private boolean solicitaComisionista;
    private boolean mfaHabilitado;
    private String planSuscripcion;
    private boolean esPremium;
    // Preferencias de notificación
    private boolean notificacionEmail;
    private boolean notificacionSms;
    private boolean notificacionWhatsapp;
    private List<String> tiposNotificacion;
    // Preferencias de operación
    private String tipoOrdenDefault;
    private String vistaPortafolio;

    public PerfilInversionistaDTO() {}

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getNivelExperiencia() { return nivelExperiencia; }
    public void setNivelExperiencia(String nivelExperiencia) { this.nivelExperiencia = nivelExperiencia; }

    public List<String> getInteresesMercado() { return interesesMercado; }
    public void setInteresesMercado(List<String> interesesMercado) { this.interesesMercado = interesesMercado; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(String numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getEstiloTrading() { return estiloTrading; }
    public void setEstiloTrading(String estiloTrading) { this.estiloTrading = estiloTrading; }

    public String getRangoIngresos() { return rangoIngresos; }
    public void setRangoIngresos(String rangoIngresos) { this.rangoIngresos = rangoIngresos; }

    public boolean isSolicitaComisionista() { return solicitaComisionista; }
    public void setSolicitaComisionista(boolean solicitaComisionista) { this.solicitaComisionista = solicitaComisionista; }

    public boolean isMfaHabilitado() { return mfaHabilitado; }
    public void setMfaHabilitado(boolean mfaHabilitado) { this.mfaHabilitado = mfaHabilitado; }

    public String getPlanSuscripcion() { return planSuscripcion; }
    public void setPlanSuscripcion(String planSuscripcion) { this.planSuscripcion = planSuscripcion; }

    public boolean isEsPremium() { return esPremium; }
    public void setEsPremium(boolean esPremium) { this.esPremium = esPremium; }

    public boolean isNotificacionEmail() { return notificacionEmail; }
    public void setNotificacionEmail(boolean notificacionEmail) { this.notificacionEmail = notificacionEmail; }

    public boolean isNotificacionSms() { return notificacionSms; }
    public void setNotificacionSms(boolean notificacionSms) { this.notificacionSms = notificacionSms; }

    public boolean isNotificacionWhatsapp() { return notificacionWhatsapp; }
    public void setNotificacionWhatsapp(boolean notificacionWhatsapp) { this.notificacionWhatsapp = notificacionWhatsapp; }

    public List<String> getTiposNotificacion() { return tiposNotificacion; }
    public void setTiposNotificacion(List<String> tiposNotificacion) { this.tiposNotificacion = tiposNotificacion; }

    public String getTipoOrdenDefault() { return tipoOrdenDefault; }
    public void setTipoOrdenDefault(String tipoOrdenDefault) { this.tipoOrdenDefault = tipoOrdenDefault; }

    public String getVistaPortafolio() { return vistaPortafolio; }
    public void setVistaPortafolio(String vistaPortafolio) { this.vistaPortafolio = vistaPortafolio; }
}
