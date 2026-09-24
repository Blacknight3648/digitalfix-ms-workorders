package df.digitalfix_ms_workorders.repository;

import df.digitalfix_ms_workorders.model.WorkOrder;
import df.digitalfix_ms_workorders.enums.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    List<WorkOrder> findByEstado(WorkOrderStatus estado);
}
