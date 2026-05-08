package com.wd.custapi.service;

import com.wd.custapi.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Ownership check for customer-side change-request actions.
 *
 * <p>Without this guard, any authenticated customer could request OTPs or attempt
 * to approve any {@code crId} simply by guessing IDs. That would expose two
 * weaknesses simultaneously:
 *
 * <ol>
 *   <li><b>DoS against rate limits.</b> A customer can burn another customer's
 *       5/24h OTP allowance by submitting OTP requests for that customer's CRs.</li>
 *   <li><b>Audit pollution.</b> Failed approve attempts (WRONG_CODE / EXPIRED)
 *       leave history rows on a CR the actor never had business touching, making
 *       forensic timelines noisier.</li>
 * </ol>
 *
 * <p>The check works in two hops:
 *
 * <ol>
 *   <li>Ask portal-API for the project that owns the CR (HMAC-signed,
 *       {@link PortalApiClient#fetchCrProjectId}).</li>
 *   <li>Use the existing {@link DashboardService#getProjectByIdAndEmail} pattern
 *       to assert that the authenticated customer owns that project. If
 *       DashboardService says no, it throws — we translate into
 *       {@link AccessDeniedException} so the controller layer maps to 403.</li>
 * </ol>
 *
 * <p>This mirrors the access pattern already used by every other customer-facing
 * project-scoped controller (CustomerInvoiceController, CustomerDelayLogController,
 * CustomerBoqController, etc.).
 */
@Service
public class CrAccessGuard {

    private static final Logger log = LoggerFactory.getLogger(CrAccessGuard.class);

    private final PortalApiClient portalApiClient;
    private final DashboardService dashboardService;

    public CrAccessGuard(PortalApiClient portalApiClient, DashboardService dashboardService) {
        this.portalApiClient = portalApiClient;
        this.dashboardService = dashboardService;
    }

    /**
     * @throws AccessDeniedException if the customer cannot access the CR's project,
     *                                or if the CR does not exist.
     */
    public void assertCustomerCanAccessCr(Long customerUserId, String customerEmail, Long crId) {
        Long projectId;
        try {
            projectId = portalApiClient.fetchCrProjectId(crId);
        } catch (RuntimeException e) {
            log.warn("CR {} customer {} ({}) — portal lookup failed: {}",
                crId, customerUserId, customerEmail, e.getMessage());
            throw new AccessDeniedException("CR not found or inaccessible");
        }

        try {
            Project p = dashboardService.getProjectByIdAndEmail(projectId, customerEmail);
            if (p == null) {
                throw new AccessDeniedException("CR not in customer's project");
            }
        } catch (RuntimeException e) {
            // DashboardService throws plain RuntimeException with "access denied" or
            // "not found" in the message when the customer doesn't own the project.
            log.warn("CR {} customer {} ({}) project {} — access check rejected: {}",
                crId, customerUserId, customerEmail, projectId, e.getMessage());
            throw new AccessDeniedException("CR not in customer's project");
        }
    }
}
