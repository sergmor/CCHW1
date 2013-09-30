package edu.columbia.cc.workers;

import java.util.concurrent.Callable;

import com.amazonaws.services.ec2.AmazonEC2;

import edu.columbia.cc.platform.Action;
import edu.columbia.cc.user.User;

public class VMWorker implements Callable<Object> {
	
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

	public void checkCommand() {
		//Switch if-else for command
	}

	@Override
	public Object call() throws Exception {
		
		checkCommand();
		return null;
	}

}
