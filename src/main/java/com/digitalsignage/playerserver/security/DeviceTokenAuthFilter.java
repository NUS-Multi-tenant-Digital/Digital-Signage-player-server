package com.digitalsignage.playerserver.security;

import com.digitalsignage.playerserver.entity.Screen;
import com.digitalsignage.playerserver.repository.ScreenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class DeviceTokenAuthFilter extends OncePerRequestFilter {

    private final ScreenRepository screenRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/health",
            "/api/v1/player/register"
    );

    public DeviceTokenAuthFilter(ScreenRepository screenRepository, ObjectMapper objectMapper) {
        this.screenRepository = screenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // 放行 CORS 预检请求（OPTIONS 不带 Authorization）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (PUBLIC_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, 401, "UNAUTHORIZED", "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        Optional<Screen> screenOpt = screenRepository.findByDeviceToken(token);

        if (screenOpt.isEmpty()) {
            sendError(response, 401, "TOKEN_INVALID", "Token is invalid or expired");
            return;
        }

        Screen screen = screenOpt.get();
        if (!"activated".equalsIgnoreCase(screen.getActivationStatus())) {
            sendError(response, 401, "TOKEN_INVALID", "Device is not activated");
            return;
        }

        request.setAttribute("deviceId", screen.getDeviceCode());
        request.setAttribute("tenantId", String.valueOf(screen.getOrganizationId()));
        request.setAttribute("devicePrincipal", new DevicePrincipal(screen.getId(), screen.getDeviceCode()));

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "success", false,
                "error", Map.of("code", code, "message", message)
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
