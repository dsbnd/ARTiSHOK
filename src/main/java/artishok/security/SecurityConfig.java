package artishok.security;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5500}")
    private String[] allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource())).csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz

                        .requestMatchers("/api/auth/**", // Регистрация и вход
                                "/api/public/**", // Публичные данные
                                "/api/images/test",  // Разрешаем тестовый endpoint
                                "/api/files/**",     // Разрешаем доступ к файлам
                                "/api/images/upload",
                                "/swagger-ui/**", // Swagger UI
                                "/v3/api-docs/**", // Swagger документация
                                "/swagger-resources/**", "/webjars/**", "/error")
                        .permitAll()
                        .requestMatchers("/exhibition-events/**", "/galleries/**", "/api/health", "/api/test",
                                "/api/debug/**")
                        .permitAll()
                        // Администратор (только ADMIN)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Владельцы галерей (GALLERY_OWNER и ADMIN)
                        .requestMatchers("/gallery-owner/**").hasAnyRole("GALLERY_OWNER", "ADMIN")

                        // Художники (ARTIST и ADMIN)
                        .requestMatchers("/artist/**").hasAnyRole("ARTIST", "ADMIN")
                        // Администратор (только ADMIN)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Владельцы галерей (GALLERY_OWNER и ADMIN)
                        .requestMatchers("/api/galleries/manage/**", "/api/events/manage/**",
                                "/api/bookings/confirm/**")
                        .hasAnyRole("GALLERY_OWNER", "ADMIN")

                        // Художники (ARTIST, GALLERY_OWNER и ADMIN)
                        .requestMatchers("/api/artworks/**", "/api/bookings/**")
                        .hasAnyRole("ARTIST", "GALLERY_OWNER", "ADMIN")

                        // Профиль (все аутентифицированные пользователи)
                        .requestMatchers("/api/profile/**").authenticated()

                        // Защищенные API (все аутентифицированные)
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();


        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));


        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));


        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Cache-Control", "Origin",
                        "Access-Control-Request-Method", "Access-Control-Request-Headers", "Accept-Language"));


        configuration.setAllowCredentials(true);


        configuration.setMaxAge(3600L);


        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}