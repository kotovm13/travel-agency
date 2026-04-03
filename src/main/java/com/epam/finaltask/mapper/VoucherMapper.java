package com.epam.finaltask.mapper;

import com.epam.finaltask.dto.request.VoucherCreateDTO;
import com.epam.finaltask.dto.response.VoucherDTO;
import com.epam.finaltask.model.Voucher;

public interface VoucherMapper {
    Voucher toVoucher(VoucherDTO voucherDTO);
    Voucher toVoucher(VoucherCreateDTO createDTO);
    void updateVoucherFromDTO(VoucherCreateDTO createDTO, Voucher voucher);
    VoucherDTO toVoucherDTO(Voucher voucher);
}
