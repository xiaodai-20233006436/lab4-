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
}
