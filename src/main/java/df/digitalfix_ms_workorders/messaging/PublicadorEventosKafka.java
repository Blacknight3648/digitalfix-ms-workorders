package df.digitalfix_ms_workorders.messaging;

import df.digitalfix_ms_workorders.dto.event.WorkOrderKafkaEvent;

/**
 * Puerto de salida para publicar eventos de ciclo de vida de órdenes de trabajo en el bus de
 * streaming (tópico {@code workorders.events} en Apache Kafka).
 */
public interface PublicadorEventosKafka {

    void publicarEventoOrden(WorkOrderKafkaEvent evento);
}
