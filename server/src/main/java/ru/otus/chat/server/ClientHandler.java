package ru.otus.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientHandler {
    private final Socket socket;
    private final Server server;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final String username;
    private static int userCount = 0;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        userCount++;
        username = "user" + userCount;
        new Thread(() -> {
            try {
                System.out.println("Клиент подключился " + socket.getPort());
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equalsIgnoreCase("/exit")) {
                            sendMsg("/exitDone");
                            break;
                        } else if (message.startsWith("/w")) {
                            privateMessage(message);
                        }
                    } else {
                        server.broadcastMessage(username + " : " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public String getUsername() {
        return username;
    }

    public void sendMsg(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void privateMessage(String message) throws IOException {
        String[] partsOfMessage = message.split(" ", 3);
        List<ClientHandler> clients = server.getClients();
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(partsOfMessage[1]) || client.equals(this)) {
                client.out.writeUTF(username + " : " + partsOfMessage[2]);
            }
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
