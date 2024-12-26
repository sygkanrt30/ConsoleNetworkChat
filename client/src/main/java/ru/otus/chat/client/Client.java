package ru.otus.chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    private final Scanner scanner;

    public Client() throws IOException {
        scanner = new Scanner(System.in);
        socket = new Socket("localhost", 8189);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equalsIgnoreCase("/exitDone")) {
                            break;
                        }
                        if (message.startsWith("/authDone")) {
                            String[] partOfMessage = message.split(" ");
                            System.out.println("Удалось успешно войти в чат с именем пользователя "
                                    + partOfMessage[1] + "\nСтатус пользователя: " + partOfMessage[2]);
                        }
                        if (message.startsWith("/regDone")) {
                            System.out.println("Удалось успешно зарегистрироваться с именем пользователя "
                                    + message.split(" ")[1] + "\nСтатус пользователя: " + message.split(" ")[2]);
                        }
                        if (message.startsWith("/kickDone")) {
                            System.out.println("Администратор исключил вас из чата");
                        }
                    } else {
                        System.out.println(message);
                    }
                }
            } catch (EOFException e) {
                System.err.println("Соединение с сервером было закрыто!");
            } catch (IOException e) {
                System.err.println("Разорвано соединение с сервером!!!");
            } finally {
                disconnect();
            }
        }).start();

        while (true) {
            String message = scanner.nextLine();
            out.writeUTF(message);
            if (message.equalsIgnoreCase("/exit")) {
                break;
            }
        }
        disconnect();
    }

    private void disconnect() {
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

