package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.dao.MusicalRoleDao;
import io.github.Romariok.orkestro.dto.MusicalRoleDTO;
import io.github.Romariok.orkestro.mapper.MusicalRoleMapper;
import io.github.Romariok.orkestro.models.Instrument;
import io.github.Romariok.orkestro.models.UserInstrument;
import io.github.Romariok.orkestro.models.UserInstrumentId;
import io.github.Romariok.orkestro.repository.InstrumentRepository;
import io.github.Romariok.orkestro.repository.UserInstrumentRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MusicalRoleServiceTest {

   @Mock
   private MusicalRoleDao musicalRoleDao;

   @Mock
   private MusicalRoleMapper musicalRoleMapper;

   @Mock
   private UserInstrumentRepository userInstrumentRepository;

   @Mock
   private InstrumentRepository instrumentRepository;

   @Mock
   private UserRepository userRepository;

   @InjectMocks
   private MusicalRoleService musicalRoleService;

   @Test
   void getUserMusicalRoles_returnsMappedDtos() {
      Long userId = 1L;
      Instrument instrument = Instrument.builder()
            .id(5L)
            .name("Violin")
            .build();

      MusicalRoleDTO dto = new MusicalRoleDTO(5L, "Violin");

      when(musicalRoleDao.findUserInstruments(userId)).thenReturn(List.of(instrument));
      when(musicalRoleMapper.toDtoList(List.of(instrument))).thenReturn(List.of(dto));

      List<MusicalRoleDTO> result = musicalRoleService.getUserMusicalRoles(userId);

      assertEquals(1, result.size());
      assertEquals(5L, result.getFirst().getInstrumentId());
      assertEquals("Violin", result.getFirst().getInstrumentName());
   }

   @Test
   void addInstrumentToUser_userNotFound_throwsEntityNotFound() {
      Long userId = 1L;
      Long instrumentId = 5L;

      when(userRepository.existsById(userId)).thenReturn(false);

      assertThrows(
            EntityNotFoundException.class,
            () -> musicalRoleService.addInstrumentToUser(userId, instrumentId));
   }

   @Test
   void addInstrumentToUser_instrumentNotFound_throwsEntityNotFound() {
      Long userId = 1L;
      Long instrumentId = 5L;

      when(userRepository.existsById(userId)).thenReturn(true);
      when(instrumentRepository.existsById(instrumentId)).thenReturn(false);

      assertThrows(
            EntityNotFoundException.class,
            () -> musicalRoleService.addInstrumentToUser(userId, instrumentId));
   }

   @Test
   void addInstrumentToUser_alreadyExists_doesNotSave() {
      Long userId = 1L;
      Long instrumentId = 5L;

      when(userRepository.existsById(userId)).thenReturn(true);
      when(instrumentRepository.existsById(instrumentId)).thenReturn(true);
      when(userInstrumentRepository.existsById(any(UserInstrumentId.class))).thenReturn(true);

      musicalRoleService.addInstrumentToUser(userId, instrumentId);

      verify(userInstrumentRepository, never()).save(any(UserInstrument.class));
   }

   @Test
   void addInstrumentToUser_success_savesRelation() {
      Long userId = 1L;
      Long instrumentId = 5L;

      when(userRepository.existsById(userId)).thenReturn(true);
      when(instrumentRepository.existsById(instrumentId)).thenReturn(true);
      when(userInstrumentRepository.existsById(any(UserInstrumentId.class))).thenReturn(false);

      musicalRoleService.addInstrumentToUser(userId, instrumentId);

      ArgumentCaptor<UserInstrument> captor = ArgumentCaptor.forClass(UserInstrument.class);
      verify(userInstrumentRepository).save(captor.capture());
      UserInstrument saved = captor.getValue();

      assertEquals(userId, saved.getUserId());
      assertEquals(instrumentId, saved.getInstrumentId());
   }

   @Test
   void removeInstrumentFromUser_deletesRelation() {
      Long userId = 1L;
      Long instrumentId = 5L;

      musicalRoleService.removeInstrumentFromUser(userId, instrumentId);

      ArgumentCaptor<UserInstrumentId> captor = ArgumentCaptor.forClass(UserInstrumentId.class);
      verify(userInstrumentRepository).deleteById(captor.capture());
      UserInstrumentId id = captor.getValue();

      assertEquals(userId, id.getUserId());
      assertEquals(instrumentId, id.getInstrumentId());
   }

   @Test
   void setUserInstruments_userNotFound_throwsEntityNotFound() {
      Long userId = 1L;
      when(userRepository.existsById(userId)).thenReturn(false);

      assertThrows(
            EntityNotFoundException.class,
            () -> musicalRoleService.setUserInstruments(userId, List.of(1L, 2L)));
   }

   @Test
   void setUserInstruments_instrumentMissing_throwsEntityNotFound() {
      Long userId = 1L;
      when(userRepository.existsById(userId)).thenReturn(true);

      Instrument instrument = Instrument.builder()
            .id(1L)
            .build();

      when(instrumentRepository.findAllById(any()))
            .thenReturn(List.of(instrument));

      assertThrows(
            EntityNotFoundException.class,
            () -> musicalRoleService.setUserInstruments(userId, List.of(1L, 2L)));
   }

   @Test
   void setUserInstruments_replacesExistingRelations() {
      Long userId = 1L;

      when(userRepository.existsById(userId)).thenReturn(true);

      Instrument instrument1 = Instrument.builder()
            .id(1L)
            .build();
      Instrument instrument3 = Instrument.builder()
            .id(3L)
            .build();

      when(instrumentRepository.findAllById(any()))
            .thenReturn(List.of(instrument1, instrument3));

      UserInstrument ui1 = UserInstrument.builder()
            .userId(userId)
            .instrumentId(1L)
            .build();

      UserInstrument ui2 = UserInstrument.builder()
            .userId(userId)
            .instrumentId(2L)
            .build();

      when(userInstrumentRepository.findByUserId(userId)).thenReturn(List.of(ui1, ui2));

      musicalRoleService.setUserInstruments(userId, List.of(1L, 3L));

      verify(userInstrumentRepository).delete(ui2);

      ArgumentCaptor<UserInstrument> captor = ArgumentCaptor.forClass(UserInstrument.class);
      verify(userInstrumentRepository).save(captor.capture());
      UserInstrument saved = captor.getValue();

      assertEquals(userId, saved.getUserId());
      assertEquals(3L, saved.getInstrumentId());
   }
}
