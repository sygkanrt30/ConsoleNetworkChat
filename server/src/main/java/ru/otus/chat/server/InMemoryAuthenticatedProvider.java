package ru.otus.chat.server;

import java.sql.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryAuthenticatedProvider implements AuthenticatedProvider {

    private record User(String login, String password, String username, String role) {
    }

    private static Connection connection;
    public static final String ALL_INFO_SELECT = "SELECT * FROM users;";
    public static final String INSERT_USERS = "INSERT INTO users (login, password, role, name) VALUES (?, ?, ?, ?)";
    private List<User> users;
    private final Server server;
    public static final String PASSWORD_FOR_ADMIN = "159357";

    public InMemoryAuthenticatedProvider(Server server) {
        this.server = server;
    }

    @Override
    public void initialize() {
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/chat_users", "postgres", "5432");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Инициализация BD прошла успешно!!!");
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        users = getAllInfo();
        String authUsername = getUsernameByLoginAndPassword(login, password);
        String role = getRoleByLoginAndPassword(login, password);
        if (authUsername == null) {
            clientHandler.sendMsg("Неверный логин/пароль");
            return false;
        }
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMsg("Указанная учетная запись уже занята");
            return false;
        }
        clientHandler.setUsername(authUsername);
        clientHandler.setRole(role);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/authDone " + authUsername + " " + role);

        return true;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String name, String passwordForAdmin) {
        users = getAllInfo();
        if (login.length() < 3 || password.length() < 3 || name.length() < 3) {
            clientHandler.sendMsg("Логин 3+ символа, пароль 3+ символа, имя пользователя 3+ символа");
            return false;
        }
        if (isLoginAlreadyExists(login)) {
            clientHandler.sendMsg("Указанный логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExists(name)) {
            clientHandler.sendMsg("Указанное имя пользователя уже занято");
            return false;
        }
        String role = passwordForAdmin.equals(PASSWORD_FOR_ADMIN) ? "admin" : "user";
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_USERS)){
            connection.setAutoCommit(false);
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, role);
            preparedStatement.setString(4, name);
            preparedStatement.executeUpdate();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        clientHandler.setUsername(name);
        clientHandler.setRole(role);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/regDone " + name + " " + role);
        return true;
    }

    private List<User> getAllInfo() {
        List<User> newUsers = new CopyOnWriteArrayList<>();
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(ALL_INFO_SELECT)) {
                while (resultSet.next()) {
                    String login = resultSet.getString(2);
                    String password = resultSet.getString(3);
                    String role = resultSet.getString(4);
                    String name = resultSet.getString(5);
                    User user = new User(login, password, name, role);
                    newUsers.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return newUsers;
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.username;
            }
        }
        return null;
    }

    private String getRoleByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.role;
            }
        }
        return null;
    }

    private boolean isLoginAlreadyExists(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsernameAlreadyExists(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return true;
            }
        }
        return false;
    }
}
