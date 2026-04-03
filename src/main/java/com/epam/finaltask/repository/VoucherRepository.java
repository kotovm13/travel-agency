package com.epam.finaltask.repository;

import com.epam.finaltask.model.Voucher;
import com.epam.finaltask.model.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface VoucherRepository extends JpaRepository<Voucher, UUID>, JpaSpecificationExecutor<Voucher> {

    long countByStatus(VoucherStatus status);
}
