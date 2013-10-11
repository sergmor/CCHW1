package edu.columbia.cc.workers;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3Client;

import edu.columbia.cc.platform.Action;
import edu.columbia.cc.user.User;

public class VMWorker implements Callable<User> {
	
	private User user = null;
	private Action command = null;
	private Ec2Worker ec2 = null;
	private EbsWorker ebs = null;
	private AmazonEC2Client cloud = null;
	private AmazonS3Client s3 = null;
	private AmazonAutoScalingClient autoScaling = null;
	private AmazonCloudWatchClient cloudWatch = null;
	private AutoscaleWorker aw = null;
	
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

	public void checkCommand()
	{
		/*AWSCredentials awsCredentials = new PropertiesCredentials(
   			 this.getClass().getResourceAsStream("AwsCredentials.properties"));
		this.cloud  = new AmazonEC2Client(awsCredentials);*/
		
		AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
		this.cloud  = new AmazonEC2Client(credentialsProvider);
		this.s3 = new AmazonS3Client(credentialsProvider);
		this.autoScaling = new AmazonAutoScalingClient(credentialsProvider);
		this.cloudWatch = new AmazonCloudWatchClient(credentialsProvider);
		
		this.ec2 = new Ec2Worker()
						.withUser(user)
						.withCloud(cloud)
						.withS3(s3);
		
		this.aw = new AutoscaleWorker().withAutoscale(autoScaling)
										.withCloudWatch(cloudWatch);
		
		User tempUser = null;
		System.out.println("Credentials : " + credentialsProvider.toString() );
		try
		{
			if(command.equals(Action.CREATE))
			{
				System.out.println("attempting to create VM ...");
				tempUser = ec2.processCreateRequest();
				
				aw.createLoadBalancer(tempUser);
				aw.setupAutoScale(tempUser);
				
			}
			else if (command.equals(Action.RELAUNCH))
			{
				tempUser = ec2.processRelaunchRequest();
				
				aw.setupAutoScale(tempUser);
			}
			else if(command.equals(Action.DELETE))
			{
				System.out.println("Attempting to remove alarm configuration ...");
				aw.tearDownAutoScale(tempUser);
				System.out.println("attempting to delete VM ...");
				tempUser = ec2.processDeleteRequest();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public User call() throws Exception {
		
		checkCommand();
		return user;
	}

}
