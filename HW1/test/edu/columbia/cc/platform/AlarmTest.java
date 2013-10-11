package edu.columbia.cc.platform;

import java.io.IOException;

import edu.columbia.cc.user.User;
import edu.columbia.cc.user.VirtualMachine;
import edu.columbia.cc.workers.AutoscaleWorker;
import edu.columbia.cc.workers.CloudWatchWorker;

public class AlarmTest {

	private static CloudWatchWorker cww = new CloudWatchWorker();
	private static AutoscaleWorker aw = new AutoscaleWorker();
	
	public static void main(String[] args) throws IOException {
		
		 //cww.monitorUtil("i-f6916391");
	
		User temp = new User();
		long id = System.currentTimeMillis();		
		temp.setId(id);
		temp.setUserid(Long.toString(temp.getId()));
		VirtualMachine vm = new VirtualMachine();
		vm.setInstanceType("t1.micro");
		vm.setZone("us-east-1a");
		temp.setVm(vm);
		temp.setKeyName("12345_key");
		temp.setAmi_id("ami-76f0061f");
		temp.setSecurityGroupName("12345_group");
		temp.getVm().setInstanceId("i-1e703c7a");
		aw.createLoadBalancer(temp);
		aw.setupAutoScale(temp);
		
		
	}

}
