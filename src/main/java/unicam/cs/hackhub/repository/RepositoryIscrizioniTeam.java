package unicam.cs.hackhub.repository;

import unicam.cs.hackhub.domain.implementazione.Hackathon;
import unicam.cs.hackhub.domain.implementazione.IscrizioneTeam;
import unicam.cs.hackhub.domain.implementazione.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositoryIscrizioniTeam extends JpaRepository<IscrizioneTeam, String> {

    /**
     * Trova un iscrizione dal team e dall'hackathon
     *
     * @param team      il team
     * @param hackathon l'hackathon
     * @return l'iscrizione, un optional vuoto se non esiste
     */
    Optional<IscrizioneTeam> findByTeamAndHackathon(Team team, Hackathon hackathon);

    /**
     * Trova tutte le iscrizioni relative ad un hackathon
     *
     * @param hackathon l'hackathon
     * @return la lista delle iscrizioni, se non esiste nessuna iscrizione ritorna una lista vuota
     */
    List<IscrizioneTeam> findAllByHackathon(Hackathon hackathon);

    /**
     * Trova un iscrizione dal team associato
     *
     * @param team il team
     * @return l'iscrizione, un optional vuoto se non esiste
     */
    Optional<IscrizioneTeam> findByTeam(Team team);

    List<IscrizioneTeam> findAllByTeam(Team team);

    boolean existsByHackathonAndTeam(Hackathon hackathon, Team team);
}
