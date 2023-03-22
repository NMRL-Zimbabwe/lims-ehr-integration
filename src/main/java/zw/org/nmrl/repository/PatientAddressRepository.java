package zw.org.nmrl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.org.nmrl.domain.PatientAddress;

/**
 * Spring Data JPA repository for the {@link PatientAddress} entity.
 */
@Repository
public interface PatientAddressRepository extends JpaRepository<PatientAddress, String> {
    PatientAddress findByPatientId(String patientId);
}
