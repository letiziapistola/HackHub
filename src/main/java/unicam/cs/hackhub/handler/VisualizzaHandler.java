package unicam.cs.hackhub.handler;

import org.springframework.transaction.annotation.Transactional;
import unicam.cs.hackhub.boundary.dto.*;
import unicam.cs.hackhub.domain.RuoloStaff;
import unicam.cs.hackhub.domain.implementazione.*;
import unicam.cs.hackhub.eccezioni.ConflictException;
import unicam.cs.hackhub.eccezioni.NotFoundException;
import unicam.cs.hackhub.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VisualizzaHandler {

    private final RepositoryHackathon repositoryHackathon;
    private final RepositoryRichiesta repositoryRichiesta;
    private final RepositoryNotifica repositoryNotifica;
    private final RepositoryUtente repositoryUtente;
    private final RepositoryStaff repositoryStaff;

    /**
     * Costruttore che inizializza questo handler per visualizzare liste di oggetti
     *
     * @param repositoryHackathon la repository degli hackathon
     */
    public VisualizzaHandler(RepositoryHackathon repositoryHackathon, RepositoryRichiesta repositoryRichiesta, RepositoryNotifica repositoryNotifica, RepositoryUtente repositoryUtente, RepositoryStaff repositoryStaff) {
        this.repositoryHackathon = repositoryHackathon;
        this.repositoryRichiesta = repositoryRichiesta;
        this.repositoryNotifica = repositoryNotifica;
        this.repositoryUtente = repositoryUtente;
        this.repositoryStaff = repositoryStaff;
    }

    private Hackathon validaAutorizzazioni(String nomeUtente, String nomeHackathon) {
        verificaUtenteOrFail(nomeUtente);
        Staff staff = repositoryStaff.findByUtente_NomeUtente(nomeUtente)
                .orElseThrow(() -> new ConflictException("L'utente non è membro di nessuno staff"));
        if (!staff.getHackathon().getNome().equals(nomeHackathon)) {
            throw new ConflictException("L'utente non è membro dello staff di questo hackathon");
        }
        return repositoryHackathon.findByNome(nomeHackathon)
                .orElseThrow(() -> new NotFoundException("Hackathon non trovato"));
    }

    private Utente verificaUtenteOrFail(String nomeUtente) {
        return repositoryUtente.findByNomeUtente(nomeUtente).orElseThrow(() ->
                new NotFoundException("Utente non trovato"));
    }

    /**
     * Metodo che ritorna la lista di valutazioni delle sottomissioni consegnate a un hackathon
     *
     * @param nomeHackathon il nome dell'hackathon di riferimento
     * @return la lista delle valutazioni
     */
    public List<ValutazioneRequest> viewValutazioni(String nomeUtente, String nomeHackathon) {
        Hackathon hackathon = validaAutorizzazioni(nomeUtente, nomeHackathon);
        List<Valutazione> valutazioni = new ArrayList<>();
        for (IscrizioneTeam i : hackathon.getIscrizioni()) {
            valutazioni.add(i.getSottomissione().getValutazione());
        }
        return valutazioni.stream().map(v -> new ValutazioneRequest(v.getDescrizione(), v.getVoto())).collect(Collectors.toList());
    }

    /**
     * Metodo che ritorna la lista di sottomissioni consegnate in un hackathon
     *
     * @param nomeHackathon il nome dell'hackathon di riferimento
     * @return la lista di sottomissioni
     */
    public List<SottomissioneDTO> viewSottomissioni(String nomeUtente, String nomeHackathon) {
        Hackathon hackathon = validaAutorizzazioni(nomeUtente, nomeHackathon);
        List<Sottomissione> sottomissioni = new ArrayList<>();
        for (IscrizioneTeam i : hackathon.getIscrizioni()) {
            sottomissioni.add(i.getSottomissione());
        }

        return sottomissioni.stream().map(s -> new SottomissioneDTO
                (s.getLink(), s.getValutazione().getDescrizione(), s.getValutazione().getVoto())).collect(Collectors.toList());
    }

    /**
     * Metodo che ritorna la lista di iscrizioni effettuate a un hackathon
     *
     * @param nomeHackathon il nome dell'hackathon di riferimento
     * @return la lista delle iscrizioni
     */
    public List<IscrizioneTeamDTO> viewIscrizioni(String nomeUtente, String nomeHackathon) {
        Hackathon hackathon = validaAutorizzazioni(nomeUtente, nomeHackathon);
        return hackathon.getIscrizioni().stream().map(i -> new IscrizioneTeamDTO
                (i.getHackathon().getNome(), i.getTeam().getNome(), i.getSottomissione().getLink())).collect(Collectors.toList());
    }

    /**
     * Metodo che ritorna la lista di richieste pendenti in formato JSON
     *
     * @param nomeUtente il nome utente dell'utente destinatario delle richieste
     * @return una lista di richieste JSON
     */
    public List<RichiestaDTO> viewRichieste(String nomeUtente) {
        Utente utente = verificaUtenteOrFail(nomeUtente);
        List<Richiesta> listRichieste = repositoryRichiesta.findAllByDestinatario(utente);
        return listRichieste.stream().map(r -> new RichiestaDTO(r.getIdRichiesta(), r.getPayload()))
                .collect(Collectors.toList());
    }

    /**
     * Metodo che ritorna la lista di notifiche destinate all'utente di riferimento
     *
     * @param nomeUtente il nome utente dell'utente destinatario delle notifiche
     * @return la lista di notifiche dto
     */
    public List<NotificaDTO> viewNotifiche(String nomeUtente) {
        Utente utente = verificaUtenteOrFail(nomeUtente);
        List<Notifica> listNotifiche = repositoryNotifica.findAllByDestinatario(utente);
        return listNotifiche.stream().map(n -> new NotificaDTO(n.getPayload()))
                .collect(Collectors.toList());
    }

    /**
     * Metodo che ritorna la lista di informazioni pubbliche destinate all'utente di riferimento
     *
     * @return la lista di hackathon dto
     */
    @Transactional(readOnly = true)
    public List<InfoHackathonDTO> viewInfoHackathon() {
        List<Hackathon> listHackathon = repositoryHackathon.findAll();
        List<InfoHackathonDTO> listInfoHackathonDTO = new ArrayList<>();
        for (Hackathon h : listHackathon) {
            InfoHackathonDTO infoHackathon = creaHackathon(h);
            listInfoHackathonDTO.add(infoHackathon);
        }
        return listInfoHackathonDTO;
    }

    private InfoHackathonDTO creaHackathon(Hackathon h) {
        int numeroTeamIscritti = h.getIscrizioni().size();
        int postiRimanenti = h.getMaxIscrizioni() - numeroTeamIscritti;
        List<Staff> staff = h.getStaff();
        String organizzatore = ottieniNomeStaff(staff, RuoloStaff.ORGANIZZATORE);
        List<String> mentori = new ArrayList<>();
        List<Staff> staffMentori = staff.stream().filter(s -> s.getRuolo() == RuoloStaff.MENTORE).toList();
        for (Staff s : staffMentori) {
            mentori.add(s.getUtente().getNomeUtente());
        }
        String giudice = ottieniNomeStaff(staff, RuoloStaff.GIUDICE);
        return new InfoHackathonDTO(h.getIdHackathon(), h.getNome(), h.getPeriodo().getDataInizio(), h.getPeriodo().getDataFine(), h.getLuogo(),
                h.getPremio(), h.getTeamMin(), h.getTeamMax(), h.getRegolamento(), h.getScadenzaIscrizioni(),
                h.getStatoEnum(), numeroTeamIscritti, h.getMaxIscrizioni(), postiRimanenti, giudice, mentori, organizzatore);
    }

    private String ottieniNomeStaff(List<Staff> staff, RuoloStaff ruolo) {
        for (Staff s : staff) {
            if (s.getRuolo() == ruolo) {
                return s.getUtente().getNomeUtente();
            }
        }
        return null;
    }

    /**
     * Metodo che ritorna le informazioni pubbliche di un hackathon dato il suo id
     * @param id l'id dell'hackathon di riferimento
     * @return un oggetto InfoHackathonDTO contenente le informazioni pubbliche dell'hackathon
     */
    @Transactional(readOnly = true)
    public InfoHackathonDTO viewInfoHackathonById(String id) {
        Hackathon h = repositoryHackathon.findById(id).orElseThrow(() ->
                new NotFoundException("Hackathon non trovato"));
        return creaHackathon(h);
    }
}
