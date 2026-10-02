package com.example.seatlock.dto;

import com.example.seatlock.model.SeatStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private String tier;
    private BigDecimal price;
    private SeatStatus status;
}