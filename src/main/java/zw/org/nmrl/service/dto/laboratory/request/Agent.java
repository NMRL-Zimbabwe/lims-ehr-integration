package zw.org.nmrl.service.dto.laboratory.request;

public class Agent {

    private String name;

    private String id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Agent [name=" + name + ", id=" + id + "]";
    }
}
