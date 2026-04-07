package com.epam.finaltask.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUpDTO {

    @NotNull(message = "{validation.user.topup.required}")
    @DecimalMin(value = "0.01", message = "{validation.user.topup.min}")
    @DecimalMax(value = "99999999.99", message = "{validation.user.topup.max}")
    private BigDecimal amount;
}
