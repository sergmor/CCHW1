package edu.columbia.cc.user;

import edu.columbia.cc.user.VirtualMachine;

public class User {
	
	private long id = Long.MIN_VALUE;
	private String userid = ""; 
	private String key = "";
	private String ami_id = "";
	private String serurityGroup = "";
	private String ip = "";
	private String volumeId = "";
	private VirtualMachine vm = null;
	
	public User() {
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getAmi_id() {
		return ami_id;
	}
	public void setAmi_id(String ami_id) {
		this.ami_id = ami_id;
	}
	public String getSerurityGroup() {
		return serurityGroup;
	}
	public void setSerurityGroup(String serurityGroup) {
		this.serurityGroup = serurityGroup;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getVolumeId() {
		return volumeId;
	}
	public void setVolumeId(String volumeId) {
		this.volumeId = volumeId;
	}
	public VirtualMachine getVm() {
		return vm;
	}
	public void setVm(VirtualMachine vm) {
		this.vm = vm;
	}

	


}
