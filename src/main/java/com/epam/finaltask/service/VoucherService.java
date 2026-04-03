package com.epam.finaltask.service;

import com.epam.finaltask.dto.request.VoucherCreateDTO;
import com.epam.finaltask.dto.request.VoucherFilterDTO;
import com.epam.finaltask.dto.response.VoucherDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VoucherService {

    VoucherDTO create(VoucherCreateDTO request);

    VoucherDTO update(UUID id, VoucherCreateDTO request);

    void delete(UUID id);

    VoucherDTO markHot(UUID id, boolean hot);

    VoucherDTO setDiscount(UUID id, Integer discount);

    VoucherDTO getById(UUID id);

    Page<VoucherDTO> findFiltered(VoucherFilterDTO filter, Pageable pageable);

    Page<VoucherDTO> findAllForManager(String search, String dateFilter, String tourType, Pageable pageable);
}
