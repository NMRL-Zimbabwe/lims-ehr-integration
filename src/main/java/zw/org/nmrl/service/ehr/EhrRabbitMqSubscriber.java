package zw.org.nmrl.service.ehr;

import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * RabbitMQ worker responsible for subscribing to integration layer and fetch analysis request.
 * Entry point of EHR
 *
 * @author Lawrence Chirowodza
 * @date 2020
 */

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.org.nmrl.domain.AnalysisRequest;
import zw.org.nmrl.service.AnalysisRequestService;
import zw.org.nmrl.service.dto.MiddlewareAnalysisRequestDTO;
import zw.org.nmrl.service.patient.resolver.PatientMiddlewareResolver;

@Service
@Transactional
public class EhrRabbitMqSubscriber {

    private static final Logger log = LoggerFactory.getLogger(EhrRabbitMqSubscriber.class);

    @Autowired
    AnalysisRequestService analysisRequestService;

    @Autowired
    PatientMiddlewareResolver patientMiddlewareResolver;

    @Autowired
    private ObjectMapper mapper;

    @RabbitListener(
        containerFactory = "ehrContainerFactory",
        bindings = @QueueBinding(value = @Queue(value = "nmrl"), exchange = @Exchange(value = "amq.direct"), key = "nmrl")
    )
    public void receivedMessage(Message msg) throws IOException, TimeoutException {
        String string = new String(msg.getBody());
        msg.getMessageProperties().getHeader("request");

        if (msg.getMessageProperties().isRedelivered()) {
            log.info("Error during receiving msg: {}", msg.getBody());
            //https://rest.bluedotsms.com
            //api/SendSMS?api_id=API9147119203&api_password=Nmrlsupp@rt&sms_type=P&encoding=T&sender_id=LabResults&phonenumber={recipient}&textmessage={message}

            return;
        }

        log.debug("Received Message: {}" + msg.getBody());

        /**
         * Messages are received with minimum information and other meaningful
         * information should be resolved through probing integration layer graphql
         * APIs. This is also the entry point of interaction between LIMS and EHR
         *
         **/

        MiddlewareAnalysisRequestDTO receivedMessage = mapper.readValue(string, MiddlewareAnalysisRequestDTO.class);

        /**
         * The record to be saved here is then resolved in PatientMiddlewareResolver and
         * LaboratoryRequestMiddlewareResolver classes to create patient and laboratory
         * request
         */
        if (receivedMessage != null) {
            AnalysisRequest analysisRequest = analysisRequestService.createAnalysisRequest(receivedMessage);

            if (analysisRequest != null) {
                patientMiddlewareResolver.resolvePatient(analysisRequest);
            }
        }
    }
}
