package com.epam.finaltask.repository;

import com.epam.finaltask.model.User;
import com.epam.finaltask.model.enums.Role;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {

    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_ACTIVE = "active";

    private UserSpecification() {
    }

    public static Specification<User> usernameContains(String search) {
        String escaped = escapeLikeWildcards(search.toLowerCase());
        return (root, query, cb) -> cb.like(cb.lower(root.get(FIELD_USERNAME)), "%" + escaped + "%");
    }

    public static Specification<User> emailContains(String email) {
        String escaped = escapeLikeWildcards(email.toLowerCase());
        return (root, query, cb) -> cb.like(cb.lower(root.get(FIELD_EMAIL)), "%" + escaped + "%");
    }

    public static Specification<User> hasRole(String role) {
        return (root, query, cb) -> cb.equal(root.get(FIELD_ROLE), Role.valueOf(role));
    }

    public static Specification<User> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get(FIELD_ACTIVE), active);
    }

    private static String escapeLikeWildcards(String value) {
        return value.replace("%", "\\%").replace("_", "\\_");
    }
}
