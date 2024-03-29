package serversimpleim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import serversimpleim.datatypes.SimpleIMUser;

import com.tolmms.simpleim.datatypes.ListMessage;
import com.tolmms.simpleim.datatypes.LoginMessage;
import com.tolmms.simpleim.datatypes.LoginMessageAnswer;
import com.tolmms.simpleim.datatypes.LogoutMessage;
import com.tolmms.simpleim.datatypes.Procedures;
import com.tolmms.simpleim.datatypes.RegisterMessage;
import com.tolmms.simpleim.datatypes.RegisterMessageAnswer;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.datatypes.UserInfoAnswerMessage;
import com.tolmms.simpleim.datatypes.UserInfoRequestMessage;
import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class UdpServer extends BaseServer {
    public static final int UDP_BUFFER = 10 * 1024; // 10Kb
    
	private static final int NUMBER_USER_INFO_REQUEST_RETRIES = 5;
	private static final int SECONDS_TO_CHECK_USER_INFO = 15;
    
    DatagramSocket inSocket = null;
    DatagramSocket outSocket = null;
    
    BlockingQueue<DatagramPacket> incomingRequests = null;
    BlockingQueue<DatagramPacket> outgoingRequests = null;

    HandleIncomingPackets handleIncomingPackets = null;
    HandleRequests handleRequests = null;
    HandleOutgoingPackets handleOutgoingPackets = null;
    HandleUserInfoUpdates handleUserInfoUpdates = null;


    public UdpServer() throws IOException {
    	this(4445);
    }


    public UdpServer(int port) throws IOException {
        super();
        inSocket = new DatagramSocket(port);
        outSocket = new DatagramSocket();
        
        try {
			serverUserInfo = new UserInfo(UserInfo.SERVER_USERNAME, inSocket.getLocalAddress().getHostAddress(), inSocket.getLocalPort());
		} catch (InvalidDataException e) {
			if (DEBUG)
				System.out.println("oops... initialising serverUserInfo");
		}
        
        incomingRequests = new LinkedBlockingQueue<>();
        outgoingRequests = new LinkedBlockingQueue<>();
    }

    public void run() {	    	
    	handleIncomingPackets = new HandleIncomingPackets();
    	handleRequests = new HandleRequests();
        handleOutgoingPackets = new HandleOutgoingPackets();
        handleUserInfoUpdates = new HandleUserInfoUpdates();
        

        handleIncomingPackets.start();
        handleRequests.start();
        handleOutgoingPackets.start();
        handleUserInfoUpdates.start();
        
        System.out.println("Server Started");
    }
    
    
	private class HandleIncomingPackets extends Thread {
		private boolean canRun = true;
		private byte[] buf;
		private DatagramPacket packet;

		@Override
		public void run() {
			while (canRun) {
				
				buf = new byte[UDP_BUFFER];
				packet = new DatagramPacket(buf, buf.length);

				try {
					inSocket.receive(packet);
				} catch (IOException e2) {
					if (DEBUG)
						System.out.println("ops... receiving a packet from socket");
					continue;
				}

				incomingRequests.add(packet);
			}
		}
		
		public void stopHandleMessages() {
			canRun = false;
		}
	}
	    
	private class HandleOutgoingPackets extends Thread {
		private boolean canRun = true;

		@Override
		public void run() {
			while (canRun) {
				DatagramPacket dp = null;
				
				try {
					dp = outgoingRequests.take();
				} catch (InterruptedException e1) { }
				
				
				if (dp == null)
					continue;

				try {
					outSocket.send(dp);
				} catch (IOException e) {
					if (DEBUG)
						System.out.println("could not send a message: " + dp.toString());
				}
			}
		}

		public void stopHandleMessages() {
			canRun = false;
		}

	}
	    
	private class HandleRequests extends Thread {
		private boolean canRun = true;

		@Override
		public void run() {
			while (canRun) {
				DatagramPacket dp = null;
				
				try {
					dp = incomingRequests.take();
				} catch (InterruptedException e1) { }
				
				if (dp == null)
					continue;

				String the_msg = new String(dp.getData(), 0, dp.getLength());
				the_msg = the_msg.trim();
				
				String message_type = null;
				try {
					message_type = Procedures.getMessageType(the_msg);
				} catch (XmlMessageReprException e) {
					if (DEBUG)
						System.out.println("Cannot get the message type :( " + e.getMessage());
					continue;
				}

				if (message_type == null) {
					if (DEBUG)
						System.out.println("message type is null");
					continue;
				}

				if (Procedures.isLoginMessage(message_type)) {
					manageLoginRequest(dp);
				} else if (Procedures.isRegisterMessage(message_type)) {
					manageRegisterRequest(dp);
				} else if (Procedures.isLogoutMessage(message_type)) {					
					manageLogoutRequest(dp);
				} else if (Procedures.isUserInfoAnswerMessage(message_type)) {					
					manageUserInfoAnswer(dp);
				}

				// do stuff
			}
		}

		public void stopHandleMessages() {
			canRun = false;
		}
	}
	
	private class HandleUserInfoUpdates extends Thread {
		boolean canRun = true;
		
		@Override
		public void run() {

			while (canRun) {

				Calendar c = Calendar.getInstance();
				Date now = c.getTime();

				for (SimpleIMUser simu : registeredUsers) {
					// now - last >= Seconds to check
					// now >= last + seconds to check

					if (!simu.getUser().isOnline())
						continue;
					
					c.setTime(simu.last_update);
					c.add(Calendar.SECOND, SECONDS_TO_CHECK_USER_INFO * NUMBER_USER_INFO_REQUEST_RETRIES);
					if (now.after(c.getTime())) {
						
						simu.getUser().setOffline();
						
						if (DEBUG)
							System.out.println("HandlerUserInfoUpdates - set "+  simu.getUser() + " offline...");

						continue;
					}
					
					c.setTime(simu.last_update);
					c.add(Calendar.SECOND, SECONDS_TO_CHECK_USER_INFO);
					if (now.after(c.getTime())) {
						
						sendUserInfoRequest(simu);

						if (DEBUG)
							System.out.println("HandlerUserInfoUpdates - send a userInfoRequest... to: " + simu.getUser());
					}
				}
				
				try {
					Thread.sleep(SECONDS_TO_CHECK_USER_INFO * 1000);
				} catch (InterruptedException e) { }
			}
		}

		public void stopHandleMessages() {
			canRun = false;
		}

		
	}
	
	/* 
	 * various requests/messages managers 
	 * 
	 */
	private void sendUserInfoRequest(SimpleIMUser simu) {
		UserInfoRequestMessage uirm = new UserInfoRequestMessage(serverUserInfo);
		
		String uirmXml = null;
		try {
			uirmXml = uirm.toXML();
		} catch (ParserConfigurationException | TransformerException e1) {
			//unlikely to be here
		}
		
		DatagramPacket p;
		try {
			p = new DatagramPacket(uirmXml.getBytes(), uirmXml.getBytes().length, 
												InetAddress.getByName(simu.getUser().getIp()),
												simu.getUser().getPort());
		} catch (UnknownHostException e) {
			/* if there's error... nothing to do */
			return;
		}
		
		outgoingRequests.add(p);
	}
	
	private void manageUserInfoAnswer(DatagramPacket dp) {
		String msg = new String(dp.getData(), 0, dp.getLength());
		UserInfoAnswerMessage uiam;
		
		try {
			uiam = UserInfoAnswerMessage.fromXML(msg);
		} catch (XmlMessageReprException e) {
			//nothing to do...
			return;
		}
		
		UserInfo source = uiam.getSource();
		
		SimpleIMUser simu = getTheUserFromRegistered(source);
		
		if (simu == null)
			return;
		
		simu.getUser().locationData(source.hasLocationData());
		simu.getUser().setAltitude(source.getAltitude());
		simu.getUser().setLatitude(source.getLatitude());
		simu.getUser().setLongitude(source.getLongitude());
		simu.getUser().setIP(source.getIp());
		simu.getUser().setPort(source.getPort());
		simu.getUser().setOnline();
		simu.last_update = Calendar.getInstance().getTime();
		
	}

	private void manageLogoutRequest(DatagramPacket dp) {
		String request = new String(dp.getData(), 0, dp.getLength());
		LogoutMessage lm = null;
		
		try {
			lm = LogoutMessage.fromXML(request);
		} catch (XmlMessageReprException e) {
			return;
		}
		
		UserInfo source = lm.getSource();
		
		SimpleIMUser s = getTheUserFromRegistered(source);
		
		if (s == null)
			return;
		
		s.getUser().setOffline();
		s.last_update = Calendar.getInstance().getTime();
		
	}

	private void manageLoginRequest(DatagramPacket packet) {
    	String request = new String(packet.getData(), 0, packet.getLength());
    	
    	LoginMessage lm = null;
    	UserInfo userOfMessage = null;
    	String password = null;
    	SimpleIMUser s;
    	String answer = null;
    	
    	InetAddress address;
		int port;
    	
    	try {
			lm = LoginMessage.fromXML(request);
		} catch (XmlMessageReprException e1) {
			if (DEBUG) 
        		System.out.println("Login message cannot be serialized :(");
		}
    	
    	userOfMessage = lm.getUser();
    	password = lm.getPassword();
    	
    	if (DEBUG) 
    		System.out.println("Login message recieved from \"" + userOfMessage.getUsername() +"\"");
    	
    	s = getTheUserFromRegistered(userOfMessage);
    	
    	if (s == null || (s != null && !s.samePassword(password))) {
    		if (DEBUG) 
        		System.out.println("Sending REFUSE login to \"" + userOfMessage.getUsername() +"\"");
        	
    		try {
				answer = new LoginMessageAnswer(userOfMessage).toXML();
			} catch (ParserConfigurationException | TransformerException e) {
				if (DEBUG) 
            		System.out.println("ops... should not be here :(");
			}
    		
    		address = packet.getAddress();
    		port = packet.getPort();

    		outgoingRequests.add(new DatagramPacket(answer.getBytes(), answer.getBytes().length, address, port));
    	} else {
    		if (DEBUG) 
        		System.out.println("Sending ACCEPT login to \"" + userOfMessage.getUsername() +"\"");
    		
    		address = packet.getAddress();
    		port = packet.getPort();
    		
    		s.getUser().locationData(userOfMessage.hasLocationData());
    		s.getUser().setAltitude(userOfMessage.getAltitude());
    		s.getUser().setLatitude(userOfMessage.getLatitude());
    		s.getUser().setLongitude(userOfMessage.getLongitude());
    		s.getUser().setIP(address.getHostAddress());
    		s.getUser().setPort(port);
    		s.getUser().setOnline();
    		s.last_update = Calendar.getInstance().getTime();
    		
    		try {
				answer = new LoginMessageAnswer(s.getUser(), String.valueOf(s.getUser().hashCode())).toXML();
			} catch (ParserConfigurationException | TransformerException e) { 
				if (DEBUG) 
            		System.out.println("ops... making a loginAnswer message");
			}
    		
    		outgoingRequests.add(new DatagramPacket(answer.getBytes(), answer.getBytes().length, address, port));
				
    		ListMessage listMessage = new ListMessage();
    		fillListMessage(listMessage, s);
    		
    		try {
				answer = listMessage.toXML();
			} catch (ParserConfigurationException | TransformerException e) { }
    		
    		outgoingRequests.add(new DatagramPacket(answer.getBytes(),  answer.getBytes().length, address, port));
    		
    	}
    }
    
	private void manageRegisterRequest(DatagramPacket dp) {
		String request = new String(dp.getData(), 0, dp.getLength());
		RegisterMessage rm = null;
		String answer = null;
    	
    	try {
			rm = RegisterMessage.fromXML(request);
		} catch (XmlMessageReprException e) {
			if (DEBUG) 
        		System.out.println("Register message cannot be serialized :(");   					
		}
    	
    	if (DEBUG) 
    		System.out.println("Register message recieved from \"" + rm.getUser().getUsername() +"\"");
    	
    	
    	if (registeredUsers.contains(new SimpleIMUser(rm.getUser(), "DUMMY")) || rm.getUser().equals(serverUserInfo)) {
    		if (DEBUG) 
        		System.out.println("Sending REFUSE register to \"" + rm.getUser().getUsername() +"\"");
        	
    		try {
				answer = new RegisterMessageAnswer(rm.getUser(), RegisterMessageAnswer.REFUSED).toXML();
			} catch (ParserConfigurationException | TransformerException e) {
				if (DEBUG) 
            		System.out.println("ops... should not be here :("); 
			} catch (InvalidDataException e) {
				if (DEBUG) 
					System.out.println("ops... should not be here :("); 
			}               		
    	} else {
    		if (DEBUG) 
        		System.out.println("Sending ACCEPT register to \"" + rm.getUser().getUsername() +"\"");
        	
    		rm.getUser().setOffline();
    		addUserToRegisteredUsers(new SimpleIMUser(rm.getUser(), rm.getPassword()));
    		
    		try {
				answer = new RegisterMessageAnswer(rm.getUser(),RegisterMessageAnswer.ACCEPTED).toXML();
			} catch (ParserConfigurationException | TransformerException e) {
				if (DEBUG) 
            		System.out.println("ops... should not be here :("); 
			} catch (InvalidDataException e) {
				if (DEBUG) 
					System.out.println("ops... should not be here :("); 
			}
    	}
    	
		outgoingRequests.add(new DatagramPacket(answer.getBytes(), answer.getBytes().length, dp.getAddress(), dp.getPort()));
	}

    @Override
    protected void finalize() throws Throwable {
    	handleUserInfoUpdates.stopHandleMessages();
    	handleRequests.stopHandleMessages();
    	handleOutgoingPackets.stopHandleMessages();
    	handleIncomingPackets.stopHandleMessages();
    	
    	inSocket.close();
    	outSocket.close();
        super.finalize();
    }

}

