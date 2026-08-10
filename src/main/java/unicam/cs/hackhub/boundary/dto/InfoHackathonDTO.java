package unicam.cs.hackhub.boundary.dto;

import unicam.cs.hackhub.domain.StatoEnum;
import unicam.cs.hackhub.domain.implementazione.statePattern.StatoHackathon;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InfoHackathonDTO(
        @NotBlank String id,
        @NotBlank String nome,
        @NotNull LocalDate dataInizio,
        @NotNull LocalDate dataFine,
        @NotBlank String luogo,
        @NotNull @Positive BigDecimal premio,
        @Min(3) @Max(6) int teamMin,
        @Max(6) @Min(3) int teamMax,
        @NotBlank String regolamento,
        @NotNull LocalDateTime scadenzaIscrizioni,
        @NotNull StatoEnum stato,
        int numeroTeamIscritti,
        @Max(40) int maxIscrizioni,
        int postiRimanenti,
        @NotBlank String nomeGiudice,
        @NotEmpty @NotNull @Size(min = 1) java.util.List<@NotBlank String> nomeMentori,
        @NotBlank String nomeOrganizzatore
) {
}
