package com.epam.finaltask.util;

public final class PathConstants {

    // === Common paths ===
    public static final String PATH_LOGIN = "/login";
    public static final String PATH_REGISTER = "/register";
    public static final String PATH_LOGOUT = "/logout";
    public static final String PATH_CATALOGUE = "/catalogue";
    public static final String PATH_ERROR = "/error";

    // === Role-based paths ===
    public static final String PATH_MANAGER = "/manager";
    public static final String PATH_ADMIN = "/admin";

    // === Static resources ===
    public static final String PATH_CSS = "/css";
    public static final String PATH_JS = "/js";
    public static final String PATH_IMAGES = "/images";

    // === API paths ===
    public static final String PATH_API = "/api";
    public static final String PATH_API_V1_AUTH = "/api/v1/auth";
    public static final String PATH_API_V1_VOUCHERS = "/api/v1/vouchers";

    // === Swagger paths ===
    public static final String PATH_SWAGGER_UI = "/swagger-ui";
    public static final String PATH_API_DOCS = "/v3/api-docs";

    // === Dev paths ===
    public static final String PATH_H2_CONSOLE = "/h2-console";

    private PathConstants() {
    }
}
