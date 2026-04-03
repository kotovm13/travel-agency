package com.epam.finaltask.dto.request;

import com.epam.finaltask.validation.DateRange;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DateRange
public class VoucherCreateDTO {

    @NotBlank(message = "{validation.voucher.title.required}")
    @Size(max = 255, message = "{validation.voucher.title.size}")
    private String title;

    @Size(max = 1000, message = "{validation.voucher.description.size}")
    private String description;

    @NotNull(message = "{validation.voucher.price.required}")
    @Positive(message = "{validation.voucher.price.positive}")
    private Double price;

    @NotNull(message = "{validation.voucher.tourType.required}")
    private String tourType;

    @NotNull(message = "{validation.voucher.transferType.required}")
    private String transferType;

    @NotNull(message = "{validation.voucher.hotelType.required}")
    private String hotelType;

    @NotNull(message = "{validation.voucher.arrivalDate.required}")
    @Future(message = "{validation.voucher.arrivalDate.future}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate arrivalDate;

    @NotNull(message = "{validation.voucher.evictionDate.required}")
    @Future(message = "{validation.voucher.evictionDate.future}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate evictionDate;

    @Builder.Default
    private boolean hot = false;

    @Min(value = 0, message = "{validation.voucher.discount.min}")
    @Max(value = 100, message = "{validation.voucher.discount.max}")
    @Builder.Default
    private Integer discount = 0;

    @NotNull(message = "{validation.voucher.quantity.required}")
    @Min(value = 1, message = "{validation.voucher.quantity.min}")
    @Builder.Default
    private Integer quantity = 1;
}
