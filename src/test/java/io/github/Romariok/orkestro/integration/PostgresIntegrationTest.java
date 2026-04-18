package io.github.Romariok.orkestro.integration;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.repository.EventRepository;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class PostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationUserRepository organizationUserRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void cleanUp() {
        eventRepository.deleteAll();
        organizationUserRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }


    @Test
    void saveOrganization_persistsAndReturnsWithId() {
        Organization org = Organization.builder()
                .name("Symphony Orchestra")
                .location("Kyiv")
                .description("Main orchestra")
                .build();

        Organization saved = organizationRepository.save(org);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Symphony Orchestra");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByNameContainingIgnoreCase_returnsMatchingOrganizations() {
        organizationRepository.save(Organization.builder().name("Kyiv Philharmonic").location("Kyiv").build());
        organizationRepository.save(Organization.builder().name("Lviv Chamber Orchestra").location("Lviv").build());
        organizationRepository.save(Organization.builder().name("Odesa Jazz Band").location("Odesa").build());

        List<Organization> results = organizationRepository.findByNameContainingIgnoreCase("orchestra");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).contains("Orchestra");
    }

    @Test
    void findByNameContainingIgnoreCase_withPaging_returnsPage() {
        organizationRepository.save(Organization.builder().name("Orchestra A").location("City A").build());
        organizationRepository.save(Organization.builder().name("Orchestra B").location("City B").build());
        organizationRepository.save(Organization.builder().name("Band C").location("City C").build());

        Page<Organization> page = organizationRepository.findByNameContainingIgnoreCase(
                "orchestra", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void deleteOrganization_removesFromDatabase() {
        Organization org = organizationRepository.save(
                Organization.builder().name("Temp Orchestra").location("City").build());
        Long id = org.getId();

        organizationRepository.deleteById(id);

        assertThat(organizationRepository.findById(id)).isEmpty();
    }

    @Test
    void updateOrganization_changesName() {
        Organization org = organizationRepository.save(
                Organization.builder().name("Old Name").location("City").build());

        org.setName("New Name");
        org.setDescription("Updated description");
        Organization updated = organizationRepository.save(org);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
    }


    @Test
    void saveUser_persistsAndReturnsWithId() {
        User user = buildUser("john", "john@example.com");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("john");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByUsername_returnsUser() {
        userRepository.save(buildUser("alice", "alice@example.com"));

        Optional<User> found = userRepository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void existsByUsername_returnsTrueWhenExists() {
        userRepository.save(buildUser("bob", "bob@example.com"));

        assertThat(userRepository.existsByUsername("bob")).isTrue();
        assertThat(userRepository.existsByUsername("unknown")).isFalse();
    }

    @Test
    void findByUsername_returnsEmptyForNonExistent() {
        Optional<User> found = userRepository.findByUsername("nobody");

        assertThat(found).isEmpty();
    }

    @Test
    void findByNameContainingIgnoreCase_returnsMatchingUsers() {
        userRepository.save(buildUser("ivan_k", "ivan@example.com", "Ivan Kovalenko"));
        userRepository.save(buildUser("oksana_m", "oksana@example.com", "Oksana Melnyk"));

        List<User> results = userRepository.findByNameContainingIgnoreCase("ivan");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("ivan_k");
    }


    @Test
    void saveOrganizationUser_persistsMembership() {
        Organization org = organizationRepository.save(
                Organization.builder().name("Test Org").location("City").build());
        User user = userRepository.save(buildUser("member1", "m1@example.com"));

        OrganizationUser membership = OrganizationUser.builder()
                .organizationId(org.getId())
                .userId(user.getId())
                .status(OrganizationUserStatusType.ACCEPTED)
                .build();
        organizationUserRepository.save(membership);

        Optional<OrganizationUser> found = organizationUserRepository.findByOrganizationIdAndUserId(
                org.getId(), user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(OrganizationUserStatusType.ACCEPTED);
    }

    @Test
    void findByOrganizationIdAndStatus_returnsMatchingMembers() {
        Organization org = organizationRepository.save(
                Organization.builder().name("Org").location("City").build());
        User user1 = userRepository.save(buildUser("u1", "u1@example.com"));
        User user2 = userRepository.save(buildUser("u2", "u2@example.com"));

        organizationUserRepository.save(OrganizationUser.builder()
                .organizationId(org.getId()).userId(user1.getId())
                .status(OrganizationUserStatusType.ACCEPTED).build());
        organizationUserRepository.save(OrganizationUser.builder()
                .organizationId(org.getId()).userId(user2.getId())
                .status(OrganizationUserStatusType.PENDING).build());

        List<OrganizationUser> accepted = organizationUserRepository.findByOrganizationIdAndStatus(
                org.getId(), OrganizationUserStatusType.ACCEPTED);

        assertThat(accepted).hasSize(1);
        assertThat(accepted.get(0).getUserId()).isEqualTo(user1.getId());
    }

    @Test
    void countByOrganizationIdAndStatus_returnsCorrectCount() {
        Organization org = organizationRepository.save(
                Organization.builder().name("Count Org").location("City").build());
        User u1 = userRepository.save(buildUser("cu1", "cu1@example.com"));
        User u2 = userRepository.save(buildUser("cu2", "cu2@example.com"));

        organizationUserRepository.save(OrganizationUser.builder()
                .organizationId(org.getId()).userId(u1.getId())
                .status(OrganizationUserStatusType.ACCEPTED).build());
        organizationUserRepository.save(OrganizationUser.builder()
                .organizationId(org.getId()).userId(u2.getId())
                .status(OrganizationUserStatusType.ACCEPTED).build());

        long count = organizationUserRepository.countByOrganizationIdAndStatus(
                org.getId(), OrganizationUserStatusType.ACCEPTED);

        assertThat(count).isEqualTo(2);
    }


    @Test
    void saveEvent_persistsWithAllFields() {
        Organization org = organizationRepository.save(
                Organization.builder().name("Event Org").location("Kyiv").build());
        User creator = userRepository.save(buildUser("creator", "creator@example.com"));

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        Event event = Event.builder()
                .organizationId(org.getId())
                .creatorUserId(creator.getId())
                .title("Spring Concert")
                .description("Annual spring concert")
                .eventType(EventType.CONCERT)
                .location("Concert Hall")
                .startTime(start)
                .endTime(end)
                .sendRsvp(true)
                .includeAllOrganizationMembers(true)
                .build();

        Event saved = eventRepository.save(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Spring Concert");
        assertThat(saved.getEventType()).isEqualTo(EventType.CONCERT);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByOrganizationId_returnsOrgEvents() {
        Organization org1 = organizationRepository.save(
                Organization.builder().name("Org 1").location("City").build());
        Organization org2 = organizationRepository.save(
                Organization.builder().name("Org 2").location("City").build());
        User creator = userRepository.save(buildUser("ev_creator", "ev@example.com"));

        eventRepository.save(buildEvent(org1.getId(), creator.getId(), "Event A"));
        eventRepository.save(buildEvent(org1.getId(), creator.getId(), "Event B"));
        eventRepository.save(buildEvent(org2.getId(), creator.getId(), "Event C"));

        List<Event> org1Events = eventRepository.findByOrganizationId(org1.getId());

        assertThat(org1Events).hasSize(2);
        assertThat(org1Events).extracting(Event::getTitle)
                .containsExactlyInAnyOrder("Event A", "Event B");
    }

    @Test
    void findByOrganizationIdAndStartTimeBetween_returnsEventsInRange() {
        Organization org = organizationRepository.save(
                Organization.builder().name("Range Org").location("City").build());
        User creator = userRepository.save(buildUser("range_creator", "range@example.com"));

        Instant now = Instant.now();
        eventRepository.save(Event.builder()
                .organizationId(org.getId()).creatorUserId(creator.getId())
                .title("In Range").eventType(EventType.REHEARSAL)
                .startTime(now.plus(2, ChronoUnit.DAYS))
                .endTime(now.plus(2, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS))
                .sendRsvp(false).includeAllOrganizationMembers(false).build());

        eventRepository.save(Event.builder()
                .organizationId(org.getId()).creatorUserId(creator.getId())
                .title("Out of Range").eventType(EventType.REHEARSAL)
                .startTime(now.plus(30, ChronoUnit.DAYS))
                .endTime(now.plus(30, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS))
                .sendRsvp(false).includeAllOrganizationMembers(false).build());

        List<Event> results = eventRepository.findByOrganizationIdAndStartTimeBetween(
                org.getId(),
                now.plus(1, ChronoUnit.DAYS),
                now.plus(7, ChronoUnit.DAYS));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("In Range");
    }

    @Test
    void deleteEvent_removesFromDatabase() {
        Organization org = organizationRepository.save(
                Organization.builder().name("Del Org").location("City").build());
        User creator = userRepository.save(buildUser("del_creator", "del@example.com"));
        Event event = eventRepository.save(buildEvent(org.getId(), creator.getId(), "To Delete"));

        eventRepository.deleteById(event.getId());

        assertThat(eventRepository.findById(event.getId())).isEmpty();
    }


    private User buildUser(String username, String email) {
        return buildUser(username, email, username);
    }

    private User buildUser(String username, String email, String name) {
        return User.builder()
                .username(username)
                .name(name)
                .email(email)
                .password("hashed_password")
                .notificationChannel(NotificationChannelType.EMAIL)
                .preferredLanguage(UserLanguageType.EN)
                .build();
    }

    private Event buildEvent(Long orgId, Long creatorId, String title) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        return Event.builder()
                .organizationId(orgId)
                .creatorUserId(creatorId)
                .title(title)
                .eventType(EventType.REHEARSAL)
                .startTime(start)
                .endTime(start.plus(2, ChronoUnit.HOURS))
                .sendRsvp(false)
                .includeAllOrganizationMembers(false)
                .build();
    }
}
