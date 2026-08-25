package unicam.cs.hackhub.servizi;

import unicam.cs.hackhub.domain.implementazione.Utente;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Pattern: Singleton, gestione dei token Jwt
 */
@Service
public class ServizioJwt {

    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Metodo helper per prelevare la chiave
     *
     * @return la chiave segreta per firmare i token Jwt, ottenuta dalla stringa jwtSecret
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Metodo che genera un token Jwt per un Utente che tenta di accedere alla piattaforma
     *
     * @param utente l'oggetto Utente che prova ad autenticarsi alla piattaforma
     * @return un token Jwt
     */
    public String generaToken(Utente utente) {
        if (utente == null) throw new IllegalArgumentException("L'utente passato è nullo");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(utente.getNomeUtente())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Metodo per estrarre il nomeUtente da un token Jwt
     *
     * @param token il token Jwt da cui si deve effettuare l'estrazione
     * @return nomeUtente
     */
    public String estraiNomeUtente(String token) {
        if (token == null) throw new IllegalArgumentException("Il token è nullo");

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Verifica che un token Jwt sia valido per un certo Utente
     *
     * @param token  il token Jwt da validare
     * @param utente l'utente che prova l'autenticazione alla piattaforma
     * @throws JwtException se il token non è valido, è scaduto o non appartiene all'utente
     * @throws IllegalArgumentException se il token o l'utente sono nulli
     */
    public void validaToken(String token, Utente utente) {
        if (token == null || utente == null)
            throw new IllegalArgumentException("Token o utente passati nulli");

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!utente.getNomeUtente().equals(claims.getSubject()))
            throw new JwtException("Token non corrispondente all'utente");
    }
}
