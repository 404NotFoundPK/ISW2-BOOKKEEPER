package org.apache.bookkeeper.examTests;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;



public final class MockDNS{
	private Thread thread = null;
    private static volatile boolean running = false;
    private static final int UDP_SIZE = 512;
    private final int port;
    private int requestCount = 0;
    private static volatile int userCounter=0;

    private static volatile MockDNS INSTANCE;


    private MockDNS(int port) {
        this.port = port;
    }

    public static synchronized boolean isRunning()
    {
    	return running;
    }
    public static synchronized boolean isUp()
    {
    	if(INSTANCE != null)
    		return true;
    	else
    		return false;
    }
    public static synchronized MockDNS getInstance(int port)
    {
    	if(INSTANCE == null) {
    		INSTANCE = new MockDNS(port);
    	}
    	return INSTANCE;
    }

    private synchronized void addCounter()
    {
    	userCounter++;
    }

    public void start() {
    	addCounter();
    	if(userCounter == 1)
    	{
            running = true;
            thread = new Thread(() -> {
                try {
                    serve();
                } catch (IOException ex) {
                    stop();
                    throw new RuntimeException(ex);
                }
            });
            thread.start();
    	}
    }

    public synchronized void stop() {
    	userCounter--;
    	if(userCounter == 0)
    	{
            running = false;
            thread.interrupt();
            thread = null;
    	}

    }

    public  int getRequestCount() {
        return requestCount;
    }

    private void serve() throws IOException {
        DatagramSocket socket = new DatagramSocket(port);
        while (running) {
            process(socket);
        }
    }

	private void process(DatagramSocket socket) throws IOException {
        byte[] in = new byte[UDP_SIZE];

        // Read the request
        DatagramPacket indp = new DatagramPacket(in, UDP_SIZE);
        socket.receive(indp);
        ++requestCount;
        System.out.println(String.format("processing... %d", requestCount));

        // Build the response
        Message request = new Message(in);
        //Setto l'header per indicare che è un pacchetto di risposta.
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.addRecord(request.getQuestion(), Section.QUESTION);

        if(request.getQuestion().getName().toString().contains("8.8.8.8"))
        	response.addRecord(Record.fromString(request.getQuestion().getName(), Type.PTR, DClass.IN, 86400, "mock.name", Name.root), Section.ANSWER);

        byte[] resp = response.toWire();
        DatagramPacket outdp = new DatagramPacket(resp, resp.length, indp.getAddress(), indp.getPort());
        socket.send(outdp);
    }

	public static void main(String args[])
	{
		MockDNS.getInstance(53).start();
	}

}
