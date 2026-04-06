package com.epam.finaltask.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Security security = new Security();
    private Pagination pagination = new Pagination();

    @Data
    public static class Security {
        private String defaultPassword = "12345678";
        private long jwtExpirationMs = 86400000;
        private long blockedCacheRefreshMs = 30000;
        private int passwordMinLength = 8;
    }

    @Data
    public static class Pagination {
        private int cataloguePageSize = 9;
        private int managerPageSize = 10;
        private int userPageSize = 10;
        private int adminPageSize = 15;
        private int apiPageSize = 20;
    }
}
