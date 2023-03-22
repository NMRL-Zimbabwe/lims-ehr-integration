package zw.org.nmrl.service.dto;

import java.util.Map;
import org.springframework.cloud.cloudfoundry.com.fasterxml.jackson.annotation.JsonIgnore;
import zw.org.nmrl.service.dto.patient.resolver.PatientResolverDTO;

public class PatientGraphqlDTO {

    private Map<String, String> data;

    @JsonIgnore
    Map<String, PatientResolverDTO> person;

    public Map<String, PatientResolverDTO> getPerson() {
        return person;
    }

    public void setPerson(Map<String, PatientResolverDTO> person) {
        this.person = person;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
