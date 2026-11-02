package com.homework.online.config;

import com.homework.online.common.AuthUser;
import com.homework.online.domain.User;
import com.homework.online.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * 解析登录令牌，把当前用户放入 SecurityContext。
 * 令牌来源按优先级：Authorization: Bearer → X-Auth-Token 头 → ?token= 参数 → hw_token Cookie。
 * 多通道是为了兼容反向代理 / 预览网关可能丢弃某个请求头的部署环境。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "hw_token";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /** 供控制器 / 诊断接口复用：返回令牌与来源 */
    public TokenInfo resolve(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return new TokenInfo(header.substring(7).trim(), "authorization-header");
        }
        String alt = request.getHeader("X-Auth-Token");
        if (alt != null && !alt.isBlank()) {
            return new TokenInfo(alt.trim(), "x-auth-token-header");
        }
        String param = request.getParameter("token");
        if (param != null && !param.isBlank()) {
            return new TokenInfo(param.trim(), "query");
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    String value = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
                    return new TokenInfo(value, "cookie");
                }
            }
        }
        return new TokenInfo(null, null);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        TokenInfo tokenInfo = resolve(request);
        if (tokenInfo.token() != null) {
            try {
                Long userId = jwtService.parseUserId(tokenInfo.token());
                Optional<User> user = userRepository.findById(userId);
                user.ifPresent(u -> {
                    AuthUser principal = new AuthUser(u.getId(), u.getUsername(), u.getRole().name());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    public record TokenInfo(String token, String source) {
    }
}
