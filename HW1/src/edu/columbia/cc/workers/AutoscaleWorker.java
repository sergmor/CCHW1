package edu.columbia.cc.workers;

import java.util.ArrayList;
import java.util.Collection;

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
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.Listener;

import edu.columbia.cc.user.User;

public class AutoscaleWorker {
	
	private static final double UP_THRESHOLD=85;
	private static final double DOWN_THRESHOLD=20;
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
														.withSecurityGroups(cUser.getSecurityGroupName())
														.withKeyName(cUser.getKeyName())
														.withInstanceType(cUser.getVm().getInstanceType());
	System.out.println("About to create LaunchConf");
		autoScaling.createLaunchConfiguration(confRequest);
		
		CreateAutoScalingGroupRequest groupRequest = new CreateAutoScalingGroupRequest()
														.withAutoScalingGroupName(cUser.getUserid())
														.withLaunchConfigurationName(cUser.getUserid())
														.withAvailabilityZones(cUser.getVm().getZone())
														.withLoadBalancerNames(cUser.getUserid()+"-lb")
														.withMaxSize(2)
														.withMinSize(0)
														.withDesiredCapacity(0);
		
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
											.withAlarmName("AddCapacity" + "-" + cUser.getUserid())
											.withMetricName("CPUUtilization")
											.withNamespace("AWS/EC2")
											.withStatistic("Average")
											.withPeriod(PERIOD)
											.withThreshold(UP_THRESHOLD)
											.withComparisonOperator("GreaterThanOrEqualToThreshold")
											.withDimensions(new Dimension().withName("AutoScalingGroupName=").withValue(cUser.getUserid()))
											.withEvaluationPeriods(2)												
											.withActionsEnabled(true)
											.withAlarmActions(upReturn.getPolicyARN());
	System.out.println("Will try to create alarm : " + upAlarm.getAlarmName());
		cloudWatch.putMetricAlarm(upAlarm);
		
		PutMetricAlarmRequest downAlarm = new PutMetricAlarmRequest()
											.withAlarmName("RemoveCapacity" + "-" + cUser.getUserid())
											.withMetricName("CPUUtilization")
											.withNamespace("AWS/EC2")
											.withStatistic("Average")
											.withPeriod(PERIOD)
											.withThreshold(DOWN_THRESHOLD)
											.withComparisonOperator("LessThanOrEqualToThreshold")
											.withDimensions(new Dimension().withName("AutoScalingGroupName=").withValue(cUser.getUserid()))
											.withEvaluationPeriods(1)												
											.withActionsEnabled(true)
											.withAlarmActions(downReturn.getPolicyARN());
		
	System.out.println("Will try to create alarm : " + downAlarm.getAlarmName());
		cloudWatch.putMetricAlarm(downAlarm);
	}
	
	public String createLoadBalancer(User cUser) {
		String lbName = null;
		AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
		AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = new AmazonElasticLoadBalancingClient(credentialsProvider);
		
		
		//amazonElasticLoadBalancingClient.createLoadBalancer();		
		Listener listener = new Listener()
							.withProtocol("TCP")
							.withInstancePort(22)
							.withLoadBalancerPort(1025);
						
	
		Collection<Listener> endpoint = new ArrayList<Listener>();
		endpoint.add(listener);
		
		CreateLoadBalancerListenersRequest balancerListenersRequest = new CreateLoadBalancerListenersRequest()
																		.withLoadBalancerName(cUser.getUserid()+"-lb")
																		.withListeners(endpoint);
		CreateLoadBalancerRequest balancerRequest = new CreateLoadBalancerRequest()
													.withLoadBalancerName(cUser.getUserid()+"-lb")
													.withListeners(endpoint)													
													.withAvailabilityZones(cUser.getVm().getZone());
	System.out.println("Trying to create LB");											
		CreateLoadBalancerResult newLoadBalancer =  amazonElasticLoadBalancingClient.createLoadBalancer(balancerRequest);	
		
		return lbName = cUser.getUserid()+"-lb";
	}
	
	
	
}
