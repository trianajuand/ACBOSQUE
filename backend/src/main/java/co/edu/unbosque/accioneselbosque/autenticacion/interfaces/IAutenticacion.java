package co.edu.unbosque.accioneselbosque.autenticacion.interfaces;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.LoginRequestDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.LoginResponseDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.MFARequestDTO;

public interface IAutenticacion {

    LoginResponseDTO iniciarSesion(LoginRequestDTO solicitud);

    LoginResponseDTO verificarMfa(MFARequestDTO solicitud);

    void cerrarSesion(String token);
}
