package com.market.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpMethod;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final JwtAuthEntryPoint authEntryPoint;

    // تحسين: حقن الـ UserDetailsService لربط الفلتر بقاعدة البيانات وتوحيد الـ Principal
    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, JwtAuthEntryPoint authEntryPoint) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.authEntryPoint = authEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = header.substring(7);

        if (!jwtUtil.validateToken(token)) {
            // Invalid/expired token should not block public GET resources.
            if (isPublicRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            authEntryPoint.commence(request, response, new BadCredentialsException("Invalid or expired JWT token"));
            return;
        }

        String email = jwtUtil.extractEmail(token);

        // التأكد من أن الإيميل موجود وأن المستخدم غير مصادق عليه مسبقاً في هذا الطلب
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // load user details; if user no longer exists, treat as unauthorized
            UserDetails userDetails;
            try {
                userDetails = this.userDetailsService.loadUserByUsername(email);
            } catch (UsernameNotFoundException ex) {
                authEntryPoint.commence(request, response, new BadCredentialsException("User not found for JWT subject"));
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // إضافة تفاصيل الطلب (مثل رقم الـ IP)
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return HttpMethod.GET.matches(method)
                && (uri.startsWith("/api/products") || uri.startsWith("/api/categories") || uri.equals("/api/health") || uri.startsWith("/swagger-ui") || uri.startsWith("/api-docs"));
    }
}