package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.dao.TechnicalRoleDao;
import io.github.Romariok.orkestro.dto.TechnicalRoleDTO;
import io.github.Romariok.orkestro.mapper.TechnicalRoleMapper;
import io.github.Romariok.orkestro.models.Role;
import io.github.Romariok.orkestro.models.UserRole;
import io.github.Romariok.orkestro.models.UserRoleId;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TechnicalRoleServiceTest {

    @Mock
    private TechnicalRoleDao technicalRoleDao;

    @Mock
    private TechnicalRoleMapper technicalRoleMapper;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TechnicalRoleService technicalRoleService;

    @Test
    void getUserRoles_returnsMappedDtos() {
        Long userId = 1L;
        Role role = Role.builder()
                .id(10L)
                .build();
        TechnicalRoleDTO dto = new TechnicalRoleDTO();
        dto.setId(10L);

        when(technicalRoleDao.findUserRoles(userId)).thenReturn(List.of(role));
        when(technicalRoleMapper.toDtoList(List.of(role))).thenReturn(List.of(dto));

        List<TechnicalRoleDTO> result = technicalRoleService.getUserRoles(userId);

        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().getId());
    }

    @Test
    void assignRoleToUser_userNotFound_throwsEntityNotFound() {
        Long userId = 1L;
        Long roleId = 10L;
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(
                EntityNotFoundException.class,
                () -> technicalRoleService.assignRoleToUser(userId, roleId));
    }

    @Test
    void assignRoleToUser_roleNotFound_throwsEntityNotFound() {
        Long userId = 1L;
        Long roleId = 10L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> technicalRoleService.assignRoleToUser(userId, roleId));
    }

    @Test
    void assignRoleToUser_roleAlreadyAssigned_doesNotSave() {
        Long userId = 1L;
        Long roleId = 10L;

        Role role = Role.builder()
                .id(roleId)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsById(any(UserRoleId.class))).thenReturn(true);

        technicalRoleService.assignRoleToUser(userId, roleId);

        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void assignRoleToUser_success_savesUserRole() {
        Long userId = 1L;
        Long roleId = 10L;

        Role role = Role.builder()
                .id(roleId)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsById(any(UserRoleId.class))).thenReturn(false);

        technicalRoleService.assignRoleToUser(userId, roleId);

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(captor.capture());
        UserRole saved = captor.getValue();

        assertEquals(userId, saved.getUserId());
        assertEquals(roleId, saved.getRoleId());
    }

    @Test
    void removeRoleFromUser_deletesUserRole() {
        Long userId = 1L;
        Long roleId = 10L;

        technicalRoleService.removeRoleFromUser(userId, roleId);

        ArgumentCaptor<UserRoleId> captor = ArgumentCaptor.forClass(UserRoleId.class);
        verify(userRoleRepository).deleteById(captor.capture());
        UserRoleId id = captor.getValue();

        assertEquals(userId, id.getUserId());
        assertEquals(roleId, id.getRoleId());
    }
}


