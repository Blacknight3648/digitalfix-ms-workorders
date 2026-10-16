-- Script V2 para la base de datos Oracle de ms-workorders
-- WORK_ORDERS (cliente/servicio/tecnico como texto libre) se reemplaza por el modelo normalizado
-- ORDEN_TRABAJO + DETALLE_OT_SERVICIO + DETALLE_OT_REPUESTO. Sus datos no son migrables de forma
-- automatica porque los nuevos registros referencian clientes y servicios por ID.
DROP TABLE WORK_ORDERS CASCADE CONSTRAINTS PURGE;
