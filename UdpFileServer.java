import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class UdpFileServer
{
    private DatagramSocket socket;// UDP socket for communication
    private final int port;// Server port
    private final int bufferSize; // Buffer size for data transfer
    private final String fileDirectory;// Directory where files are stored
    public UdpFileServer(int port, int bufferSize, String fileDirectory) throws IOException
    {
        this.port = port;
        this.bufferSize = bufferSize;
        this.fileDirectory = fileDirectory;
        this.socket = new DatagramSocket(null);
        socket.setReuseAddress(true);// Allow address reuse
        socket.bind(new InetSocketAddress(port));// Bind to port
        System.out.println("File server started on port: " + port);
        System.out.println("File directory: " + new File(fileDirectory).getAbsolutePath());
    }
     public void start()
    {
        try
        {
            while (true)// Main loop
            {
                byte[] buffer = new byte[bufferSize];
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(requestPacket);// Wait for request
                String fileName = new String(requestPacket.getData(), 0, requestPacket.getLength(), StandardCharsets.UTF_8).trim();
                InetAddress clientAddress = requestPacket.getAddress();
                int clientPort = requestPacket.getPort();
                System.out.printf("[%s:%d] Requested file: %s\n", clientAddress, clientPort, fileName);
                if (!isValidFileName(fileName))
                {
                    sendErrorResponse("Invalid file name format", clientAddress, clientPort);
                    continue;
                }
              
