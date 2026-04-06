package com.epam.finaltask.repository;

import com.epam.finaltask.model.Voucher;
import com.epam.finaltask.model.enums.HotelType;
import com.epam.finaltask.model.enums.TourType;
import com.epam.finaltask.model.enums.TransferType;
import com.epam.finaltask.model.enums.VoucherStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class VoucherSpecification {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_TOUR_TYPE = "tourType";
    private static final String FIELD_TRANSFER_TYPE = "transferType";
    private static final String FIELD_HOTEL_TYPE = "hotelType";
    private static final String FIELD_PRICE = "price";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_ARRIVAL_DATE = "arrivalDate";

    private VoucherSpecification() {
    }

    public static Specification<Voucher> hasStatus(VoucherStatus status) {
        return (root, query, cb) -> cb.equal(root.get(FIELD_STATUS), status);
    }

    public static Specification<Voucher> hasTourType(String tourType) {
        return (root, query, cb) -> cb.equal(root.get(FIELD_TOUR_TYPE), TourType.valueOf(tourType));
    }

    public static Specification<Voucher> hasTransferType(String transferType) {
        return (root, query, cb) -> cb.equal(root.get(FIELD_TRANSFER_TYPE), TransferType.valueOf(transferType));
    }

    public static Specification<Voucher> hasHotelType(String hotelType) {
        return (root, query, cb) -> cb.equal(root.get(FIELD_HOTEL_TYPE), HotelType.valueOf(hotelType));
    }

    public static Specification<Voucher> priceBetween(Double minPrice, Double maxPrice) {
        return (root, query, cb) -> {
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get(FIELD_PRICE), minPrice, maxPrice);
            } else if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get(FIELD_PRICE), minPrice);
            } else {
                return cb.lessThanOrEqualTo(root.get(FIELD_PRICE), maxPrice);
            }
        };
    }

    public static Specification<Voucher> arrivalDateAfterToday() {
        return (root, query, cb) -> cb.greaterThan(root.get(FIELD_ARRIVAL_DATE), LocalDate.now());
    }

    public static Specification<Voucher> arrivalDateBeforeOrEqualToday() {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(FIELD_ARRIVAL_DATE), LocalDate.now());
    }

    public static Specification<Voucher> titleContains(String search) {
        String escaped = escapeLikeWildcards(search.toLowerCase());
        return (root, query, cb) -> cb.like(cb.lower(root.get(FIELD_TITLE)), "%" + escaped + "%");
    }

    private static String escapeLikeWildcards(String value) {
        return value.replace("%", "\\%").replace("_", "\\_");
    }
}
