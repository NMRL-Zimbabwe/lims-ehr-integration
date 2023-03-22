package zw.org.nmrl.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;
import zw.org.nmrl.config.Constants;
import zw.org.nmrl.domain.AnalysisRequest;
import zw.org.nmrl.domain.User;
import zw.org.nmrl.security.AuthoritiesConstants;
import zw.org.nmrl.service.AnalysisRequestService;
import zw.org.nmrl.service.dto.AnalysisRequestDTO;
import zw.org.nmrl.service.dto.MiddlewareAnalysisRequestDTO;
import zw.org.nmrl.service.dto.UserDTO;
import zw.org.nmrl.web.rest.errors.BadRequestAlertException;
import zw.org.nmrl.web.rest.errors.EmailAlreadyUsedException;
import zw.org.nmrl.web.rest.errors.LoginAlreadyUsedException;

/**
 * REST controller for managing users.
 * <p>
 * This class accesses the {@link User} entity, and needs to fetch its collection of authorities.
 * <p>
 * For a normal use-case, it would be better to have an eager relationship between User and Authority,
 * and send everything to the client side: there would be no View Model and DTO, a lot less code, and an outer-join
 * which would be good for performance.
 * <p>
 * We use a View Model and a DTO for 3 reasons:
 * <ul>
 * <li>We want to keep a lazy association between the user and the authorities, because people will
 * quite often do relationships with the user, and we don't want them to get the authorities all
 * the time for nothing (for performance reasons). This is the #1 goal: we should not impact our users'
 * application because of this use-case.</li>
 * <li> Not having an outer join causes n+1 requests to the database. This is not a real issue as
 * we have by default a second-level cache. This means on the first HTTP call we do the n+1 requests,
 * but then all authorities come from the cache, so in fact it's much better than doing an outer join
 * (which will get lots of data from the database, for each HTTP call).</li>
 * <li> As this manages users, for security reasons, we'd rather have a DTO layer.</li>
 * </ul>
 * <p>
 * Another option would be to have a specific JPA entity graph to handle this case.
 */
@RestController
@RequestMapping("/api")
public class AnalysisRequestResource {

    private final Logger log = LoggerFactory.getLogger(AnalysisRequestResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AnalysisRequestService analysisRequestService;

    public AnalysisRequestResource(AnalysisRequestService analysisRequestService) {
        this.analysisRequestService = analysisRequestService;
    }

    /**
     * {@code POST  /users}  : Creates a new user.
     * <p>
     * Creates a new user if the login and email are not already used, and sends an
     * mail with an activation link.
     * The user needs to be activated on creation.
     *
     * @param userDTO the user to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new user, or with status {@code 400 (Bad Request)} if the login or email is already in use.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     * @throws BadRequestAlertException {@code 400 (Bad Request)} if the login or email is already in use.
     */
    @PostMapping("/analysis-request")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<AnalysisRequest> createUser(@Valid @RequestBody MiddlewareAnalysisRequestDTO analysisRequestDTO)
        throws URISyntaxException {
        log.debug("REST request to save User : {}", analysisRequestDTO);

        if (analysisRequestDTO.getNotificationId() != null) {
            throw new BadRequestAlertException("A new user cannot already have an ID", "userManagement", "idexists");
            // Lowercase the user login before comparing with database

        } else {
            AnalysisRequest request = analysisRequestService.createAnalysisRequest(analysisRequestDTO);
            return ResponseEntity
                .created(new URI("/api/analysis-request/" + request))
                .headers(HeaderUtil.createAlert(applicationName, "userManagement.created", request.getAnalysisRequestId()))
                .body(request);
        }
    }
}
