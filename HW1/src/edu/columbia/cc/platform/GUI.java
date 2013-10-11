package edu.columbia.cc.platform;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.columbia.cc.user.User;
import edu.columbia.cc.user.VirtualMachine;


public class GUI extends Frame implements WindowListener, ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static Button createButton;
	public static Button deleteButton;
	public static Button relaunchButton;
	
	List<User> users = new ArrayList<User>();
	Spawner chef = Spawner.INSTANCE;

	public static void main(String[] args)
	{
		GUI gui = new GUI();
//		gui.setSize(new Dimension(500, 200));
    	gui.setTitle("Amazon EC2 client");
    	gui.setVisible(true);
    	
    	
	}
	
	public GUI()
	{
		setLayout(new FlowLayout(FlowLayout.LEFT));
		addWindowListener(this);
		setBackground(Color.GRAY);
		
		createButton = new Button("Create Instances");
    	createButton.addActionListener(this);
    	
    	deleteButton = new Button("Delete Instances");
    	deleteButton.addActionListener(this);
    	
    	relaunchButton = new Button("Relaunch Instances");
    	relaunchButton.addActionListener(this);
    	
    	add(createButton);
    	add(deleteButton);
    	add(relaunchButton);
    	
    	pack();
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
	
	public void createUsers()
	{
		try {
			setUp();
			
			for (User temp : users) {
				chef.commissionVM(temp);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deleteUsers()
	{
		try {
			chef.deleteAll();
//		chef.shutdown();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void updateUsers()
	{
		try {
			chef.relaunchAll();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowClosing(WindowEvent arg0)
	{
		System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		if (arg0.getSource() == createButton)
		{
			try
			{
				createUsers();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (arg0.getSource() == deleteButton)
		{
			try {
				deleteUsers();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (arg0.getSource() == relaunchButton)
		{
			updateUsers();
		}
		
	}

}
