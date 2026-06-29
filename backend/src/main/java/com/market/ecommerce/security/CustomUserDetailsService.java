package com.market.ecommerce.security;

import com.market.ecommerce.entity.User;
import com.market.ecommerce.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // جلب المستخدم من قاعدة البيانات بواسطة البريد الإلكتروني
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("لم يتم العثور على مستخدم بالبريد: " + email));

        // تحويل كيان المستخدم الخاص بك إلى كيان يفهمه Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}