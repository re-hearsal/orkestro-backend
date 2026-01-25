package io.github.Romariok.orkestro.event.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.Romariok.orkestro.event.models.enums.EventAttendanceStatus;
import io.github.Romariok.orkestro.event.models.enums.EventParticipantSourceType;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_participants")
@IdClass(EventParticipantId.class)
public class EventParticipant {

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "source", nullable = false)
    private EventParticipantSourceType source;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @ColumnDefault("'PENDING'")
    @Column(name = "rsvp_status", nullable = false)
    private EventRsvpStatus rsvpStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @ColumnDefault("'UNKNOWN'")
    @Column(name = "attendance_status", nullable = false)
    private EventAttendanceStatus attendanceStatus;

    @Column(name = "rsvp_at")
    private Instant rsvpAt;
}
