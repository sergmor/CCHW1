package edu.columbia.cc.workers;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;

import edu.columbia.cc.user.User;

public class AutoscaleWorker {
	
	private static final double UP_THRESHOLD=50;
	private static final double DOWN_THRESHOLD=10;
	private static final int PERIOD=120;

	public void setupAutoScale(User cUser) {
		//Create credentials & clients
		AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
		AmazonAutoScalingClient autoScaling = new AmazonAutoScalingClient(credentialsProvider);
		AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentialsProvider);
		
		//We will create a launch configuration and group per user
		CreateLaunchConfigurationRequest confRequest = new CreateLaunchConfigurationRequest()
														.withLaunchConfigurationName(cUser.getUserid())
														.withImageId(cUser.getAmi_id())
														.withInstanceType(cUser.getVm().getInstanceType());
	System.out.println("About to create LaunchConf");
		autoScaling.createLaunchConfiguration(confRequest);
		
		CreateAutoScalingGroupRequest groupRequest = new CreateAutoScalingGroupRequest()
														.withAutoScalingGroupName(cUser.getUserid())
														.withLaunchConfigurationName(cUser.getUserid())
														.withAvailabilityZones(cUser.getVm().getZone())
														.withMaxSize(2)
														.withMinSize(1);
	System.out.println("About to create groupRequest");
		autoScaling.createAutoScalingGroup(groupRequest);
		PutScalingPolicyRequest upPolicy = new PutScalingPolicyRequest()
											.withPolicyName(cUser.getId()+"_upScale")
											.withAutoScalingGroupName(cUser.getUserid())
											.withScalingAdjustment(1)
											.withAdjustmentType("ChangeInCapacity");
		//Create the actual policies and print the ARN
		
		PutScalingPolicyRequest downPolicy = new PutScalingPolicyRequest()
											.withPolicyName(cUser.getId()+"_downScale")
											.withAutoScalingGroupName(cUser.getUserid())
											.withScalingAdjustment(-1)
											.withAdjustmentType("ChangeInCapacity");
		PutScalingPolicyResult upReturn = autoScaling.putScalingPolicy(upPolicy);
	System.out.println(upReturn.getPolicyARN());
		PutScalingPolicyResult downReturn = autoScaling.putScalingPolicy(downPolicy);
	System.out.println(downReturn.getPolicyARN());
	
		//Create the alarms that will use the policies
		PutMetricAlarmRequest upAlarm = new PutMetricAlarmRequest()
											.withAlarmName("AddCapacity")
											.withMetricName("CPUUtilization")
											.withNamespace("AWS/EC2")
											.withStatistic("Average")
											.withPeriod(PERIOD)
											.withThreshold(UP_THRESHOLD)
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
											.withPeriod(PERIOD)
											.withThreshold(DOWN_THRESHOLD)
											.withComparisonOperator("GreaterThanOrEqualToThreshold")
											.withDimensions(new Dimension().withName("AutoScalingGroupName=").withValue(cUser.getUserid()))
											.withEvaluationPeriods(1)												
											.withActionsEnabled(true)
											.withAlarmActions(downReturn.getPolicyARN());
	System.out.println("Will try to create alarm");
		cloudWatch.putMetricAlarm(downAlarm);
	}
	
	
	
}
