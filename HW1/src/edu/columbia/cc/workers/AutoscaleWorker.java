package edu.columbia.cc.workers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeletePolicyRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribePoliciesRequest;
import com.amazonaws.services.autoscaling.model.DescribePoliciesResult;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.autoscaling.model.ScalingPolicy;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.Listener;

import edu.columbia.cc.user.User;

public class AutoscaleWorker {
	
	private static final double UP_THRESHOLD=50;
	private static final double DOWN_THRESHOLD=20;
	private static final int PERIOD=60;
	private String stop = "arn:aws:automate:us-east-1:ec2:stop";
	AmazonAutoScalingClient autoScaling = null;
	AmazonCloudWatchClient cloudWatch = null;
	
	public AutoscaleWorker withAutoscale(AmazonAutoScalingClient client) {
		this.autoScaling=client;
		return this;
	}
	
	public AutoscaleWorker withCloudWatch(AmazonCloudWatchClient client) {
		this.cloudWatch = client;
		return this;
	}

	public void setupAutoScale(User cUser)
	{
		try {
		
			
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
															.withMinSize(1);														;
			
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
												.withDimensions(new Dimension().withName("AutoScalingGroupName=")
																	.withValue(cUser.getUserid())
																	.withName("InstanceId").withValue(cUser.getVm().getInstanceId()))															.withEvaluationPeriods(2)												
												.withActionsEnabled(true)
												.withAlarmActions(upReturn.getPolicyARN());
System.out.println("Will try to create alarm : " + upAlarm.getAlarmName());
			cloudWatch.putMetricAlarm(upAlarm);
			
			PutMetricAlarmRequest downAlarm = new PutMetricAlarmRequest()
												.withAlarmName("RemoveCapacity")
												.withMetricName("CPUUtilization")
												.withNamespace("AWS/EC2")
												.withStatistic("Average")
												.withPeriod(PERIOD)
												.withThreshold(DOWN_THRESHOLD)
												.withComparisonOperator("LessThanOrEqualToThreshold")
												.withDimensions(new Dimension().withName("AutoScalingGroupName=")
																	.withValue(cUser.getUserid())
																	.withName("InstanceId").withValue(cUser.getVm().getInstanceId()))
												.withEvaluationPeriods(2)												
												.withActionsEnabled(true)
												.withAlarmActions(downReturn.getPolicyARN());
			
System.out.println("Will try to create alarm : " + downAlarm.getAlarmName());
			cloudWatch.putMetricAlarm(downAlarm);
			
			List<String> actions = new ArrayList<String>();
			actions.add(stop);
			
			ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
			dimensions.add(new Dimension().withName("InstanceId").withValue(cUser.getVm().getInstanceId()));
			
			
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
		} catch (AmazonServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AmazonClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		System.out.println("Created " + newLoadBalancer.getDNSName());	
		return lbName = cUser.getUserid()+"-lb";
	}
	
	public void tearDownAutoScale(User cUser)
	{
		
		try {
				System.out.println("Connected for user : " + cUser.getUserid());
				
			   	DescribeAutoScalingGroupsResult dpr = autoScaling.describeAutoScalingGroups();
				List<AutoScalingGroup> groups = dpr.getAutoScalingGroups();
				for (AutoScalingGroup group : groups)
				{
					String groupN = group.getAutoScalingGroupName();
					if (groupN.contains(cUser.getUserid()))
					System.out.println("Name : " + groupN);
					System.out.println("Removing autoscale group for : " + cUser.getUserid());
					DeleteAutoScalingGroupRequest deleteAutoScalingGroupRequest = new DeleteAutoScalingGroupRequest()
																						.withAutoScalingGroupName(groupN)
																						.withForceDelete(true);
					autoScaling.deleteAutoScalingGroup(deleteAutoScalingGroupRequest);
					
					DescribePoliciesRequest polsRequest = new DescribePoliciesRequest()
															.withAutoScalingGroupName(groupN);
															
					DescribePoliciesResult pols = autoScaling.describePolicies(polsRequest);
					
			    	List<ScalingPolicy> policies = pols.getScalingPolicies();
			    	for (ScalingPolicy policy : policies) {
			    		
			    		DeletePolicyRequest deletePolicyRequest = new DeletePolicyRequest()
																.withPolicyName(policy.getPolicyName())
																.withAutoScalingGroupName(groupN);
			    		autoScaling.deletePolicy(deletePolicyRequest);
					}
				}
				
				
				System.out.println("Getting all alarms.");
				DescribeAlarmsResult alarm = cloudWatch.describeAlarms();
				List<MetricAlarm> alarms = alarm.getMetricAlarms();
				for (MetricAlarm metricAlarm : alarms)
				{
					if (metricAlarm.getAlarmName().contains(cUser.getUserid()))
					{
						System.out.println("Alarm : " + metricAlarm.getAlarmName());
						DeleteAlarmsRequest deleteAlarmsRequest = new DeleteAlarmsRequest()
																.withAlarmNames(metricAlarm.getAlarmName());
						System.out.println("Deleting alarm : " + metricAlarm.getAlarmName());
						cloudWatch.deleteAlarms(deleteAlarmsRequest);
						System.out.println("Alarm deleted.");
					}
					
				}
		} catch (AmazonServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AmazonClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        	
	}
	
	
	
}
