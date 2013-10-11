README - COMS 6998E Cloud Computing
======================================

For the use of the program the class GUI.java, from this class the main functionality can be invoked in the following fashion:

1-Create Instances.
This button supports the mass creation functionality. As of now, when the button is clicked a total of two users is created. The platform can, however, support large user creation in a concurrent fashion. 
For each user that is created the system will output the actions in the Console. The final user will have the following elements:
- VM (using specified AMI)
- Block Storage
- Amazon Elastic IP
- File created in S3 bucket
- AutoScaling group
  - Alarms for end of day
  - Alarms for CPU utilisation peak (upScale+downScale)
- Elastic Load Balancer to manage the Autoscaling sessions

Once the process is finished the system guarantees the user can connect through SSH to the created instance, since this is part of the creation verification process. 


2- Delete Instance.
By clicking on this button the instances associated to the different users will be:
- Persisted into a snapshot, creating an AMI
Also the following cleanup operations will be performed:
- Volume detaching.
- Instance termination

The rest of the infrastructure remains in place, since it is will be reused once the instances are relaunched.

3- Relaunch Instance.
This operation restores the user instances to the saved state of the snapshot taken when deleting them. The following conditions are met:
- Elastic IPs are the same as when the instances were first created
- Block device mapping are consistent to the user specifications
- User IDs are maintained throughout the different operations

4- Autoscaling behaviour.
In order to test the autoscaling it is necessary to connect through SSH to the instance and create a peak for the CPU Utilisation. For this we provide the following Python script

SCRIPT
*************************
a=1
b=1
while 1:
    c=a+b
    a=b
    b=c
    print c
*************************

Thanks!

Rituparna
Pratyush
Daniel
