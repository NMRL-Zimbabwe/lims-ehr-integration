package zw.org.nmrl.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.cloud.cloudfoundry.com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A user.
 */
@Entity
@Table(name = "patient")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Patient extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "patient_id", unique = true, nullable = false)
    private String patientId;

    private String firstname;

    private String lastname;

    private String gender;

    private String primary_referrer;

    private String primary_referrer_id;

    private String art;

    @JsonIgnore
    private int retry;

    @JsonIgnore
    @Column(name = "error_reason")
    private String errorReason;

    @Column(name = "dob")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dob;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    /*
     * @OneToMany(mappedBy = "person")
     *
     * @JsonIgnore
     *
     * @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE) private
     * Set<Address> addresses = new HashSet<>();
     */

    public String getPrimary_referrer() {
        return primary_referrer;
    }

    public void setPrimary_referrer(String primary_referrer) {
        this.primary_referrer = primary_referrer;
    }

    public String getPrimary_referrer_id() {
        return primary_referrer_id;
    }

    public void setPrimary_referrer_id(String primary_referrer_id) {
        this.primary_referrer_id = primary_referrer_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Patient)) {
            return false;
        }
        return patientId != null && patientId.equals(((Patient) o).patientId);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getArt() {
        return art;
    }

    public void setArt(String art) {
        this.art = art;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    // prettier-ignore
	@Override
	public String toString() {
		return "Patient [patientId=" + patientId + ", firstname=" + firstname + ", lastname=" + lastname + ", gender="
				+ gender + ", primary_referrer=" + primary_referrer + ", primary_referrer_id=" + primary_referrer_id
				+ ", art=" + art + ", retry=" + retry + ", errorReason=" + errorReason + ", dob=" + dob + "]";
	}
}
