package com.wd.custapi.dto;

import java.util.List;

public class LoginResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserInfo user;
    private List<String> permissions;
    private Long projectCount;
    private String redirectUrl;
    
    // Constructors
    public LoginResponse() {}
    
    public LoginResponse(String accessToken, String refreshToken, Long expiresIn, UserInfo user, List<String> permissions) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.permissions = permissions;
        this.projectCount = 0L;
        this.redirectUrl = "/dashboard";
    }
    
    public LoginResponse(String accessToken, String refreshToken, Long expiresIn, UserInfo user, List<String> permissions, Long projectCount, String redirectUrl) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.permissions = permissions;
        this.projectCount = projectCount;
        this.redirectUrl = redirectUrl;
    }
    
    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public UserInfo getUser() {
        return user;
    }
    
    public void setUser(UserInfo user) {
        this.user = user;
    }
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    public Long getProjectCount() { return projectCount; }
    public void setProjectCount(Long projectCount) { this.projectCount = projectCount; }
    
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    
    // Inner class for user information
    public static class UserInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private String phone;
        private String whatsappNumber;
        private String address;
        private String companyName;
        private String gstNumber;
        private String customerType;

        // Constructors
        public UserInfo() {}

        public UserInfo(Long id, String email, String firstName, String lastName, String role) {
            this.id = id;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.role = role;
        }

        public UserInfo(Long id, String email, String firstName, String lastName, String role,
                        String phone, String whatsappNumber, String address, String companyName,
                        String gstNumber, String customerType) {
            this(id, email, firstName, lastName, role);
            this.phone = phone;
            this.whatsappNumber = whatsappNumber;
            this.address = address;
            this.companyName = companyName;
            this.gstNumber = gstNumber;
            this.customerType = customerType;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
        
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getWhatsappNumber() {
            return whatsappNumber;
        }

        public void setWhatsappNumber(String whatsappNumber) {
            this.whatsappNumber = whatsappNumber;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getGstNumber() {
            return gstNumber;
        }

        public void setGstNumber(String gstNumber) {
            this.gstNumber = gstNumber;
        }

        public String getCustomerType() {
            return customerType;
        }

        public void setCustomerType(String customerType) {
            this.customerType = customerType;
        }
    }
}

