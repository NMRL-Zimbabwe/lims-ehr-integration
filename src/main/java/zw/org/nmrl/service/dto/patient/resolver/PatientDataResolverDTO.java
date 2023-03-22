package zw.org.nmrl.service.dto.patient.resolver;

public class PatientDataResolverDTO {

    private People people;

    public People getPeople() {
        return people;
    }

    public void setPeople(People people) {
        this.people = people;
    }

    @Override
    public String toString() {
        return "PatientDataResolverDTO [people=" + people + "]";
    }
}
