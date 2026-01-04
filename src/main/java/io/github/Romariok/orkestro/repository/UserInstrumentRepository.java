package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.role.Instrument;
import io.github.Romariok.orkestro.models.user.UserInstrument;
import io.github.Romariok.orkestro.models.user.UserInstrumentId;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInstrumentRepository extends JpaRepository<UserInstrument, UserInstrumentId> {

    @Query("SELECT i FROM Instrument i JOIN UserInstrument ui ON ui.instrumentId = i.id WHERE ui.userId = :userId")
    List<Instrument> findInstrumentsByUserId(@Param("userId") Long userId);

    List<UserInstrument> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
