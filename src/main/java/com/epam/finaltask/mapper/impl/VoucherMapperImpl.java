package com.epam.finaltask.mapper.impl;

import com.epam.finaltask.dto.request.VoucherCreateDTO;
import com.epam.finaltask.dto.response.VoucherDTO;
import com.epam.finaltask.mapper.VoucherMapper;
import com.epam.finaltask.model.Voucher;
import com.epam.finaltask.model.enums.HotelType;
import com.epam.finaltask.model.enums.TourType;
import com.epam.finaltask.model.enums.TransferType;
import com.epam.finaltask.model.enums.VoucherStatus;
import org.springframework.stereotype.Component;

@Component
public class VoucherMapperImpl implements VoucherMapper {

    @Override
    public Voucher toVoucher(VoucherDTO dto) {
        if (dto == null) return null;

        Voucher voucher = new Voucher();
        voucher.setId(dto.getId());
        voucher.setTitle(dto.getTitle());
        voucher.setDescription(dto.getDescription());
        voucher.setPrice(dto.getPrice());
        if (dto.getTourType() != null) voucher.setTourType(TourType.valueOf(dto.getTourType()));
        if (dto.getTransferType() != null) voucher.setTransferType(TransferType.valueOf(dto.getTransferType()));
        if (dto.getHotelType() != null) voucher.setHotelType(HotelType.valueOf(dto.getHotelType()));
        if (dto.getStatus() != null) voucher.setStatus(VoucherStatus.valueOf(dto.getStatus()));
        voucher.setArrivalDate(dto.getArrivalDate());
        voucher.setEvictionDate(dto.getEvictionDate());
        voucher.setHot(dto.isHot());
        voucher.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : 0);
        voucher.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);
        return voucher;
    }

    @Override
    public Voucher toVoucher(VoucherCreateDTO dto) {
        if (dto == null) return null;

        return Voucher.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .tourType(TourType.valueOf(dto.getTourType()))
                .transferType(TransferType.valueOf(dto.getTransferType()))
                .hotelType(HotelType.valueOf(dto.getHotelType()))
                .status(VoucherStatus.AVAILABLE)
                .arrivalDate(dto.getArrivalDate())
                .evictionDate(dto.getEvictionDate())
                .isHot(dto.isHot())
                .discount(dto.getDiscount())
                .quantity(dto.getQuantity())
                .build();
    }

    @Override
    public void updateVoucherFromDTO(VoucherCreateDTO dto, Voucher voucher) {
        voucher.setTitle(dto.getTitle());
        voucher.setDescription(dto.getDescription());
        voucher.setPrice(dto.getPrice());
        voucher.setTourType(TourType.valueOf(dto.getTourType()));
        voucher.setTransferType(TransferType.valueOf(dto.getTransferType()));
        voucher.setHotelType(HotelType.valueOf(dto.getHotelType()));
        voucher.setArrivalDate(dto.getArrivalDate());
        voucher.setEvictionDate(dto.getEvictionDate());
        voucher.setHot(dto.isHot());
        voucher.setDiscount(dto.getDiscount());
        voucher.setQuantity(dto.getQuantity());
    }

    @Override
    public VoucherDTO toVoucherDTO(Voucher voucher) {
        if (voucher == null) return null;

        return VoucherDTO.builder()
                .id(voucher.getId())
                .title(voucher.getTitle())
                .description(voucher.getDescription())
                .price(voucher.getPrice())
                .tourType(voucher.getTourType() != null ? voucher.getTourType().name() : null)
                .transferType(voucher.getTransferType() != null ? voucher.getTransferType().name() : null)
                .hotelType(voucher.getHotelType() != null ? voucher.getHotelType().name() : null)
                .status(voucher.getStatus() != null ? voucher.getStatus().name() : null)
                .arrivalDate(voucher.getArrivalDate())
                .evictionDate(voucher.getEvictionDate())
                .hot(voucher.isHot())
                .discount(voucher.getDiscount())
                .quantity(voucher.getQuantity())
                .build();
    }
}
