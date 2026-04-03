package com.epam.finaltask.config;

import com.epam.finaltask.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class BlockedUserFilter extends OncePerRequestFilter {

    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String REDIRECT_BLOCKED = "/login?blocked=true";
    private static final String PATH_LOGIN = "/login";
    private static final String PATH_REGISTER = "/register";
    private static final String PATH_CSS = "/css";
    private static final String PATH_JS = "/js";
    private static final String PATH_IMAGES = "/images";
    private static final String PATH_ERROR = "/error";
    private static final long REFRESH_INTERVAL_MS = 30000; // 30 seconds delay

    private final UserRepository userRepository;
    private final Set<String> blockedUsernames = ConcurrentHashMap.newKeySet();

    public BlockedUserFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
        refreshBlockedUsers();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !ANONYMOUS_USER.equals(authentication.getPrincipal())) {

            String username = authentication.getName();

            if (blockedUsernames.contains(username)) {
                log.warn("Blocked user attempted access: username={}", username);
                SecurityContextHolder.clearContext();
                request.getSession().invalidate();
                response.sendRedirect(REDIRECT_BLOCKED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith(PATH_LOGIN) || path.startsWith(PATH_REGISTER)
                || path.startsWith(PATH_CSS) || path.startsWith(PATH_JS)
                || path.startsWith(PATH_IMAGES) || path.startsWith(PATH_ERROR);
    }

    @Scheduled(fixedRate = REFRESH_INTERVAL_MS)
    public void refreshBlockedUsers() {
        Set<String> freshBlocked = ConcurrentHashMap.newKeySet();
        userRepository.findAllByActive(false).forEach(user -> freshBlocked.add(user.getUsername()));
        blockedUsernames.clear();
        blockedUsernames.addAll(freshBlocked);
        log.debug("Refreshed blocked users cache: {} users", blockedUsernames.size());
    }

    public void evictUser(String username) {
        blockedUsernames.add(username);
    }
}
