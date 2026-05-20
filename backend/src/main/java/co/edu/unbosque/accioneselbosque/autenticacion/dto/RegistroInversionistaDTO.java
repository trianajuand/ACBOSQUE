package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RegistroInversionistaDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo invalido")
    private String correo;

    @NotBlank(message = "La contrasenia es obligatoria")
    @Size(min = 8, message = "La contrasenia debe tener al menos 8 caracteres")
    private String contrasenia;

    @NotBlank(message = "El tipo de identificacion es obligatorio")
    private String tipoIdentificacion;

    @NotBlank(message = "El numero de identificacion es obligatorio")
    private String numeroIdentificacion;

    private String fechaNacimiento;

    @NotBlank(message = "La direccion es obligatoria")
    private String direccion;

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    @NotBlank(message = "El codigo postal es obligatorio")
    private String codigoPostal;

    private String pais;
    private String nivelExperiencia;
    private List<String> interesesMercado;
    private String telefono;
    private String estiloTrading;
    private String rangoIngresos;
    private String tipoOrdenDefault;
    private String vistaPortafolio;
    private boolean notificacionEmail;
    private boolean notificacionSms;
    private boolean notificacionWhatsapp;
    private boolean solicitaComisionista;

    // BASICO (null = BASICO), PREMIUM_MENSUAL, PREMIUM_ANUAL
    private String planSuscripcion;

    public RegistroInversionistaDTO() {
    }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) { this.contrasenia = contrasenia; }

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

    public String getNivelExperiencia() { return nivelExperiencia; }
    public void setNivelExperiencia(String nivelExperiencia) { this.nivelExperiencia = nivelExperiencia; }

    public List<String> getInteresesMercado() { return interesesMercado; }
    public void setInteresesMercado(List<String> interesesMercado) { this.interesesMercado = interesesMercado; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEstiloTrading() { return estiloTrading; }
    public void setEstiloTrading(String estiloTrading) { this.estiloTrading = estiloTrading; }

    public String getRangoIngresos() { return rangoIngresos; }
    public void setRangoIngresos(String rangoIngresos) { this.rangoIngresos = rangoIngresos; }

    public String getTipoOrdenDefault() { return tipoOrdenDefault; }
    public void setTipoOrdenDefault(String tipoOrdenDefault) { this.tipoOrdenDefault = tipoOrdenDefault; }

    public String getVistaPortafolio() { return vistaPortafolio; }
    public void setVistaPortafolio(String vistaPortafolio) { this.vistaPortafolio = vistaPortafolio; }

    public boolean isNotificacionEmail() { return notificacionEmail; }
    public void setNotificacionEmail(boolean notificacionEmail) { this.notificacionEmail = notificacionEmail; }

    public boolean isNotificacionSms() { return notificacionSms; }
    public void setNotificacionSms(boolean notificacionSms) { this.notificacionSms = notificacionSms; }

    public boolean isNotificacionWhatsapp() { return notificacionWhatsapp; }
    public void setNotificacionWhatsapp(boolean notificacionWhatsapp) { this.notificacionWhatsapp = notificacionWhatsapp; }

    public boolean isSolicitaComisionista() { return solicitaComisionista; }
    public void setSolicitaComisionista(boolean solicitaComisionista) { this.solicitaComisionista = solicitaComisionista; }

    public String getPlanSuscripcion() { return planSuscripcion; }
    public void setPlanSuscripcion(String planSuscripcion) { this.planSuscripcion = planSuscripcion; }
}
