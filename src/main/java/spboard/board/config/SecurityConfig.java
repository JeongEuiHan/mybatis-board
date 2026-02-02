package spboard.board.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import spboard.board.Repository.UserMapper;
import spboard.board.config.auth.MyAccessDeniedHandler;
import spboard.board.config.auth.MyAuthenticationEntryPoint;
import spboard.board.config.auth.MyLoginSuccessHandler;
import spboard.board.config.auth.MyLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserMapper userMapper;

    // 로그인하지 않은 유저들만 접근 가능한 URL
    private static final String[] anonymousUserUrl = {
            "/users/login",
            "/users/join"
    };

    // 로그인한 유저들만 접근 가능한 URL
    private static final String[] authenticatedUserUrl = {
            "/boards/*/edit",      // boards 바로 아래 edit
            "/boards/*/delete",    // boards 바로 아래 delete
            "/likes/**",
            "/users/myPage/**",
            "/users/edit",
            "/users/delete"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화
                .csrf(csrf -> csrf.disable())
                // CORS 기본 설정
                .cors(Customizer.withDefaults())
                // URL 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(anonymousUserUrl).anonymous() // 비로그인 사용자전용
                        .requestMatchers(authenticatedUserUrl).authenticated() // 로그인 사용자전용
                        .requestMatchers("/boards/greeting/write").hasAnyAuthority("BRONZE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/boards/greeting").hasAnyAuthority("BRONZE", "ADMIN")
                        .requestMatchers("/boards/free/write").hasAnyAuthority("SILVER", "GOLD", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/boards/free").hasAnyAuthority("SILVER", "GOLD", "ADMIN")
                        .requestMatchers("/boards/gold/**").hasAnyAuthority("GOLD", "ADMIN")
                        .requestMatchers("/users/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/comments/**").hasAnyAuthority("BRONZE", "SILVER", "GOLD", "ADMIN")
                        .anyRequest().permitAll()
                )
                // 폼 로그인
                .formLogin(form -> form
                        .loginPage("/users/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .failureUrl("/users/login?fail")
                        .successHandler(new MyLoginSuccessHandler(userMapper))
                )
                // 로그아웃
                .logout(logout -> logout
                        .logoutUrl("/users/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(new MyLogoutSuccessHandler())
                )
                // 인증/인가 예외 처리
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(new MyAccessDeniedHandler(userMapper))
                        .authenticationEntryPoint(new MyAuthenticationEntryPoint())
                );

        return http.build();
    }

}
