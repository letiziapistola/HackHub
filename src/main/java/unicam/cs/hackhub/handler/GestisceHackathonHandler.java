package unicam.cs.hackhub.handler;

import unicam.cs.hackhub.domain.RuoloStaff;
import unicam.cs.hackhub.domain.implementazione.*;
import unicam.cs.hackhub.eccezioni.BadRequestException;
import unicam.cs.hackhub.eccezioni.ConflictException;
import unicam.cs.hackhub.eccezioni.NotFoundException;
import unicam.cs.hackhub.eccezioni.TransizioneNonConsentitaException;
import unicam.cs.hackhub.repository.*;
import unicam.cs.hackhub.servizi.ServizioNotifiche;
import unicam.cs.hackhub.servizi.esterni.SistemaDiPagamentoMock;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

import static unicam.cs.hackhub.domain.TipoNotifica.*;

@Service
public class GestisceHackathonHandler {
    private final ServizioNotifiche servizioNotifiche;
    private final RepositoryStaff repositoryStaff;
    private final RepositoryTeam repositoryTeam;
    private final RepositoryUtente repositoryUtente;
    private final RepositoryHackathon repositoryHackathon;
    private final RepositoryIscrizioniTeam repositoryIscrizioniTeam;
    private final SistemaDiPagamentoMock sistemaDiPagamento;
    private final RepositoryInvitoStaff repositoryInvitoStaff;

    public GestisceHackathonHandler(ServizioNotifiche servizioNotifiche, RepositoryStaff repositoryStaff, RepositoryTeam repositoryTeam, RepositoryUtente repositoryUtente, RepositoryHackathon repositoryHackathon, RepositoryIscrizioniTeam repositoryIscrizioniTeam, SistemaDiPagamentoMock sistemaDiPagamento, RepositoryInvitoStaff repositoryInvitoStaff) {
        this.servizioNotifiche = servizioNotifiche;
        this.repositoryStaff = repositoryStaff;
        this.repositoryTeam = repositoryTeam;
        this.repositoryUtente = repositoryUtente;
        this.repositoryHackathon = repositoryHackathon;
        this.repositoryIscrizioniTeam = repositoryIscrizioniTeam;
        this.sistemaDiPagamento = sistemaDiPagamento;
        this.repositoryInvitoStaff = repositoryInvitoStaff;
    }

    @Transactional
    public void segnalaViolazione(String nomeMentore, String nomeTeam, String nomeHackathon) {
        Hackathon hackathon = repositoryHackathon.findByNome(nomeHackathon).orElseThrow(() -> new NotFoundException("Hackathon non trovato"));
        validaAutorizzazione(nomeMentore, RuoloStaff.MENTORE, hackathon);
        Team team = repositoryTeam.findByNome(nomeTeam).orElseThrow(() -> new NotFoundException("Team non trovato"));
        repositoryIscrizioniTeam.findByTeamAndHackathon(team, hackathon).orElseThrow(() -> new NotFoundException("Il team non è iscritto a questo hackathon"));
        try {
            hackathon.getStato().verificaEspulsioneTeamConsentita(hackathon);
        } catch (TransizioneNonConsentitaException ex) {
            throw new ConflictException("Non è possibile segnalare una violazione se l'hackathon non è in corso");
        }
        Staff organizzatore = hackathon.getStaff().stream().filter(staff -> staff.getRuolo() == RuoloStaff.ORGANIZZATORE).findFirst().orElseThrow(() -> new NotFoundException("Organizzatore dell'hackathon non trovato"));
        servizioNotifiche.creaNotifica(organizzatore.getUtente(), VIOLAZIONE_REGOLAMENTO, "Il team " + team.getNome() + " ha violato il regolamento dell'hackathon");
    }

    @Transactional
    public void nominaMentori(String nomeUtente, String nomeUtenteDaInvitare, String nomeHackathon) {
        Hackathon hackathon = repositoryHackathon.findByNome(nomeHackathon).orElseThrow(() -> new NotFoundException("Hackathon non trovato"));
        validaAutorizzazione(nomeUtente, RuoloStaff.ORGANIZZATORE, hackathon);
        Utente staffDaInvitare = repositoryUtente.findByNomeUtente(nomeUtenteDaInvitare).orElseThrow(() -> new NotFoundException("Utente da invitare non trovato"));
        try {
            hackathon.getStato().verificaNominaMentoriConsentita(hackathon);
        } catch (TransizioneNonConsentitaException ex) {
            throw new ConflictException("Non è possibile nominare mentori al di fuori della fase 'iscrizioni aperte'");
        }
        boolean giaStaff = hackathon.getStaff().stream().anyMatch(staff -> staff.getUtente().getNomeUtente().equals(staffDaInvitare.getNomeUtente()));
        if (giaStaff) {
            throw new BadRequestException("L'utente da invitare è già nello staff");
        }
        servizioNotifiche.creaInvitoStaff(nomeUtente, staffDaInvitare, hackathon, RuoloStaff.MENTORE);
    }

    @Transactional
    public void eliminaHackathon(String nomeUtente, String nomeHackathon) {
        Hackathon hackathon = repositoryHackathon.findByNome(nomeHackathon).orElseThrow(() -> new NotFoundException("Hackathon non trovato"));
        validaAutorizzazione(nomeUtente, RuoloStaff.ORGANIZZATORE, hackathon);
        try {
            hackathon.getStato().verificaEliminazioneConsentita(hackathon);
        } catch (TransizioneNonConsentitaException ex) {
            throw new ConflictException("Non è possibile eliminare un hackathon in corso o concluso");
        }
        List<Team> teams = repositoryIscrizioniTeam.findAllByHackathon(hackathon).stream().map(IscrizioneTeam::getTeam).toList();
        for (Team team : teams) {
            for (MembroTeam membro : team.getMembri()) {
                servizioNotifiche.creaNotifica(membro.getUtente(), HACKATHON_CANCELLATO, "L'hackathon a cui eri iscritto è stato cancellato");
            }
        }
        repositoryInvitoStaff.deleteAllByHackathon(hackathon);
        repositoryHackathon.delete(hackathon);
    }

    @Transactional
    public void espelliTeam(String nomeUtente, String nomeHackathon, String nomeTeam) {
        Hackathon hackathon = repositoryHackathon.findByNome(nomeHackathon).orElseThrow(() -> new NotFoundException("Hackathon non trovato"));
        validaAutorizzazione(nomeUtente, RuoloStaff.ORGANIZZATORE, hackathon);
        Team team = repositoryTeam.findByNome(nomeTeam).orElseThrow(() -> new NotFoundException("Team non trovato"));
        repositoryIscrizioniTeam.findByTeamAndHackathon(team, hackathon).orElseThrow(() -> new NotFoundException("Iscrizione del team all'hackathon non trovata"));
        try {
            hackathon.getStato().verificaEspulsioneTeamConsentita(hackathon);
        } catch (TransizioneNonConsentitaException ex) {
            throw new ConflictException("Non è possibile espellere un team da un hackathon non ancora in corso");
        }
        hackathon.rimuoviIscrizione(team);
        repositoryHackathon.save(hackathon);
        for (MembroTeam membro : team.getMembri()) {
            servizioNotifiche.creaNotifica(membro.getUtente(), ESPULSIONE_TEAM, "Il tuo team è stato espulso dall'hackathon " + hackathon.getNome());
        }
    }

    @Transactional
    public void proclamaVincitore(String nomeUtente, String nomeHackathon, String nomeTeam) {
        Hackathon hackathon = repositoryHackathon.findByNome(nomeHackathon).orElseThrow(() -> new NotFoundException("Hackathon non trovato"));
        validaAutorizzazione(nomeUtente, RuoloStaff.ORGANIZZATORE, hackathon);
        try {
            hackathon.getStato().verificaProclamazioneConsentita(hackathon);
        } catch (TransizioneNonConsentitaException ex) {
            throw new ConflictException("Hackathon non concluso, impossibile proclamare il vincitore");
        }
        Team team = repositoryTeam.findByNome(nomeTeam).orElseThrow(() -> new NotFoundException("Team non trovato"));
        repositoryIscrizioniTeam.findByTeamAndHackathon(team, hackathon).orElseThrow(() -> new NotFoundException("Il team non è iscritto all'hackathon"));
        for (MembroTeam membro : team.getMembri()) {
            servizioNotifiche.creaNotifica(membro.getUtente(), VITTORIA, "Il tuo team ha vinto l'hackathon");
        }
        List<Team> teams = repositoryIscrizioniTeam.findAllByHackathon(hackathon).stream().map(IscrizioneTeam::getTeam).toList();
        for (Team altroTeam : teams) {
            if (!altroTeam.equals(team)) {
                for (MembroTeam membro : altroTeam.getMembri()) {
                    servizioNotifiche.creaNotifica(membro.getUtente(), SCONFITTA, "Il tuo team non ha vinto l'hackathon");
                }
            }
        }
    }

    @Transactional
    public void attivaLiquidazionePremio(String nomeUtente, String nomeHackathon, String nomeTeam) {
        Hackathon hackathon = repositoryHackathon.findByNome(nomeHackathon).orElseThrow(() -> new NotFoundException("Hackathon non trovato"));
        Staff organizzatore = validaAutorizzazione(nomeUtente, RuoloStaff.ORGANIZZATORE, hackathon);
        Team team = repositoryTeam.findByNome(nomeTeam).orElseThrow(() -> new NotFoundException("Team non trovato"));
        repositoryIscrizioniTeam.findByTeamAndHackathon(team, hackathon).orElseThrow(() -> new NotFoundException("Il team non è iscritto all'hackathon"));
        try {
            hackathon.getStato().verificaLiquidazionePremioConsentita(hackathon);
        } catch (TransizioneNonConsentitaException ex) {
            throw new ConflictException("Hackathon non concluso, impossibile liquidare il premio");
        }
        for (MembroTeam membro : team.getMembri()) {
            sistemaDiPagamento.pagaPremio(organizzatore.getUtente().getRecapitoBancario(), membro.getUtente().getRecapitoBancario(), hackathon.getPremio());
        }
    }

    private Staff validaAutorizzazione(String nomeUtente, RuoloStaff ruoloStaff, Hackathon hackathon) {
        return repositoryStaff.findByUtente_NomeUtenteAndHackathonAndRuolo(nomeUtente, hackathon, ruoloStaff).orElseThrow(() -> new NotFoundException("L'utente non appartiene allo staff dell'hackathon con il ruolo richiesto"));
    }
}