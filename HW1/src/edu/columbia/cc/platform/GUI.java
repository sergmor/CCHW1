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

import edu.columbia.cc.user.User;
import edu.columbia.cc.user.VirtualMachine;


public class GUI extends Frame implements WindowListener, ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static Button createButton;
	public static Button deleteButton;
	
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
		
		createButton = new Button("Create Users");
    	createButton.addActionListener(this);
    	
    	deleteButton = new Button("Delete users");
    	deleteButton.addActionListener(this);
    	
    	add(createButton);
    	add(deleteButton);
    	
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
	
	public void createUsers() throws Exception
	{
		setUp();
		
		for (User temp : users) {
			chef.commissionVM(temp);
		}
	}
	
	public void deleteUsers() throws Exception
	{
		chef.deleteAll();
//		chef.shutdown();
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
		
	}

}
