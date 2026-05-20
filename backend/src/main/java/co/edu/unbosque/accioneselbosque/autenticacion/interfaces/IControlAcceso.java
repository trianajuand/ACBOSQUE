package co.edu.unbosque.accioneselbosque.autenticacion.interfaces;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.RegistroInversionistaDTO;

public interface IControlAcceso {

    void registrarInversionista(RegistroInversionistaDTO solicitud);

    void confirmarRegistro(String correo, String codigo);

    void solicitarRecuperacionPassword(String correo);

    void resetearPassword(String token, String nuevaContrasenia);
}
