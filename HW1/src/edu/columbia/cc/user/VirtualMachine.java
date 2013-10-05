package edu.columbia.cc.user;


public class VirtualMachine {

	private String instanceId = "";
	private String instanceType = "";
	private String internalIp = "";
	private String primaryVolumeId = "";
	private String extraVolumeId = "";
	private String zone = "";
	


	public VirtualMachine() {
		
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getInstanceType() {
		return instanceType;
	}

	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}

	public String getInternalIp() {
		return internalIp;
	}

	public void setInternalIp(String internalIp) {
		this.internalIp = internalIp;
	}

	public String getPrimaryVolumeId() {
		return primaryVolumeId;
	}

	public void setPrimaryVolumeId(String primaryVolumeId) {
		this.primaryVolumeId = primaryVolumeId;
	}

	public String getExtraVolumeId() {
		return extraVolumeId;
	}

	public void setExtraVolumeId(String volumeId) {
		this.extraVolumeId = volumeId;
	}
	
	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

}
