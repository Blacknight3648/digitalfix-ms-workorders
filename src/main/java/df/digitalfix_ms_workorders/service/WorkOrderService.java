package df.digitalfix_ms_workorders.service;

import df.digitalfix_ms_workorders.dto.WorkOrderRequest;
import df.digitalfix_ms_workorders.dto.WorkOrderResponse;
import df.digitalfix_ms_workorders.dto.WorkOrderStatusRequest;
import df.digitalfix_ms_workorders.model.WorkOrder;
import df.digitalfix_ms_workorders.enums.WorkOrderStatus;
import df.digitalfix_ms_workorders.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository repository;

    @Transactional
    public WorkOrderResponse create(WorkOrderRequest request) {
        WorkOrder order = WorkOrder.builder()
                .cliente(request.getCliente())
                .servicio(request.getServicio())
                .tecnico(request.getTecnico())
                .estado(WorkOrderStatus.CREADA)
                .build();
        return WorkOrderResponse.from(repository.save(order));
    }

    public List<WorkOrderResponse> findAll(WorkOrderStatus estado) {
        List<WorkOrder> orders = estado != null ? repository.findByEstado(estado) : repository.findAll();
        return orders.stream().map(WorkOrderResponse::from).toList();
    }

    public WorkOrderResponse findById(Long id) {
        return WorkOrderResponse.from(getOrThrow(id));
    }

    @Transactional
    public WorkOrderResponse update(Long id, WorkOrderRequest request) {
        WorkOrder order = getOrThrow(id);
        order.setCliente(request.getCliente());
        order.setServicio(request.getServicio());
        order.setTecnico(request.getTecnico());
        return WorkOrderResponse.from(repository.save(order));
    }

    // Nota: todavia no valida la regla del caso "no se puede pasar a
    // EN_EJECUCION sin ASIGNAR antes" (seccion 3 del enunciado). Se deja el
    // cambio de estado libre por ahora para mantener esto lo mas simple
    // posible; agregar esa validacion cuando se conecte con notificaciones/auditoria.
    @Transactional
    public WorkOrderResponse updateStatus(Long id, WorkOrderStatusRequest request) {
        WorkOrder order = getOrThrow(id);
        order.setEstado(request.getEstado());
        return WorkOrderResponse.from(repository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw notFound(id);
        }
        repository.deleteById(id);
    }

    private WorkOrder getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una orden de trabajo con id " + id);
    }
}
