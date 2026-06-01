package com.wd.custapi.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    // Inner class for user information
    @Getter
    @Setter
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

        @SuppressWarnings("java:S107") // data carrier — many fields by design
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
    }
}

