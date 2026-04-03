package com.epam.finaltask.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherFilterDTO {

    private String tourType;
    private String transferType;
    private String hotelType;
    private Double minPrice;
    private Double maxPrice;
    private String search;
    private String sort;
}
