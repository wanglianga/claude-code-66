package com.citywater.burst.web;

import com.citywater.burst.config.CurrentUser;
import com.citywater.burst.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUser currentUser;

    @GetMapping("/me")
    public Map<String, Object> me() {
        AppUser u = currentUser.require();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", u.getUsername());
        m.put("displayName", u.getDisplayName());
        m.put("role", u.getRole().name());
        m.put("roleLabel", u.getRole().getLabel());
        return m;
    }
}
