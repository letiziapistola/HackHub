package unicam.cs.hackhub.repository;

import unicam.cs.hackhub.domain.implementazione.Hackathon;
import unicam.cs.hackhub.domain.implementazione.Richiesta;
import unicam.cs.hackhub.domain.implementazione.Utente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepositoryRichiesta  extends JpaRepository<Richiesta, String> {

    /**
     * Trova le richieste con un certo destinatario
     * @param destinatario il destinatario
     * @return la lista delle richieste, se non è presente nessuna richiesta con quel destinatario ritorna
     * una lista vuota
     */
    List<Richiesta> findAllByDestinatario(Utente destinatario);
}
