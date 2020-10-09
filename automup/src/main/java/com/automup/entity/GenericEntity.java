package com.automup.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

@MappedSuperclass
public abstract class GenericEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATED_ON")
	protected Date createdOn;

	@Column(name = "CREATED_BY")
	protected String createdBy;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "LAST_UPDATED_ON")
	protected Date lastUpdatedOn;

	@Column(name = "LAST_UPDATED_BY")
	protected String lastUpdatedBy;
	
	@Version
	protected Long version;
	
	@PrePersist
	protected void onCreate() {
//		this.createdBy = SecurityUtils.getLoggedUserName();
		this.createdOn = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
//		this.lastUpdatedBy = SecurityUtils.getLoggedUserName();
		this.lastUpdatedOn = new Date();
	}
	
	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Date getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(Date lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	public String getLastUpdatedBy() {
		return lastUpdatedBy;
	}

	public void setLastUpdatedBy(String lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
