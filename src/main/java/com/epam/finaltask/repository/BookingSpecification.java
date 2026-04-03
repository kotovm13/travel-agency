package com.epam.finaltask.repository;

import com.epam.finaltask.model.Booking;
import com.epam.finaltask.model.enums.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

public final class BookingSpecification {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_VOUCHER = "voucher";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_USER = "user";
    private static final String FIELD_USERNAME = "username";

    private BookingSpecification() {
    }

    public static Specification<Booking> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get(FIELD_STATUS), BookingStatus.valueOf(status));
    }

    public static Specification<Booking> voucherTitleContains(String search) {
        String escaped = escapeLikeWildcards(search.toLowerCase());
        return (root, query, cb) -> cb.like(cb.lower(root.get(FIELD_VOUCHER).get(FIELD_TITLE)), "%" + escaped + "%");
    }

    public static Specification<Booking> usernameContains(String username) {
        String escaped = escapeLikeWildcards(username.toLowerCase());
        return (root, query, cb) -> cb.like(cb.lower(root.get(FIELD_USER).get(FIELD_USERNAME)), "%" + escaped + "%");
    }

    private static String escapeLikeWildcards(String value) {
        return value.replace("%", "\\%").replace("_", "\\_");
    }
}
