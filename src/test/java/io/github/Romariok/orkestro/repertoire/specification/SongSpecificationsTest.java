package io.github.Romariok.orkestro.repertoire.specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.repertoire.models.Song;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
class SongSpecificationsTest {

    @Mock
    Root<Song> root;

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
    void isInOrganization_callsEqual() {
        when(cb.equal(path, 5L)).thenReturn(predicate);

        Predicate result = SongSpecifications.isInOrganization(5L).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(path, 5L);
    }

    @Test
    void titleOrComposerOrTagContainsIgnoreCase_null_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = SongSpecifications
                .titleOrComposerOrTagContainsIgnoreCase(null)
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void titleOrComposerOrTagContainsIgnoreCase_blank_returnsConjunction() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = SongSpecifications
                .titleOrComposerOrTagContainsIgnoreCase("   ")
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).conjunction();
    }

    @Test
    void titleOrComposerOrTagContainsIgnoreCase_value_callsOrWithAllThreePredicates() {
        Path titlePath = mock(Path.class);
        Path composerPath = mock(Path.class);
        Path rootIdPath = mock(Path.class);

        // title predicate chain
        Expression<String> lowerTitle = mock(Expression.class);
        Predicate titleLike = mock(Predicate.class);

        // composer predicate chain
        Expression<String> coalescedComposer = mock(Expression.class);
        Expression<String> lowerComposer = mock(Expression.class);
        Predicate composerLike = mock(Predicate.class);

        // subquery for tags
        Subquery<Integer> sq = mock(Subquery.class);
        Root<Song> songSubRoot = mock(Root.class);
        Join tagsJoin = mock(Join.class);
        Path songIdPath = mock(Path.class);
        Expression<String> lowerTags = mock(Expression.class);
        Predicate likeTagPred = mock(Predicate.class);
        Predicate equalIdPred = mock(Predicate.class);
        Predicate andTagPred = mock(Predicate.class);
        Expression<Integer> literalExpr = mock(Expression.class);
        Predicate existsPred = mock(Predicate.class);

        when(root.get("title")).thenReturn(titlePath);
        when(root.get("composer")).thenReturn(composerPath);
        when(root.get("id")).thenReturn(rootIdPath);

        // title
        when(cb.lower(titlePath)).thenReturn(lowerTitle);
        when(cb.like(eq(lowerTitle), anyString())).thenReturn(titleLike);

        // composer
        when(cb.coalesce(any(Expression.class), eq(""))).thenReturn(coalescedComposer);
        when(cb.lower(coalescedComposer)).thenReturn(lowerComposer);
        when(cb.like(eq(lowerComposer), anyString())).thenReturn(composerLike);

        // tag subquery
        when(query.subquery(Integer.class)).thenReturn(sq);
        when(sq.from(Song.class)).thenReturn(songSubRoot);
        when(songSubRoot.join(eq("tags"), any())).thenReturn(tagsJoin);
        when(cb.lower(tagsJoin)).thenReturn(lowerTags);
        when(cb.like(eq(lowerTags), anyString())).thenReturn(likeTagPred);
        when(songSubRoot.get("id")).thenReturn(songIdPath);
        when(cb.equal(songIdPath, rootIdPath)).thenReturn(equalIdPred);
        when(cb.and(equalIdPred, likeTagPred)).thenReturn(andTagPred);
        when(cb.literal(1)).thenReturn(literalExpr);
        when(sq.select(literalExpr)).thenReturn(sq);
        when(sq.where(andTagPred)).thenReturn(sq);
        when(cb.exists(sq)).thenReturn(existsPred);

        when(cb.or(titleLike, composerLike, existsPred)).thenReturn(predicate);

        Predicate result = SongSpecifications
                .titleOrComposerOrTagContainsIgnoreCase("Symphony")
                .toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).or(titleLike, composerLike, existsPred);
    }
}
