package com.homework.online.config;

import com.homework.online.common.AuthUser;
import com.homework.online.domain.User;
import com.homework.online.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 连接诊断接口：定位「登录后仍提示未登录」这类部署环境问题 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepository;

    public DebugController(JwtAuthFilter jwtAuthFilter, UserRepository userRepository) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userRepository = userRepository;
    }

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> auth(HttpServletRequest request) {
        JwtAuthFilter.TokenInfo tokenInfo = jwtAuthFilter.resolve(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("tokenSource", tokenInfo.source() == null ? "none" : tokenInfo.source());
        body.put("hasAuthorizationHeader", request.getHeader("Authorization") != null);
        body.put("hasXAuthTokenHeader", request.getHeader("X-Auth-Token") != null);
        body.put("hasCookie", hasCookie(request));
        body.put("tokenLength", tokenInfo.token() == null ? 0 : tokenInfo.token().length());
        body.put("user", null);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser authUser)) {
            body.put("reason", tokenInfo.token() == null
                    ? "服务端没有收到任何登录凭证（请求头与 Cookie 都没有令牌）"
                    : "令牌无效或已过期");
            return ResponseEntity.ok(body);
        }
        User user = userRepository.findById(authUser.id()).orElse(null);
        if (user == null) {
            body.put("reason", "令牌对应的用户不存在");
            return ResponseEntity.ok(body);
        }
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("name", user.getName());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole().name());
        body.put("ok", true);
        body.put("user", userInfo);
        return ResponseEntity.ok(body);
    }

    private boolean hasCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        for (var cookie : request.getCookies()) {
            if (JwtAuthFilter.COOKIE_NAME.equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }
}
