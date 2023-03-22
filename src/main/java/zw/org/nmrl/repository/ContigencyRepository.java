package zw.org.nmrl.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.org.nmrl.domain.Contigency;
import zw.org.nmrl.domain.Laboratory;
import zw.org.nmrl.domain.LaboratoryRequest;
import zw.org.nmrl.domain.Sample;
import zw.org.nmrl.service.dto.LaboratoryDTO;

/**
 * Spring Data JPA repository for the {@link Sample} entity.
 */
@Repository
public interface ContigencyRepository extends JpaRepository<Contigency, String> {
    List<Contigency> findBySentToLimsIsNull();
}
