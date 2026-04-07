package com.example.auth_service.repository;

import com.example.auth_service.dto.TenantFilterDTO;
import com.example.auth_service.entity.Tenant;
import org.springframework.data.jpa.domain.Specification;

public class TenantSpecification {

    public static Specification<Tenant> withFilter(TenantFilterDTO filter) {
        return (root, query, cb) -> {
            Specification<Tenant> spec = Specification.where(null);

            if (filter.getName() != null && !filter.getName().isEmpty()) {
                spec = spec.and((r, q, c) -> cb.like(cb.lower(r.get("name")), "%" + filter.getName().toLowerCase() + "%"));
            }

            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                spec = spec.and((r, q, c) -> cb.equal(r.get("status"), filter.getStatus()));
            }

            return spec.toPredicate(root, query, cb);
        };
    }
}
