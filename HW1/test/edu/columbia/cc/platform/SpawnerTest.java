/**
 * 
 */
package edu.columbia.cc.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.columbia.cc.user.User;
import edu.columbia.cc.user.VirtualMachine;


public class SpawnerTest {

	List<User> users = new ArrayList<User>();
	Spawner chef = Spawner.INSTANCE;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
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

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		//chef.deleteAll();
		chef.shutdown();
	}

	/**
	 * Test method for {@link edu.columbia.cc.platform.Spawner#commissionVM(edu.columbia.cc.user.User)}.
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testCommissionVM() throws InterruptedException, ExecutionException {
		for (User temp : users) {
			chef.commissionVM(temp);
		}
	}

	/**
	 * Test method for {@link edu.columbia.cc.platform.Spawner#decommissionVM(edu.columbia.cc.user.User)}.
	 */
	@Test
	public void testDecommissionVM() {
		//fail("Not yet implemented");
	}

}
