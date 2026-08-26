package unicam.cs.hackhub.boundary;

import unicam.cs.hackhub.boundary.dto.HackathonCreatedResponse;
import unicam.cs.hackhub.boundary.dto.HackathonRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unicam.cs.hackhub.handler.CreaHackathonHandler;


@RestController
@RequestMapping("/api/hackathon")
@Validated
public class CreaHackathonBoundary {

    private final CreaHackathonHandler handler;

    public CreaHackathonBoundary(CreaHackathonHandler handler) {
        this.handler = handler;
    }

    /**
     * Metodo che avvia la creazione di un hackathon
     * @param nomeUtente il nome dell'utente che vuole creare l'hackathon
     * @param request la richiesta contenente i dati dell'hackathon da creare
     * @return una nuova chiamata http
     */
    @PostMapping
    public ResponseEntity<HackathonCreatedResponse> avviaCreazioneHackathon(
            @AuthenticationPrincipal String nomeUtente,
            @Valid @RequestBody HackathonRequest request
            ) {
        HackathonCreatedResponse hackathonCreatedResponse = handler.avviaCreazioneHackathon(request, nomeUtente);
        return ResponseEntity.ok(hackathonCreatedResponse);
    }

}
