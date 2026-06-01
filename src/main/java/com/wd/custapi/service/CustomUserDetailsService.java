package com.wd.custapi.service;

import com.wd.custapi.repository.CustomerUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final CustomerUserRepository customerUserRepository;

    public CustomUserDetailsService(CustomerUserRepository customerUserRepository) {
        this.customerUserRepository = customerUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Customer user not found with email: " + email));
    }
}

