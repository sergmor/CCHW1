package edu.columbia.cc.workers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;

import edu.columbia.cc.user.User;

public class CloudWatchWorker {
	
	private Double averageCPU = 0.0;
	private String stop = "arn:aws:automate:us-east-1:ec2:stop";
	private String stopTemplate = "arn:aws:automate:SITE:ec2:stop";
	private String terminate = "arn:aws:automate:us-east-1:ec2:terminate";
	private String terminateTemplate = "arn:aws:automate:SITE:ec2:terminate";
	

	public double monitorUtil (String insId) throws IOException
	{
		try {
		//AWSCredentials credentials = new PropertiesCredentials( CloudWatchWorker.class.getResourceAsStream("AwsCredentials.properties"));
		
		/*********************************************
 		*  	#1 Create Amazon Client object
 		*********************************************/
 		//System.out.println("#1 Create Amazon Client object"); #Not needed
 		//AmazonEC2 ec2 = new AmazonEC2Client(credentials);
 		
 	    // we assume that we've already created an instance. Use the id of the instance.
 	    String instanceId = insId;
 			
 		/***********************************
		 *   #3 Monitoring (CloudWatch)
		 *********************************/
		
		//create CloudWatch client
 	    AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
		AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentialsProvider) ;
		
		//create request message
		GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
		
		//set up request message
		statRequest.setNamespace("AWS/EC2"); //namespace
		statRequest.setPeriod(60); //period of data
		ArrayList<String> stats = new ArrayList<String>();
		
		//Use one of these strings: Average, Maximum, Minimum, SampleCount, Sum 
		stats.add("Average"); 
		//stats.add("Sum");
		statRequest.setStatistics(stats);
		
		//Use one of these strings: CPUUtilization, NetworkIn, NetworkOut, DiskReadBytes, DiskWriteBytes, DiskReadOperations  
		statRequest.setMetricName("CPUUtilization"); 
		
		// set time
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.add(GregorianCalendar.SECOND, -1 * calendar.get(GregorianCalendar.SECOND)); // 1 second ago
		Date endTime = calendar.getTime();
		calendar.add(GregorianCalendar.MINUTE, -10); // 10 minutes ago
		Date startTime = calendar.getTime();
		statRequest.setStartTime(startTime);
		statRequest.setEndTime(endTime);
		
		//specify an instance
		ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
		dimensions.add(new Dimension().withName("InstanceId").withValue(instanceId));
		statRequest.setDimensions(dimensions);
		
		//get statistics
		GetMetricStatisticsResult statResult = cloudWatch.getMetricStatistics(statRequest);
		
		//display
		List<Datapoint> dataList = statResult.getDatapoints();
		for (Datapoint data : dataList){
			averageCPU = data.getAverage();
			System.out.println("Average CPU utlilization for last 10 minutes: "+averageCPU);
		}    
		List<String> actions = new ArrayList<String>();
		actions.add(stop);
		//actions.add("");
		
		PutMetricAlarmRequest defaultAlarm = new PutMetricAlarmRequest()
												.withNamespace("AWS/EC2")
												.withMetricName("CPUUtilization")
												.withDimensions(dimensions)
												.withPeriod(300)
												.withStatistic("Average")
												.withAlarmName("Stop-EZ2-Instance")
												.withComparisonOperator("LessThanThreshold")
												.withThreshold(10.0)
												.withEvaluationPeriods(1)												
												.withActionsEnabled(true)
												.withAlarmActions(actions);
		System.out.println("Will try to create alarm");
		cloudWatch.putMetricAlarm(defaultAlarm);
 			
		}   catch (AmazonServiceException ase) {
		    System.out.println("Caught Exception: " + ase.getMessage());
		    System.out.println("Reponse Status Code: " + ase.getStatusCode());
		    System.out.println("Error Code: " + ase.getErrorCode());
		    System.out.println("Request ID: " + ase.getRequestId());
		}

		return averageCPU;
	}

	public void setupAutoScale(User cUser) {
		AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
		AmazonAutoScalingClient autoScaling = new AmazonAutoScalingClient(credentialsProvider);
		AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentialsProvider);
		CreateLaunchConfigurationRequest confRequest = new CreateLaunchConfigurationRequest()
														.withLaunchConfigurationName(cUser.getUserid())
														.withImageId(cUser.getAmi_id())
														.withInstanceType(cUser.getVm().getInstanceType());
	System.out.println("About to create LaunchConf");
		autoScaling.createLaunchConfiguration(confRequest);
		
		CreateAutoScalingGroupRequest groupRequest = new CreateAutoScalingGroupRequest()
														.withAutoScalingGroupName(cUser.getUserid())
														.withLaunchConfigurationName(cUser.getUserid())
														.withAvailabilityZones("us-east-1b")
														.withMaxSize(2)
														.withMinSize(1);
	System.out.println("About to create groupRequest");
		autoScaling.createAutoScalingGroup(groupRequest);
		PutScalingPolicyRequest upPolicy = new PutScalingPolicyRequest()
											.withPolicyName(cUser.getId()+"_upScale")
											.withAutoScalingGroupName(cUser.getUserid())
											.withScalingAdjustment(1)
											.withAdjustmentType("ChangeInCapacity");
				
		PutScalingPolicyRequest downPolicy = new PutScalingPolicyRequest()
											.withPolicyName(cUser.getId()+"_inScale")
											.withAutoScalingGroupName(cUser.getUserid())
											.withScalingAdjustment(-1)
											.withAdjustmentType("ChangeInCapacity");
		PutScalingPolicyResult upReturn = autoScaling.putScalingPolicy(upPolicy);
	System.out.println(upReturn.getPolicyARN());
		PutScalingPolicyResult downReturn = autoScaling.putScalingPolicy(downPolicy);
	System.out.println(downReturn.getPolicyARN());
	
		PutMetricAlarmRequest upAlarm = new PutMetricAlarmRequest()
											.withAlarmName("AddCapacity")
											.withMetricName("CPUUtilization")
											.withNamespace("AWS/EC2")
											.withStatistic("Average")
											.withPeriod(120)
											.withThreshold(50.0)
											.withComparisonOperator("GreaterThanOrEqualToThreshold")
											.withDimensions(new Dimension().withName("AutoScalingGroupName=").withValue(cUser.getUserid()))
											.withEvaluationPeriods(1)												
											.withActionsEnabled(true)
											.withAlarmActions(upReturn.getPolicyARN());
	System.out.println("Will try to create alarm");
		cloudWatch.putMetricAlarm(upAlarm);
		
		PutMetricAlarmRequest downAlarm = new PutMetricAlarmRequest()
											.withAlarmName("RemoveCapacity")
											.withMetricName("CPUUtilization")
											.withNamespace("AWS/EC2")
											.withStatistic("Average")
											.withPeriod(120)
											.withThreshold(30.0)
											.withComparisonOperator("GreaterThanOrEqualToThreshold")
											.withDimensions(new Dimension().withName("AutoScalingGroupName=").withValue(cUser.getUserid()))
											.withEvaluationPeriods(1)												
											.withActionsEnabled(true)
											.withAlarmActions(downReturn.getPolicyARN());
	System.out.println("Will try to create alarm");
		cloudWatch.putMetricAlarm(downAlarm);
	}
	
	
	
	
}


