package co.edu.unbosque.accioneselbosque.trazabilidad.interfaces;

import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;

public interface IAuditLog {

    void registrar(TipoEvento tipo, String correoUsuario, String detalle);
}
