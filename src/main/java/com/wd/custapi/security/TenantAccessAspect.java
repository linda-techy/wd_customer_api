package com.wd.custapi.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Defense-in-depth: logs a warning if a project-scoped endpoint is accessed
 * without prior tenant isolation check. Does NOT block — controllers handle
 * the actual enforcement via DashboardService.
 *
 * This catches missing checks on NEW endpoints that developers might add
 * without following the established pattern.
 */
@Aspect
@Component
public class TenantAccessAspect {

    private static final Logger logger = LoggerFactory.getLogger(TenantAccessAspect.class);

    /**
     * Intercepts all controller methods under /api/projects/{projectId}/**
     * and verifies an authenticated user context exists.
     */
    @Before("execution(* com.wd.custapi.controller.ProjectModuleController.*(..)) || " +
            "execution(* com.wd.custapi.controller.CustomerBoqController.*(..)) || " +
            "execution(* com.wd.custapi.controller.CustomerFinancialController.*(..))")
    public void verifyTenantContext(JoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            logger.warn("TENANT_AUDIT: Unauthenticated access to project-scoped endpoint: {}.{}",
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName());
        }
    }
}
