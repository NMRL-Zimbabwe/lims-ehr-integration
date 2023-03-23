package zw.org.nmrl.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import zw.org.nmrl.domain.Client;

/**
 * Service Interface for managing {@link Client}.
 */
public interface ClientService {
    Client save(Client client);

    Page<Client> findAll(Pageable pageable);
}
