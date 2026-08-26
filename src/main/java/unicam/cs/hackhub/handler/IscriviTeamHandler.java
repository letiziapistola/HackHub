package unicam.cs.hackhub.handler;

import unicam.cs.hackhub.domain.RuoloTeam;
import unicam.cs.hackhub.domain.implementazione.*;
import unicam.cs.hackhub.domain.implementazione.Hackathon;
import unicam.cs.hackhub.domain.implementazione.IscrizioneTeam;
import unicam.cs.hackhub.domain.implementazione.MembroTeam;
import unicam.cs.hackhub.domain.implementazione.Team;
import unicam.cs.hackhub.eccezioni.ConflictException;
import unicam.cs.hackhub.eccezioni.ForbiddenException;
import unicam.cs.hackhub.eccezioni.NotFoundException;
import unicam.cs.hackhub.eccezioni.TransizioneNonConsentitaException;
import unicam.cs.hackhub.repository.RepositoryHackathon;
import unicam.cs.hackhub.repository.RepositoryIscrizioniTeam;
import unicam.cs.hackhub.repository.RepositoryMembriTeam;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IscriviTeamHandler {
    private final RepositoryMembriTeam repositoryMembriTeam;
    private final RepositoryHackathon repositoryHackathon;
    private final RepositoryIscrizioniTeam repositoryIscrizioniTeam;

    /**
     * Crea una nuova istanza dell'handler che gestisce l'iscrizione dei team
     *
     * @param repositoryMembriTeam     la repository dei membri del team
     * @param repositoryHackathon      la repository degli hackathon
     * @param repositoryIscrizioniTeam la repository delle iscrizioni
     */
    public IscriviTeamHandler(RepositoryMembriTeam repositoryMembriTeam, RepositoryHackathon repositoryHackathon, RepositoryIscrizioniTeam repositoryIscrizioniTeam) {
        this.repositoryMembriTeam = repositoryMembriTeam;
        this.repositoryHackathon = repositoryHackathon;
        this.repositoryIscrizioniTeam = repositoryIscrizioniTeam;
    }

    /**
     * Avvia un iscrizione di un team ad un hackathon
     *
     * @param nomeUtente    il nome dell'utente che vuole iscrivere il team
     * @param nomeHackathon il nome dell'hackathon
     */
    @Transactional
    public void avviaIscrizioneHackathon(String nomeUtente, String nomeHackathon) {
        MembroTeam leader = validazioneLeader(nomeUtente);
        Team team = leader.getTeam();
        Hackathon hackathon = repositoryHackathon.findByNome(nomeHackathon).orElseThrow(() ->
                new NotFoundException("Hackathon non trovato"));
        checkIscrizioneInHackathon(hackathon, team);

        IscrizioneTeam iscrizione = new IscrizioneTeam(team, hackathon);
        hackathon.aggiungiIscrizione(iscrizione);
        repositoryHackathon.save(hackathon);
    }

    private MembroTeam validazioneLeader(String nomeUtente){
        MembroTeam leader = repositoryMembriTeam.findByUtente_NomeUtente(nomeUtente).orElseThrow(() ->
                new NotFoundException("L'utente non è membro di nessun team"));

        if (!leader.getRuolo().equals(RuoloTeam.LEADER))
            throw new ForbiddenException("L'utente non è il leader del team");
        return leader;
    }
    /**
     * Controlla che un team si possa iscrivere ad un hackathon
     *
     * @param hackathon l'hackathon
     * @param team      il team
     */
    private void checkIscrizioneInHackathon(Hackathon hackathon, Team team) {
        hackathon.getStato().verificaIscrizioneConsentita(hackathon);
        if (team.getNumMembri() < hackathon.getTeamMin() || team.getNumMembri() > hackathon.getTeamMax())
            throw new ConflictException("Il numero di membri del team non è compatibile con i requisiti dell'hackathon");

        if (hackathon.getIscrizioni().size() >= hackathon.getMaxIscrizioni())
            throw new ConflictException("Il numero massimo di iscrizioni è già stato raggiunto");
        if (repositoryIscrizioniTeam.existsByHackathonAndTeam(hackathon, team))
            throw new ConflictException("Il team è già iscritto all'hackathon");
        List<String> idMembriTeam = team.getMembri().stream().map(MembroTeam::getUtente).map(Utente::getIdUtente).toList();
        List<String> idMembriStaff = hackathon.getStaff().stream().map(Staff::getUtente).map(Utente::getIdUtente).toList();
        if (idMembriStaff.stream().anyMatch(idMembriTeam::contains)) {
            throw new ConflictException("Un membro del team è anche membro dello staff dell'hackathon");
        }
    }

    /**
     * Annulla l'iscrizione ad un hackathon
     *
     * @param nomeUtente    il nome dell'utente
     * @param nomeHackathon il nome dell'hackathon
     */
    @Transactional
    public void annullaIscrizioneHackathon(String nomeUtente, String nomeHackathon) {
        Hackathon hackathon = repositoryHackathon.findByNome(nomeHackathon).orElseThrow(() ->
                new NotFoundException("Hackathon non trovato"));
        try {
            hackathon.getStato().verificaAnnullamentoIscrizioneConsentito(hackathon);
        } catch (TransizioneNonConsentitaException ex) {
            throw new ConflictException("Non è possibile annullare l'iscrizione se le iscrizioni sono chiuse");
        }
        MembroTeam leader = validazioneLeader(nomeUtente);
        Team team = leader.getTeam();
        hackathon.rimuoviIscrizione(team);
        repositoryHackathon.save(hackathon);
    }
}
