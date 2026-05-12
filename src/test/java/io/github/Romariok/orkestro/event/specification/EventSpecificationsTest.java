package io.github.Romariok.orkestro.event.specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.EventSection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
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
class EventSpecificationsTest {

    @Mock
    Root<Event> root;

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
    void organizationEquals_callsEqual() {
        when(cb.equal(path, 42L)).thenReturn(predicate);

        Predicate result = EventSpecifications.organizationEquals(42L).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(path, 42L);
    }

    @Test
    void intersectsDateRange_bothNull_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = EventSpecifications.intersectsDateRange(null, null).toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void intersectsDateRange_bothNonNull_callsAndWithBothConditions() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-12-31T00:00:00Z");
        Predicate ltePredicate = mock(Predicate.class);
        Predicate gtePredicate = mock(Predicate.class);
        when(cb.lessThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(ltePredicate);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(gtePredicate);
        when(cb.and(ltePredicate, gtePredicate)).thenReturn(predicate);

        Predicate result = EventSpecifications.intersectsDateRange(from, to).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).and(ltePredicate, gtePredicate);
    }

    @Test
    void intersectsDateRange_onlyFrom_callsGreaterThanOrEqualTo() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(predicate);

        Predicate result = EventSpecifications.intersectsDateRange(from, null).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).greaterThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }

    @Test
    void intersectsDateRange_onlyTo_callsLessThanOrEqualTo() {
        Instant to = Instant.parse("2024-12-31T00:00:00Z");
        when(cb.lessThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(predicate);

        Predicate result = EventSpecifications.intersectsDateRange(null, to).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }

    @Test
    void includeAllOrganizationMembers_callsIsTrue() {
        when(cb.isTrue(any())).thenReturn(predicate);

        Predicate result = EventSpecifications.includeAllOrganizationMembers().toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).isTrue(any());
    }

    @Test
    void hasAnySection_nullCollection_returnsDisjunction() {
        when(cb.disjunction()).thenReturn(predicate);

        Predicate result = EventSpecifications.hasAnySection(null).toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).disjunction();
    }

    @Test
    void hasAnySection_emptyCollection_returnsDisjunction() {
        when(cb.disjunction()).thenReturn(predicate);

        Predicate result = EventSpecifications.hasAnySection(List.of()).toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).disjunction();
    }

    @Test
    void hasAnySection_nonEmpty_createsSubqueryAndReturnsExists() {
        Subquery<Long> subquery = mock(Subquery.class);
        Root<EventSection> subRoot = mock(Root.class);
        Path eventIdPath = mock(Path.class);
        Path sectionIdPath = mock(Path.class);
        Path rootIdPath = mock(Path.class);
        Predicate equalPredicate = mock(Predicate.class);
        Predicate inPredicate = mock(Predicate.class);
        Predicate existsPredicate = mock(Predicate.class);

        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(EventSection.class)).thenReturn(subRoot);
        when(subRoot.get("eventId")).thenReturn(eventIdPath);
        when(subRoot.get("sectionId")).thenReturn(sectionIdPath);
        when(root.get("id")).thenReturn(rootIdPath);
        when(cb.equal(eventIdPath, rootIdPath)).thenReturn(equalPredicate);
        when(sectionIdPath.in(anyCollection())).thenReturn(inPredicate);
        when(subquery.select(any())).thenReturn(subquery);
        when(subquery.where(equalPredicate, inPredicate)).thenReturn(subquery);
        when(cb.exists(subquery)).thenReturn(existsPredicate);

        Predicate result = EventSpecifications.hasAnySection(List.of(1L, 2L)).toPredicate(root, query, cb);

        assertSame(existsPredicate, result);
        verify(cb).exists(subquery);
    }

    @Test
    void hasParticipantUser_null_returnsDisjunction() {
        when(cb.disjunction()).thenReturn(predicate);

        Predicate result = EventSpecifications.hasParticipantUser(null).toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).disjunction();
    }

    @Test
    void hasParticipantUser_nonNull_createsSubqueryAndReturnsExists() {
        Subquery<Long> subquery = mock(Subquery.class);
        Root<EventParticipant> subRoot = mock(Root.class);
        Path participantEventIdPath = mock(Path.class);
        Path participantUserIdPath = mock(Path.class);
        Path rootIdPath = mock(Path.class);
        Predicate equalEvent = mock(Predicate.class);
        Predicate equalUser = mock(Predicate.class);
        Predicate existsPredicate = mock(Predicate.class);

        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(EventParticipant.class)).thenReturn(subRoot);
        when(subRoot.get("eventId")).thenReturn(participantEventIdPath);
        when(subRoot.get("userId")).thenReturn(participantUserIdPath);
        when(root.get("id")).thenReturn(rootIdPath);
        when(cb.equal(participantEventIdPath, rootIdPath)).thenReturn(equalEvent);
        when(cb.equal(participantUserIdPath, 99L)).thenReturn(equalUser);
        when(subquery.select(any())).thenReturn(subquery);
        when(subquery.where(equalEvent, equalUser)).thenReturn(subquery);
        when(cb.exists(subquery)).thenReturn(existsPredicate);

        Predicate result = EventSpecifications.hasParticipantUser(99L).toPredicate(root, query, cb);

        assertSame(existsPredicate, result);
        verify(cb).exists(subquery);
    }

    @Test
    void titleContains_null_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = EventSpecifications.titleContains(null).toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void titleContains_blank_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = EventSpecifications.titleContains("   ").toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void titleContains_value_callsLike() {
        Expression<String> lowerExpr = mock(Expression.class);
        when(cb.lower(any(Expression.class))).thenReturn(lowerExpr);
        when(cb.like(eq(lowerExpr), anyString())).thenReturn(predicate);

        Predicate result = EventSpecifications.titleContains("Concert").toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).like(eq(lowerExpr), eq("%concert%"));
    }

    @Test
    void hasAnyTag_null_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = EventSpecifications.hasAnyTag(null).toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void hasAnyTag_empty_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = EventSpecifications.hasAnyTag(List.of()).toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void hasAnyTag_allBlank_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = EventSpecifications.hasAnyTag(List.of("  ", "")).toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void hasAnyTag_validTags_createsSubqueryAndReturnsExists() {
        Subquery<Long> subquery = mock(Subquery.class);
        Root<Event> subRoot = mock(Root.class);
        Join tagsJoin = mock(Join.class);
        Expression<String> tagsExpr = mock(Expression.class);
        Expression<String> lowerExpr = mock(Expression.class);
        Path subIdPath = mock(Path.class);
        Path rootIdPath = mock(Path.class);
        Predicate equalPredicate = mock(Predicate.class);
        Predicate inPredicate = mock(Predicate.class);
        Predicate existsPredicate = mock(Predicate.class);

        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(Event.class)).thenReturn(subRoot);
        when(subRoot.join("tags")).thenReturn(tagsJoin);
        when(tagsJoin.as(String.class)).thenReturn(tagsExpr);
        when(cb.lower(tagsExpr)).thenReturn(lowerExpr);
        when(lowerExpr.in(anyCollection())).thenReturn(inPredicate);
        when(subRoot.get("id")).thenReturn(subIdPath);
        when(root.get("id")).thenReturn(rootIdPath);
        when(cb.equal(subIdPath, rootIdPath)).thenReturn(equalPredicate);
        when(subquery.select(any())).thenReturn(subquery);
        when(subquery.where(equalPredicate, inPredicate)).thenReturn(subquery);
        when(cb.exists(subquery)).thenReturn(existsPredicate);

        Predicate result = EventSpecifications.hasAnyTag(List.of("classical", "concert")).toPredicate(root, query, cb);

        assertSame(existsPredicate, result);
        verify(cb).exists(subquery);
    }
}
