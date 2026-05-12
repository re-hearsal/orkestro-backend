package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventAttendanceStatus;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAttendanceRowDTO {

    private Long userId;

    private String name;

    private Long profileImageFileId;

    private EventRsvpStatus rsvpStatus;

    private EventAttendanceStatus attendanceStatus;
}
