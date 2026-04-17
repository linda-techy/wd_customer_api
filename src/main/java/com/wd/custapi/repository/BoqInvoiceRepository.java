package com.wd.custapi.repository;

import com.wd.custapi.model.BoqInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoqInvoiceRepository extends JpaRepository<BoqInvoice, Long> {

    List<BoqInvoice> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
