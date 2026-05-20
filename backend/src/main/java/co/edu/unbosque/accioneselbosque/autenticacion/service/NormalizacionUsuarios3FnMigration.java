package co.edu.unbosque.accioneselbosque.autenticacion.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(0)
public class NormalizacionUsuarios3FnMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NormalizacionUsuarios3FnMigration.class);

    private static final List<String> COLUMNAS_USUARIO_LEGACY = List.of(
            "nivel_experiencia",
            "intereses_mercado",
            "especialidades_mercado",
            "alpaca_account_id",
            "pendiente_cuenta_alpaca",
            "telefono",
            "tipo_identificacion",
            "numero_identificacion",
            "fecha_nacimiento",
            "direccion",
            "ciudad",
            "codigo_postal",
            "pais",
            "estilo_trading",
            "rango_ingresos",
            "solicita_comisionista",
            "plan_suscripcion",
            "es_premium",
            "stripe_customer_id",
            "stripe_suscripcion_id",
            "fecha_expiracion_premium",
            "notificacion_email",
            "notificacion_sms",
            "notificacion_whatsapp",
            "tipos_notificacion",
            "tipo_orden_default",
            "vista_portafolio"
    );

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public NormalizacionUsuarios3FnMigration(JdbcTemplate jdbc,
                                             NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public void run(String... args) {
        if (!tablaExiste("usuario")) {
            return;
        }
        asegurarTablaUsuarioGeneral();
        asegurarTablaInversionista();
        asegurarTablaComisionista();
        asegurarColumnasPerfilSiFaltan();
        crearPerfilesFaltantes();
        migrarColumnasInversionista();
        migrarColumnasComisionista();
        normalizarDefaultsPerfil();
        asegurarRestricciones();
        eliminarColumnasLegacyUsuario();
    }

    private void asegurarTablaUsuarioGeneral() {
        addColumn("usuario", "nombre_completo", "varchar(255)");
        addColumn("usuario", "correo", "varchar(255)");
        addColumn("usuario", "contrasenia", "varchar(255)");
        addColumn("usuario", "rol", "varchar(255)");
        addColumn("usuario", "estado_cuenta", "varchar(255)");
        addColumn("usuario", "mfa_habilitado", "boolean");
        addColumn("usuario", "fecha_creacion", "timestamp");
        addColumn("usuario", "fecha_actualizacion", "timestamp");
        ejecutarSeguro("ALTER TABLE usuario ALTER COLUMN mfa_habilitado SET DEFAULT false");
        ejecutarSeguro("ALTER TABLE usuario ALTER COLUMN fecha_creacion SET DEFAULT now()");
    }

    private void asegurarTablaInversionista() {
        ejecutarSeguro("""
                CREATE TABLE IF NOT EXISTS inversionista (
                    id bigserial PRIMARY KEY,
                    usuario_id bigint NOT NULL,
                    nivel_experiencia varchar(255),
                    intereses_mercado varchar(500),
                    telefono varchar(255),
                    tipo_identificacion varchar(255),
                    numero_identificacion varchar(255),
                    fecha_nacimiento varchar(255),
                    direccion varchar(255),
                    ciudad varchar(255),
                    codigo_postal varchar(255),
                    pais varchar(255),
                    estilo_trading varchar(255),
                    rango_ingresos varchar(255),
                    solicita_comisionista boolean,
                    alpaca_account_id varchar(255),
                    pendiente_cuenta_alpaca boolean,
                    plan_suscripcion varchar(255),
                    es_premium boolean,
                    stripe_customer_id varchar(255),
                    stripe_suscripcion_id varchar(255),
                    fecha_expiracion_premium date,
                    notificacion_email boolean,
                    notificacion_sms boolean,
                    notificacion_whatsapp boolean,
                    tipos_notificacion varchar(500),
                    tipo_orden_default varchar(255),
                    vista_portafolio varchar(255),
                    fecha_creacion timestamp NOT NULL DEFAULT now(),
                    fecha_actualizacion timestamp
                )
                """);
    }

    private void asegurarTablaComisionista() {
        ejecutarSeguro("""
                CREATE TABLE IF NOT EXISTS comisionista (
                    id bigserial PRIMARY KEY,
                    usuario_id bigint NOT NULL,
                    especialidades_mercado varchar(500),
                    fecha_creacion timestamp NOT NULL DEFAULT now(),
                    fecha_actualizacion timestamp
                )
                """);
    }

    private void asegurarColumnasPerfilSiFaltan() {
        if (!tablaExiste("inversionista")) {
            return;
        }
        addColumn("inversionista", "nivel_experiencia", "varchar(255)");
        addColumn("inversionista", "intereses_mercado", "varchar(500)");
        addColumn("inversionista", "telefono", "varchar(255)");
        addColumn("inversionista", "tipo_identificacion", "varchar(255)");
        addColumn("inversionista", "numero_identificacion", "varchar(255)");
        addColumn("inversionista", "fecha_nacimiento", "varchar(255)");
        addColumn("inversionista", "direccion", "varchar(255)");
        addColumn("inversionista", "ciudad", "varchar(255)");
        addColumn("inversionista", "codigo_postal", "varchar(255)");
        addColumn("inversionista", "pais", "varchar(255)");
        addColumn("inversionista", "estilo_trading", "varchar(255)");
        addColumn("inversionista", "rango_ingresos", "varchar(255)");
        addColumn("inversionista", "solicita_comisionista", "boolean");
        addColumn("inversionista", "alpaca_account_id", "varchar(255)");
        addColumn("inversionista", "pendiente_cuenta_alpaca", "boolean");
        addColumn("inversionista", "plan_suscripcion", "varchar(255)");
        addColumn("inversionista", "es_premium", "boolean");
        addColumn("inversionista", "stripe_customer_id", "varchar(255)");
        addColumn("inversionista", "stripe_suscripcion_id", "varchar(255)");
        addColumn("inversionista", "fecha_expiracion_premium", "date");
        addColumn("inversionista", "notificacion_email", "boolean");
        addColumn("inversionista", "notificacion_sms", "boolean");
        addColumn("inversionista", "notificacion_whatsapp", "boolean");
        addColumn("inversionista", "tipos_notificacion", "varchar(500)");
        addColumn("inversionista", "tipo_orden_default", "varchar(255)");
        addColumn("inversionista", "vista_portafolio", "varchar(255)");
        addColumn("inversionista", "fecha_creacion", "timestamp");
        addColumn("inversionista", "fecha_actualizacion", "timestamp");

        if (tablaExiste("comisionista")) {
            addColumn("comisionista", "especialidades_mercado", "varchar(500)");
            addColumn("comisionista", "fecha_creacion", "timestamp");
            addColumn("comisionista", "fecha_actualizacion", "timestamp");
        }
    }

    private void crearPerfilesFaltantes() {
        if (tablaExiste("inversionista")) {
            ejecutarSeguro("""
                    INSERT INTO inversionista (
                        usuario_id, nivel_experiencia, intereses_mercado, pais, solicita_comisionista,
                        notificacion_email, notificacion_sms, notificacion_whatsapp, tipos_notificacion,
                        tipo_orden_default, vista_portafolio, plan_suscripcion, es_premium,
                        pendiente_cuenta_alpaca, fecha_creacion
                    )
                    SELECT u.id, 'PRINCIPIANTE', 'AAPL,MSFT,TSLA', 'CO', false,
                        true, false, false, 'ORDENES,MERCADO,SEGURIDAD',
                        'MARKET', 'LISTA', 'BASICO', false, false, now()
                    FROM usuario u
                    WHERE u.rol IN ('INVERSIONISTA', 'INVERSIONISTA_PREMIUM')
                      AND NOT EXISTS (SELECT 1 FROM inversionista i WHERE i.usuario_id = u.id)
                    """);
        }
        if (tablaExiste("comisionista")) {
            ejecutarSeguro("""
                    INSERT INTO comisionista (usuario_id, fecha_creacion)
                    SELECT u.id, now()
                    FROM usuario u
                    WHERE u.rol = 'COMISIONISTA'
                      AND NOT EXISTS (SELECT 1 FROM comisionista c WHERE c.usuario_id = u.id)
                    """);
        }
    }

    private void migrarColumnasInversionista() {
        copiarTexto("inversionista", "nivel_experiencia");
        copiarTexto("inversionista", "intereses_mercado");
        copiarTexto("inversionista", "telefono");
        copiarTexto("inversionista", "tipo_identificacion");
        copiarTexto("inversionista", "numero_identificacion");
        copiarTexto("inversionista", "fecha_nacimiento");
        copiarTexto("inversionista", "direccion");
        copiarTexto("inversionista", "ciudad");
        copiarTexto("inversionista", "codigo_postal");
        copiarTexto("inversionista", "pais");
        copiarTexto("inversionista", "estilo_trading");
        copiarTexto("inversionista", "rango_ingresos");
        copiarBooleano("inversionista", "solicita_comisionista");
        copiarTexto("inversionista", "alpaca_account_id");
        copiarBooleano("inversionista", "pendiente_cuenta_alpaca");
        copiarTexto("inversionista", "plan_suscripcion");
        copiarBooleano("inversionista", "es_premium");
        copiarTexto("inversionista", "stripe_customer_id");
        copiarTexto("inversionista", "stripe_suscripcion_id");
        copiarFecha("inversionista", "fecha_expiracion_premium");
        copiarBooleano("inversionista", "notificacion_email");
        copiarBooleano("inversionista", "notificacion_sms");
        copiarBooleano("inversionista", "notificacion_whatsapp");
        copiarTexto("inversionista", "tipos_notificacion");
        copiarTexto("inversionista", "tipo_orden_default");
        copiarTexto("inversionista", "vista_portafolio");
    }

    private void migrarColumnasComisionista() {
        copiarTexto("comisionista", "especialidades_mercado");
    }

    private void copiarTexto(String tablaDestino, String columna) {
        copiar(tablaDestino, columna, "NULLIF(u." + columna + "::text, '')");
    }

    private void copiarBooleano(String tablaDestino, String columna) {
        copiar(tablaDestino, columna, """
                CASE lower(u.%s::text)
                    WHEN 'true' THEN true
                    WHEN 't' THEN true
                    WHEN '1' THEN true
                    WHEN 'si' THEN true
                    WHEN 'yes' THEN true
                    WHEN 'false' THEN false
                    WHEN 'f' THEN false
                    WHEN '0' THEN false
                    WHEN 'no' THEN false
                    ELSE NULL
                END
                """.formatted(columna));
    }

    private void copiarFecha(String tablaDestino, String columna) {
        copiar(tablaDestino, columna, """
                CASE
                    WHEN u.%s IS NULL THEN NULL
                    WHEN u.%s::text ~ '^\\d{4}-\\d{2}-\\d{2}' THEN (substring(u.%s::text from 1 for 10))::date
                    ELSE NULL
                END
                """.formatted(columna, columna, columna));
    }

    private void copiar(String tablaDestino, String columna, String expresionOrigen) {
        if (!tablaExiste(tablaDestino) || !columnaExiste("usuario", columna) || !columnaExiste(tablaDestino, columna)) {
            return;
        }
        ejecutarSeguro("""
                UPDATE %s destino
                SET %s = COALESCE(destino.%s, %s)
                FROM usuario u
                WHERE destino.usuario_id = u.id
                """.formatted(tablaDestino, columna, columna, expresionOrigen));
    }

    private void normalizarDefaultsPerfil() {
        ejecutarSeguro("""
                UPDATE inversionista
                SET nivel_experiencia = COALESCE(nivel_experiencia, 'PRINCIPIANTE'),
                    intereses_mercado = COALESCE(intereses_mercado, 'AAPL,MSFT,TSLA'),
                    pais = COALESCE(pais, 'CO'),
                    solicita_comisionista = COALESCE(solicita_comisionista, false),
                    notificacion_email = COALESCE(notificacion_email, true),
                    notificacion_sms = COALESCE(notificacion_sms, false),
                    notificacion_whatsapp = COALESCE(notificacion_whatsapp, false),
                    tipos_notificacion = COALESCE(tipos_notificacion, 'ORDENES,MERCADO,SEGURIDAD'),
                    tipo_orden_default = COALESCE(tipo_orden_default, 'MARKET'),
                    vista_portafolio = COALESCE(vista_portafolio, 'LISTA'),
                    plan_suscripcion = COALESCE(plan_suscripcion, 'BASICO'),
                    es_premium = COALESCE(es_premium, false),
                    pendiente_cuenta_alpaca = COALESCE(pendiente_cuenta_alpaca, false),
                    fecha_creacion = COALESCE(fecha_creacion, now())
                """);
        ejecutarSeguro("UPDATE comisionista SET fecha_creacion = COALESCE(fecha_creacion, now())");
    }

    private void asegurarRestricciones() {
        addUniqueConstraint("usuario", "uk_usuario_correo", "correo");
        addUniqueConstraint("inversionista", "uk_inversionista_usuario", "usuario_id");
        addUniqueConstraint("comisionista", "uk_comisionista_usuario", "usuario_id");
        addForeignKey("inversionista", "fk_inversionista_usuario", "usuario_id", "usuario", "id");
        addForeignKey("comisionista", "fk_comisionista_usuario", "usuario_id", "usuario", "id");
    }

    private void addUniqueConstraint(String tabla, String nombre, String columna) {
        if (!tablaExiste(tabla) || constraintExiste(nombre)) {
            return;
        }
        ejecutarSeguro("ALTER TABLE " + tabla + " ADD CONSTRAINT " + nombre + " UNIQUE (" + columna + ")");
    }

    private void addForeignKey(String tabla, String nombre, String columna, String tablaReferencia, String columnaReferencia) {
        if (!tablaExiste(tabla) || !tablaExiste(tablaReferencia) || constraintExiste(nombre) || existenHuerfanos(tabla, columna, tablaReferencia, columnaReferencia)) {
            return;
        }
        ejecutarSeguro("ALTER TABLE " + tabla + " ADD CONSTRAINT " + nombre
                + " FOREIGN KEY (" + columna + ") REFERENCES " + tablaReferencia + "(" + columnaReferencia + ") ON DELETE CASCADE");
    }

    private void eliminarColumnasLegacyUsuario() {
        for (String columna : COLUMNAS_USUARIO_LEGACY) {
            ejecutarSeguro("ALTER TABLE usuario DROP COLUMN IF EXISTS " + columna);
        }
    }

    private void addColumn(String tabla, String columna, String tipo) {
        if (!tablaExiste(tabla)) {
            return;
        }
        ejecutarSeguro("ALTER TABLE " + tabla + " ADD COLUMN IF NOT EXISTS " + columna + " " + tipo);
    }

    private void ejecutarSeguro(String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception e) {
            log.warn("No se pudo ejecutar migracion de normalizacion de usuarios: {}", resumir(sql), e);
        }
    }

    private String resumir(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }

    private boolean tablaExiste(String tabla) {
        Integer count = namedJdbc.queryForObject("""
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = current_schema()
                  AND table_name = :tabla
                """, new MapSqlParameterSource("tabla", tabla), Integer.class);
        return count != null && count > 0;
    }

    private boolean columnaExiste(String tabla, String columna) {
        Integer count = namedJdbc.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = :tabla
                  AND column_name = :columna
                """, new MapSqlParameterSource()
                        .addValue("tabla", tabla)
                        .addValue("columna", columna), Integer.class);
        return count != null && count > 0;
    }

    private boolean constraintExiste(String nombre) {
        Integer count = namedJdbc.queryForObject("""
                SELECT count(*)
                FROM information_schema.table_constraints
                WHERE table_schema = current_schema()
                  AND constraint_name = :nombre
                """, new MapSqlParameterSource("nombre", nombre), Integer.class);
        return count != null && count > 0;
    }

    private boolean existenHuerfanos(String tabla, String columna, String tablaReferencia, String columnaReferencia) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*)
                FROM %s t
                LEFT JOIN %s r ON r.%s = t.%s
                WHERE t.%s IS NOT NULL
                  AND r.%s IS NULL
                """.formatted(tabla, tablaReferencia, columnaReferencia, columna, columna, columnaReferencia), Integer.class);
        return count != null && count > 0;
    }
}
