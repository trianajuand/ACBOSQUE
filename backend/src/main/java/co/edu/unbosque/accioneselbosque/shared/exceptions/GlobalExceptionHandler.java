package co.edu.unbosque.accioneselbosque.shared.exceptions;

import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaDTO> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(RespuestaDTO.error(mensaje));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<RespuestaDTO> manejarCorreoExistente(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<RespuestaDTO> manejarCuentaBloqueada(AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidMfaException.class)
    public ResponseEntity<RespuestaDTO> manejarMfaInvalido(InvalidMfaException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<RespuestaDTO> manejarTokenInvalido(InvalidTokenException ex) {
        return ResponseEntity.badRequest().body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<RespuestaDTO> manejarUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(FondosInsuficientesException.class)
    public ResponseEntity<RespuestaDTO> manejarFondosInsuficientes(FondosInsuficientesException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(HoldingInsuficienteException.class)
    public ResponseEntity<RespuestaDTO> manejarHoldingInsuficiente(HoldingInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(OrdenNoEncontradaException.class)
    public ResponseEntity<RespuestaDTO> manejarOrdenNoEncontrada(OrdenNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(SimboloInvalidoException.class)
    public ResponseEntity<RespuestaDTO> manejarSimboloInvalido(SimboloInvalidoException ex) {
        return ResponseEntity.badRequest().body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(StripeCheckoutException.class)
    public ResponseEntity<RespuestaDTO> manejarStripeCheckout(StripeCheckoutException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(RespuestaDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaDTO> manejarGeneral(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RespuestaDTO.error("Error interno del servidor"));
    }
}
