package unicam.cs.hackhub.boundary.dto;

import jakarta.validation.constraints.*;

/**
 * dto che rappresenta il body JSON per la risposta alla richiesta di autenticazione
 * @param token il token jwt generato
 * @param tipo il tipo di jwt usato (in genere Bearer)
 */
public record AuthResponse(
        @NotBlank String token,
        @NotBlank String tipo,
        @NotBlank String nomeUtente
) {
}
