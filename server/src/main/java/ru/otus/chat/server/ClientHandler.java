package ru.otus.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class ClientHandler {
    private final Socket socket;
    private final Server server;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String username;
    private String role;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                System.out.println("Клиент подключился на порту: " + socket.getPort());
                while (true) {
                    sendMsg("Для начала работы надо пройти аутентификацию. Формат команды /auth login password \n" +
                            "или регистрацию. Формат команды /reg login password username password_for_admin");
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equalsIgnoreCase("/exit")) {
                            sendMsg("/exitDone");
                            break;
                        }
                        if (message.startsWith("/auth")) {
                            String[] element = message.split(" ");
                            if (element.length != 3) {
                                sendMsg("Неверный формат команды /auth");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .authenticate(this, element[1], element[2])) {
                                break;
                            }
                        }
                        if (message.startsWith("/reg")) {
                            String[] element = message.split(" ");
                            if (element.length != 5) {
                                sendMsg("Неверный формат команды /reg");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .registration(this, element[1], element[2], element[3], element[4])) {
                                break;
                            }
                        }
                    }
                }
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equalsIgnoreCase("/exit")) {
                            break;
                        } else if (message.startsWith("/w")) {
                            privateMessage(message);
                        } else if (message.startsWith("/kick")) {
                            if (role.equals("admin")) {
                                try {
                                    String name = message.split(" ")[1];
                                    boolean isSucsess = kickUser(name);
                                    if (isSucsess) {
                                        server.broadcastMessage(name + " покинул(a) чат");
                                    } else {
                                        this.out.writeUTF("Пользователь " + name + " не найден");
                                    }
                                } catch (SocketException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                sendMsg("Для использование этой команды у вашей учетной записи должна быть роль администратора");
                            }
                        }
                    } else {
                        server.broadcastMessage(username + " : " + message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Клиент на порту: " + socket.getPort() + " отключился!");
            } finally {
                disconnect(this);
            }
        }).start();
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public boolean kickUser(String name) throws IOException {
        List<ClientHandler> clients = server.getClients();
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(name)) {
                client.out.writeUTF("/kickDone");
                this.out.writeUTF(client.getUsername() + " успешно исключен из чата");
                disconnect(client);
                return true;
            }
        }
        return false;
    }

    public void disconnect(ClientHandler client) {
        server.unsubscribe(client);
        try {
            if (client.in != null) {
                client.in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (client.out != null) {
                client.out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (client.socket != null) {
                client.socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
