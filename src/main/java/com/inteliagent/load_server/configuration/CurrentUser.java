package com.inteliagent.load_server.configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
public final class CurrentUser {
    private CurrentUser() {}
    public static Jwt token() { return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
    public static int id() { return ((Number) token().getClaim("uid")).intValue(); }
    public static int cede() { return ((Number) token().getClaim("cede")).intValue(); }
    public static boolean admin() { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); }
}
