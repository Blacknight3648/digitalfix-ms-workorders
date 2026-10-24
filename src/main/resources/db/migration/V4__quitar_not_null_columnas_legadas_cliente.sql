-- =====================================================================
-- V4: columnas legadas de CLIENTE que bloqueaban todo INSERT
-- =====================================================================
-- Una versión anterior de la entidad Cliente usaba NOMBRES_CLIENTE,
-- APELLIDO_PATERNO_CLIENTE y APELLIDO_MATERNO_CLIENTE (hoy es NOMBRE_CLIENTE).
-- ddl-auto=update agrega columnas pero nunca quita las viejas, así que quedaron
-- con restricciones "IS NOT NULL" (SYS_C...) y todo INSERT fallaba con ORA-01400.
--
-- Solo se quitan esas restricciones: las columnas y sus datos se conservan. En una
-- base creada desde cero las columnas no existen y el bloque no hace nada.

BEGIN
    FOR r IN (
        SELECT DISTINCT c.constraint_name
        FROM user_constraints c
        JOIN user_cons_columns cc ON cc.constraint_name = c.constraint_name
        WHERE c.table_name = 'CLIENTE'
          AND c.constraint_type = 'C'
          AND cc.column_name IN ('NOMBRES_CLIENTE', 'APELLIDO_PATERNO_CLIENTE', 'APELLIDO_MATERNO_CLIENTE')
    ) LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE CLIENTE DROP CONSTRAINT ' || r.constraint_name;
    END LOOP;
END;
/
