package io.github.Romariok.orkestro.section.specification;

import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.UserInstrument;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Collection;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class SectionUserSpecifications {

    private SectionUserSpecifications() {
    }

    public static Specification<SectionUser> isSectionMember(Long sectionId) {
        return (root, query, cb) -> cb.equal(root.get("sectionId"), sectionId);
    }

    public static Specification<SectionUser> userNameOrUsernameContainsIgnoreCase(String rawQuery) {
        return (root, query, cb) -> {
            if (rawQuery == null) {
                return cb.conjunction();
            }
            String normalized = rawQuery.trim();
            if (normalized.isBlank()) {
                return cb.conjunction();
            }

            Join<SectionUser, User> userJoin = root.join("user", JoinType.INNER);
            String like = "%" + normalized.toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(userJoin.get("name")), like),
                    cb.like(cb.lower(userJoin.get("username")), like));
        };
    }

    public static Specification<SectionUser> userHasAnySectionRole(Long sectionId, Collection<Long> roleIds) {
        return (root, query, cb) -> {
            if (roleIds == null || roleIds.isEmpty()) {
                return cb.conjunction();
            }

            Subquery<Integer> sq = query.subquery(Integer.class);
            Root<UserRole> ur = sq.from(UserRole.class);
            Root<Role> r = sq.from(Role.class);

            Predicate userJoin = cb.equal(ur.get("userId"), root.get("userId"));
            Predicate roleJoin = cb.equal(r.get("id"), ur.get("roleId"));
            Predicate scope = cb.equal(r.get("scope"), RoleScopeType.SECTION);
            Predicate section = cb.equal(r.get("sectionId"), sectionId);
            Predicate roleIn = r.get("id").in(roleIds);

            sq.select(cb.literal(1));
            sq.where(cb.and(userJoin, roleJoin, scope, section, roleIn));
            return cb.exists(sq);
        };
    }

    public static Specification<SectionUser> userHasAnyInstrument(Collection<Long> instrumentIds) {
        return (root, query, cb) -> {
            if (instrumentIds == null || instrumentIds.isEmpty()) {
                return cb.conjunction();
            }

            Subquery<Integer> sq = query.subquery(Integer.class);
            Root<UserInstrument> ui = sq.from(UserInstrument.class);

            Predicate userJoin = cb.equal(ui.get("userId"), root.get("userId"));
            Predicate instrumentIn = ui.get("instrumentId").in(instrumentIds);

            sq.select(cb.literal(1));
            sq.where(cb.and(userJoin, instrumentIn));
            return cb.exists(sq);
        };
    }
}

