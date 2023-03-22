package zw.org.nmrl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.org.nmrl.domain.Test;
import zw.org.nmrl.domain.User;

/**
 * Spring Data JPA repository for the {@link Test} entity.
 */
@Repository
public interface TestTypeRepository extends JpaRepository<Test, String> {}
