import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3Client;

import edu.columbia.cc.platform.Spawner;
import edu.columbia.cc.user.User;
import edu.columbia.cc.user.VirtualMachine;
import edu.columbia.cc.workers.Ec2Worker;


public class TestEc2Worker {
	
	List<User> users = new ArrayList<User>();
	Spawner chef = Spawner.INSTANCE;

	public static void main(String[] args) throws Exception
	{
		// TODO Auto-generated method stub
		TestEc2Worker t = new TestEc2Worker();
		t.execute();

	}
	
	public  void execute() throws Exception
	{
		createUsers();
		
		
//		createNew();
//		delete();
//		relaunch();
	}
	
	public void setUp() throws Exception
	{
		
		for(int i=0; i<2; i++) {
			User temp = new User();
			long id = System.currentTimeMillis();
			id += i*100;
			temp.setId(id);
			temp.setUserid(Long.toString(temp.getId()));
			VirtualMachine vm = new VirtualMachine();
			vm.setInstanceType("t1.micro");
			temp.setVm(vm);
			temp.setAmi_id("ami-76f0061f");
			users.add(temp);
		}
		chef.powerOn();
		
	}


	public void createUsers() throws Exception
	{
		setUp();
		
		for (User temp : users) {
			chef.commissionVM(temp);
		}
	}
	
	public void createNew() throws IOException
	{
		AWSCredentials credentials = new PropertiesCredentials(
	   			 this.getClass().getResourceAsStream("AwsCredentials.properties"));

	        AmazonEC2Client ec2 = new AmazonEC2Client(credentials);
	        AmazonS3Client s3 = new AmazonS3Client(credentials);
			
			VirtualMachine vm = new VirtualMachine();
			vm.setInstanceType("t1.micro");
			
			User user = new User();
			user.setAmi_id("ami-76f0061f");
			user.setUserid("12345");
			user.setVm(vm);
			
			Ec2Worker ec2Worker = new Ec2Worker()
								.withUser(user)
								.withCloud(ec2)
								.withS3(s3);
			
			ec2Worker.processCreateRequest();
	}
	
	public void delete() throws IOException
	{
		AWSCredentials credentials = new PropertiesCredentials(
	   			 this.getClass().getResourceAsStream("AwsCredentials.properties"));

	        AmazonEC2Client ec2 = new AmazonEC2Client(credentials);
	        AmazonS3Client s3 = new AmazonS3Client(credentials);
			
			VirtualMachine vm = new VirtualMachine();
			vm.setInstanceType("t1.micro");
			vm.setExtraVolumeId("vol-b158fbf2");
			vm.setInstanceId("i-be35a4c3");
			vm.setPrimaryVolumeId("vol-ee59faad");
			vm.setPublicIp("54.205.167.63");
			vm.setZone("us-east-1a");
			
			User user = new User();
			user.setAmi_id("ami-76f0061f");
			user.setElasticIp("184.73.241.235");
			user.setUserid("12345");
			user.setKeyName("12345_key");
			user.setSecurityGroupName("12345_group");
			user.setVm(vm);
			
			Ec2Worker ec2Worker = new Ec2Worker()
								.withUser(user)
								.withCloud(ec2)
								.withS3(s3);
			
			ec2Worker.processDeleteRequest();
	}
	
	public void relaunch() throws IOException
	{
		AWSCredentials credentials = new PropertiesCredentials(
	   			 this.getClass().getResourceAsStream("AwsCredentials.properties"));

	        AmazonEC2Client ec2 = new AmazonEC2Client(credentials);
	        AmazonS3Client s3 = new AmazonS3Client(credentials);
			
			VirtualMachine vm = new VirtualMachine();
			vm.setInstanceType("t1.micro");
			vm.setExtraVolumeId("vol-b158fbf2");
			vm.setInstanceId("i-be35a4c3");
			vm.setPrimaryVolumeId("vol-ee59faad");
			vm.setPublicIp("54.205.167.63");
			vm.setZone("us-east-1a");
			
			User user = new User();
			user.setAmi_id("ami-913363f8");
			user.setElasticIp("184.73.241.235");
			user.setUserid("12345");
			user.setKeyName("12345_key");
			user.setSecurityGroupName("12345_group");
			user.setVm(vm);
			
			Ec2Worker ec2Worker = new Ec2Worker()
								.withUser(user)
								.withCloud(ec2)
								.withS3(s3);
			
			ec2Worker.processRelaunchRequest();
	}

}
