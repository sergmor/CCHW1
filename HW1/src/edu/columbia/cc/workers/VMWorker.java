package edu.columbia.cc.workers;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;

import edu.columbia.cc.platform.Action;
import edu.columbia.cc.user.User;

public class VMWorker implements Callable<User> {
	
	private User user = null;
	private Action command = null;
	private Ec2Worker ec2 = null;
	private EbsWorker ebs = null;
	private AmazonEC2Client cloud = null;
	
	public VMWorker() {}
	
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

	public void checkCommand() throws InterruptedException, IOException
	{
		AWSCredentials awsCredentials = new PropertiesCredentials(
   			 this.getClass().getResourceAsStream("AwsCredentials.properties"));
		
		this.cloud  = new AmazonEC2Client(awsCredentials);
		this.ec2 = new Ec2Worker()
						.withUser(user)
						.withCloud(cloud);

		User tempUser = null;
		if(command.equals(Action.CREATE))
		{
			tempUser = ec2.processCreateRequest();
		}
		else if (command.equals(Action.RELAUNCH))
		{
			tempUser = ec2.processRelaunchRequest();
		}
		else if(command.equals(Action.DELETE))
		{
			tempUser = ec2.processDeleteRequest();
		}
	}

	@Override
	public User call() throws Exception {
		
		checkCommand();
		return user;
	}

}
