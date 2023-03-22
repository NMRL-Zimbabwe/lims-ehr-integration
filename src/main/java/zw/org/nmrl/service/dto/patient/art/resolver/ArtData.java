package zw.org.nmrl.service.dto.patient.art.resolver;

public class ArtData {

    private Art art;

    public Art getArt() {
        return art;
    }

    public void setArt(Art art) {
        this.art = art;
    }

    @Override
    public String toString() {
        return "ArtData [art=" + art + "]";
    }
}
