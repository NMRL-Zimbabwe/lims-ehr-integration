package zw.org.nmrl.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.org.nmrl.domain.PatientIdentifier;
import zw.org.nmrl.domain.PatientPhone;

/**
 * Spring Data JPA repository for the {@link PatientIdentifier} entity.
 */
@Repository
public interface PatientIdentifierRepository extends JpaRepository<PatientIdentifier, String> {
    PatientIdentifier findByNumber(String number);

    Collection<PatientIdentifier> findByPatientId(String patientId);
}
