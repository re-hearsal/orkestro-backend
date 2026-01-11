package io.github.Romariok.orkestro.user.dao;

import io.github.Romariok.orkestro.user.models.Instrument;
import io.github.Romariok.orkestro.user.repository.UserInstrumentRepository;

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


