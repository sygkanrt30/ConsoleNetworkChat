package ru.otus.chat.server;

public class ServerApp {
    public static void main(String[] args) {
        new Server(8189).start();
    }
}
