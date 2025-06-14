import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;

public class UdpFileClient
{   //Core member variables
    private DatagramSocket socket;
    private String serverIp;
    private int serverPort;
    private final int bufferSize = 4096;
    public UdpFileClient(String serverIp, int serverPort) throws SocketException//Initialize the client
    {
        this.socket = new DatagramSocket();//Create a UDP socket
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }
    public void start()//Client main loop
    {
        try (Scanner scanner = new Scanner(System.in))//Use Scanner to read user input
        {
            System.out.println("File download client started!");
            System.out.println("Available commands: download <filename>, exit");
            while (true)//Main loop
            {
                System.out.print("> ");
                String input = scanner.nextLine().trim();// Read user input
                if (input.equalsIgnoreCase("exit"))
                {
                    break;
                }
                if (!input.startsWith("download "))
                {
                    System.out.println("Invalid command, use: download <filename> or exit");
                    continue;
                }
                String fileName = input.substring(9).trim();
                if (!isValidFileName(fileName))//Verify the validity of the file name
                {
                    System.out.println("Error: Filename can only contain letters, numbers, underscores and hyphens");
                    continue;
                }
                downloadFile(fileName);
            }
        }
        catch (IOException e)
        {
            System.err.println("Client error: " + e.getMessage());
        }
        finally
        {
            if (socket != null && !socket.isClosed())
            {
                socket.close();//Close the socket
            }
        }
    }
}
