package unicam.cs.hackhub.repository;

import unicam.cs.hackhub.domain.RuoloStaff;
import unicam.cs.hackhub.domain.implementazione.Hackathon;
import unicam.cs.hackhub.domain.implementazione.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositoryStaff extends JpaRepository<Staff, String> {

    /**
     * Trova un membro dello staff dal suo nome
     *
     * @param nomeUtente il nome dell'utente
     * @return il membro dello staff se esiste, altrimenti un optional vuoto
     */
    Optional<Staff> findByUtente_NomeUtente(String nomeUtente);

    /**
     * Trova un membro dello staff dal suo nome
     *
     * @param nomeUtente il nome dell'utente
     * @return il membro dello staff se esiste, altrimenti un optional vuoto
     */
    Optional<Staff> getStaffByUtente_NomeUtente(String nomeUtente);

    Optional<Staff> findByUtente_NomeUtenteAndHackathonAndRuolo(String nomeUtente, Hackathon hackathon, RuoloStaff ruolo);
}
