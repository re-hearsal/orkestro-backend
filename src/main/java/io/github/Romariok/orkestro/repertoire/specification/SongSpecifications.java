package io.github.Romariok.orkestro.repertoire.specification;

import io.github.Romariok.orkestro.repertoire.models.Song;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class SongSpecifications {

   private SongSpecifications() {
   }

   public static Specification<Song> isInOrganization(Long organizationId) {
      return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
   }

   public static Specification<Song> titleOrComposerOrTagContainsIgnoreCase(String rawQuery) {
      return (root, query, cb) -> {
         if (rawQuery == null) {
            return cb.conjunction();
         }
         String normalized = rawQuery.trim();
         if (normalized.isBlank()) {
            return cb.conjunction();
         }

         String like = "%" + normalized.toLowerCase(Locale.ROOT) + "%";

         Predicate titleLike = cb.like(cb.lower(root.get("title")), like);
         Predicate composerLike = cb.like(
               cb.lower(cb.coalesce(root.get("composer"), "")),
               like);

         Subquery<Integer> sq = query.subquery(Integer.class);
         Root<Song> song = sq.from(Song.class);
         Join<Song, String> tags = song.join("tags", JoinType.INNER);
         sq.select(cb.literal(1));
         sq.where(cb.and(
               cb.equal(song.get("id"), root.get("id")),
               cb.like(cb.lower(tags), like)));
         Predicate tagLike = cb.exists(sq);

         return cb.or(titleLike, composerLike, tagLike);
      };
   }
}

