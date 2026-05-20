package co.edu.unbosque.accioneselbosque.shared.dto;

public class RespuestaDTO {

    private String mensaje;
    private String error;
    private Object data;

    public RespuestaDTO() {}

    public RespuestaDTO(String mensaje) {
        this.mensaje = mensaje;
    }

    public static RespuestaDTO error(String error) {
        RespuestaDTO r = new RespuestaDTO();
        r.error = error;
        return r;
    }

    public static RespuestaDTO exito(String mensaje) {
        RespuestaDTO r = new RespuestaDTO();
        r.mensaje = mensaje;
        return r;
    }

    public static RespuestaDTO exito(Object data) {
        RespuestaDTO r = new RespuestaDTO();
        r.data = data;
        return r;
    }

    public static RespuestaDTO exito(String mensaje, Object data) {
        RespuestaDTO r = new RespuestaDTO();
        r.mensaje = mensaje;
        r.data = data;
        return r;
    }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
