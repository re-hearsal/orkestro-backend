package io.github.Romariok.orkestro.organization.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.user.service.MusicalRoleService;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationMusicalRoleServiceTest {

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private MusicalRoleService musicalRoleService;

    @InjectMocks
    private OrganizationMusicalRoleService organizationMusicalRoleService;

    @Test
    void setMemberInstruments_userNotAcceptedMember_throwsBusinessException() {
        Long organizationId = 1L;
        Long userId = 10L;
        List<Long> instrumentIds = List.of(1L, 2L);

        when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> organizationMusicalRoleService.setMemberInstruments(organizationId, userId, instrumentIds));

        verify(musicalRoleService, never()).setUserInstruments(userId, instrumentIds);
    }

    @Test
    void setMemberInstruments_userPending_throwsBusinessException() {
        Long organizationId = 1L;
        Long userId = 10L;
        List<Long> instrumentIds = List.of(1L);

        OrganizationUser pending = OrganizationUser.builder()
                .organizationId(organizationId)
                .userId(userId)
                .status(OrganizationUserStatusType.PENDING)
                .build();

        when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.of(pending));

        assertThrows(
                BusinessException.class,
                () -> organizationMusicalRoleService.setMemberInstruments(organizationId, userId, instrumentIds));

        verify(musicalRoleService, never()).setUserInstruments(userId, instrumentIds);
    }

    @Test
    void setMemberInstruments_acceptedMember_delegatesToMusicalRoleService() {
        Long organizationId = 1L;
        Long userId = 10L;
        List<Long> instrumentIds = List.of(1L, 2L);

        OrganizationUser accepted = OrganizationUser.builder()
                .organizationId(organizationId)
                .userId(userId)
                .status(OrganizationUserStatusType.ACCEPTED)
                .build();

        when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.of(accepted));

        organizationMusicalRoleService.setMemberInstruments(organizationId, userId, instrumentIds);

        verify(musicalRoleService).setUserInstruments(userId, instrumentIds);
    }

    @Test
    void addMemberInstrument_userNotAcceptedMember_throwsBusinessException() {
        Long organizationId = 1L;
        Long userId = 10L;
        Long instrumentId = 5L;

        when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> organizationMusicalRoleService.addMemberInstrument(organizationId, userId, instrumentId));

        verify(musicalRoleService, never()).addInstrumentToUser(userId, instrumentId);
    }

    @Test
    void addMemberInstrument_acceptedMember_delegatesToMusicalRoleService() {
        Long organizationId = 1L;
        Long userId = 10L;
        Long instrumentId = 5L;

        OrganizationUser accepted = OrganizationUser.builder()
                .organizationId(organizationId)
                .userId(userId)
                .status(OrganizationUserStatusType.ACCEPTED)
                .build();

        when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.of(accepted));

        organizationMusicalRoleService.addMemberInstrument(organizationId, userId, instrumentId);

        verify(musicalRoleService).addInstrumentToUser(userId, instrumentId);
    }

    @Test
    void removeMemberInstrument_userNotAcceptedMember_throwsBusinessException() {
        Long organizationId = 1L;
        Long userId = 10L;
        Long instrumentId = 5L;

        when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> organizationMusicalRoleService.removeMemberInstrument(organizationId, userId, instrumentId));

        verify(musicalRoleService, never()).removeInstrumentFromUser(userId, instrumentId);
    }

    @Test
    void removeMemberInstrument_acceptedMember_delegatesToMusicalRoleService() {
        Long organizationId = 1L;
        Long userId = 10L;
        Long instrumentId = 5L;

        OrganizationUser accepted = OrganizationUser.builder()
                .organizationId(organizationId)
                .userId(userId)
                .status(OrganizationUserStatusType.ACCEPTED)
                .build();

        when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.of(accepted));

        organizationMusicalRoleService.removeMemberInstrument(organizationId, userId, instrumentId);

        verify(musicalRoleService).removeInstrumentFromUser(userId, instrumentId);
    }
}
