package org.bazar.vektrlabs.controller.rest;

import org.jericho.common.util.RestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class SessionController {

    @GetMapping("/api/csrf")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> csrf(CsrfToken token) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(RestUtil.buildResponse(
                        Map.of("token", token.getToken(), "headerName", token.getHeaderName(),
                                "parameterName", token.getParameterName()),
                        HttpStatus.OK, "CSRF token ready.").getBody());
    }

    @GetMapping("/complete-profile")
    public String completeProfile() {
        return "complete-profile";
    }

    @GetMapping("/auth/continue")
    public String continueAfterLogin() {
        return "auth-continue";
    }

    @GetMapping("/")
    public String home() {
        return "landing";
    }
}
