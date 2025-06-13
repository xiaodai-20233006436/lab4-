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
    
   
