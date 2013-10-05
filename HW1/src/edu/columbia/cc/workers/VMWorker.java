package edu.columbia.cc.workers;

import java.util.concurrent.Callable;

import com.amazonaws.services.ec2.AmazonEC2;

import edu.columbia.cc.platform.Action;
import edu.columbia.cc.user.User;

public class VMWorker implements Callable<User> {
	
	private User user = null;
	private Action command = null;
	private Ec2Worker ec2 = null;
	private EbsWorker ebs = null;
	private AmazonEC2 cloud = null;
	
	public VMWorker() {
		
	}
	
	
	
	public User getUser() {
		return user;
	}



	public void setUser(User user) {
		this.user = user;
	}



	public Action getCommand() {
		return command;
	}



	public void setCommand(Action command) {
		this.command = command;
	}

	public void checkCommand() throws InterruptedException {
		if(command.equals(Action.CREATE)){
			createVM();
		}
		else if(command.equals(Action.DELETE)) {
			deleteVM();
		}
	}

	private void deleteVM() {
		// TODO Auto-generated method stub
		
	}



	private void createVM() throws InterruptedException {
		
		System.out.println("Executing on thread "+Thread.currentThread().getId());
		System.out.println("Will create a VM for user "+ user.getId());
		Thread.sleep(100);
		System.out.println("Created a VM for user "+ user.getId());
	}



	@Override
	public User call() throws Exception {
		
		checkCommand();
		return user;
	}

}
