package com.wd.custapi.config;

import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Permission;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.Role;
import com.wd.custapi.model.enums.ProjectPhase;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.PermissionRepository;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.RoleRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds baseline test data for integration tests.
 * <p>
 * Call {@link #seed()} once (idempotent) to populate roles, permissions,
 * users, and projects. All entities are accessible via getters after seeding.
 */
@Component
public class TestDataSeeder {

    private static final String DEFAULT_PASSWORD = "password123";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final CustomerUserRepository customerUserRepository;
    private final ProjectRepository projectRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private boolean seeded = false;

    // Roles
    private Role customerRole;
    private Role adminRole;
    private Role architectRole;
    private Role viewerRole;

    // Users
    private CustomerUser customerA;
    private CustomerUser customerB;
    private CustomerUser customerC;

    // Permissions
    private Permission projectView;
    private Permission boqView;
    private Permission boqApprove;
    private Permission paymentView;
    private Permission changeOrderView;
    private Permission changeOrderApprove;
    private Permission siteReportView;
    private Permission supportCreate;

    // Projects
    private Project residentialVilla;
    private Project commercialOffice;
    private Project renovationHome;

    public TestDataSeeder(RoleRepository roleRepository,
                          PermissionRepository permissionRepository,
                          CustomerUserRepository customerUserRepository,
                          ProjectRepository projectRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.customerUserRepository = customerUserRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    /**
     * Seeds all baseline test data. Safe to call multiple times (idempotent).
     */
    public synchronized void seed() {
        // Always reseed users to ensure passwords are correct (tests may modify them)
        seedPermissions();
        seedRoles();
        seedUsers();
        if (!seeded) {
            seedProjects();
            seeded = true;
        }
    }

    private void seedPermissions() {
        projectView = findOrCreatePermission("PROJECT_VIEW", "View projects");
        boqView = findOrCreatePermission("BOQ_VIEW", "View BOQ documents");
        boqApprove = findOrCreatePermission("BOQ_APPROVE", "Approve BOQ documents");
        paymentView = findOrCreatePermission("PAYMENT_VIEW", "View payments");
        changeOrderView = findOrCreatePermission("CHANGE_ORDER_VIEW", "View change orders");
        changeOrderApprove = findOrCreatePermission("CHANGE_ORDER_APPROVE", "Approve change orders");
        siteReportView = findOrCreatePermission("SITE_REPORT_VIEW", "View site reports");
        supportCreate = findOrCreatePermission("SUPPORT_CREATE", "Create support tickets");
    }

    private void seedRoles() {
        Set<Permission> customerPermissions = new HashSet<>();
        customerPermissions.add(projectView);
        customerPermissions.add(boqView);
        customerPermissions.add(boqApprove);
        customerPermissions.add(paymentView);
        customerPermissions.add(changeOrderView);
        customerPermissions.add(changeOrderApprove);
        customerPermissions.add(siteReportView);
        customerPermissions.add(supportCreate);

        Set<Permission> adminPermissions = new HashSet<>(customerPermissions);

        Set<Permission> architectPermissions = new HashSet<>();
        architectPermissions.add(projectView);
        architectPermissions.add(boqView);
        architectPermissions.add(siteReportView);

        Set<Permission> viewerPermissions = new HashSet<>();
        viewerPermissions.add(projectView);
        viewerPermissions.add(boqView);

        customerRole = findOrCreateRole("CUSTOMER", "Customer role", customerPermissions);
        adminRole = findOrCreateRole("ADMIN", "Administrator role", adminPermissions);
        architectRole = findOrCreateRole("ARCHITECT", "Architect role", architectPermissions);
        viewerRole = findOrCreateRole("VIEWER", "Viewer role", viewerPermissions);
    }

    private void seedUsers() {
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        customerA = findOrCreateUser("customerA@test.com", "Alice", "Anderson",
                "9876543210", encodedPassword, customerRole);
        customerB = findOrCreateUser("customerB@test.com", "Bob", "Baker",
                "9876543211", encodedPassword, customerRole);
        customerC = findOrCreateUser("customerC@test.com", "Charlie", "Clark",
                "9876543212", encodedPassword, customerRole);
    }

    private void seedProjects() {
        residentialVilla = findOrCreateProject("Residential Villa", "PRJ-001",
                "RESIDENTIAL", "Bangalore", ProjectPhase.PLANNING,
                new BigDecimal("5000000.00"), 2500.0);
        commercialOffice = findOrCreateProject("Commercial Office", "PRJ-002",
                "COMMERCIAL", "Mumbai", ProjectPhase.DESIGN,
                new BigDecimal("15000000.00"), 8000.0);
        renovationHome = findOrCreateProject("Renovation Home", "PRJ-003",
                "RENOVATION", "Chennai", ProjectPhase.CONSTRUCTION,
                new BigDecimal("2000000.00"), 1800.0);

        // Associate each project with its customer via the ManyToMany relationship
        associateProjectWithCustomer(customerA, residentialVilla);
        associateProjectWithCustomer(customerB, commercialOffice);
        associateProjectWithCustomer(customerC, renovationHome);
    }

    // ---- Helper methods ----

    private Permission findOrCreatePermission(String name, String description) {
        return permissionRepository.findByName(name).orElseGet(() -> {
            Permission p = new Permission();
            p.setName(name);
            p.setDescription(description);
            return permissionRepository.save(p);
        });
    }

    private Role findOrCreateRole(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            r.setDescription(description);
            r.setPermissions(permissions);
            return roleRepository.save(r);
        });
    }

    private CustomerUser findOrCreateUser(String email, String firstName, String lastName,
                                           String phone, String encodedPassword, Role role) {
        CustomerUser u = customerUserRepository.findByEmail(email).orElseGet(CustomerUser::new);
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setPhone(phone);
        u.setPassword(encodedPassword);
        u.setRole(role);
        u.setEnabled(true);
        u.setEmailVerified(true);
        return customerUserRepository.save(u);
    }

    private Project findOrCreateProject(String name, String code, String projectType,
                                         String location, ProjectPhase phase,
                                         BigDecimal budget, Double sqFeet) {
        List<Project> existing = projectRepository.findAll();
        for (Project p : existing) {
            if (name.equals(p.getName())) {
                return p;
            }
        }
        Project p = new Project();
        p.setName(name);
        p.setCode(code);
        p.setProjectType(projectType);
        p.setLocation(location);
        p.setProjectPhase(phase);
        p.setSqFeet(sqFeet);
        p.setStartDate(LocalDate.now());
        // Project entity lacks a setBudget setter; set via reflection
        setField(p, "budget", budget);
        return projectRepository.save(p);
    }

    /**
     * Reflectively sets a field value on an entity when no public setter exists.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "' on "
                    + target.getClass().getSimpleName(), e);
        }
    }

    private void associateProjectWithCustomer(CustomerUser user, Project project) {
        Set<Project> projects = user.getProjects();
        if (projects == null) {
            projects = new HashSet<>();
        }
        if (!projects.contains(project)) {
            projects.add(project);
            user.setProjects(projects);
            customerUserRepository.save(user);
        }
    }

    // ---- Getters ----

    public Role getCustomerRole() { return customerRole; }
    public Role getAdminRole() { return adminRole; }
    public Role getArchitectRole() { return architectRole; }
    public Role getViewerRole() { return viewerRole; }

    public CustomerUser getCustomerA() { return customerA; }
    public CustomerUser getCustomerB() { return customerB; }
    public CustomerUser getCustomerC() { return customerC; }

    public Permission getProjectView() { return projectView; }
    public Permission getBoqView() { return boqView; }
    public Permission getBoqApprove() { return boqApprove; }
    public Permission getPaymentView() { return paymentView; }
    public Permission getChangeOrderView() { return changeOrderView; }
    public Permission getChangeOrderApprove() { return changeOrderApprove; }
    public Permission getSiteReportView() { return siteReportView; }
    public Permission getSupportCreate() { return supportCreate; }

    public Project getResidentialVilla() { return residentialVilla; }
    public Project getCommercialOffice() { return commercialOffice; }
    public Project getRenovationHome() { return renovationHome; }

    public String getDefaultPassword() { return DEFAULT_PASSWORD; }
}
