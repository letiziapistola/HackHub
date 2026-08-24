package unicam.cs.hackhub.boundary.dto;

import unicam.cs.hackhub.domain.implementazione.MembroTeam;

import java.util.List;

public record TeamResponse(
        String nomeTeam,
        String nomeLeader,
        List<String> nomiMembri
) {
}
