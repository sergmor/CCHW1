package edu.columbia.cc.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.ImageState;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import edu.columbia.cc.user.User;

public class Ec2Worker
{
	private AmazonEC2Client cloud = null;
	private AmazonS3Client s3 = null;
	private User user = null;
	
	public Ec2Worker() {}
	
	public Ec2Worker withUser(User user)
	{
		this.user = user;
		return this;
	}
	
	public Ec2Worker withCloud(AmazonEC2Client cloud)
	{
		this.cloud = cloud;
		return this;
	}
	
	public Ec2Worker withS3(AmazonS3Client s3)
	{
		this.s3 = s3;
		return this;
	}
	
	public User processCreateRequest() throws IOException
	{
		createKeyPair();
		createSecurityGroup();
		addRulesToSecurityGroup();
		createInstance();
		createAndAssociateElasticIp();
		createS3Bucket();
		createAndAttachExtraVolume();
		
		System.out.println("\n\nDone creating new instance.");
		
		return this.user;
	}
	
	public User processRelaunchRequest() throws IOException
	{
		createInstance();
		associateExistingElasticIp();
		updateS3Bucket();
		deregisterExistingImage();
		deletePrimarySnapshot();
		attachExtraVolume();
		
		System.out.println("\nDone restoring from existing AMI.");
		return this.user;
	}
	
	public User processDeleteRequest()
	{
		detachExtraVolume();
		createPrimarySnapshot();
		deleteInstance();
		
		System.out.println("\nDone deleting instance.");
		return this.user;
	}
	
	private void createAndAttachExtraVolume()
	{
		try
		{
			System.out.println("Trying to create volume ...");
			CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest()
							.withAvailabilityZone(user.getVm().getZone())
							.withSize(1)
							.withVolumeType("standard");
			CreateVolumeResult createVolumeResult = cloud.createVolume(createVolumeRequest);
			System.out.println("Request completed.");
			
			String volumeId = createVolumeResult.getVolume().getVolumeId();
			System.out.println("Created volume with ID: " + volumeId);
			this.user.getVm().setExtraVolumeId(volumeId);
			
			String instanceId = this.user.getVm().getInstanceId();
			System.out.println("Trying to attach volume to instance with instanceId: " + instanceId);
			AttachVolumeRequest attachVolumeRequest = new AttachVolumeRequest();
			attachVolumeRequest.setVolumeId(volumeId);
			attachVolumeRequest.setInstanceId(instanceId);
			attachVolumeRequest.setDevice("/dev/sdf");
			this.cloud.attachVolume(attachVolumeRequest);
			
			System.out.println("Successfully attached.");
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
	}
	
	public void deregisterExistingImage()
	{
		try
		{
			String imageId = this.user.getAmi_id();
			System.out.println("Trying to de-register ami with amiId : " + imageId);
			DeregisterImageRequest deregisterImageRequest = new DeregisterImageRequest()
												.withImageId(imageId);
			this.cloud.deregisterImage(deregisterImageRequest);
			System.out.println("Done.");
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
		
	}
	
	private void deletePrimarySnapshot()
	{
		String snapshotId = "";
		String primaryVolumeId = this.user.getVm().getPrimaryVolumeId();
		String status = "completed";
		
		try
		{
			Filter[] filters = new Filter[2];
			filters[0] = new Filter().withName("volume-id").withValues(primaryVolumeId);
			filters[1] = new Filter().withName("status").withValues(status);

			DescribeSnapshotsRequest describeSnapshotsRequest = new DescribeSnapshotsRequest().withFilters(filters);
			DescribeSnapshotsResult describeSnapshotResult = cloud.describeSnapshots(describeSnapshotsRequest);

			Snapshot snapshot = describeSnapshotResult.getSnapshots().get(0);
			snapshotId = snapshot.getSnapshotId();
			
			System.out.println("Trying to delete snapshot with snapshotId:" + snapshotId);
			DeleteSnapshotRequest deleteSnapshotRequest = new DeleteSnapshotRequest()
												.withSnapshotId(snapshotId);
			this.cloud.deleteSnapshot(deleteSnapshotRequest);
			System.out.println("Request sent.");
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
	}
	
	
	private void attachExtraVolume()
	{
		try
		{
			String instanceId = this.user.getVm().getInstanceId();
			String extraVolumeId = this.user.getVm().getExtraVolumeId();
			
			System.out.println("Trying to attach volume to instance with instanceId: " + instanceId);
			AttachVolumeRequest attachVolumeRequest = new AttachVolumeRequest();
			attachVolumeRequest.setVolumeId(extraVolumeId);
			attachVolumeRequest.setInstanceId(instanceId);
			attachVolumeRequest.setDevice("/dev/sdf");
			this.cloud.attachVolume(attachVolumeRequest);
			
			System.out.println("Successfully attached.");
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	private KeyPair createKeyPair() throws AmazonServiceException
	{
		/*Generate a unique keyName*/
		String keyName = this.user.getUserid() + "_key";
		/*Bind the keyName to the user.*/
		this.getUser().setKeyName(keyName);

		try
        {
        	System.out.println("Creating a Key Pair : " + keyName);
        	CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest().withKeyName(keyName);
        	CreateKeyPairResult createKeyPairResult = this.cloud.createKeyPair(createKeyPairRequest);
        	System.out.println("Created the Key Pair.");
        	
        	String keyMaterial = createKeyPairResult.getKeyPair().getKeyMaterial();
        	System.out.println("RSA private key:");
        	System.out.println(keyMaterial);
        	
        	savePrivateKeyFile(keyMaterial);
		}
        catch (AmazonServiceException  e)
		{
			e.printStackTrace();
			throw e;
		}
		return null;
	}
	
	public void savePrivateKeyFile(String str)
    {
		String privateKeyFileName = this.user.getKeyName()+".pem";
    	try
    	{
    		System.out.println("Saving private key in : " + privateKeyFileName);
			BufferedWriter bw = new BufferedWriter(new PrintWriter(privateKeyFileName));
			bw.write(str);
			bw.close();
			System.out.println("File saved.");
		}
    	catch (FileNotFoundException e)
    	{
			e.printStackTrace();
		}
    	catch (IOException e)
    	{
			e.printStackTrace();
		}
    }
	
	public void createSecurityGroup() throws AmazonServiceException
    {
		/*Generate a unique securityGroupName.*/
		String securityGroupName = this.user.getUserid() + "_group";
		
		/*Bind the securityGroupName to the user.*/
		this.getUser().setSecurityGroupName(securityGroupName);
		
    	try
        {
        	System.out.println("Creating a security group : " + securityGroupName);
			CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest()
																	.withGroupName(securityGroupName)
																	.withDescription(securityGroupName);
			this.cloud.createSecurityGroup(createSecurityGroupRequest);
			System.out.println("Created the security group.");
			
		}
        catch (AmazonServiceException  e)
		{
			e.printStackTrace();
			throw e;
		}
    }
	
	public void addRulesToSecurityGroup() throws AmazonServiceException
    {
    	String ipAddr = "0.0.0.0/0";
    	String securityGroupName = this.user.getSecurityGroupName();
    	
        // Create a range of IP's to populate.
        List<String> ipRanges = new ArrayList<String>();
        ipRanges.add(ipAddr);

        // Open up port 22 for TCP traffic to the associated IP from
        ArrayList<IpPermission> ipPermissions = new ArrayList<IpPermission> ();
        IpPermission ipPermissionSSH = new IpPermission()
        									.withIpProtocol("tcp")
        									.withFromPort(new Integer(22))
        									.withToPort(new Integer(22))
        									.withIpRanges(ipRanges);
        
        ipPermissions.add(ipPermissionSSH);

        try
        {
        	System.out.println("Adding rules to group : " + securityGroupName);
            AuthorizeSecurityGroupIngressRequest ingressRequest =
                new AuthorizeSecurityGroupIngressRequest()
            			.withGroupName(securityGroupName)
            			.withIpPermissions(ipPermissions);
            this.cloud.authorizeSecurityGroupIngress(ingressRequest);
            System.out.println("Rules added.");
        }
        catch (AmazonServiceException e)
        {
        	e.printStackTrace();
        	throw e;
        }
    }
	
	/* to create an instance, following attributes should be known beforehand:
	 * - imageID : eg. 'ami-76f0061f' (32bit amazon linux AMI)
	 * - instanceType : eg. 't1.micro'
	 * - keyName : eg. 'user1_key1'
	 * - securityGroupName : eg. 'group1'
	 * - privateKeyFileName : eg. 'user1_key1.pem', this will be written locally and given to user for login.
	 * - */
	
	/*
	 * After creation, the method will give back the following information:
	 * - instanceID :
	 * - publicIP : 
	 * - privateIP :
	 */
	private void createInstance() throws AmazonServiceException, AmazonClientException
    {
		String instanceId = "";
		try
		{
			String imageId = this.user.getAmi_id();
			String instanceType = this.user.getVm().getInstanceType();
			String securityGroupName = this.user.getSecurityGroupName();
			String keyName = this.user.getKeyName();
			
			System.out.println("\nRequesting a new instance of imageID : " + imageId + ", instanceType : " + instanceType);
			RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
														.withImageId(imageId)
														.withInstanceType(instanceType)
														.withMinCount(1)
														.withMaxCount(1)
														.withSecurityGroups(securityGroupName)
														.withKeyName(keyName);
			
			RunInstancesResult runInstancesResult = this.cloud.runInstances(runInstancesRequest);
			instanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
			System.out.println("Request complete.");
			
			Instance requestedInstance = null;
			while (true)
			{
				System.out.println("Checking state ...");
				DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
				DescribeInstancesResult describeInstancesResult = this.cloud.describeInstances(describeInstancesRequest);
				requestedInstance = describeInstancesResult.getReservations().get(0).getInstances().get(0);
				if (requestedInstance.getState().getName().equalsIgnoreCase(InstanceStateName.Running.name()))
				{
					System.out.println(requestedInstance.getState().getName());
					break;
				}
				else
				{
					System.out.println("Still " + requestedInstance.getState().getName());
					System.out.println("Sleeping for 5s ...");
					try{Thread.sleep(5 * 1000);}catch(InterruptedException e){e.printStackTrace();}
				}
			}
			
			System.out.println("New instance created: " + requestedInstance.toString());
			
			/* Fill other details of the user object, using details from the new instance */
			this.user.getVm().setInstanceId(instanceId);
			this.user.getVm().setPrimaryVolumeId(requestedInstance.getBlockDeviceMappings().get(0).getEbs().getVolumeId());
			this.user.getVm().setPublicIp(requestedInstance.getPublicIpAddress());
			this.user.getVm().setZone(requestedInstance.getPlacement().getAvailabilityZone());
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
			throw e;
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
			throw e;
		}
		boolean isInitialized = false;
		while(!isInitialized) {
			isInitialized = true;
				try
				{
					checkLogin(this.user.getKeyName()+".pem");
				}
				catch (IOException e)
				{
					// There's something wrong with key file
					e.printStackTrace();
					isInitialized = false;
				}
				catch (JSchException e)
				{
					// The machine is not initialized yet
					System.out.println("Still unable to connect.");
					System.out.println("Sleeping for 10s ...");
					isInitialized = false;
					try{Thread.sleep(10 * 1000);}catch(InterruptedException e1){e1.printStackTrace();}
					
				}
				if(isInitialized) break;					
		}
    }
	
	private void createAndAssociateElasticIp()
	{
		try
		{
			System.out.println("Trying to get an elastic IP for the instance ...");
			AllocateAddressRequest allocateAddressRequest = new AllocateAddressRequest();
			AllocateAddressResult allocateAddressResult = this.cloud.allocateAddress(allocateAddressRequest);
			System.out.println("Request sent.");
			
			String elasticIp = allocateAddressResult.getPublicIp();
			String instanceId = this.user.getVm().getInstanceId();
			System.out.println("Elastic IP received in response : " + elasticIp);
			
			System.out.println("Trying to associate elastic IP '" + elasticIp + "' with instanceId '" + instanceId + "'");
			AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest(instanceId, elasticIp);
			this.cloud.associateAddress(associateAddressRequest);
			System.out.println("Request sent.");
			
			this.user.setElasticIp(elasticIp);
			System.out.println("Saved the elastic IP with the User.");
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
	}
	
	private void associateExistingElasticIp()
	{
		String elasticIp = this.user.getElasticIp();
		String instanceId = this.user.getVm().getInstanceId();
		
		System.out.println("Existing elastic IP for the user '" + this.user.getUserid() + "' is " + this.user.getElasticIp());
		System.out.println("Trying to associate elastic IP '" + elasticIp + "' with instanceId '" + instanceId + "'");
		AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest(instanceId, elasticIp);
		this.cloud.associateAddress(associateAddressRequest);
		System.out.println("Request sent.");
	}
	
	private void detachExtraVolume()
	{
		try
		{
			String extraVolumeId = this.user.getVm().getExtraVolumeId();
			String instanceId = this.user.getVm().getInstanceId();
			
			System.out.println("Attempting to detach volume: " + extraVolumeId + " from instance with instanceId: " + instanceId);
			DetachVolumeRequest detachVolumeRequest = new DetachVolumeRequest()
										.withVolumeId(extraVolumeId)
										.withInstanceId(instanceId);
			cloud.detachVolume(detachVolumeRequest);
			System.out.println("Successfully detached.");
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
	}
	
	private void deleteInstance()
	{
		String instanceId = this.user.getVm().getInstanceId();
		try
		{
			System.out.println("\nRequesting termination of instance with instanceId : " + instanceId);
			TerminateInstancesRequest tir = new TerminateInstancesRequest().withInstanceIds(instanceId);
			this.cloud.terminateInstances(tir);
			System.out.println("Request complete.");
			
			Instance requestedInstance = null;
			while (true)
			{
				System.out.println("Checking state ...");
				DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
				DescribeInstancesResult describeInstancesResult = this.cloud.describeInstances(describeInstancesRequest);
				requestedInstance = describeInstancesResult.getReservations().get(0).getInstances().get(0);
				if (requestedInstance.getState().getName().equalsIgnoreCase(InstanceStateName.Terminated.name()))
				{
					System.out.println(requestedInstance.getState().getName());
					break;
				}
				else
				{
					System.out.println("Still " + requestedInstance.getState().getName());
					System.out.println("Sleeping for 5s ...");
					try{Thread.sleep(5 * 1000);}catch(InterruptedException e){e.printStackTrace();}
				}
			}
			
			System.out.println("Done.");
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
	}
	
	private void createPrimarySnapshot()
	{
		try
		{
			String instanceId = this.user.getVm().getInstanceId();
			String amiName = this.user.getUserid() + "_ami";
			System.out.println("Attempting to create AMI out of instance with instanceId : ");
			CreateImageRequest createImageRequest = new CreateImageRequest()
									.withInstanceId(instanceId)
									.withName(amiName);
			CreateImageResult createImageResult = this.cloud.createImage(createImageRequest);
			System.out.println("Request sent.");
			String imageId = createImageResult.getImageId();
			System.out.println("amiID of requested image : " + imageId);
			
			while (true)
			{
				DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest()
												.withImageIds(imageId);
				DescribeImagesResult describeImagesResult = cloud.describeImages(describeImagesRequest);
				String state = describeImagesResult.getImages().get(0).getState();
				if (state.equalsIgnoreCase(ImageState.Available.name()))
				{
					System.out.println(state);
					break;
				}
				else
				{
					System.out.println("Still " + state);
					System.out.println("Sleeping for 10s ...");
					try{Thread.sleep(10 * 1000);} catch (InterruptedException e) {e.printStackTrace();}
				}
			}
			
			this.user.setAmi_id(imageId);
			System.out.println("Done creating AMI with ID : " + imageId);
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
	}
	
	private void checkLogin(String keyPath) throws IOException, JSchException {
       	
		JSch jsch=new JSch();
       	jsch.addIdentity(keyPath);
       	jsch.setConfig("StrictHostKeyChecking", "no");

       	//enter your own EC2 instance IP here
       	Session session=jsch.getSession("ec2-user", this.user.getVm().getPublicIp(), 22);

       	System.out.println("Attempting to connect ...");
       	
       	session.connect();
       	System.out.println("Connected");
       	//run stuff
       	String command = "whoami;hostname";
       	Channel channel=session.openChannel("exec");
           ((ChannelExec)channel).setCommand(command);
           channel.setInputStream(null);
           ((ChannelExec)channel).setErrStream(System.err);
           
           InputStream in=channel.getInputStream();
      
           channel.connect();
      
       	 byte[] tmp=new byte[1024];
            while(true){
              while(in.available()>0){
                int i=in.read(tmp, 0, 1024);
                if(i<0)break;
                System.out.print(new String(tmp, 0, i));
              }
              if(channel.isClosed()){
                System.out.println("exit-status: "+channel.getExitStatus());
                break;
              }
              try{Thread.sleep(1000);}catch(Exception ee){}
            }
            channel.disconnect();
            session.disconnect();
          
       }
	
	private void createS3Bucket() throws IOException
	{
		String userId = this.user.getUserid();
		String bucketName = userId + "-bucket";
		String bucketKeyName = userId + "-bucket-key";
		String dummyData = "dummy-data";
		
		try
		{	    
	        //create bucket
			s3.createBucket(bucketName);	                      
			
			//set value
			File file = File.createTempFile(userId, ".txt");
			file.deleteOnExit();
			Writer writer = new OutputStreamWriter(new FileOutputStream(file));
			writer.write(dummyData);
			writer.close();
			
			//put object - bucket, key, value(file)
			s3.putObject(new PutObjectRequest(bucketName, bucketKeyName, file));
			
            //get object
            System.out.println("Reading data from S3....");				
            S3Object object = s3.getObject(new GetObjectRequest(bucketName, bucketKeyName));
            BufferedReader reader = new BufferedReader(
            	    new InputStreamReader(object.getObjectContent()));
            String data = null;
            while ((data = reader.readLine()) != null)
            {
                System.out.println(data);
            }
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
	}


	private void updateS3Bucket() throws IOException
	{
		String userId = this.user.getUserid();
		String bucketName = userId + "-bucket";
		String bucketKeyName = userId + "-bucket-key";
		String dummyData = "dummy-data";
		
		try
		{	    	                      			
			//set value
			File file = File.createTempFile(userId, ".txt");
			file.deleteOnExit();
			Writer writer = new OutputStreamWriter(new FileOutputStream(file));
			writer.write(dummyData);
			writer.close();
			
			//put object - bucket, key, value(file)
			s3.putObject(new PutObjectRequest(bucketName, bucketKeyName, file));
			
            //get object
            System.out.println("Reading data from S3....");				
            S3Object object = s3.getObject(new GetObjectRequest(bucketName, bucketKeyName));
            BufferedReader reader = new BufferedReader(
            	    new InputStreamReader(object.getObjectContent()));
            String data = null;
            while ((data = reader.readLine()) != null)
            {
                System.out.println(data);
            }
			
		}
		catch (AmazonServiceException e)
		{
			e.printStackTrace();
		}
		catch (AmazonClientException e)
		{
			e.printStackTrace();
		}
	}	
	

	public AmazonEC2Client getCloud() {
		return cloud;
	}

	public void setCloud(AmazonEC2Client cloud) {
		this.cloud = cloud;
	}

	public AmazonS3Client getS3() {
		return s3;
	}

	public void setS3(AmazonS3Client s3) {
		this.s3 = s3;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
