package zw.org.nmrl.service.patient.resolver;

/**
 * Resolver class responsible for converting integration AnalysisRequest received from
 * RabbitMQ. The class extracts patient details form graphql and saves the record in the database.
 * If the patient exist it is updated
 *
 * @author Lawrence Chirowodza
 * @date 2020
 */

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.transaction.NotSupportedException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import zw.org.nmrl.domain.AnalysisRequest;
import zw.org.nmrl.domain.Patient;
import zw.org.nmrl.repository.AnalysisRequestRepository;
import zw.org.nmrl.repository.PatientRepository;
import zw.org.nmrl.service.AnalysisRequestService;
import zw.org.nmrl.service.PatientService;
import zw.org.nmrl.service.dto.AuthenticationResponseOne;
import zw.org.nmrl.service.dto.patient.resolver.PatientMainResponseResolver;
import zw.org.nmrl.service.dto.patient.resolver.PatientResolverDTO;
import zw.org.nmrl.service.laboratoryRequest.resolver.LaboratoryRequestMiddlewareResolver;

@Service
public class PatientMiddlewareResolver {

    private static final Logger log = LoggerFactory.getLogger(PatientMiddlewareResolver.class);

    @Autowired
    PatientService patientService;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    private AnalysisRequestService analysisRequestService;

    @Autowired
    AnalysisRequestRepository analysisRequestRepository;

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    LaboratoryRequestMiddlewareResolver laboratoryRequestMiddlewareResolver;

    private AuthenticationResponseOne token;

    @Value("${myConfig.myBaseGateway}")
    private String gateway;

    @Autowired
    RestTemplateBuilder builder;

    @Value("1000")
    private int connectTimeout;

    @Value("1000")
    private int readTimeout;

    @Value("true")
    private boolean connectionEnabled;

    @Value("budirro")
    private String client;

    @Value("")
    private String exchange;

    public PatientMiddlewareResolver() {
        super();
    }

    @Data
    @AllArgsConstructor
    class Credentials {

        private String username;
        private String password;
        private boolean rememberMe;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isRememberMe() {
            return rememberMe;
        }

        public void setRememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
        }

        public Credentials(String username, String password, boolean rememberMe) {
            super();
            this.username = username;
            this.password = password;
            this.rememberMe = rememberMe;
        }
    }

    @PostConstruct
    private void connect() {
        try {
            log.info("Connecting to central repository {}", gateway);

            authenticate();

            log.info("Connected to central repository {}", gateway);
        } catch (Exception e) {
            log.warn("Could not retrieve access token", e.getMessage());
        }
    }

    private RestTemplate rest() {
        // TODO add connection timeout here
        // return
        // builder.setConnectTimeout(Duration.ofMillis(connectTimeout)).setReadTimeout(Duration.ofMillis(connectTimeout)).build();
        return builder.build();
    }

    private void authenticate() throws NotSupportedException {
        if (!connectionEnabled) {
            throw new NotSupportedException("Service disabled");
        }

        Credentials credentials = new Credentials("user", "user", true);

        String url = String.join("/", gateway, "auth", "login");

        HttpHeaders headers = csrfHeaders();

        HttpEntity<Credentials> data = new HttpEntity<>(credentials, headers);

        token = rest().exchange(url, HttpMethod.POST, data, AuthenticationResponseOne.class).getBody();

        log.info("Authentification to central repository {}");
    }

    private HttpHeaders csrfHeaders() {
        CsrfToken csrfToken = csrfTokenRepository.generateToken(null);

        HttpHeaders headers = basicAuthHeaders();

        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", "XSRF-TOKEN=" + csrfToken.getToken());

        return headers;
    }

    private HttpHeaders basicAuthHeaders() {
        String plainCreds = "user" + ":" + "user";

        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.getEncoder().encode(plainCredsBytes);

        String base64Creds = new String(base64CredsBytes);

        HttpHeaders headers = new HttpHeaders();

        headers.add("Authorization", "Basic " + base64Creds);

        return headers;
    }

    private HttpHeaders bearerTokenHeaders() {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + token.getAccess_token());

        return headers;
    }

    @Scheduled(fixedRate = 5000)
    public void resolvePatientDetails() {
        List<AnalysisRequest> requests = analysisRequestRepository.findFirst75ByPatientResolvedIsNull();

        for (AnalysisRequest request : requests) {
            resolvePatient(request);
        }
    }

    public void resolvePatient(AnalysisRequest request) {
        try {
            authenticate();

            String patientId = request.getPatientId();

            // @formatter:off

			/**
			 * integration graphql layer query to fetch patient demographics
			 */

			String query = "{\"query\":\"query ($id: String) { people { id (id: $id) {id firstname lastname birthdate sex birthdate age {  years months } address { street city town { id name }  } identification {  number type { id name  }  }  phone } }}\", \"variables\":{\"id\":\""
					+ patientId + "\"}}";
            // @formatter:on

            log.info("Query ---------------------------------------------->  {}", query);

            String url = String.join("/", gateway, "client", "api", "graphql");

            HttpEntity<String> entity = new HttpEntity<String>(query, bearerTokenHeaders());

            ResponseEntity<PatientMainResponseResolver> response = rest()
                .exchange(url, HttpMethod.POST, entity, PatientMainResponseResolver.class);

            log.debug("ResponseEntity DATA  {}", response);

            /**
             * If the above query returns data then proceed to store the patient. if The
             * patient exist update the demographics just in case the details were changed
             * in ehr. The above comments refers to <-- savePatientDemographics(patientId,
             * response.getBody().getData().getPerson(), request); -> method implementation
             *
             */

            if (response.getBody() != null && response.getBody().getData() != null && response.getBody().getData().getPeople() != null) {
                savePatientDemographics(patientId, response.getBody().getData().getPeople().getId(), request);
            }
        } catch (Exception e) {
            log.error("Failed Connecting to central repository {}", e);
        }
    }

    private void savePatientDemographics(String patientId, PatientResolverDTO patientResolver, AnalysisRequest request) throws Exception {
        if (patientRepository.findByPatientId(patientId) != null) {
            /**
             * If the patient is found in the repository update the record
             */
            flagResolvedAnalysisRequest(request);
            patientService.updatePatient(patientResolver, patientId);
        } else {
            /**
             * If the patient is not found in the repository create a new patient record
             */
            Patient patientSaved = patientService.createPatient(patientResolver, patientId);
            log.info("Patient saved  {}", patientResolver.getLastname());
            if (patientSaved != null) {
                //flagResolvedAnalysisRequest(request); //TODO move to art query
            } else {
                log.info("Error while trying to save patient", patientResolver.getLastname());
            }
        }
    }

    private void flagResolvedAnalysisRequest(AnalysisRequest request) {
        request.setPatientResolved("Resolved");
        analysisRequestService.updateAnalysisRequest(request);
    }
}
