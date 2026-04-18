package io.github.Romariok.orkestro.organization.specification;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.UserInstrument;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
class OrganizationUserSpecificationsTest {

    @Mock
    Root<OrganizationUser> root;

    @Mock
    CriteriaQuery<Object> query;

    @Mock
    CriteriaBuilder cb;

    @Mock
    Predicate predicate;

    @Mock
    Path path;

    @BeforeEach
    void setUp() {
        when(root.get(anyString())).thenReturn(path);
    }

    @Test
    void isOrganizationMember_callsAndWithTwoEquals() {
        Predicate orgEquals = mock(Predicate.class);
        Predicate statusEquals = mock(Predicate.class);
        when(cb.equal(path, 1L)).thenReturn(orgEquals);
        when(cb.equal(path, OrganizationUserStatusType.ACCEPTED)).thenReturn(statusEquals);
        when(cb.and(orgEquals, statusEquals)).thenReturn(predicate);

        Predicate result = OrganizationUserSpecifications
                .isOrganizationMember(1L, OrganizationUserStatusType.ACCEPTED)
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).and(orgEquals, statusEquals);
    }

    @Test
    void userNameOrUsernameContainsIgnoreCase_null_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = OrganizationUserSpecifications
                .userNameOrUsernameContainsIgnoreCase(null)
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void userNameOrUsernameContainsIgnoreCase_blank_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = OrganizationUserSpecifications
                .userNameOrUsernameContainsIgnoreCase("   ")
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void userNameOrUsernameContainsIgnoreCase_value_callsOrWithLikes() {
        Join userJoin = mock(Join.class);
        Path namePath = mock(Path.class);
        Path usernamePath = mock(Path.class);
        Expression<String> lowerName = mock(Expression.class);
        Expression<String> lowerUsername = mock(Expression.class);
        Predicate nameLike = mock(Predicate.class);
        Predicate usernameLike = mock(Predicate.class);

        when(root.join(anyString(), any())).thenReturn(userJoin);
        when(userJoin.get("name")).thenReturn(namePath);
        when(userJoin.get("username")).thenReturn(usernamePath);
        when(cb.lower(any(Expression.class))).thenReturn(lowerName, lowerUsername);
        when(cb.like(eq(lowerName), anyString())).thenReturn(nameLike);
        when(cb.like(eq(lowerUsername), anyString())).thenReturn(usernameLike);
        when(cb.or(nameLike, usernameLike)).thenReturn(predicate);

        Predicate result = OrganizationUserSpecifications
                .userNameOrUsernameContainsIgnoreCase("John")
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).or(nameLike, usernameLike);
    }

    @Test
    void userHasAnyOrganizationRole_nullRoles_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = OrganizationUserSpecifications
                .userHasAnyOrganizationRole(1L, null)
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void userHasAnyOrganizationRole_emptyRoles_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = OrganizationUserSpecifications
                .userHasAnyOrganizationRole(1L, List.of())
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void userHasAnyOrganizationRole_nonEmpty_createsSubqueryAndReturnsExists() {
        Subquery<Integer> sq = mock(Subquery.class);
        Root urRoot = mock(Root.class);
        Root rRoot = mock(Root.class);
        Path urUserIdPath = mock(Path.class);
        Path urRoleIdPath = mock(Path.class);
        Path rIdPath = mock(Path.class);
        Path rScopePath = mock(Path.class);
        Path rOrgIdPath = mock(Path.class);
        Path rootUserIdPath = mock(Path.class);
        Predicate userJoinPred = mock(Predicate.class);
        Predicate roleJoinPred = mock(Predicate.class);
        Predicate scopePred = mock(Predicate.class);
        Predicate orgPred = mock(Predicate.class);
        Predicate roleInPred = mock(Predicate.class);
        Predicate andPred = mock(Predicate.class);
        Predicate existsPred = mock(Predicate.class);
        Expression<Integer> literalExpr = mock(Expression.class);

        when(query.subquery(Integer.class)).thenReturn(sq);
        when(sq.from(UserRole.class)).thenReturn(urRoot);
        when(sq.from(Role.class)).thenReturn(rRoot);
        when(urRoot.get("userId")).thenReturn(urUserIdPath);
        when(urRoot.get("roleId")).thenReturn(urRoleIdPath);
        when(rRoot.get("id")).thenReturn(rIdPath);
        when(rRoot.get("scope")).thenReturn(rScopePath);
        when(rRoot.get("organizationId")).thenReturn(rOrgIdPath);
        when(root.get("userId")).thenReturn(rootUserIdPath);
        when(cb.equal(urUserIdPath, rootUserIdPath)).thenReturn(userJoinPred);
        when(cb.equal(rIdPath, urRoleIdPath)).thenReturn(roleJoinPred);
        when(cb.equal(rScopePath, RoleScopeType.ORGANIZATION)).thenReturn(scopePred);
        when(cb.equal(rOrgIdPath, 1L)).thenReturn(orgPred);
        when(rIdPath.in(anyCollection())).thenReturn(roleInPred);
        when(cb.literal(1)).thenReturn(literalExpr);
        when(sq.select(literalExpr)).thenReturn(sq);
        when(cb.and(userJoinPred, roleJoinPred, scopePred, orgPred, roleInPred)).thenReturn(andPred);
        when(sq.where(andPred)).thenReturn(sq);
        when(cb.exists(sq)).thenReturn(existsPred);

        Predicate result = OrganizationUserSpecifications
                .userHasAnyOrganizationRole(1L, List.of(10L, 20L))
                .toPredicate(root, query, cb);

        assertSame(existsPred, result);
        verify(cb).exists(sq);
    }

    @Test
    void userHasAnyInstrument_nullIds_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = OrganizationUserSpecifications
                .userHasAnyInstrument(null)
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void userHasAnyInstrument_emptyIds_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = OrganizationUserSpecifications
                .userHasAnyInstrument(List.of())
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void userHasAnyInstrument_nonEmpty_createsSubqueryAndReturnsExists() {
        Subquery<Integer> sq = mock(Subquery.class);
        Root uiRoot = mock(Root.class);
        Path uiUserIdPath = mock(Path.class);
        Path uiInstrumentIdPath = mock(Path.class);
        Path rootUserIdPath = mock(Path.class);
        Predicate userJoinPred = mock(Predicate.class);
        Predicate instrumentInPred = mock(Predicate.class);
        Predicate andPred = mock(Predicate.class);
        Predicate existsPred = mock(Predicate.class);
        Expression<Integer> literalExpr = mock(Expression.class);

        when(query.subquery(Integer.class)).thenReturn(sq);
        when(sq.from(UserInstrument.class)).thenReturn(uiRoot);
        when(uiRoot.get("userId")).thenReturn(uiUserIdPath);
        when(uiRoot.get("instrumentId")).thenReturn(uiInstrumentIdPath);
        when(root.get("userId")).thenReturn(rootUserIdPath);
        when(cb.equal(uiUserIdPath, rootUserIdPath)).thenReturn(userJoinPred);
        when(uiInstrumentIdPath.in(anyCollection())).thenReturn(instrumentInPred);
        when(cb.literal(1)).thenReturn(literalExpr);
        when(sq.select(literalExpr)).thenReturn(sq);
        when(cb.and(userJoinPred, instrumentInPred)).thenReturn(andPred);
        when(sq.where(andPred)).thenReturn(sq);
        when(cb.exists(sq)).thenReturn(existsPred);

        Predicate result = OrganizationUserSpecifications
                .userHasAnyInstrument(List.of(5L, 6L))
                .toPredicate(root, query, cb);

        assertSame(existsPred, result);
        verify(cb).exists(sq);
    }
}
