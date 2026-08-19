package unicam.cs.hackhub.handler;

import unicam.cs.hackhub.boundary.dto.AuthResponse;
import unicam.cs.hackhub.boundary.dto.LoginRequest;
import unicam.cs.hackhub.boundary.dto.RegisterRequest;
import unicam.cs.hackhub.domain.implementazione.Utente;
import unicam.cs.hackhub.eccezioni.BadRequestException;
import unicam.cs.hackhub.repository.RepositoryUtente;
import unicam.cs.hackhub.servizi.ServizioJwt;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EffettuaAutenticazioneHandler {

    private final RepositoryUtente repositoryUtente;
    private final PasswordEncoder passwordEncoder;
    private final ServizioJwt servizioJwt;

    /**
     * Costruttore che inizializza questo handler
     *
     * @param repositoryUtente la repository degli utento
     * @param passwordEncoder  l'encoder della password
     * @param servizioJwt      il servizio jwt
     */
    public EffettuaAutenticazioneHandler(RepositoryUtente repositoryUtente, PasswordEncoder passwordEncoder, ServizioJwt servizioJwt) {
        this.repositoryUtente = repositoryUtente;
        this.passwordEncoder = passwordEncoder;
        this.servizioJwt = servizioJwt;
    }

    /**
     * Metodo che attiva la procedura di registrazione di un Visitatore alla piattaforma, per poter accedere
     * e partecipare agli hackathon
     *
     * @param request il dto di richiesta di registrazione
     */
    @Transactional
    public void attivaRegistrazione(RegisterRequest request) {
        repositoryUtente.findByNomeUtente(request.nomeUtente()).ifPresent(u -> {
            throw new BadRequestException("Esiste già un utente con questo nome");
        });
        Utente utente = new Utente(
                request.nomeUtente(),
                request.email(),
                passwordEncoder.encode(request.password()));
        repositoryUtente.save(utente);
    }

    /**
     * Metodo che attiva l'autenticazione di un utente tramite una richiesta di login
     *
     * @param request la richiesta
     * @return una nuova authResponse
     */
    @Transactional
    public AuthResponse attivaAutenticazione(LoginRequest request) {
        Utente utente = repositoryUtente.findByNomeUtente(request.nomeUtente())
                .orElseThrow(() -> new BadRequestException("Nome utente errato"));
        if (!passwordEncoder.matches(request.password(), utente.getPasswordHash()))
            throw new BadRequestException("Password errata");
        String token = servizioJwt.generaToken(utente);
        return new AuthResponse(token, "Bearer", utente.getNomeUtente());
    }
}
