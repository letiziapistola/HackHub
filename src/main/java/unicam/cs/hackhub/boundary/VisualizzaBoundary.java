package unicam.cs.hackhub.boundary;

import unicam.cs.hackhub.boundary.dto.*;
import unicam.cs.hackhub.boundary.dto.*;
import unicam.cs.hackhub.handler.VisualizzaHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api")
@Validated
public class VisualizzaBoundary {

    private final VisualizzaHandler handler;

    public VisualizzaBoundary(VisualizzaHandler handler) {
        this.handler = handler;
    }

    /**
     * Metodo del boundary che ritorna una lista di valutazioni
     * @param nomeHackathon id dell'hackathon di riferimento
     * @return esito della chiamata http
     */
    @GetMapping("hackathon/{nomeHackathon}/valutazioni")
    public ResponseEntity<List<ValutazioneRequest>> viewValutazioni(
            @PathVariable String nomeHackathon,
            @AuthenticationPrincipal String nomeUtente) {
        List<ValutazioneRequest> listaValutazioni = handler.viewValutazioni(nomeUtente, nomeHackathon);
        return ResponseEntity.status(HttpStatus.OK).body(listaValutazioni);
    }

    /**
     * Metodo del boundary che ritorna una lista di sottomissioni
     * @param nomeHackathon id dell'hackathon di riferimento
     * @return esito della chiamata http
     */
    @GetMapping("/hackathon/{nomeHackathon}/sottomissioni")
    public ResponseEntity<List<SottomissioneDTO>> viewSottomissioni(
            @PathVariable String nomeHackathon,
            @AuthenticationPrincipal String nomeUtente) {
        List<SottomissioneDTO> listaSottomissioni = handler.viewSottomissioni(nomeUtente, nomeHackathon);
        return ResponseEntity.status(HttpStatus.OK).body(listaSottomissioni);
    }

    /**
     * Metodo del boundary che ritorna una lista di iscrizioni
     * @param nomeHackathon id dell'hackathon di riferimento
     * @return esito della chiamata http
     */
    @GetMapping("/hackathon/{nomeHackathon}/iscrizioni")
    public ResponseEntity<List<IscrizioneTeamDTO>> viewIscrizioni(
            @PathVariable String nomeHackathon,
            @AuthenticationPrincipal String nomeUtente) {
        List<IscrizioneTeamDTO> listaIscrizioni = handler.viewIscrizioni(nomeUtente, nomeHackathon);
        return ResponseEntity.status(HttpStatus.OK).body(listaIscrizioni);
    }

    /**
     * Metodo del boundary che ritorna una lista di richieste
     * @return la lista di dto
     */
    @GetMapping("/richieste")
    public ResponseEntity<List<RichiestaDTO>> viewRichieste(@AuthenticationPrincipal String nomeUtente) {
        List<RichiestaDTO> listaRichieste = handler.viewRichieste(nomeUtente);
        return ResponseEntity.status(HttpStatus.OK).body(listaRichieste);
    }

    /**
     * Metodo del boundary che ritorna una lista di notifiche
     * @return la lista di dto
     */
    @GetMapping("/notifiche")
    public ResponseEntity<List<NotificaDTO>> viewNotifiche(@AuthenticationPrincipal String nomeUtente) {
        List<NotificaDTO> listaNotifiche = handler.viewNotifiche(nomeUtente);
        return ResponseEntity.status(HttpStatus.OK).body(listaNotifiche);
    }

    /**
     * Metodo del boundary che ritorna le informazioni pubbliche degli hackathon
     * @return la lista di dto
     */
    @GetMapping("/hackathon")
    public ResponseEntity<List<InfoHackathonDTO>> viewInfoHackathon() {
        List<InfoHackathonDTO> infoHackathon = handler.viewInfoHackathon();
        return ResponseEntity.status(HttpStatus.OK).body(infoHackathon);
    }

    /**
     * Metodo del boundary che ritorna le informazioni pubbliche di un hackathon specifico
     * @param id l'id dell'hackathon di riferimento
     * @return la lista di dto
     */
    @GetMapping("/hackathon/{id}")
    public ResponseEntity<InfoHackathonDTO> viewInfoHackathonById(@PathVariable String id) {
        InfoHackathonDTO infoHackathon = handler.viewInfoHackathonById(id);
        return ResponseEntity.status(HttpStatus.OK).body(infoHackathon);
    }
}
