package zw.org.nmrl.service.ehr;

/**
 * Class responsible for sending analysis request rejection to EHR
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
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import zw.org.nmrl.domain.LaboratoryRequest;
import zw.org.nmrl.repository.LaboratoryRequestRepository;
import zw.org.nmrl.service.PatientService;
import zw.org.nmrl.service.dto.AuthenticationResponseOne;
import zw.org.nmrl.service.dto.RejectSampleReason;
import zw.org.nmrl.service.dto.SampleRejection;
import zw.org.nmrl.service.dto.patient.resolver.PatientMainResponseResolver;
import zw.org.nmrl.service.enums.LaboratoryRequestStatus;

@Service
public class SendSampleRejectionToEhr {

    private static final Logger log = LoggerFactory.getLogger(SendSampleRejectionToEhr.class);

    @Autowired
    PatientService patientService;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    private LaboratoryRequestRepository laboratoryRequestRepository;

    private AuthenticationResponseOne token;

    @Value("$ {myConfig.myBaseGateway}")
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

    public SendSampleRejectionToEhr() {
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

    // TODO native query to search for analysis request not in patient table

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

        log.info("Rutendo ********************************************************");

        Credentials credentials = new Credentials("user", "user", true);

        String url = String.join("/", gateway, "auth", "login");

        HttpHeaders headers = csrfHeaders();

        HttpEntity<Credentials> data = new HttpEntity<>(credentials, headers);

        token = rest().exchange(url, HttpMethod.POST, data, AuthenticationResponseOne.class).getBody();

        log.info("Authentification to central repository {}", token.getAccess_token());
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

    // @Scheduled(fixedRate = 25000)
    public void rejectedSample() {
        String rejected = LaboratoryRequestStatus.REJECTED.toString();
        /**
         * Fetch all analysis request that have not been sent to ehr with status of
         * rejected
         */
        List<LaboratoryRequest> requests = laboratoryRequestRepository.findFirst10BySentToEhrIsNullAndStatus(rejected);

        for (LaboratoryRequest request : requests) {
            sendRejectedSample(request);
        }
    }

    private void sendRejectedSample(LaboratoryRequest request) {
        try {
            authenticate();
            log.info("Connecting to central repository {}", gateway);

            String id = request.getMiddlewareAnalysisRequestUuid();

            SampleRejection rejection = new SampleRejection();
            rejection.setDate(request.getDateCollected().toString());
            rejection.setLabReference(request.getLabReferenceSampleId());

            RejectSampleReason reason = new RejectSampleReason();

            reason.setCode("01");
            rejection.setRejectSampleReason(reason);

            String url = String.join("/", gateway, "api", "laboratory-request" + id + "reject");

            HttpEntity<SampleRejection> entity = new HttpEntity<SampleRejection>(rejection, bearerTokenHeaders());

            ResponseEntity<PatientMainResponseResolver> response = rest()
                .exchange(url, HttpMethod.POST, entity, PatientMainResponseResolver.class);

            log.info("ResponseEntity  {}", response.getBody().getData().getPeople());

            updateRejectedSampleSentToEhr(request);
        } catch (Exception e) {
            log.info("Failed Connecting to central repository {}", e);
        }
    }

    private void updateRejectedSampleSentToEhr(LaboratoryRequest request) {
        request.setStatus(LaboratoryRequestStatus.REJECTED.toString());
        request.setStatus(LaboratoryRequestStatus.SENT_TO_EHR.toString());
        laboratoryRequestRepository.save(request);
    }
}
