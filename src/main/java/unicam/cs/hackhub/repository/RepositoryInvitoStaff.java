package unicam.cs.hackhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unicam.cs.hackhub.domain.implementazione.Hackathon;
import unicam.cs.hackhub.domain.implementazione.InvitoStaff;

public interface RepositoryInvitoStaff extends JpaRepository<InvitoStaff, String> {
    void deleteAllByHackathon(Hackathon hackathon);
}
