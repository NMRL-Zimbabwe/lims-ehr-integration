package zw.org.nmrl.service.patient.art.resolver;

import java.util.ArrayList;
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
import zw.org.nmrl.domain.Patient;
import zw.org.nmrl.repository.PatientRepository;
import zw.org.nmrl.service.PatientService;
import zw.org.nmrl.service.dto.AuthenticationResponseOne;
import zw.org.nmrl.service.dto.patient.art.resolver.PatientArtResponseResolver;
import zw.org.nmrl.service.dto.patient.art.resolver.PersonArt;
import zw.org.nmrl.service.laboratoryRequest.resolver.LaboratoryRequestMiddlewareResolver;

@Service
public class PatientArtMiddlewareResolver {

    private static final Logger log = LoggerFactory.getLogger(PatientArtMiddlewareResolver.class);

    @Autowired
    PatientService patientService;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

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

    public PatientArtMiddlewareResolver() {
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
        int retry = 4;
        List<Patient> patients = patientRepository.findFirst25ByArtIsNullAndRetryLessThan(retry);

        for (Patient patient : patients) {
            resolvePatientArt(patient);
        }
    }

    public void resolvePatientArt(Patient patient) {
        try {
            authenticate();

            String patientId = patient.getPatientId();

            // @formatter:off

			/**
			 * integration graphql layer query to fetch patient demographics
			 */

			String query = "{\"query\":\"query ($id: String) {  art { person(personId: $id) { number }  }}\", \"variables\":{\"id\":\""
					+ patientId + "\"}}";
            // @formatter:on

            log.info("Query ---------------------------------------------->  {}", query);

            String url = String.join("/", gateway, "macrodb", "api", "graphql");

            HttpEntity<String> entity = new HttpEntity<String>(query, bearerTokenHeaders());

            ResponseEntity<String> response1 = rest().exchange(url, HttpMethod.POST, entity, String.class);

            ResponseEntity<PatientArtResponseResolver> response = rest()
                .exchange(url, HttpMethod.POST, entity, PatientArtResponseResolver.class);

            log.info("ResponseEntity  {}", response1);

            /**
             * If the above query returns data then proceed to store the patient. if The
             * patient exist update the demographics just in case the details were changed
             * in ehr. The above comments refers to <-- savePatientDemographics(patientId,
             * response.getBody().getData().getPerson(), request); -> method implementation
             *
             */

            if (
                response.getBody() != null &&
                response.getBody().getData() != null &&
                response.getBody().getData().getArt() != null &&
                response.getBody().getData().getArt().getPerson() != null
            ) {
                updatePatientArt(patientId, response.getBody().getData().getArt().getPerson(), patient);
            }
        } catch (Exception e) {
            log.error("Failed Connecting to central repository {}", e);
        }
    }

    private void updatePatientArt(String patientId, List<PersonArt> arts, Patient patient) throws Exception {
        if (patientRepository.findByPatientId(patientId) != null) {
            /**
             * If the patient is found in the repository update the record
             */
            patientService.updatePatientArt(patient, arts);
        } else {
            patientService.updatePatientErrorLog(patient, "Failed to Update ART");

            log.info("Error while trying to save patient ART", patient.getLastname());
        }
    }
}
