package com.cropdeal.userservice.service;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService {


    UserDetailsService userDetailService();
}

