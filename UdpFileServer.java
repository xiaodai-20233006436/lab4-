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
            while (true)
            {
                byte[] buffer = new byte[bufferSize];
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(requestPacket);
                String fileName = new String(requestPacket.getData(), 0, requestPacket.getLength(), StandardCharsets.UTF_8).trim();
                InetAddress clientAddress = requestPacket.getAddress();
                int clientPort = requestPacket.getPort();
                System.out.printf("[%s:%d] Requested file: %s\n", clientAddress, clientPort, fileName);
                if (!isValidFileName(fileName))
                {
                    sendErrorResponse("Invalid file name format", clientAddress, clientPort);
                    continue;
                }
                Path filePath = Paths.get(fileDirectory, fileName);
                if (!Files.exists(filePath))
                {
                    sendErrorResponse("File does not exist", clientAddress, clientPort);
                    continue;
                }
                if (!Files.isReadable(filePath))
                {
                    sendErrorResponse("File is not readable", clientAddress, clientPort);
                    continue;
                }
                try
                {
                    byte[] fileData = Files.readAllBytes(filePath);
                    int totalSize = fileData.length;
                    int chunkSize = bufferSize - 10; // Reserve space for metadata
                    int totalChunks = (int) Math.ceil((double) totalSize / chunkSize);

                    // Send file info
                    String fileInfo = totalSize + ":" + totalChunks;
                    sendResponse(fileInfo.getBytes(StandardCharsets.UTF_8), clientAddress, clientPort);

                    // Send file in chunks
                    int offset = 0;
                    for (int chunkNum = 0; offset < totalSize; chunkNum++)
                    {
                        int currentChunkSize = Math.min(chunkSize, totalSize - offset);
                        byte[] chunkData = new byte[currentChunkSize + 4];
                        System.arraycopy(intToBytes(chunkNum), 0, chunkData, 0, 4);
                        System.arraycopy(fileData, offset, chunkData, 4, currentChunkSize);

                        sendResponse(chunkData, clientAddress, clientPort);
                        offset += currentChunkSize;

                        System.out.printf("Sending progress: %d/%d (%.1f%%)\r",
                                chunkNum + 1, totalChunks, (offset * 100.0 / totalSize));
                    }
                    System.out.println("\nFile " + fileName + " sent successfully");
                } catch (IOException e)
                {
                    sendErrorResponse("File read error: " + e.getMessage(), clientAddress, clientPort);
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("Server error: " + e.getMessage());
        } finally
        {
            if (socket != null && !socket.isClosed())
            {
                socket.close();
            }
        }
    }
    private boolean isValidFileName(String fileName) //The correct specification of the file name
    {
        return fileName.matches("^[a-zA-Z0-9_\\-]+(\\.[a-zA-Z0-9]+)?$");
    }
    private void sendResponse(byte[] data, InetAddress address, int port) throws IOException// // Send response to client
    {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }
    private void sendErrorResponse(String message, InetAddress address, int port)//// Send error response to client
    {
        try
        {
            String errorMsg = "ERROR:" + message;
            sendResponse(errorMsg.getBytes(StandardCharsets.UTF_8), address, port);
            System.err.println("Sending error response: " + errorMsg);
        } catch (IOException e) {
            System.err.println("Failed to send error response: " + e.getMessage());
        }
    }
    public static void main(String[] args)
    {
        int port = 9091;//The input port number is specified
        String fileDir = args.length > 0 ? args[0] : "./";//Obtain the file storage directory parameter
        try
        {
            UdpFileServer server = new UdpFileServer(port, 4096, fileDir);
            server.start();
        } 
        catch (IOException e)//Exception handling
        {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}
          
              
