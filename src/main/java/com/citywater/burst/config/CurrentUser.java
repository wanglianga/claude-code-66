package com.citywater.burst.config;

import com.citywater.burst.model.AppUser;
import com.citywater.burst.repo.AppUserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * 当前登录用户工具。
 */
@Component
public class CurrentUser {

    private final AppUserRepo userRepo;

    public CurrentUser(AppUserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public AppUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(UNAUTHORIZED, "未登录");
        }
        return userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "用户不存在"));
    }

    public String displayName() {
        return require().getDisplayName();
    }
}
