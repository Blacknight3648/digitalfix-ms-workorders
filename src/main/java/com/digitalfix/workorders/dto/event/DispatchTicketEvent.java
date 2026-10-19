package com.digitalfix.workorders.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchTicketEvent {

    private Long workOrderId;
    private String technicianId;
    private String crewId;
    private String serviceAddress;
    private String clientPhone;
    private LocalDateTime scheduledDate;
    private String serviceDescription;
}
