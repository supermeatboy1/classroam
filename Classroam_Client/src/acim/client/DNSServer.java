package acim.client;

import java.io.IOException;
import java.net.*;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

public class DNSServer extends Thread {
	private static final int PORT = 5300;
	private ConnectionThread connThread;
	public DNSServer(ConnectionThread thread) {
		connThread = thread;
	}
	public void run() {
		System.out.println("Starting local DNS server...");
		try (DatagramSocket socket = new DatagramSocket(PORT)) {
			byte[] buffer = new byte[1024];
			while (true) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				try {
	                socket.receive(packet);
	
	                InetAddress clientAddress = packet.getAddress();
	                System.out.println("Received DNS query from: " + clientAddress.getHostAddress());
	
	                Message request = new Message(packet.getData());
	                Record queryRecord = request.getQuestion();
	                String domain = queryRecord.getName().toString();
	                domain = domain.substring(0, domain.length() - 1);
	                System.out.println("Query for: " + domain);
	                connThread.enqueueCommand("domain access " + domain);
	                
	                byte[] responseData = forwardToUpstream(packet.getData());
	                System.out.println("Response length: " + responseData.length);
	                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());
	                socket.send(responsePacket);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
    private static byte[] forwardToUpstream(byte[] queryData) throws IOException {
        try (DatagramSocket upstreamSocket = new DatagramSocket()) {
            InetAddress upstreamAddress = InetAddress.getByAddress(new byte[] {94, (byte) 140,14,14});

            // Send the DNS query to the upstream server
            DatagramPacket upstreamPacket = new DatagramPacket(queryData, queryData.length, upstreamAddress, 53);
            upstreamSocket.send(upstreamPacket);

            // Receive the DNS response
            byte[] responseBuffer = new byte[512];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            upstreamSocket.receive(responsePacket);

            return responsePacket.getData();
        }
    }
	private static Message createResponse(Message request) {
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);

        Record question = request.getQuestion();
        response.addRecord(question, Section.QUESTION);

        try {
            InetAddress ip = InetAddress.getByName("139.59.101.162");
            Record answer = new ARecord(question.getName(), DClass.IN, 60, ip);
            response.addRecord(answer, Section.ANSWER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }
}
