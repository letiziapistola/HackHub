package unicam.cs.hackhub.handler;

import unicam.cs.hackhub.boundary.dto.HackathonCreatedResponse;
import unicam.cs.hackhub.boundary.dto.HackathonRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unicam.cs.hackhub.domain.RuoloStaff;
import unicam.cs.hackhub.domain.implementazione.Hackathon;
import unicam.cs.hackhub.domain.implementazione.Periodo;
import unicam.cs.hackhub.domain.implementazione.Staff;
import unicam.cs.hackhub.domain.implementazione.Utente;
import unicam.cs.hackhub.eccezioni.BadRequestException;
import unicam.cs.hackhub.eccezioni.ConflictException;
import unicam.cs.hackhub.eccezioni.ForbiddenException;
import unicam.cs.hackhub.eccezioni.NotFoundException;
import unicam.cs.hackhub.repository.RepositoryHackathon;
import unicam.cs.hackhub.repository.RepositoryUtente;
import unicam.cs.hackhub.servizi.HackathonBuilder;
import unicam.cs.hackhub.servizi.ServizioNotifiche;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreaHackathonHandler {

    private final RepositoryUtente repositoryUtenti;
    private final RepositoryHackathon repositoryHackathon;
    private final ServizioNotifiche servizioNotifiche;

    /**
     * Crea un handler che si occupa di gestire tutte le operazioni necessarie per creare un hackathon
     *
     * @param repositoryUtenti    la repository per recuperare gli utenti che saranno organizzatori, mentori e giudici
     * @param repositoryHackathon la repository per salvare l'hackathon creato
     * @param servizioNotifiche   il servizio per inviare le notifiche agli utenti invitati come giudici e mentori
     */
    public CreaHackathonHandler(RepositoryUtente repositoryUtenti, RepositoryHackathon repositoryHackathon, ServizioNotifiche servizioNotifiche) {
        this.repositoryUtenti = repositoryUtenti;
        this.repositoryHackathon = repositoryHackathon;
        this.servizioNotifiche = servizioNotifiche;
    }

    /**
     * Avvia la creazione di un hackathon, verificando che il nome dell'hackathon non sia già esistente. Se tutte le
     * verifiche passano, imposta i dati dell'hackathon usando il builder, imposta l'organizzatore, invia gli inviti a
     * giudice e mentori, e salva l'hackathon nel database.
     *
     * @param request    la richiesta di creazione
     * @param nomeUtente il nome utente dell'organizzatore che sta creando l'hackathon
     */
    @Transactional
    public HackathonCreatedResponse avviaCreazioneHackathon(HackathonRequest request, String nomeUtente) {
        validazione(request, nomeUtente);
        HackathonBuilder builder = new HackathonBuilder();
        builder.reset();
        buildSteps(builder, request);
        Hackathon hackathon = builder.getRisultato();
        Staff organizzatore = gestisciOrganizzatore(nomeUtente, hackathon);
        gestisciInvitiStaff(organizzatore, request.nomeMentori(), request.nomeGiudice());
        return new HackathonCreatedResponse(hackathon.getIdHackathon(), hackathon.getNome());
    }

    /**
     * Controlla che i dati siano corretti
     *
     * @param request    la richiesta di creazione dell'hackathon
     * @param nomeUtente l'organizzatore che crea l'hackathon
     */
    private void validazione(HackathonRequest request, String nomeUtente) {
        if (repositoryHackathon.existsByNome(request.nome())) {
            throw new ConflictException("Esiste già un hackathon con questo nome");
        }
        if (repositoryUtenti.findByNomeUtente(request.nomeGiudice()).isEmpty() || request.nomeMentori().stream().anyMatch(nome -> repositoryUtenti.findByNomeUtente(nome).isEmpty())) {
            throw new NotFoundException("Uno o più utenti specificati non esistono");
        }
        if (request.nomeGiudice().equals(nomeUtente) || request.nomeMentori().contains(nomeUtente)) {
            throw new BadRequestException("L'organizzatore non può essere anche giudice o mentore");
        }
        if (request.nomeMentori().contains(request.nomeGiudice())) {
            throw new BadRequestException("Un utente non può essere sia giudice che mentore");
        }
        if (request.nomeMentori().size() != request.nomeMentori().stream().distinct().count()) {
            throw new BadRequestException("Non possono esserci nomi duplicati tra i mentori");
        }
        if (!request.scadenzaIscrizioni().isBefore(request.dataInizio().atStartOfDay())) {
            throw new BadRequestException("La scadenza delle iscrizioni deve essere prima dell'inizio dell'hackathon");
        }
    }

    /**
     * Costruisce un hackathon tramite builder
     *
     * @param builder il builder
     * @param request il dto che contiene i dati dell'hackathon da costruire
     */
    private void buildSteps(HackathonBuilder builder, HackathonRequest request) {
        builder.impostaNome(request.nome());
        Periodo periodo = new Periodo(request.dataInizio(), request.dataFine());
        builder.impostaPeriodo(periodo);
        builder.impostaLuogo(request.luogo());
        builder.impostaPremio(request.premio());
        builder.impostaTeamMin(request.teamMin());
        builder.impostaTeamMax(request.teamMax());
        builder.impostaRegolamento(request.regolamento());
        builder.impostaScadenzaIscrizioni(request.scadenzaIscrizioni());
        builder.impostaMaxIscrizioni(request.maxIscrizioni());
    }

    /**
     * Gestione degli inviti allo staff per un hackathon
     *
     * @param organizzatore l'utente che ha creato l'hackathon
     * @param nomiMentori   i nomi degli utenti che si vogliono invitare come mentori
     * @param nomeGiudice   il nome dell'utente che si vuole invitare come giudice
     */
    private void gestisciInvitiStaff(Staff organizzatore, List<String> nomiMentori, String nomeGiudice) {
        Map<Utente, RuoloStaff> destinatari = gestisciStaff(nomiMentori, nomeGiudice);
        List<Utente> utentiDestinatari = destinatari.keySet().stream().toList();
        String nomeOrganizzatore = organizzatore.getUtente().getNomeUtente();
        for (Utente d : utentiDestinatari)
            servizioNotifiche.creaInvitoStaff(nomeOrganizzatore, d, organizzatore.getHackathon(), destinatari.get(d));
    }

    /**
     * Controlla che i nomi degli utenti legati allo staff siano presenti nel sistema
     *
     * @param nomiMentori i nomi dei mentori
     * @param nomeGiudice il nome del giudice
     * @return una nuova HashMap che associa l'utente esistente al suo ruolo
     */
    private Map<Utente, RuoloStaff> gestisciStaff(List<String> nomiMentori, String nomeGiudice) {
        List<Utente> mentori = nomiMentori.stream().map(nome -> repositoryUtenti.findByNomeUtente(nome).orElseThrow(() -> new NotFoundException("L'utente specificato non esiste: " + nome))).toList();
        Utente giudice = repositoryUtenti.findByNomeUtente(nomeGiudice).orElseThrow(() -> new NotFoundException("L'utente specificato non esiste: " + nomeGiudice));
        return new HashMap<>() {{
            put(giudice, RuoloStaff.GIUDICE);
            mentori.forEach(mentore -> put(mentore, RuoloStaff.MENTORE));
        }};
    }

    /**
     * Controlla che l'organizzatore esista come utente e lo aggiunge allo staff
     *
     * @param nomeUtente il nome utente dell'organizzatore
     * @param hackathon  l'hackathon
     */
    private Staff gestisciOrganizzatore(String nomeUtente, Hackathon hackathon) {
        Utente organizzatore = repositoryUtenti.findByNomeUtente(nomeUtente).orElseThrow(() -> new NotFoundException("L' utente non esiste: " + nomeUtente));
        Staff staffOrganizzatore = new Staff(organizzatore, RuoloStaff.ORGANIZZATORE);
        hackathon.aggiungiStaff(staffOrganizzatore);
        repositoryHackathon.save(hackathon);
        return staffOrganizzatore;
    }
}
