package com.firstaid.infrastructure.security;

import com.firstaid.infrastructure.database.entity.UserEntity;
import com.firstaid.infrastructure.database.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity(debug = true)
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true) // włącza możliwość zabezpieczenia metod na poziomie
// klas, tak aby te metody mogły być wykorzystywane tylko przez użytkowników z uprawnieniami
@SuppressWarnings("unused")
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    public AuthenticationManager authenticationManager(
//        HttpSecurity httpSecurity,
//        PasswordEncoder passwordEncoder,
//        UserDetailsService userDetailsService
//    ) {
//        return httpSecurity.getSharedObject(AuthenticationManagerBuilder.class)
//                .userDetailsService(userDetailsService)
//                .passwordEncoder(passwordEncoder)
//                .bu
//    }

//    Podobno tak się teraz robi:

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authProvider);
    }

//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
//        httpSecurity.authorizeHttpRequests()
//                .requestMatchers("/login", "/error", "/images/oh_no.png").permitAll()
//                .requestMatchers("/employees/**", "/images/**").hasAnyAuthority("USER", "ADMIN")
//                .requestMatchers(HttpMethod.DELETE).hasAnyAuthority("ADMIN")
//                .and()
//                .formLogin()
//                .permitAll()
//                .logout()
//                .lS
//    }


    @Bean
    @ConditionalOnProperty(value = "spring.security.enabled", havingValue = "true", matchIfMissing = true)
    // to ustawienie jest domyślne, chyba nie trzeba tego pisać
    public SecurityFilterChain securityEnabled(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "spring.security.enabled", havingValue = "false")
    // jeżeli w property ustawimy spring.security.enabled na false to będzie to oznaczało, że ten bean ma być
    // zarejestrowany
    public SecurityFilterChain securityDisabled(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return httpSecurity.build();
    }
    //    @Bean
//    public UserDetailsService userDetailsService() {
//        return new InMemoryUserDetailsManager(
//                User.withUsername("admin")
//                        .password(passwordEncoder().encode("admin123"))
//                        .roles("ADMIN")
//                        .build()
//        );
//    }
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            UserEntity userEntity = userRepository.findByUserName(username);
            if (userEntity == null) {
                throw new UsernameNotFoundException("User not found: " + username);
            }

            // Pobranie ról użytkownika i ich konwersja na grantedauthority
            List<GrantedAuthority> authorities = userEntity.getRole().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole())) // Prefiks ROLE_
                    .collect(Collectors.toList());

            return org.springframework.security.core.userdetails.User
                    .withUsername(userEntity.getUserName())
                    .password(userEntity.getPassword()) // Haszowane hasło z bazy
                    .authorities(authorities) // Nadanie rzeczywistych ról użytkownikowi
                    .build();
        };
    }
}
