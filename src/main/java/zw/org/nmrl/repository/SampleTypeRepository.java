package zw.org.nmrl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.org.nmrl.domain.Client;
import zw.org.nmrl.domain.Sample;
import zw.org.nmrl.domain.User;

/**
 * Spring Data JPA repository for the {@link Sample} entity.
 */
@Repository
public interface SampleTypeRepository extends JpaRepository<Sample, String> {}
