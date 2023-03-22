package zw.org.nmrl.service.ehr;

import java.time.LocalDate;
/**
 * Class responsible for sending analysis request acknowledgement to EHR
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
import zw.org.nmrl.domain.LaboratoryRequest;
import zw.org.nmrl.repository.LaboratoryRequestRepository;
import zw.org.nmrl.service.PatientService;
import zw.org.nmrl.service.dto.AuthenticationResponseOne;
import zw.org.nmrl.service.dto.SampleAcceptance;
import zw.org.nmrl.service.dto.patient.resolver.PatientMainResponseResolver;
import zw.org.nmrl.service.enums.LaboratoryRequestStatus;

@Service
public class SendSampleAcceptanceToEhr {

    private static final Logger log = LoggerFactory.getLogger(SendSampleAcceptanceToEhr.class);

    @Autowired
    PatientService patientService;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    private LaboratoryRequestRepository laboratoryRequestRepository;

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

    public SendSampleAcceptanceToEhr() {
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

        HttpHeaders headers = csrfHeaders("user", "user");

        HttpEntity<Credentials> data = new HttpEntity<>(credentials, headers);

        token = rest().exchange(url, HttpMethod.POST, data, AuthenticationResponseOne.class).getBody();

        log.info("Authentification to central repository {}");
    }

    /*
     * private HttpHeaders csrfHeaders() { CsrfToken csrfToken =
     * csrfTokenRepository.generateToken(null);
     *
     * HttpHeaders headers = basicAuthHeaders();
     *
     * headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
     * headers.add("Cookie", "XSRF-TOKEN=" + csrfToken.getToken());
     *
     * return headers; }
     */
    private HttpHeaders getModifyRequestAuthHeaders(String username, String password) {
        final HttpHeaders authHeaders = csrfHeaders(username, password);
        authHeaders.setAll(bearerTokenHeaders().toSingleValueMap());
        return authHeaders;
    }

    public HttpHeaders csrfHeaders(final String username, final String password) {
        final CsrfToken csrfToken = csrfTokenRepository.generateToken(null);
        final HttpHeaders headers = basicAuthHeaders(username, password);
        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", "XSRF-TOKEN=" + csrfToken.getToken());

        return headers;
    }

    private HttpHeaders basicAuthHeaders(String username, String password) {
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

    @Scheduled(fixedRate = 25000)
    public void resolveSampleAcceptanceDetails() {
        List<LaboratoryRequest> requests = laboratoryRequestRepository.findFirst10BySentToEhrIsNullAndAcknowledgeSampleReceiptIsNotNull();

        for (LaboratoryRequest request : requests) {
            sendSampleAcceptance(request);
            log.debug("Sample received acknowledgement............. {}", request);
        }
    }

    private void sendSampleAcceptance(LaboratoryRequest request) {
        try {
            authenticate();
            log.info("Connecting to central repository {}", gateway);

            String id = request.getMiddlewareAnalysisRequestUuid();

            SampleAcceptance accept = new SampleAcceptance();

            accept.setDate(LocalDate.now().toString());
            accept.setLabReference(request.getLabReferenceSampleId());

            String url = String.join("/", gateway, "lmis", "api", "notification/" + id + "/receive");

            log.info("Sample acceptance url {}", url);

            HttpEntity<SampleAcceptance> entity = new HttpEntity<SampleAcceptance>(accept, getModifyRequestAuthHeaders("user", "user"));

            ResponseEntity<PatientMainResponseResolver> response = rest()
                .exchange(url, HttpMethod.POST, entity, PatientMainResponseResolver.class);

            log.info("Sending sample aknowledgement receipt  {}", response.getStatusCode());

            updateSampleAcceptanceSentToEhr(request);
        } catch (Exception e) {
            log.info("Failed Connecting to central repository {}", e);
        }
    }

    private void updateSampleAcceptanceSentToEhr(LaboratoryRequest request) {
        request.setAcknowledgeRecordReceipt(LaboratoryRequestStatus.ACKNOWLEDGE_RECEIPT.toString());
        request.setSentToEhr(LaboratoryRequestStatus.SENT_TO_EHR.toString());
        laboratoryRequestRepository.save(request);
    }
}
