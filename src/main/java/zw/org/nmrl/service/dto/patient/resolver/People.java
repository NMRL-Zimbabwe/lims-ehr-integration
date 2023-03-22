package zw.org.nmrl.service.dto.patient.resolver;

public class People {

    private PatientResolverDTO id;

    public PatientResolverDTO getId() {
        return id;
    }

    public void setId(PatientResolverDTO id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ClassPojo [id = " + id + "]";
    }
}
