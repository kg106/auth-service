package com.example.auth_service.repository;

import com.example.auth_service.dto.UserFilterDTO;
import com.example.auth_service.entity.Tenant;
import com.example.auth_service.entity.User;
import com.example.auth_service.entity.UserRole;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> withFilter(UserFilterDTO filter) {
        return (root, query, cb) -> {
            Specification<User> spec = Specification.where(null);

            if (filter.getEmail() != null && !filter.getEmail().isEmpty()) {
                spec = spec.and((r, q, c) -> cb.like(cb.lower(r.get("email")), "%" + filter.getEmail().toLowerCase() + "%"));
            }

            if (filter.getName() != null && !filter.getName().isEmpty()) {
                spec = spec.and((r, q, c) -> cb.like(cb.lower(r.get("name")), "%" + filter.getName().toLowerCase() + "%"));
            }

            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                spec = spec.and((r, q, c) -> cb.equal(r.get("status"), filter.getStatus()));
            }

            if (filter.getRoleName() != null && !filter.getRoleName().isEmpty()) {
                spec = spec.and((r, q, c) -> {
                    Join<User, UserRole> roleJoin = r.join("role");
                    return cb.equal(roleJoin.get("name"), filter.getRoleName());
                });
            }

            if (filter.getTenantId() != null) {
                spec = spec.and((r, q, c) -> {
                    Join<User, Tenant> tenantJoin = r.join("tenant");
                    return cb.equal(tenantJoin.get("id"), filter.getTenantId());
                });
            }

            return spec.toPredicate(root, query, cb);
        };
    }
}
