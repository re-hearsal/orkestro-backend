package io.github.Romariok.orkestro.section.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.github.Romariok.orkestro.user.models.User;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "section_users")
@IdClass(SectionUserId.class)
public class SectionUser {

    @Id
    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
}


