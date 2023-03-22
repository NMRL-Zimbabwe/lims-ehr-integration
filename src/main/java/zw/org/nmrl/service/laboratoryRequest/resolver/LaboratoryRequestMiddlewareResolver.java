package zw.org.nmrl.service.laboratoryRequest.resolver;

/**
 * Resolver class responsible for converting analysis request from
 * integration layer received earlier to a laboratory request.
 * The laboratory request should be build to match the
 * laboratory request "FORM" requirements.
 *
 * @author lawrence Chirowodza
 * @date 2020
 */

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.transaction.NotSupportedException;
import javax.transaction.Transactional;
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
import zw.org.nmrl.repository.AnalysisRequestRepository;
import zw.org.nmrl.service.AnalysisRequestService;
import zw.org.nmrl.service.LaboratoryRequestService;
import zw.org.nmrl.service.PatientService;
import zw.org.nmrl.service.dto.AuthenticationResponseOne;
import zw.org.nmrl.service.dto.laboratory.request.LaboratoryRequestDataDTO;
import zw.org.nmrl.service.dto.laboratory.request.LaboratoryRequestEhrDTO;

@Service
@Transactional
public class LaboratoryRequestMiddlewareResolver {

    private static final Logger log = LoggerFactory.getLogger(LaboratoryRequestMiddlewareResolver.class);

    @Autowired
    PatientService patientService;

    @Autowired
    AnalysisRequestService analysisRequestService;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    AnalysisRequestRepository analysisRequestRepository;

    @Autowired
    LaboratoryRequestService laboratoryRequestService;

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

    public LaboratoryRequestMiddlewareResolver() {
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

        Credentials credentials = new Credentials("user", "user", true);

        String url = String.join("/", gateway, "auth", "login");

        HttpHeaders headers = csrfHeaders();

        HttpEntity<Credentials> data = new HttpEntity<>(credentials, headers);

        token = rest().exchange(url, HttpMethod.POST, data, AuthenticationResponseOne.class).getBody();
        // log.info("Authentification to central repository {}",
        // token.getAccess_token());

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

    @Scheduled(fixedRate = 50000)
    public void resolveLaboratoryRequestDetails() {
        List<AnalysisRequest> requests = analysisRequestRepository.findFirst10ByLaboratoryRequestResolvedIsNullAndPatientResolvedIsNotNull();

        if (requests.size() == 0) {
            //log.info("No Unreolved patients found");
            return;
        }
        //log.info("Found Unreolved patients found {}", requests.size());
        for (AnalysisRequest request : requests) {
            loadLabRequestProcess(request);
        }
    }

    public void loadLabRequestProcess(AnalysisRequest request) {
        try {
            authenticate();

            String labRequestId = request.getMiddlewareAnalysisRequestUuid();

            // @formatter:off
			 //String query = "{\"query\":\"query ($id: String) {  person(id: $id) { lastname  firstname sex birthdate  age {years months }  address { street city     town {  id      name   }   } }}\", \"variables\":{\"id\":\""+ labRequestId +"\"}}";	    
			 String query = "{\"query\":\"query ($id: String) { notification { id(id: $id) { id labReference clientReference date status {date  status } facility { id name coding { agentContext { agent { id name } } code}} laboratory { id name coding { agentContext { agent { id name } context { id   name  } } code}} sample "
			 		+ "{ id  name coding {  agentContext{ agent {id name } context { id name  } }code }  }test {id name coding {  agentContext {   agent { id     name  } context {        id name }  }   code } }}}}\",\"variables\":{\"id\":\""+ labRequestId +"\"}}";

            // @formatter:on

            log.info("Query ----------------------------------------------> {}", query);

            String url = String.join("/", gateway, "lmis", "api", "graphql");

            log.info("Connected to .. {}", url);

            HttpEntity<String> entity = new HttpEntity<String>(query, bearerTokenHeaders());

            ResponseEntity<LaboratoryRequestDataDTO> response = rest()
                .exchange(url, HttpMethod.POST, entity, LaboratoryRequestDataDTO.class);

            log.info("ResponseEntity  {}", response.getBody().getData().getNotification());

            saveLaboratoryRequest(request, response.getBody().getData().getNotification().getId(), request.getPatientId());
        } catch (Exception e) {
            log.info("Failed Connecting to central repository {}", e);
        }
    }

    private void saveLaboratoryRequest(AnalysisRequest request, LaboratoryRequestEhrDTO laboratoryRequestDTO, String patientId) {
        log.info("AnalysisRequest Conversion  : {}", request);
        log.info("LaboratoryRequestEhrDTO From EHR  : {}", laboratoryRequestDTO);

        laboratoryRequestService.saveLaboratoryRequest(laboratoryRequestDTO, patientId);

        request.setLaboratoryRequestResolved("Resolved");
        analysisRequestService.updateAnalysisRequest(request);
    }
}
