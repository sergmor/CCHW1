package edu.columbia.cc.platform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.columbia.cc.user.User;
import edu.columbia.cc.workers.VMWorker;

public enum Spawner {
	/**
	 *Spawner is responsible for :
	 * - Threaded execution of VM creation
	 * - Starting VM Commission
	 * - Starting VM Decommission
	 * - Starting VM Elastic Behavior
	 */
	INSTANCE;
	private static final int N_WORKERS = 10;
	private static final int timeout = 2;
	private ExecutorService chief = Executors.newFixedThreadPool(N_WORKERS);
	private ScheduledExecutorService beat = Executors.newScheduledThreadPool(1);
	private Map<String,User> users = new HashMap<String, User>(); 
	private Map<String,Future<User>> usersInProgress = new HashMap<String, Future<User>>();
	
	
	/**
	 * Takes in a new user, creates a VM and returns the details
	 * @param cUser User DataObject
	 * @return User dataObject updated
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void commissionVM(User cUser) throws InterruptedException, ExecutionException {
		VMWorker vmWorker = new VMWorker(); 
		vmWorker.setCommand(Action.CREATE);
		vmWorker.setUser(cUser);
		Future<User> nUser = chief.submit(vmWorker);
		String id = cUser.getUserid() +":"+ Action.CREATE.toString();
		usersInProgress.put(id, nUser);
		
	}
	
	public void decommissionVM(User cUser) throws InterruptedException, ExecutionException {
		VMWorker vmWorker = new VMWorker();
		vmWorker.setCommand(Action.DELETE);
		vmWorker.setUser(cUser);
		Future<User> nUser = chief.submit(vmWorker);		
		String id = cUser.getUserid() +":"+ Action.DELETE.toString();
		usersInProgress.put(id, nUser);		
	}
	
	public void updateVM(User cUser) {
		VMWorker vmWorker = new VMWorker();
		vmWorker.setCommand(Action.RELAUNCH);
		vmWorker.setUser(cUser);
		Future<User> nUser = chief.submit(vmWorker);		
		String id = cUser.getId() +":"+ Action.RELAUNCH.toString();
		usersInProgress.put(id, nUser);	
	}
	
	/**
	 * Convenience method to kill threads and Executors
	 * @throws InterruptedException 
	 */
	public void powerOn() {
		Heartbeat hb = new Heartbeat();
		hb.setUsersInProgress(usersInProgress);
		beat.scheduleWithFixedDelay(hb, 1, 1, TimeUnit.MINUTES);
	}
	
	public void shutdown() throws InterruptedException {
		chief.shutdown();
		chief.awaitTermination(timeout,TimeUnit.MINUTES);		
	}
	
	public void resetHeartBeat() {
		beat.scheduleWithFixedDelay(new Heartbeat(), 1, 1, TimeUnit.MINUTES);
	}
	
	public void updateUser(Map<String,User> processed) {
		
		Set<String> keys = processed.keySet();
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			String id = iterator.next();
			if(usersInProgress.containsKey(id)) {
				User user = processed.get(id);
				if(id.contains(Action.CREATE.toString())) {
					users.put(user.getUserid(), user);
					System.out.println("***Spawner: Created final User" + user.getUserid());
				}
				else if (id.contains(Action.DELETE.toString())) {
					//users.remove(user.getUserid());
				}
				System.out.println("***Spawner: Removing from map " + id);
				usersInProgress.remove(id);
			}
		}
	}
	
	public void batchCreate(List<User> users) throws InterruptedException, ExecutionException {
		for (User temp: users) {
			this.commissionVM(temp);
		}
	}
	
	public void deleteAll() throws InterruptedException, ExecutionException {
		Set<String> keys = users.keySet();
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			String id = iterator.next();
			System.out.println("***Spawner: will decomission from final" + id);
			this.decommissionVM(users.get(id));
		}
	}
	
	public void relaunchAll() throws InterruptedException, ExecutionException {
		Set<String> keys = users.keySet();
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			String id = iterator.next();
			this.updateVM(users.get(id));
		}
	}
	
	public synchronized Map<String,Future<User>> getUsersInProgress(){
		return usersInProgress;
	}
	
}
