package com.citywater.burst.config;

import com.citywater.burst.model.AppUser;
import com.citywater.burst.repo.AppUserRepo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 演示系统：前后端同源的表单会话认证，关闭 CSRF 便于接口联调
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/css/**", "/js/**", "/favicon.ico",
                        "/actuator/health", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/login").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler((req, res, auth) -> res.sendRedirect("/"))
                .failureHandler((req, res, ex) -> res.sendRedirect("/login.html?error"))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html"))
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                if (req.getRequestURI().startsWith("/api/")) {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\":\"未登录或会话已过期\"}");
                } else {
                    res.sendRedirect("/login.html");
                }
            }));
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(AppUserRepo userRepo) {
        return username -> {
            AppUser u = userRepo.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
            return User.withUsername(u.getUsername())
                    .password(u.getPassword())
                    .roles(u.getRole().name())
                    .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
