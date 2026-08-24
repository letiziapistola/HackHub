package unicam.cs.hackhub.handler;

import jakarta.transaction.Transactional;
import unicam.cs.hackhub.boundary.dto.TeamResponse;
import unicam.cs.hackhub.domain.RuoloTeam;
import unicam.cs.hackhub.domain.implementazione.Hackathon;
import unicam.cs.hackhub.domain.implementazione.IscrizioneTeam;
import unicam.cs.hackhub.domain.implementazione.MembroTeam;
import unicam.cs.hackhub.domain.implementazione.Team;
import unicam.cs.hackhub.eccezioni.ConflictException;
import unicam.cs.hackhub.eccezioni.NotFoundException;
import unicam.cs.hackhub.eccezioni.TransizioneNonConsentitaException;
import unicam.cs.hackhub.repository.*;
import unicam.cs.hackhub.repository.RepositoryIscrizioniTeam;
import unicam.cs.hackhub.repository.RepositoryMembriTeam;
import unicam.cs.hackhub.repository.RepositoryTeam;
import unicam.cs.hackhub.servizi.ServizioNotifiche;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static unicam.cs.hackhub.domain.TipoNotifica.*;

@Service
public class GestisciTeamHandler {

    private final RepositoryTeam repositoryTeam;
    private final RepositoryMembriTeam repositoryMembriTeam;
    private final ServizioNotifiche servizioNotifiche;
    private final RepositoryIscrizioniTeam repositoryIscrizioniTeam;

    /**
     * Crea una nuova istanza dell'handler per gestire il team
     *
     * @param repositoryTeam           la repository dei team
     * @param repositoryMembriTeam     la repository per i membri dell team
     * @param servizioNotifiche        il servizio per le notifiche
     * @param repositoryIscrizioniTeam la repository per le iscrizioni dei team
     */
    public GestisciTeamHandler(RepositoryTeam repositoryTeam, RepositoryMembriTeam repositoryMembriTeam, ServizioNotifiche servizioNotifiche, RepositoryIscrizioniTeam repositoryIscrizioniTeam) {
        this.repositoryTeam = repositoryTeam;
        this.repositoryMembriTeam = repositoryMembriTeam;
        this.servizioNotifiche = servizioNotifiche;
        this.repositoryIscrizioniTeam = repositoryIscrizioniTeam;
    }

    /**
     * Metodo per cambiare nome ad un team
     *
     * @param nomeUtente il leader che vuole cambiare nome
     * @param nome       il nuovo nome del team
     */
    public void cambiaNomeTeam(String nomeUtente, String nome) {
        MembroTeam leader = validazioneLeader(nomeUtente);
        Team team = leader.getTeam();
        if (repositoryTeam.existsByNome(nome)) {
            throw new ConflictException("Esiste già un team con questo nome");
        }
        team.setNome(nome);
        repositoryTeam.save(team);
        for (MembroTeam membro : team.getMembri()) {
            servizioNotifiche.creaNotifica(membro.getUtente(), CAMBIO_NOME_TEAM, "Il team ha cambiato nome in " + nome + ".");
        }
    }

    private MembroTeam validazioneLeader(String nomeUtente){
        MembroTeam leader = repositoryMembriTeam.findByUtente_NomeUtente(nomeUtente).orElseThrow(() -> new NotFoundException("L'utente non è membro di nessun team"));
        if (leader.getRuolo() != RuoloTeam.LEADER) {
            throw new ConflictException("L'utente non è il leader del team");
        }
        return leader;
    }

    /**
     * Metodo per uscire da un team
     *
     * @param nomeUtente l'id del membro che vuole uscire
     */
    public void esciDalTeam(String nomeUtente) {
        MembroTeam membroTeam = repositoryMembriTeam.findByUtente_NomeUtente(nomeUtente).orElseThrow(() -> new NotFoundException("L'utente non è membro di nessun team"));
        Team team = repositoryTeam.findByNome(membroTeam.getTeam().getNome()).orElseThrow(() -> new NotFoundException("Il team non esiste"));
        if (!membroTeam.getTeam().equals(team)) {
            throw new ConflictException("L'utente non è membro di questo team");
        }
        validaIscrizione(team);
        if (membroTeam.getRuolo() == RuoloTeam.LEADER) {
            if (team.getNumMembri() != 1)
                throw new ConflictException("Prima di uscire dal team è necessario nominare un nuovo leader");
        }
        team.rimuoviMembro(membroTeam);
        repositoryMembriTeam.delete(membroTeam);
        repositoryTeam.save(team);
        for (MembroTeam m : team.getMembri()) {
            servizioNotifiche.creaNotifica(m.getUtente(), USCITA, "Il membro " + membroTeam.getUtente().getNomeUtente() + " è uscito dal team.");
        }
    }

    /**
     * Metodo per sciogliere un team
     *
     * @param nomeUtente il nome dell'utente che vuole sciogliere il team
     */
    public void sciogliTeam(String nomeUtente) {
        MembroTeam membroTeam = validazioneLeader(nomeUtente);
        Team team = membroTeam.getTeam();
        List<IscrizioneTeam> iscrizioni = repositoryIscrizioniTeam.findAllByTeam(team);
        if (!iscrizioni.isEmpty()) {
            for (IscrizioneTeam iscrizione : iscrizioni) {
                Hackathon hackathon = iscrizione.getHackathon();
                try {
                    hackathon.getStato().verificaAnnullamentoIscrizioneConsentito(hackathon);
                } catch (TransizioneNonConsentitaException e) {
                    throw new ConflictException("Il team è iscritto ad un hackathon con le iscrizioni chiuse, non puoi sciogliere il team");
                }
            }
            repositoryIscrizioniTeam.deleteAll(iscrizioni);
        }
        for (MembroTeam m : team.getMembri()) {
            servizioNotifiche.creaNotifica(m.getUtente(), SCIOGLIMENTO_TEAM, "Il team " + team.getNome() + " è stato sciolto.");
        }
        repositoryTeam.delete(team);
    }

    /**
     * Metodo per espellere un membro da un team
     *
     * @param nomeUtente il nome dell'utente che vuole espellere il membro
     * @param nomeMembro il nome del membro da espellere
     */
    public void espelliMembro(String nomeUtente, String nomeMembro) {
        MembroTeam leader = validazioneLeader(nomeUtente);
        MembroTeam membroDaEspellere = repositoryMembriTeam.findByUtente_NomeUtente(nomeMembro).orElseThrow(() -> new NotFoundException("Il membro da espellere non esiste"));
        if (!membroDaEspellere.getTeam().equals(leader.getTeam())) {
            throw new ConflictException("Il membro da espellere non è nel team del leader");
        }
        Team team = leader.getTeam();
        validaIscrizione(team);
        if (membroDaEspellere.getIdMembroTeam().equals(leader.getIdMembroTeam())) {
            throw new ConflictException("Il leader non può espellere se stesso");
        }
        team.getMembri().remove(membroDaEspellere);
        repositoryMembriTeam.delete(membroDaEspellere);
        repositoryTeam.save(team);
        for (MembroTeam m : team.getMembri()) {
            servizioNotifiche.creaNotifica(m.getUtente(), ESPULSIONE_TEAM, "Il membro " + membroDaEspellere.getUtente().getNomeUtente() + " è stato espulso dal team.");
        }
    }

    private void validaIscrizione(Team team){
        if (repositoryIscrizioniTeam.findByTeam(team).isPresent()) {
            throw new NotFoundException("Il team è iscritto ad un'hackathon, non puoi espellere un membro");
        }
    }

    /**
     * Metodo per trasferire il ruolo di leader ad un altro membro del team
     * @param nomeUtente il nome del leader
     * @param nomeMembro il nome membro a cui trasferire il ruolo
     */
    public void trasferisceRuoloLeader(String nomeUtente, String nomeMembro) {
        MembroTeam leader = validazioneLeader(nomeUtente);
        MembroTeam membroTeam = repositoryMembriTeam.findByUtente_NomeUtente(nomeMembro).orElseThrow(() -> new NotFoundException("Il membro da nominare non esiste"));
        if (membroTeam.getRuolo() == RuoloTeam.LEADER) {
            throw new ConflictException("Il membro da nominare è già il leader del team");
        }
        if (!membroTeam.getTeam().equals(leader.getTeam())) {
            throw new ConflictException("Il membro da nominare non è nel team del leader");
        }
        membroTeam.setRuolo(RuoloTeam.LEADER);
        leader.setRuolo(RuoloTeam.MEMBRO);
        repositoryTeam.save(leader.getTeam());
         for (MembroTeam m : leader.getTeam().getMembri()) {
            servizioNotifiche.creaNotifica(m.getUtente(), TRASFERIMENTO_LEADER, "Il membro " + membroTeam.getUtente().getNomeUtente() + " è stato nominato come nuovo leader del team.");
        }
    }

    @Transactional
    public TeamResponse visualizzaTeam(String nomeUtente) {
        MembroTeam membroTeam = repositoryMembriTeam.findByUtente_NomeUtente(nomeUtente).orElseThrow(() -> new NotFoundException("L'utente non è membro di nessun team"));
        Team team = membroTeam.getTeam();
        if (!team.getMembri().contains(membroTeam)) {
            throw new ConflictException("L'utente non è membro di questo team");
        }
        return new TeamResponse(
                team.getNome(),
                team.getMembri().stream().filter(m -> m.getRuolo() == RuoloTeam.LEADER).map(m -> m.getUtente().getNomeUtente()).findFirst().orElse(null),
                team.getMembri().stream().map(m -> m.getUtente().getNomeUtente()).collect(Collectors.toList())
        );
    }

    @Transactional
    public List<String> visualizzaIscrizioniTeam(String nomeUtente) {
        MembroTeam membroTeam = repositoryMembriTeam.findByUtente_NomeUtente(nomeUtente).orElseThrow(() -> new NotFoundException("L'utente non è membro di nessun team"));
        Team team = membroTeam.getTeam();
        List<IscrizioneTeam> iscrizioni = repositoryIscrizioniTeam.findAllByTeam(team);
        return iscrizioni.stream().map(iscrizioneTeam -> iscrizioneTeam.getHackathon().getIdHackathon()).collect(Collectors.toList());
    }
}