package com.wd.custapi.service;

import com.wd.custapi.model.ChangeOrder;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.repository.ChangeOrderRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer-side Change Order service.
 * Customers can view COs in CUSTOMER_REVIEW status and approve or reject them.
 */
@Service
@Transactional
public class CustomerChangeOrderService {

    private static final String STATUS_CUSTOMER_REVIEW = "CUSTOMER_REVIEW";

    private final ChangeOrderRepository changeOrderRepository;
    private final CustomerUserRepository customerUserRepository;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    @SuppressWarnings("java:S6813")   // self-injection requires field injection (constructor would cycle)
    private CustomerChangeOrderService self;

    public CustomerChangeOrderService(ChangeOrderRepository changeOrderRepository,
                                       CustomerUserRepository customerUserRepository) {
        this.changeOrderRepository = changeOrderRepository;
        this.customerUserRepository = customerUserRepository;
    }

    @Transactional(readOnly = true)
    public List<ChangeOrder> getProjectChangeOrders(Long projectId) {
        return changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public ChangeOrder getChangeOrder(Long coId, Long projectId) {
        return changeOrderRepository.findByIdAndProjectId(coId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Change order not found: " + coId));
    }

    @Transactional(readOnly = true)
    public List<ChangeOrder> getPendingReview(Long projectId) {
        return changeOrderRepository.findByProjectIdAndStatus(projectId, STATUS_CUSTOMER_REVIEW);
    }

    /**
     * Customer approves a CO in CUSTOMER_REVIEW status.
     * The actual credit note / CO invoice generation happens in the Portal API's
     * ChangeOrderService — here we only update the status so the portal API can
     * pick it up via its own polling / webhook mechanism.
     *
     * For now both APIs share the same DB, so we update the row directly.
     * The portal API watches for APPROVED status and triggers downstream docs.
     */
    public ChangeOrder approve(Long coId, Long projectId, String customerEmail) {
        ChangeOrder co = (self != null ? self : this).getChangeOrder(coId, projectId);

        if (!STATUS_CUSTOMER_REVIEW.equals(co.getStatus())) {
            throw new IllegalStateException(
                "Change order must be in CUSTOMER_REVIEW to approve. Current: " + co.getStatus());
        }

        CustomerUser customer = customerUserRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerEmail));

        co.setStatus("APPROVED");
        co.setApprovedAt(LocalDateTime.now());
        co.setApprovedBy(customer.getId());
        co.setCustomerReviewedAt(LocalDateTime.now());

        return changeOrderRepository.save(co);
    }

    public ChangeOrder reject(Long coId, Long projectId, String customerEmail, String reason) {
        ChangeOrder co = (self != null ? self : this).getChangeOrder(coId, projectId);

        if (!STATUS_CUSTOMER_REVIEW.equals(co.getStatus())) {
            throw new IllegalStateException(
                "Change order must be in CUSTOMER_REVIEW to reject. Current: " + co.getStatus());
        }

        CustomerUser customer = customerUserRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerEmail));

        co.setStatus("REJECTED");
        co.setRejectedAt(LocalDateTime.now());
        co.setRejectedBy(customer.getId());
        co.setRejectionReason(reason);
        co.setCustomerReviewedAt(LocalDateTime.now());

        return changeOrderRepository.save(co);
    }
}
