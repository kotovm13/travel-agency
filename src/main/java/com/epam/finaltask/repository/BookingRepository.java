package com.epam.finaltask.repository;

import com.epam.finaltask.model.Booking;
import com.epam.finaltask.model.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

    @EntityGraph(attributePaths = {"user", "voucher"})
    Page<Booking> findAllByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "voucher"})
    Page<Booking> findAllByUserIdAndStatus(UUID userId, BookingStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "voucher"})
    Page<Booking> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "voucher"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "voucher"})
    Optional<Booking> findById(UUID id);

    long countByStatus(BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.bookedPrice), 0) FROM Booking b WHERE b.status = 'PAID'")
    BigDecimal sumPaidRevenue();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.voucher.id = :voucherId AND b.status <> 'CANCELED'")
    long countActiveBookingsByVoucherId(@Param("voucherId") UUID voucherId);

    @Query("SELECT b.voucher.id, COUNT(b) FROM Booking b WHERE b.voucher.id IN :voucherIds AND b.status <> 'CANCELED' GROUP BY b.voucher.id")
    List<Object[]> countActiveBookingsByVoucherIds(@Param("voucherIds") List<UUID> voucherIds);
}
