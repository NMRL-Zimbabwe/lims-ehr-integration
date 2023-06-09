package zw.org.nmrl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import zw.org.nmrl.service.dto.RegistrationFromLims;

@Service
//@Transactional
public class FromLimsMqWorker {

    /**
     * @throws TimeoutException
     * @throws IOException
     * @date 08-10-2020
     * @writer Lawrence
     */
    @Autowired
    LaboratoryRequestService laboratoryRequestService;

    @Autowired
    private ObjectMapper mapper;

    @Value("${myConfig.myRabbitLocalGateway}")
    private String gateway;

    @RabbitListener(bindings = @QueueBinding(exchange = @Exchange("from-lims"), key = "senaite", value = @Queue("from_lims")))
    public void reciveRequestFromLIMS(Message msg) throws IOException, TimeoutException {
        String string = new String(msg.getBody());
        msg.getMessageProperties().getHeader("request");

        System.out.println("Received Message: " + msg.getBody());

        RegistrationFromLims obj = mapper.readValue(string, RegistrationFromLims.class);

        laboratoryRequestService.updateLaoratoryRequest(obj);
    }
}
