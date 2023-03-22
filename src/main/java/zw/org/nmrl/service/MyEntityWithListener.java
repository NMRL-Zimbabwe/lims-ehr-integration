package zw.org.nmrl.service;

import javax.persistence.PostUpdate;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zw.org.nmrl.domain.AnalysisRequest;
import zw.org.nmrl.service.laboratoryRequest.resolver.LaboratoryRequestMiddlewareResolver;

@Service
@Transactional
public class MyEntityWithListener {

    private static final Logger log = LoggerFactory.getLogger(MyEntityWithListener.class);
    /*
     * @Autowired LaboratoryRequestMiddlewareResolver
     * laboratoryRequestMiddlewareResolver;
     *
     * @PostUpdate public void auditNewHire(AnalysisRequest analysisRequest) { log.
     * debug("Call integration service to resolve received analysis request :{} ",
     * analysisRequest);
     * //laboratoryRequestMiddlewareResolver.loadLabRequestProcess(analysisRequest);
     * //patientMiddlewareResolver.resolvePatientDetails(); log.
     * debug("Complete Call to integration service to resolve received analysis request"
     * ); }
     */

}
