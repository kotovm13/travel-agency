package com.epam.finaltask.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatsDTO {

    private long totalUsers;
    private long activeUsers;
    private long availableVouchers;
    private long registeredOrders;
    private long paidOrders;
    private long canceledOrders;
    private double totalRevenue;
}
