package zw.org.nmrl.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.stream.Collectors;
import zw.org.nmrl.domain.Authority;
import zw.org.nmrl.domain.Laboratory;
import zw.org.nmrl.domain.User;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LaboratoryDTO {

    private String id;

    private String name;

    private String code;

    private String type;

    public LaboratoryDTO() {
        // Empty constructor needed for Jackson.
    }

    public LaboratoryDTO(Laboratory lab) {
        this.id = lab.getId();
        this.code = lab.getCode();
        this.name = lab.getName();
        this.type = lab.getType();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "LaboratoryDTO [id=" + id + ", name=" + name + ", code=" + code + ", type=" + type + "]";
    }
}
