package zw.org.nmrl.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.org.nmrl.domain.AnalysisProfile;
import zw.org.nmrl.domain.AnalysisRequest;
import zw.org.nmrl.domain.User;

/**
 * Spring Data JPA repository for the {@link AnalysisProfile} entity.
 */
@Repository
public interface AnalysisProfileRepository extends JpaRepository<AnalysisProfile, String> {}
