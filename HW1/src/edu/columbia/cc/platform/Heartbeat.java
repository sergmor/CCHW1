package edu.columbia.cc.platform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import edu.columbia.cc.user.User;

public class Heartbeat implements Runnable {

	
	
	private Map<String,Future<User>> usersInProgress;
	
	private Map<String,User> usersFinished= new HashMap<String, User>();
	
	@Override
	public void run() {
		System.out.println("***HB");
		this.usersInProgress = Spawner.INSTANCE.getUsersInProgress();		
		Set<String> keys = usersInProgress.keySet();
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			String id = iterator.next();
			Future<User> temp = usersInProgress.get(id);
			if(temp.isDone()) {
				try {
					usersFinished.put(id,temp.get());
				} catch (InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					Spawner.INSTANCE.resetHeartBeat();
					e.printStackTrace();
				}
			
			}
		}
		Spawner.INSTANCE.updateUser(usersFinished);


	}

	public Map<String, Future<User>> getUsersInProgress() {
		return usersInProgress;
	}

	public void setUsersInProgress(Map<String, Future<User>> usersInProgress) {
		this.usersInProgress = usersInProgress;
	}
	
	

}
