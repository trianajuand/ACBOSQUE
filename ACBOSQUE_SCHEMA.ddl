-- Generado por Oracle SQL Developer Data Modeler 24.3.1.351.0831
--   en:        2026-05-24 19:25:44 COT
--   sitio:      Oracle Database 11g
--   tipo:      Oracle Database 11g



-- predefined type, no DDL - MDSYS.SDO_GEOMETRY

-- predefined type, no DDL - XMLTYPE

CREATE TABLE Activo 
    ( 
     id1               INTEGER  NOT NULL , 
     Mercado_Config_id INTEGER  NOT NULL , 
     tipo              VARCHAR2 (255) , 
     ticker            VARCHAR2 (10) , 
     descripcion       VARCHAR2 (500) , 
     nombre_empresa    VARCHAR2 (255) 
    ) 
;

ALTER TABLE Activo 
    ADD CONSTRAINT Activo_PK PRIMARY KEY ( id1 ) ;

CREATE TABLE Administrador 
    ( 
     id    INTEGER  NOT NULL , 
     cargo VARCHAR2 (255) 
    ) 
;

ALTER TABLE Administrador 
    ADD CONSTRAINT Administrador_PK PRIMARY KEY ( id ) ;

ALTER TABLE Administrador 
    ADD CONSTRAINT Administrador_PKv1 UNIQUE ( id ) ;

CREATE TABLE asignacion_comisionista 
    ( 
     Comisionista_id  INTEGER  NOT NULL , 
     inversionista_id INTEGER  NOT NULL , 
     activo           CHAR (1) , 
     fecha_asignacion DATE , 
     motivo           VARCHAR2 (255) 
    ) 
;

ALTER TABLE asignacion_comisionista 
    ADD CONSTRAINT asignacion_comisionistav1_PK PRIMARY KEY ( Comisionista_id, inversionista_id ) ;

CREATE TABLE codigo_verificacion 
    ( 
     id         INTEGER  NOT NULL , 
     codigo     VARCHAR2 (255) , 
     correo     VARCHAR2 (255) , 
     expiracion TIMESTAMP , 
     tipo       VARCHAR2 (255) , 
     usado      CHAR (1) , 
     Usuario_id INTEGER  NOT NULL 
    ) 
;

ALTER TABLE codigo_verificacion 
    ADD CONSTRAINT codigo_verificacion_PK PRIMARY KEY ( id ) ;

CREATE TABLE Comisionista 
    ( 
     id INTEGER  NOT NULL 
    ) 
;

ALTER TABLE Comisionista 
    ADD CONSTRAINT Comisionista_PK PRIMARY KEY ( id ) ;

ALTER TABLE Comisionista 
    ADD CONSTRAINT Comisionista_PKv1 UNIQUE ( id ) ;

CREATE TABLE Comisionista_Especialidad 
    ( 
     Comisionista_id INTEGER  NOT NULL , 
     Especialidad_id INTEGER  NOT NULL 
    ) 
;

ALTER TABLE Comisionista_Especialidad 
    ADD CONSTRAINT Comisionista_Especialidad_PK PRIMARY KEY ( Comisionista_id, Especialidad_id ) ;

CREATE TABLE Cuenta_Fondos 
    ( 
     inversionista_id  INTEGER  NOT NULL , 
     actualizado_en    TIMESTAMP , 
     fondos_reservados NUMBER (18,4) , 
     saldo_disponible  NUMBER (18,4) 
    ) 
;
CREATE UNIQUE INDEX Cuenta_Fondos__IDX ON Cuenta_Fondos 
    ( 
     inversionista_id ASC 
    ) 
;

ALTER TABLE Cuenta_Fondos 
    ADD CONSTRAINT Cuenta_Fondos_PK PRIMARY KEY ( inversionista_id ) ;

CREATE TABLE Especialidad 
    ( 
     id                       INTEGER  NOT NULL , 
     titulo                   VARCHAR2 (255) , 
     descripcion_especialidad VARCHAR2 (255) , 
     tipo_especialidad        VARCHAR2 (255) 
    ) 
;

ALTER TABLE Especialidad 
    ADD CONSTRAINT Especialidad_PK PRIMARY KEY ( id ) ;

CREATE TABLE evento_auditoria 
    ( 
     id          INTEGER  NOT NULL , 
     detalle     VARCHAR2 (500) , 
     timestamp   TIMESTAMP , 
     tipo_evento VARCHAR2 (50) , 
     Usuario_id  INTEGER  NOT NULL 
    ) 
;

ALTER TABLE evento_auditoria 
    ADD CONSTRAINT evento_auditoria_PK PRIMARY KEY ( id ) ;

CREATE TABLE feriado_mercado 
    ( 
     id                INTEGER  NOT NULL , 
     creado_en         TIMESTAMP , 
     descripcion       VARCHAR2 (255) , 
     fecha             DATE , 
     Mercado_Config_id INTEGER  NOT NULL 
    ) 
;

ALTER TABLE feriado_mercado 
    ADD CONSTRAINT feriado_mercado_PK PRIMARY KEY ( id ) ;

CREATE TABLE holding 
    ( 
     Activo_ID        INTEGER  NOT NULL , 
     inversionista_id INTEGER  NOT NULL , 
     actualizado_en   TIMESTAMP , 
     cantidad         NUMBER (18,6) , 
     precio_promedio  NUMBER (18,6) 
    ) 
;

ALTER TABLE holding 
    ADD CONSTRAINT holding_PK PRIMARY KEY ( inversionista_id, Activo_ID ) ;

CREATE TABLE Integracion_Inversionista 
    ( 
     alpaca_account_id       VARCHAR2 (255) , 
     pendiente_cuenta_alpaca CHAR (1) , 
     stripe_customer_id      VARCHAR2 (255) , 
     stripe_suscripcion_id   VARCHAR2 (255) , 
     inversionista_id        INTEGER  NOT NULL 
    ) 
;

ALTER TABLE Integracion_Inversionista 
    ADD CONSTRAINT Integracion_Inversionista_PK PRIMARY KEY ( inversionista_id ) ;

CREATE TABLE intento_fallido 
    ( 
     id              INTEGER  NOT NULL , 
     bloqueado_hasta DATE , 
     contador        INTEGER , 
     correo          VARCHAR2 (255) , 
     ultimo_intento  TIMESTAMP , 
     Usuario_id      INTEGER  NOT NULL 
    ) 
;
CREATE UNIQUE INDEX intento_fallido__IDX ON intento_fallido 
    ( 
     Usuario_id ASC 
    ) 
;

ALTER TABLE intento_fallido 
    ADD CONSTRAINT intento_fallido_PK PRIMARY KEY ( id ) ;

CREATE TABLE Interes_Mercado 
    ( 
     Intereses_mercado_id INTEGER  NOT NULL , 
     Mercado_Config_id    INTEGER  NOT NULL 
    ) 
;

ALTER TABLE Interes_Mercado 
    ADD CONSTRAINT Referirv1_PK PRIMARY KEY ( Intereses_mercado_id, Mercado_Config_id ) ;

CREATE TABLE Intereses 
    ( 
     id                  INTEGER  NOT NULL , 
     titulo              VARCHAR2 (255) , 
     descripcion_interes VARCHAR2 (255) , 
     tipo_interes        VARCHAR2 (255) 
    ) 
;

ALTER TABLE Intereses 
    ADD CONSTRAINT Intereses_mercado_PK PRIMARY KEY ( id ) ;

CREATE TABLE Intereses_Inversionista 
    ( 
     inversionista_id     INTEGER  NOT NULL , 
     Intereses_mercado_id INTEGER  NOT NULL 
    ) 
;

ALTER TABLE Intereses_Inversionista 
    ADD CONSTRAINT Tener_PK PRIMARY KEY ( inversionista_id, Intereses_mercado_id ) ;

CREATE TABLE inversionista 
    ( 
     id                    INTEGER  NOT NULL , 
     ciudad                VARCHAR2 (255) , 
     codigo_postal         VARCHAR2 (255) , 
     direccion             VARCHAR2 (255) , 
     estilo_trading        VARCHAR2 (255) , 
     nivel_experiencia     VARCHAR2 (255) , 
     pais                  VARCHAR2 (255) , 
     ingresos_min          NUMBER (18,4) , 
     ingresos_max          NUMBER (18,4) , 
     solicita_comisionista CHAR (1) , 
     tipo_identificacion   VARCHAR2 (255) , 
     tipo_orden_default    VARCHAR2 (255) , 
     tipo_notificacion     VARCHAR2 (255) , 
     vista_portafolio      VARCHAR2 (255) 
    ) 
;

ALTER TABLE inversionista 
    ADD CONSTRAINT inversionista_PK PRIMARY KEY ( id ) ;

CREATE TABLE Mercado_Config 
    ( 
     id                  INTEGER  NOT NULL , 
     cierre_anticipado   DATE , 
     codigo              VARCHAR2 (30) , 
     fecha_actualizacion TIMESTAMP , 
     habilitado          CHAR (1) , 
     hora_apertura       DATE , 
     hora_cierre         DATE , 
     nombre              VARCHAR2 (255) , 
     zona_horaria        VARCHAR2 (30) 
    ) 
;

ALTER TABLE Mercado_Config 
    ADD CONSTRAINT Mercado_Config_PK PRIMARY KEY ( id ) ;

CREATE TABLE Orden 
    ( 
     id                    INTEGER  NOT NULL , 
     alpaca_order_id       VARCHAR2 (100) , 
     cancelada_en          TIMESTAMP , 
     cantidad              NUMBER (18,4) , 
     creada_en             TIMESTAMP , 
     ejecutada_en          TIMESTAMP , 
     estado                VARCHAR2 (30) , 
     ip_origen             VARCHAR2 (30) , 
     lado                  VARCHAR2 (10) , 
     monto_neto            NUMBER (18,4) , 
     monto_total           NUMBER (18,4) , 
     precio_ejecucion      NUMBER (18,4) , 
     precio_limite         NUMBER (18,4) , 
     precio_stop           NUMBER (18,4) , 
     tipo_orden            VARCHAR2 (20) , 
     inversionista_id      INTEGER  NOT NULL , 
     parametro_comision_id INTEGER  NOT NULL , 
     Activo_ID             INTEGER  NOT NULL , 
     Propuesta_Orden_ID    NUMBER  NULL 
    ) 
;
CREATE UNIQUE INDEX Orden__IDX ON Orden 
    ( 
     Propuesta_Orden_ID ASC 
    ) 
;

ALTER TABLE Orden 
    ADD CONSTRAINT Orden_PK PRIMARY KEY ( id ) ;

CREATE TABLE parametro_comision 
    ( 
     id                  INTEGER  NOT NULL , 
     fecha_inicio        DATE , 
     fecha_fin           DATE , 
     porcentaje_comision NUMBER (6,2) , 
     split_comisionista  NUMBER (6,2) , 
     split_plataforma    NUMBER (6,2) 
    ) 
;

ALTER TABLE parametro_comision 
    ADD CONSTRAINT parametro_comision_PK PRIMARY KEY ( id ) ;

CREATE TABLE precio_cache 
    ( 
     Activo_ID              INTEGER  NOT NULL , 
     actualizado_en         TIMESTAMP , 
     fuente                 VARCHAR2 (20) , 
     precio_actual          NUMBER (18,4) , 
     precio_cierre_anterior NUMBER (18,4) , 
     precio_maximo          NUMBER (18,4) , 
     precio_minimo          NUMBER (18,4) , 
     simbolo                VARCHAR2 (20) , 
     variacion_porcentual   NUMBER (8,4) , 
     volumen                INTEGER 
    ) 
;
CREATE UNIQUE INDEX precio_cache__IDX ON precio_cache 
    ( 
     Activo_ID ASC 
    ) 
;

ALTER TABLE precio_cache 
    ADD CONSTRAINT precio_cache_PK PRIMARY KEY ( Activo_ID ) ;

CREATE TABLE Propuesta_Orden 
    ( 
     Propuesta_Orden_ID       NUMBER  NOT NULL , 
     estado                   VARCHAR2 (255) , 
     aprobada_en              TIMESTAMP , 
     comentario_comisionsita  VARCHAR2 (500) , 
     comentario_inversionista VARCHAR2 (500) , 
     rechazada_en             TIMESTAMP , 
     firmada_en               TIMESTAMP , 
     inversionista_id         INTEGER  NOT NULL , 
     Comisionista_id          INTEGER  NOT NULL 
    ) 
;

ALTER TABLE Propuesta_Orden 
    ADD CONSTRAINT Propuesta_Orden_PK PRIMARY KEY ( Propuesta_Orden_ID ) ;

CREATE TABLE Suscripcion 
    ( 
     inversionista_id         INTEGER  NOT NULL , 
     codigo_suscripcion       VARCHAR2 (255) , 
     es_premium               CHAR (1) , 
     fecha_expiracion_premium DATE , 
     plan_suscripcion         VARCHAR2 (255) 
    ) 
;

ALTER TABLE Suscripcion 
    ADD CONSTRAINT Suscripcion_PK PRIMARY KEY ( inversionista_id ) ;

CREATE TABLE Usuario 
    ( 
     id                    INTEGER  NOT NULL , 
     contrasenia           VARCHAR2 (255) , 
     nombre_completo       VARCHAR2 (255) , 
     numero_identificacion VARCHAR2 (255) , 
     telefono              VARCHAR2 (255) , 
     correo                VARCHAR2 (255) , 
     estado_cuenta         VARCHAR2 (25) , 
     notificacion_email    CHAR (1) , 
     notificacion_sms      CHAR (1) , 
     notificacion_whatsapp CHAR (1) , 
     fecha_actualizacion   TIMESTAMP , 
     fecha_creacion        TIMESTAMP , 
     fecha_nacimiento      DATE , 
     mfa_habilitado        CHAR (1) , 
     rol                   VARCHAR2 (255) 
    ) 
;

ALTER TABLE Usuario 
    ADD CONSTRAINT CH_INH_Usuario 
    CHECK (id IN (Administrador, Comisionista, inversionista)) 
;
CREATE UNIQUE INDEX Usuario__IDX ON Usuario 
    ( 
     id ASC 
    ) 
;

ALTER TABLE Usuario 
    ADD CONSTRAINT Usuario_PK PRIMARY KEY ( id ) ;

ALTER TABLE asignacion_comisionista 
    ADD CONSTRAINT "_inversionista_FK" FOREIGN KEY 
    ( 
     inversionista_id
    ) 
    REFERENCES inversionista 
    ( 
     id
    ) 
;

ALTER TABLE Activo 
    ADD CONSTRAINT Activo_Mercado_Config_FK FOREIGN KEY 
    ( 
     Mercado_Config_id
    ) 
    REFERENCES Mercado_Config 
    ( 
     id
    ) 
;

ALTER TABLE Administrador 
    ADD CONSTRAINT Administrador_Usuario_FK FOREIGN KEY 
    ( 
     id
    ) 
    REFERENCES Usuario 
    ( 
     id
    ) 
;

ALTER TABLE codigo_verificacion 
    ADD CONSTRAINT codigo_verificacion_Usuario_FK FOREIGN KEY 
    ( 
     Usuario_id
    ) 
    REFERENCES Usuario 
    ( 
     id
    ) 
;

ALTER TABLE asignacion_comisionista 
    ADD CONSTRAINT Comisionista_FK FOREIGN KEY 
    ( 
     Comisionista_id
    ) 
    REFERENCES Comisionista 
    ( 
     id
    ) 
;

ALTER TABLE Propuesta_Orden 
    ADD CONSTRAINT Comisionista_FKv1 FOREIGN KEY 
    ( 
     Comisionista_id
    ) 
    REFERENCES Comisionista 
    ( 
     id
    ) 
;

ALTER TABLE Comisionista_Especialidad 
    ADD CONSTRAINT Comisionista_FKv3 FOREIGN KEY 
    ( 
     Comisionista_id
    ) 
    REFERENCES Comisionista 
    ( 
     id
    ) 
;

ALTER TABLE Comisionista 
    ADD CONSTRAINT Comisionista_Usuario_FK FOREIGN KEY 
    ( 
     id
    ) 
    REFERENCES Usuario 
    ( 
     id
    ) 
;

ALTER TABLE Cuenta_Fondos 
    ADD CONSTRAINT Cuenta_Fondos_inversionista_FK FOREIGN KEY 
    ( 
     inversionista_id
    ) 
    REFERENCES inversionista 
    ( 
     id
    ) 
;

ALTER TABLE Comisionista_Especialidad 
    ADD CONSTRAINT Especialidad_FK FOREIGN KEY 
    ( 
     Especialidad_id
    ) 
    REFERENCES Especialidad 
    ( 
     id
    ) 
;

ALTER TABLE evento_auditoria 
    ADD CONSTRAINT evento_auditoria_Usuario_FK FOREIGN KEY 
    ( 
     Usuario_id
    ) 
    REFERENCES Usuario 
    ( 
     id
    ) 
;

ALTER TABLE holding 
    ADD CONSTRAINT holding_Activo_FK FOREIGN KEY 
    ( 
     Activo_ID
    ) 
    REFERENCES Activo 
    ( 
     id1
    ) 
;

ALTER TABLE holding 
    ADD CONSTRAINT holding_inversionista_FK FOREIGN KEY 
    ( 
     inversionista_id
    ) 
    REFERENCES inversionista 
    ( 
     id
    ) 
;

ALTER TABLE intento_fallido 
    ADD CONSTRAINT intento_fallido_Usuario_FK FOREIGN KEY 
    ( 
     Usuario_id
    ) 
    REFERENCES Usuario 
    ( 
     id
    ) 
;

ALTER TABLE Integracion_Inversionista 
    ADD CONSTRAINT inversionista_FK FOREIGN KEY 
    ( 
     inversionista_id
    ) 
    REFERENCES inversionista 
    ( 
     id
    ) 
;

ALTER TABLE Propuesta_Orden 
    ADD CONSTRAINT inversionista_FKv1 FOREIGN KEY 
    ( 
     inversionista_id
    ) 
    REFERENCES inversionista 
    ( 
     id
    ) 
;

ALTER TABLE inversionista 
    ADD CONSTRAINT inversionista_Usuario_FK FOREIGN KEY 
    ( 
     id
    ) 
    REFERENCES Usuario 
    ( 
     id
    ) 
;

ALTER TABLE feriado_mercado 
    ADD CONSTRAINT Mercado_Config_FK FOREIGN KEY 
    ( 
     Mercado_Config_id
    ) 
    REFERENCES Mercado_Config 
    ( 
     id
    ) 
;

ALTER TABLE Orden 
    ADD CONSTRAINT Orden_Activo_FK FOREIGN KEY 
    ( 
     Activo_ID
    ) 
    REFERENCES Activo 
    ( 
     id1
    ) 
;

ALTER TABLE Orden 
    ADD CONSTRAINT Orden_inversionista_FK FOREIGN KEY 
    ( 
     inversionista_id
    ) 
    REFERENCES inversionista 
    ( 
     id
    ) 
;

ALTER TABLE Orden 
    ADD CONSTRAINT Orden_parametro_comision_FK FOREIGN KEY 
    ( 
     parametro_comision_id
    ) 
    REFERENCES parametro_comision 
    ( 
     id
    ) 
;

ALTER TABLE Orden 
    ADD CONSTRAINT Orden_Propuesta_Orden_FK FOREIGN KEY 
    ( 
     Propuesta_Orden_ID
    ) 
    REFERENCES Propuesta_Orden 
    ( 
     Propuesta_Orden_ID
    ) 
;

ALTER TABLE precio_cache 
    ADD CONSTRAINT precio_cache_Activo_FK FOREIGN KEY 
    ( 
     Activo_ID
    ) 
    REFERENCES Activo 
    ( 
     id1
    ) 
;

ALTER TABLE Interes_Mercado 
    ADD CONSTRAINT Referirv1_Intereses_mercado_FK FOREIGN KEY 
    ( 
     Intereses_mercado_id
    ) 
    REFERENCES Intereses 
    ( 
     id
    ) 
;

ALTER TABLE Interes_Mercado 
    ADD CONSTRAINT Referirv1_Mercado_Config_FK FOREIGN KEY 
    ( 
     Mercado_Config_id
    ) 
    REFERENCES Mercado_Config 
    ( 
     id
    ) 
;

ALTER TABLE Suscripcion 
    ADD CONSTRAINT Suscripcion_inversionista_FK FOREIGN KEY 
    ( 
     inversionista_id
    ) 
    REFERENCES inversionista 
    ( 
     id
    ) 
;

ALTER TABLE Intereses_Inversionista 
    ADD CONSTRAINT Tener_Intereses_mercado_FK FOREIGN KEY 
    ( 
     Intereses_mercado_id
    ) 
    REFERENCES Intereses 
    ( 
     id
    ) 
;

ALTER TABLE Intereses_Inversionista 
    ADD CONSTRAINT Tener_inversionista_FK FOREIGN KEY 
    ( 
     inversionista_id
    ) 
    REFERENCES inversionista 
    ( 
     id
    ) 
;

CREATE SEQUENCE Activo_id1_SEQ 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER Activo_id1_TRG 
BEFORE INSERT ON Activo 
FOR EACH ROW 
WHEN (NEW.id1 IS NULL) 
BEGIN 
    :NEW.id1 := Activo_id1_SEQ.NEXTVAL; 
END;
/

CREATE SEQUENCE Propuesta_Orden_Propuesta_Orde 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER Propuesta_Orden_Propuesta_Orde 
BEFORE INSERT ON Propuesta_Orden 
FOR EACH ROW 
WHEN (NEW.Propuesta_Orden_ID IS NULL) 
BEGIN 
    :NEW.Propuesta_Orden_ID := Propuesta_Orden_Propuesta_Orde.NEXTVAL; 
END;
/



-- Informe de Resumen de Oracle SQL Developer Data Modeler: 
-- 
-- CREATE TABLE                            24
-- CREATE INDEX                             5
-- ALTER TABLE                             55
-- CREATE VIEW                              0
-- ALTER VIEW                               0
-- CREATE PACKAGE                           0
-- CREATE PACKAGE BODY                      0
-- CREATE PROCEDURE                         0
-- CREATE FUNCTION                          0
-- CREATE TRIGGER                           2
-- ALTER TRIGGER                            0
-- CREATE COLLECTION TYPE                   0
-- CREATE STRUCTURED TYPE                   0
-- CREATE STRUCTURED TYPE BODY              0
-- CREATE CLUSTER                           0
-- CREATE CONTEXT                           0
-- CREATE DATABASE                          0
-- CREATE DIMENSION                         0
-- CREATE DIRECTORY                         0
-- CREATE DISK GROUP                        0
-- CREATE ROLE                              0
-- CREATE ROLLBACK SEGMENT                  0
-- CREATE SEQUENCE                          2
-- CREATE MATERIALIZED VIEW                 0
-- CREATE MATERIALIZED VIEW LOG             0
-- CREATE SYNONYM                           0
-- CREATE TABLESPACE                        0
-- CREATE USER                              0
-- 
-- DROP TABLESPACE                          0
-- DROP DATABASE                            0
-- 
-- REDACTION POLICY                         0
-- 
-- ORDS DROP SCHEMA                         0
-- ORDS ENABLE SCHEMA                       0
-- ORDS ENABLE OBJECT                       0
-- 
-- ERRORS                                   0
-- WARNINGS                                 0
