import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UdpFileClient 
{
    private DatagramSocket socket;
    private String serverIp;
    private int serverPort;
    private final int bufferSize = 4096;
    public UdpFileClient(String serverIp, int serverPort) throws SocketException
    {
        this.socket = new DatagramSocket();
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }
    public void start()
    {
        try (Scanner scanner = new Scanner(System.in))
        {
            System.out.println("File download client started! (Base64 Encoded)");
            System.out.println("Commands: download <filename>, exit");
            while (true)
            {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("exit"))
                {
                    break;
                }
                if (!input.startsWith("download "))
                {
                    System.out.println("Invalid command. Usage: download <filename>");
                    continue;
                }
                String fileName = input.substring(9).trim();
                downloadFile(fileName);
            }
        } catch (IOException e)
        {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            if (socket != null)
            {
                socket.close();
            }
        }
    }

    private void downloadFile(String fileName) throws IOException
    {   //Base64 encode the file name
        byte[] encodedName = Base64.getEncoder().encode(
                fileName.getBytes(StandardCharsets.UTF_8)
        );
        socket.send(new DatagramPacket(    //Send a request to the server
                encodedName,
                encodedName.length,
                InetAddress.getByName(serverIp),
                serverPort
        ));
        //Prepare to receive server responses
        byte[] responseBuffer = new byte[bufferSize];//Receive buffer
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
        socket.receive(responsePacket);
        String response = new String(   //Decode the server response
                responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8  //Decode using UTF-8
        );
        if (response.startsWith("ERROR:")) //Handle error responses
        {
            System.err.println("Server error: " + response.substring(6));
            return;
        }
        String[] parts = response.split(":");//Parse file information
        if (parts.length != 2) {
            System.err.println("Invalid server response format");
            return;
        }
        try
        {
            int fileSize = Integer.parseInt(parts[0]);
            int totalChunks = Integer.parseInt(parts[1]);
            System.out.printf("Downloading: %s (Size: %d bytes, Chunks: %d)\n",
                    fileName, fileSize, totalChunks);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);//Receive file chunks
            for (int chunkNum = 0; chunkNum < totalChunks; chunkNum++)
            {
                socket.receive(responsePacket);
                byte[] chunkData = Arrays.copyOf(
                        responsePacket.getData(), responsePacket.getLength()
                );
                baos.write(chunkData);
                System.out.printf("Progress: %d/%d (%.1f%%)\r", //Print the download progress
                        chunkNum + 1, totalChunks, (chunkNum + 1) * 100.0 / totalChunks);
            }
            try (FileOutputStream fos = new FileOutputStream(fileName)) //Write the received data to a file
            {
                fos.write(baos.toByteArray());
            }
            System.out.println("\nFile saved: " + new File(fileName).getAbsolutePath());//Handle number parsing errors
            
        }
        catch (NumberFormatException e)
        {
            System.err.println("Invalid server response: " + response);
        }
        
    }
    public static void main(String[] args) 
    {
        try
        {
            UdpFileClient client = new UdpFileClient("127.0.0.1", 9091);
            client.start();
        } catch (SocketException e)
        {
            System.err.println("Failed to start client: " + e.getMessage());
        }
    }
        
}
