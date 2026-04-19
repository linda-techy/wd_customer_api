package com.wd.custapi.factory;

import com.wd.custapi.model.BoqDocument;
import com.wd.custapi.model.ChangeOrder;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.enums.BoqDocumentStatus;
import com.wd.custapi.model.enums.ProjectPhase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Static factory methods for creating test entities with sensible defaults.
 * <p>
 * Entities returned are <b>detached</b> (not persisted). Call the appropriate
 * repository's {@code save()} method if you need a managed entity.
 */
public final class CustomerTestFactory {

    private CustomerTestFactory() {
        // utility class
    }

    /**
     * Creates a detached {@link CustomerUser} with the given email and name.
     * Password is set to a BCrypt-encoded "password123", role is null (set separately),
     * enabled and emailVerified default to true.
     */
    public static CustomerUser createUser(String email, String firstName, String lastName) {
        CustomerUser user = new CustomerUser();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone("9000000000");
        // Raw BCrypt hash of "password123" with strength 12 — suitable for test assertions
        user.setPassword("$2a$12$LJ3m4ys3Gzl0TdmxFYNOre5BKOFa3.GjPe8RjB1GLfWwuDAqRSvyq");
        user.setEnabled(true);
        user.setEmailVerified(true);
        return user;
    }

    /**
     * Creates a detached {@link Project} with a random UUID, PLANNING phase, and
     * the given name and type.
     */
    public static Project createProject(String name, String type) {
        Project project = new Project();
        project.setProjectUuid(UUID.randomUUID());
        project.setName(name);
        project.setCode("PRJ-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        project.setProjectType(type);
        project.setProjectPhase(ProjectPhase.PLANNING);
        project.setLocation("Test Location");
        project.setStartDate(LocalDate.now());
        project.setProgress(BigDecimal.ZERO);
        project.setSqFeet(1500.0);
        return project;
    }

    /**
     * Creates a detached {@link BoqDocument} in DRAFT status linked to the given project.
     * Amounts are set to representative test values.
     */
    public static BoqDocument createBoqDocument(Project project) {
        BoqDocument doc = new BoqDocument();
        // BoqDocument setters are package-private or via reflection in tests;
        // use the accessible fields via the builder-style approach.
        // Since BoqDocument is a read-only entity with only getters, we use
        // reflection to set fields for test purposes.
        setField(doc, "project", project);
        setField(doc, "totalValueExGst", new BigDecimal("1000000.000000"));
        setField(doc, "gstRate", new BigDecimal("0.1800"));
        setField(doc, "totalGstAmount", new BigDecimal("180000.000000"));
        setField(doc, "totalValueInclGst", new BigDecimal("1180000.000000"));
        setField(doc, "status", BoqDocumentStatus.DRAFT);
        setField(doc, "revisionNumber", 1);
        return doc;
    }

    /**
     * Creates a detached {@link ChangeOrder} linked to the given project with
     * PENDING_CUSTOMER_REVIEW status.
     */
    public static ChangeOrder createChangeOrder(Project project, String title) {
        ChangeOrder co = new ChangeOrder();
        co.setStatus("PENDING_CUSTOMER_REVIEW");
        // ChangeOrder has setters for status and review fields, but other fields
        // need reflection since they are read-only in the customer API.
        setField(co, "project", project);
        setField(co, "referenceNumber", "CO-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        setField(co, "coType", "ADDITION");
        setField(co, "title", title);
        setField(co, "description", "Test change order: " + title);
        setField(co, "justification", "Required for project scope adjustment");
        setField(co, "netAmountExGst", new BigDecimal("50000.000000"));
        setField(co, "gstRate", new BigDecimal("0.1800"));
        setField(co, "gstAmount", new BigDecimal("9000.000000"));
        setField(co, "netAmountInclGst", new BigDecimal("59000.000000"));
        return co;
    }

    /**
     * Reflectively sets a field value on an entity. Used for read-only entities
     * (e.g., BoqDocument) that lack public setters.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "' on "
                    + target.getClass().getSimpleName(), e);
        }
    }
}
