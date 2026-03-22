package com.parksmart.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RevenueReportResponse {

    private LocalDate from;
    private LocalDate to;
    private BigDecimal totalRevenue;
    private long completedBookings;
    private BigDecimal averagePerBooking;
}
