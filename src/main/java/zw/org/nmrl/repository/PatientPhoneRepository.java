package zw.org.nmrl.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.org.nmrl.domain.PatientPhone;

/**
 * Spring Data JPA repository for the {@link PatientPhone} entity.
 */
@Repository
public interface PatientPhoneRepository extends JpaRepository<PatientPhone, String> {
    PatientPhone findByNumber(String number);

    PatientPhone findFirstByPatientIdOrderByLastModifiedDateDesc(String patientId);
}
