package unicam.cs.hackhub.servizi;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import unicam.cs.hackhub.boundary.GestisciTeamBoundary;
import unicam.cs.hackhub.boundary.VisualizzaBoundary;
import unicam.cs.hackhub.domain.implementazione.Utente;
import unicam.cs.hackhub.eccezioni.ForbiddenException;
import unicam.cs.hackhub.handler.ApiExceptionHandler;
import unicam.cs.hackhub.handler.GestisciTeamHandler;
import unicam.cs.hackhub.handler.VisualizzaHandler;
import unicam.cs.hackhub.repository.RepositoryUtente;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {GestisciTeamBoundary.class, VisualizzaBoundary.class},
        properties = {
                "app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
                "app.jwt.expiration-ms=3600000"
        }
)
@Import({SecurityConfig.class, JwtFiltro.class, ServizioJwt.class, ApiExceptionHandler.class,
        SecurityJwtTest.SecurityTestConfig.class})
class SecurityJwtTest {

    private static final String SECRET =
            "0123456789012345678901234567890123456789012345678901234567890123";
    private static final String NOME_UTENTE = "utente_test";
    private static final String ENDPOINT_PROTETTO = "/api/team/mio";

    private MockMvc mockMvc;

    @Autowired
    private ServizioJwt servizioJwt;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private RepositoryUtente repositoryUtente;

    @MockitoBean
    private GestisciTeamHandler gestisciTeamHandler;

    @MockitoBean
    private VisualizzaHandler visualizzaHandler;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void richiestaProtettaConTokenValidoUsaUsernameComePrincipal() throws Exception {
        Utente utente = utente();
        String token = servizioJwt.generaToken(utente);
        when(repositoryUtente.findByNomeUtente(NOME_UTENTE)).thenReturn(Optional.of(utente));

        mockMvc.perform(get(ENDPOINT_PROTETTO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        verify(gestisciTeamHandler).visualizzaTeam(NOME_UTENTE);
    }

    @Test
    void richiestaProtettaSenzaTokenRestituisceUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT_PROTETTO))
                .andExpect(status().isUnauthorized());

        verify(gestisciTeamHandler, never()).visualizzaTeam(NOME_UTENTE);
    }

    @Test
    void richiestaProtettaConTokenScadutoRestituisceUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT_PROTETTO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenScaduto()))
                .andExpect(status().isUnauthorized());

        verify(gestisciTeamHandler, never()).visualizzaTeam(NOME_UTENTE);
    }

    @Test
    void richiestaProtettaConTokenInvalidoRestituisceUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT_PROTETTO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-non-valido"))
                .andExpect(status().isUnauthorized());

        verify(gestisciTeamHandler, never()).visualizzaTeam(NOME_UTENTE);
    }

    @Test
    void utenteAutenticatoMaNonAutorizzatoRestituisceForbidden() throws Exception {
        Utente utente = utente();
        String token = servizioJwt.generaToken(utente);
        when(repositoryUtente.findByNomeUtente(NOME_UTENTE)).thenReturn(Optional.of(utente));
        when(gestisciTeamHandler.visualizzaTeam(NOME_UTENTE))
                .thenThrow(new ForbiddenException("Utente non autorizzato"));

        mockMvc.perform(get(ENDPOINT_PROTETTO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void endpointPubblicoRestaAccessibileSenzaToken() throws Exception {
        when(visualizzaHandler.viewInfoHackathon()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/hackathon"))
                .andExpect(status().isOk());
    }

    private Utente utente() {
        return new Utente(NOME_UTENTE, "utente@example.com", "password-hash");
    }

    private String tokenScaduto() {
        Date now = new Date();
        return Jwts.builder()
                .subject(NOME_UTENTE)
                .issuedAt(new Date(now.getTime() - 2_000))
                .expiration(new Date(now.getTime() - 1_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @TestConfiguration
    @EnableWebSecurity
    static class SecurityTestConfig {
    }
}
