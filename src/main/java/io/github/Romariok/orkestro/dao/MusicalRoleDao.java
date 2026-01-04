package io.github.Romariok.orkestro.dao;

import io.github.Romariok.orkestro.models.role.Instrument;
import io.github.Romariok.orkestro.repository.UserInstrumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MusicalRoleDao {

    private final UserInstrumentRepository userInstrumentRepository;

    public List<Instrument> findUserInstruments(Long userId) {
        return userInstrumentRepository.findInstrumentsByUserId(userId);
    }
}


