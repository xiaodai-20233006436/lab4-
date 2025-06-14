import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UdpFileClient {
    private DatagramSocket socket;
    private String serverIp;
    private int serverPort;
    private final int bufferSize = 4096;
    private final int maxRetries = 5; // Maximum retry attempts
    private final int timeout = 3000; // Timeout set to 3 seconds

    public UdpFileClient(String serverIp, int serverPort) throws SocketException {
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(timeout); // Set default timeout
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("File download client started! (Base64 Encoded)");
            System.out.println("Commands: download <filename>, exit");
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
                if (!input.startsWith("download ")) {
                    System.out.println("Invalid command. Usage: download <filename>");
                    continue;
                }
                String fileName = input.substring(9).trim();
                try {
                    downloadFile(fileName);
                } catch (IOException e) {
                    System.err.println("Download failed: " + e.getMessage());
                }
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private void downloadFile(String fileName) throws IOException {
        byte[] encodedName = Base64.getEncoder().encode(
                fileName.getBytes(StandardCharsets.UTF_8)
        );

        // Add retry mechanism for sending request
        sendWithRetry(encodedName);

        byte[] responseBuffer = new byte[bufferSize];
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

        // Add retry mechanism for receiving response
        receiveWithRetry(responsePacket);

        String response = new String(
                responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8
        );

        if (response.startsWith("ERROR:")) {
            System.err.println("Server error: " + response.substring(6));
            return;
        }

        String[] parts = response.split(":");
        if (parts.length != 2) {
            System.err.println("Invalid server response format");
            return;
        }

        try {
            int fileSize = Integer.parseInt(parts[0]);
            int totalChunks = Integer.parseInt(parts[1]);
            System.out.printf("Downloading: %s (Size: %d bytes, Chunks: %d)\n",
                    fileName, fileSize, totalChunks);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
            for (int chunkNum = 0; chunkNum < totalChunks; chunkNum++) {
                // Add retry mechanism for receiving each data chunk
                receiveWithRetry(responsePacket);
                byte[] chunkData = Arrays.copyOf(
                        responsePacket.getData(), responsePacket.getLength()
                );
                baos.write(chunkData);
                System.out.printf("Progress: %d/%d (%.1f%%)\r",
                        chunkNum + 1, totalChunks, (chunkNum + 1) * 100.0 / totalChunks);
            }

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                fos.write(baos.toByteArray());
            }
            System.out.println("\nFile saved: " + new File(fileName).getAbsolutePath());
        } catch (NumberFormatException e) {
            System.err.println("Invalid server response: " + response);
        }
    }

    private void sendWithRetry(byte[] data) throws IOException {
        int retries = 0;
        while (retries < maxRetries) {
            try {
                socket.send(new DatagramPacket(
                        data,
                        data.length,
                        InetAddress.getByName(serverIp),
                        serverPort
                ));
                return;
            } catch (IOException e) {
                retries++;
                if (retries >= maxRetries) {
                    throw new IOException("Failed after " + maxRetries + " retries: " + e.getMessage());
                }
                System.err.println("Send failed, retrying (" + retries + "/" + maxRetries + ")...");
                try {
                    Thread.sleep(1000); // Wait 1 second before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry");
                }
            }
        }
    }

    private void receiveWithRetry(DatagramPacket packet) throws IOException {
        int retries = 0;
        while (retries < maxRetries) {
            try {
                socket.receive(packet);
                return;
            } catch (SocketTimeoutException e) {
                retries++;
                if (retries >= maxRetries) {
                    throw new IOException("Timeout after " + maxRetries + " retries");
                }
                System.err.println("Timeout, retrying (" + retries + "/" + maxRetries + ")...");
            }
        }
    }

    public static void main(String[] args) {
        try {
            UdpFileClient client = new UdpFileClient("127.0.0.1", 9091);
            client.start();
        } catch (SocketException e) {
            System.err.println("Failed to start client: " + e.getMessage());
        }
    }
}
