package unicam.cs.hackhub.boundary;

import unicam.cs.hackhub.boundary.dto.TeamResponse;
import unicam.cs.hackhub.handler.GestisciTeamHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team")
public class GestisciTeamBoundary {

    private final GestisciTeamHandler handler;

    public GestisciTeamBoundary(GestisciTeamHandler handler){
        this.handler = handler;
    }

    /**
     * Metodo della boundary per cambiare nome a un team
     * @param nomeUtente il nome dell'utente che vuole cambiare il nome
     * @return una nuova chiamata http
     */
    @PatchMapping()
    public ResponseEntity<Void> cambiaNome(
            @AuthenticationPrincipal String nomeUtente,
            @RequestBody String nuovoNome){
        handler.cambiaNomeTeam(nomeUtente, nuovoNome);
        return ResponseEntity.ok().build();
    }

    /**
     * Metodo della boundary per uscire da un team
     * @param nomeUtente il nome dell'utente che vuole uscire dal team
     * @return una nuova chiamata http
     */
    @DeleteMapping("/membri/me")
    public ResponseEntity<Void> esciDalTeam(
            @AuthenticationPrincipal String nomeUtente
    ){
        handler.esciDalTeam(nomeUtente);
        return ResponseEntity.ok().build();
    }

    /**
     * Metodo della boundary per sciogliere un team
     * @param nomeUtente il nome dell'utente che vuole sciogliere il team
     * @return una nuova chiamata http
     */
    @DeleteMapping("/mio")
    public ResponseEntity<Void> sciogliTeam(
            @AuthenticationPrincipal String nomeUtente){
        handler.sciogliTeam(nomeUtente);
        return ResponseEntity.ok().build();
    }

    /**
     * Metodo della boundary per espellere un membro da un team
     * @param nomeUtente il nome dell'utente che vuole espellere il membro
     * @param nomeMembro l'id del membro da espellere
     * @return una nuova chiamata http
     */
    @DeleteMapping("/membri/{nomeMembro}")
    public ResponseEntity<Void> espelliMembro(
            @AuthenticationPrincipal String nomeUtente,
            @PathVariable String nomeMembro
    ){
        handler.espelliMembro(nomeUtente, nomeMembro);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/leader")
    public ResponseEntity<Void> trasferisceRuoloLeader(
            @AuthenticationPrincipal String nomeUtente,
            @RequestParam String nomeMembro){
        handler.trasferisceRuoloLeader(nomeUtente, nomeMembro);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mio")
    public ResponseEntity<TeamResponse> visualizzaTeam(@AuthenticationPrincipal String nomeUtente){
        TeamResponse teamResponse = handler.visualizzaTeam(nomeUtente);
        return ResponseEntity.ok(teamResponse);
    }

    @GetMapping("/iscrizioni")
    public ResponseEntity<List<String>> visualizzaIscrizioniTeam(@AuthenticationPrincipal String nomeUtente){
        return ResponseEntity.ok(handler.visualizzaIscrizioniTeam(nomeUtente));
    }
}
