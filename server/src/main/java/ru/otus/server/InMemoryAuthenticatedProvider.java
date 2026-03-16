package ru.otus.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryAuthenticatedProvider implements AuthenticatedProvider {
    private class User {
        private String login;
        private String password;
        private String username;
        private UserRole role;

        public User(String login, String password, String username, UserRole role) {
            this.login = login;
            this.password = password;
            this.username = username;
            this.role = role;
        }
    }

    private Server server;
    private List<User> users;

    public InMemoryAuthenticatedProvider(Server server) {
        this.server = server;
        this.users = new CopyOnWriteArrayList<>();
        this.users.add(new User("admin", "admin", "admin", UserRole.ADMIN));
        this.users.add(new User("user1", "user1", "user1", UserRole.USER));
        this.users.add(new User("user2", "user2", "user2", UserRole.USER));
        this.users.add(new User("user3", "user3", "user3", UserRole.USER));
    }

    @Override
    public void initialize() {
        System.out.println("Сервер аутентификации запущен в режиме InMemory");
    }

    private User getUserByLoginAndPassword(String login, String password) {
        for (User user : users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user;
            }
        }

        return null;
    }

    private boolean isLoginAlreadyExists(String login) {
        for (User user : users) {
            if (user.login.equals(login)) {
                return true;
            }
        }

        return false;
    }

    private boolean isUsernameAlreadyExists(String username) {
        for (User user : users) {
            if (user.username.equals(username)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        User user = getUserByLoginAndPassword(login, password);

        if (user == null) {
            clientHandler.sendMsg("Некоректный логин / пароль");
            return false;
        }

        if (server.isUsernameBusy(user.username)) {
            clientHandler.sendMsg("Указанная учетная запись уже занята");
            return false;
        }

        clientHandler.setUsername(user.username);
        clientHandler.setRole(user.role);
        clientHandler.sendMsg("Вы подключились под ником: " + user.username + " (роль: " + user.role + ")");
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/authok " + user.username);

        return true;
    }

    @Override
    public boolean register(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3) {
            clientHandler.sendMsg("Логин должен состоять из 3+ символов");
            return false;
        }

        if (username.trim().length() < 3) {
            clientHandler.sendMsg("Имя пользователя должна состоять из 3+ символов");
            return false;
        }

        if (isLoginAlreadyExists(login)) {
            clientHandler.sendMsg("Указанный логин уже занят");
            return false;
        }

        if (isUsernameAlreadyExists(username)) {
            clientHandler.sendMsg("Указанное имя пользователя уже занято");
            return false;
        }

        users.add(new User(login, password, username, UserRole.USER));
        clientHandler.setUsername(username);
        clientHandler.setRole(UserRole.USER);
        clientHandler.sendMsg("Вы успешно зарегистрировались и подключились под ником: " + username + " (роль: USER)");
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/regok " + username);

        return true;
    }
}