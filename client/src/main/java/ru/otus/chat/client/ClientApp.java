package ru.otus.chat.client;

import java.io.IOException;

public class ClientApp {
    public static void main(String[] args) {
        try {
            new Client();
        } catch (IOException e) {
            System.err.println("ОТСУТСТВУЕТ СОЕДИНЕНИЕ С СЕРВЕРОМ!!!");
        }
    }
}
