package com.epam.finaltask.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeStatusDTO {

    @NotNull(message = "{validation.voucher.status.required}")
    @Pattern(regexp = "PAID|CANCELED", message = "{validation.voucher.status.invalid}")
    private String status;
}
