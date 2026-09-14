package df.digitalfix_ms_workorders.controller;

import df.digitalfix_ms_workorders.dto.WorkOrderRequest;
import df.digitalfix_ms_workorders.dto.WorkOrderResponse;
import df.digitalfix_ms_workorders.dto.WorkOrderStatusRequest;
import df.digitalfix_ms_workorders.enums.WorkOrderStatus;
import df.digitalfix_ms_workorders.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CRUD basico de ordenes de trabajo (seccion 3 del enunciado del caso:
// Gestion de ordenes). Sin autenticacion propia por ahora: en la arquitectura
// objetivo, este microservicio solo lo llama ms-digitalfix-bff, que es el que
// valida el JWT de Azure AD antes de reenviar la peticion.
@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService service;

    @PostMapping
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<WorkOrderResponse>> findAll(
            @RequestParam(required = false) WorkOrderStatus estado) {
        return ResponseEntity.ok(service.findAll(estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> update(
            @PathVariable Long id, @Valid @RequestBody WorkOrderRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<WorkOrderResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody WorkOrderStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
