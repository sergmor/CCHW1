package edu.columbia.cc.workers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;


public class S3Worker { 
			
	static AmazonS3Client s3;
	
	public void createS3 (AWSCredentials credentials, String InsID, String bucketName, String key) throws Exception {
		 
	    try {
	    		//AWSCredentials credentials = new PropertiesCredentials(S3Worker.class.getResourceAsStream("AwsCredentials.properties"));
	    
	    	    /************************************************
	            *    #3 S3 bucket and object
	            ***************************************************/
	    	       
	    		s3  = new AmazonS3Client(credentials);
	            
	            //Check if the bucket Key-pair already exists, if yes then add to same bucket
	            S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
	            if (object != null)
	            	{
	            	    //Current data on existing bucket
	            		BufferedReader reader = new BufferedReader(
	                	new InputStreamReader(object.getObjectContent()));
	            		String data = null;
	            		while ((data = reader.readLine()) != null) 
	            		{
	            			System.out.println(data);
	            		}
	                
	            		//set value , Use Instance ID to create the file name
		            	File file = File.createTempFile(InsID, ".txt");
		            	file.deleteOnExit();
		            	Writer writer = new OutputStreamWriter(new FileOutputStream(file));
		            	writer.write("This is a sample file added to existing bucket.\r\nYes!");
		            	writer.close();
		            
		            	//put object - bucket, key, value(file) 
		            	s3.putObject(new PutObjectRequest(bucketName, key, file));	            	
	            	}
	            else 
	            	{	            		           
	            		//create bucket
	            		s3.createBucket(bucketName);	                      
	            
	            		//set value
	            		File file = File.createTempFile(InsID, ".txt");
	            		file.deleteOnExit();
	            		Writer writer = new OutputStreamWriter(new FileOutputStream(file));
	            		writer.write("This is a sample sentence.\r\nYes!");
	            		writer.close();
	            
	            		//put object - bucket, key, value(file)
	            		s3.putObject(new PutObjectRequest(bucketName, key, file));	            	           
	            	}
	            
	            /*********************************************
	             *  #4 shutdown client object
	             *********************************************/
	            s3.shutdown();	                        
	            
	    }catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
	   	}
	}
	
}
