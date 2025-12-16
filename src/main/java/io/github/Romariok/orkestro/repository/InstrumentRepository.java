package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.Instrument;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findByName(String name);
}


