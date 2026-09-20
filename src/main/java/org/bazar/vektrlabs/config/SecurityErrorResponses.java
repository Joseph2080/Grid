package org.bazar.vektrlabs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jericho.common.util.RestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Map;

public final class SecurityErrorResponses {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SecurityErrorResponses() {
    }

    public static ResponseEntity<Map<String, Object>> response(HttpStatus status, String code, String message) {
        return RestUtil.handleException(message, status, Map.of("code", code));
    }

    public static void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        OBJECT_MAPPER.writeValue(response.getWriter(), response(status, code, message).getBody());
    }
}
