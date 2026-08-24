package unicam.cs.hackhub.boundary.dto;

import unicam.cs.hackhub.domain.implementazione.Hackathon;
import unicam.cs.hackhub.domain.implementazione.MembroTeam;

import java.util.List;

public record TeamResponse(
        String nomeTeam,
        MembroTeam leader,
        List<MembroTeam> membriTeam
) {
}
