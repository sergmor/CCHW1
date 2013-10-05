package edu.columbia.cc.user;


public class User {
	
	private long id = Long.MIN_VALUE;
	private String userid = ""; 
	private String keyName = "";
	private String ami_id = "";
	private String securityGroupName = "";
	private String ip = "";
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
	
	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public String getAmi_id() {
		return ami_id;
	}
	public void setAmi_id(String ami_id) {
		this.ami_id = ami_id;
	}
	public String getSecurityGroupName() {
		return securityGroupName;
	}
	public void setSecurityGroupName(String securityGroup) {
		this.securityGroupName = securityGroup;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}

	public VirtualMachine getVm() {
		return vm;
	}
	public void setVm(VirtualMachine vm) {
		this.vm = vm;
	}
}
