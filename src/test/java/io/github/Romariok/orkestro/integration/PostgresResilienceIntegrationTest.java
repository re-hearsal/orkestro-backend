package io.github.Romariok.orkestro.integration;

import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.liquibase.enabled=false",
                "spring.datasource.hikari.connection-timeout=3000",
                "spring.datasource.hikari.initialization-fail-timeout=1"
        }
)
class PostgresResilienceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("resilience_testdb")
            .withUsername("resilience_user")
            .withPassword("resilience_pass");

    @DynamicPropertySource
    static void configureDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void successfulQuery_beforeContainerStop_returnsResult() {
        Organization org = organizationRepository.save(
                Organization.builder()
                        .name("Resilience Test Org")
                        .location("Test City")
                        .build());

        assertThat(org.getId()).isNotNull();
        assertThat(organizationRepository.findById(org.getId())).isPresent();
    }

    @Test
    void query_afterContainerStop_throwsDataAccessException() {
        Organization org = Organization.builder()
                .name("Before Stop Org")
                .location("City")
                .build();
        organizationRepository.save(org);
        assertThat(organizationRepository.count()).isGreaterThan(0);

        postgres.stop();

        assertThatThrownBy(() -> organizationRepository.findAll())
                .isInstanceOf(DataAccessException.class);
    }
}
