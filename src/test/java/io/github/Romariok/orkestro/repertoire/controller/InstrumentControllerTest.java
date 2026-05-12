package io.github.Romariok.orkestro.repertoire.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.repertoire.repository.InstrumentRepository;
import io.github.Romariok.orkestro.user.models.Instrument;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InstrumentControllerTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private InstrumentController instrumentController;

    @Test
    void getAllInstruments_returnsOk() {
        Instrument violin = Instrument.builder().id(1L).name("Violin").iconKey(null).build();
        Instrument piano = Instrument.builder().id(2L).name("Piano").iconKey(null).build();
        when(instrumentRepository.findAll(any(Sort.class))).thenReturn(List.of(violin, piano));

        var result = instrumentController.getAllInstruments();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertEquals("Violin", result.getBody().get(0).getName());
        assertEquals("Piano", result.getBody().get(1).getName());
        assertNull(result.getBody().get(0).getPictureUrl());
    }

    @Test
    void getAllInstruments_emptyList_returnsOk() {
        when(instrumentRepository.findAll(any(Sort.class))).thenReturn(List.of());

        var result = instrumentController.getAllInstruments();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(0, result.getBody().size());
    }
}
