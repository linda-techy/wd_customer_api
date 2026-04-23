package com.wd.custapi.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class PortalUserLookupJpaImpl implements PortalUserLookup {

    @PersistenceContext
    private EntityManager em;

    @Override
    public View lookup(Long portalUserId) {
        Object[] row = (Object[]) em.createNativeQuery(
                """
                SELECT id,
                       TRIM(COALESCE(first_name, '') || ' ' || COALESCE(last_name, '')) AS name,
                       phone,
                       email,
                       NULL AS photo_url
                  FROM portal_users
                 WHERE id = :id
                """)
                .setParameter("id", portalUserId)
                .getResultStream()
                .findFirst()
                .orElse(null);
        if (row == null) return null;
        return new View(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4]);
    }
}
