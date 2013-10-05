package edu.columbia.cc.user;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

public class Details
{
//	private String filename = "F:/PRATYUSH/software/eclipse-jee-kepler/workspace/test2/db1.mdb";
	private String filename = "details.txt";
	
	public Details()
	{
		
	}
	
	public User retrieveDetails(User user) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		ObjectInputStream ois = null;
		User tempUser = null;
		boolean streamOpened = false;
		try
		{
			ois = new ObjectInputStream(new FileInputStream(filename));
			streamOpened = true;
			while ((tempUser = (User)ois.readObject()) != null)
			{
				System.out.println("Object Read : " + tempUser.getUserid());
				if (tempUser.getUserid().equalsIgnoreCase(user.getUserid()))
				{
					System.out.println("Entry exists.");
					ois.close();
					return tempUser;
				}
				tempUser = null;
			}
		}
		catch (EOFException e)
		{
			if (streamOpened)
			{
				ois.close();
			}
			System.out.println("All records read.");
		}
		
		return tempUser;
	}
}
