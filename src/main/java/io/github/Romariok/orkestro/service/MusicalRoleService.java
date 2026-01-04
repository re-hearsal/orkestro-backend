package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.dao.MusicalRoleDao;
import io.github.Romariok.orkestro.dto.role.MusicalRoleDTO;
import io.github.Romariok.orkestro.mapper.MusicalRoleMapper;
import io.github.Romariok.orkestro.models.role.Instrument;
import io.github.Romariok.orkestro.models.user.UserInstrument;
import io.github.Romariok.orkestro.models.user.UserInstrumentId;
import io.github.Romariok.orkestro.repository.InstrumentRepository;
import io.github.Romariok.orkestro.repository.UserInstrumentRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MusicalRoleService {

   private final MusicalRoleDao musicalRoleDao;
   private final MusicalRoleMapper musicalRoleMapper;
   private final UserInstrumentRepository userInstrumentRepository;
   private final InstrumentRepository instrumentRepository;
   private final UserRepository userRepository;

   @Transactional(readOnly = true)
   public List<MusicalRoleDTO> getUserMusicalRoles(Long userId) {
      List<Instrument> instruments = musicalRoleDao.findUserInstruments(userId);
      return musicalRoleMapper.toDtoList(instruments);
   }

   @Transactional
   public void addInstrumentToUser(Long userId, Long instrumentId) {
      if (!userRepository.existsById(userId)) {
         throw new EntityNotFoundException("User not found: " + userId);
      }

      if (!instrumentRepository.existsById(instrumentId)) {
         throw new EntityNotFoundException("Instrument not found: " + instrumentId);
      }

      UserInstrumentId id = UserInstrumentId.builder()
            .userId(userId)
            .instrumentId(instrumentId)
            .build();

      if (userInstrumentRepository.existsById(id)) {
         return;
      }

      UserInstrument userInstrument = UserInstrument.builder()
            .userId(userId)
            .instrumentId(instrumentId)
            .build();
      userInstrumentRepository.save(userInstrument);
   }

   @Transactional
   public void setUserInstruments(Long userId, List<Long> instrumentIds) {
      if (!userRepository.existsById(userId)) {
         throw new EntityNotFoundException("User not found: " + userId);
      }

      List<Long> safeIds = instrumentIds != null ? instrumentIds : List.of();
      Set<Long> newIds = new HashSet<>(safeIds);

      // Проверяем, что все инструменты существуют
      if (!newIds.isEmpty()) {
         List<Instrument> instruments = instrumentRepository.findAllById(newIds);
         if (instruments.size() != newIds.size()) {
            throw new EntityNotFoundException("One or more instruments not found for ids: " + newIds);
         }
      }

      // Текущие связи пользователя
      List<UserInstrument> current = userInstrumentRepository.findByUserId(userId);
      Set<Long> currentIds = current.stream()
            .map(UserInstrument::getInstrumentId)
            .collect(java.util.stream.Collectors.toSet());

      // Удаляем лишние
      for (UserInstrument ui : current) {
         if (!newIds.contains(ui.getInstrumentId())) {
            userInstrumentRepository.delete(ui);
         }
      }

      // Добавляем недостающие
      for (Long instrumentId : newIds) {
         if (!currentIds.contains(instrumentId)) {
            UserInstrument ui = UserInstrument.builder()
                  .userId(userId)
                  .instrumentId(instrumentId)
                  .build();
            userInstrumentRepository.save(ui);
         }
      }
   }

   @Transactional
   public void removeInstrumentFromUser(Long userId, Long instrumentId) {
      UserInstrumentId id = UserInstrumentId.builder()
            .userId(userId)
            .instrumentId(instrumentId)
            .build();
      userInstrumentRepository.deleteById(id);
   }
}
