package ru.otus.server;

import java.sql.*;

public class DatabaseAuthenticatedProvider implements AuthenticatedProvider {
    private Server server;
    private Connection connection;
    private String url;
    private String user;
    private String password;

    private static final String CHECK_ADMIN_QUERY = "SELECT COUNT(*) FROM users WHERE login = 'admin'";
    private static final String INSERT_ADMIN_QUERY = "INSERT INTO users (login, password, username, role) VALUES ('admin', 'admin', 'admin', 'ADMIN')";
    private static final String AUTHENTICATE_QUERY = "SELECT username, role FROM users WHERE login = ? AND password = ?";
    private static final String REGISTER_QUERY = "INSERT INTO users (login, password, username, role) VALUES (?, ?, ?, 'USER')";

    public DatabaseAuthenticatedProvider(Server server, String url, String user, String password) {
        this.server = server;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public void initialize() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url, user, password);

            System.out.println("Подключение к базе данных успешно установлено");

            createAdmin();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Драйвер PostgreSQL не найден", e);
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка подключения к базе данных", e);
        }
    }

    private void createAdmin() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(CHECK_ADMIN_QUERY);

            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute(INSERT_ADMIN_QUERY);
                System.out.println("Создан пользователь администратор");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка создания пользователя администратора");
        }
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        try (PreparedStatement pstmt = connection.prepareStatement(AUTHENTICATE_QUERY)) {
            pstmt.setString(1, login);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String username = rs.getString("username");
                String role = rs.getString("role");
                UserRole userRole = UserRole.valueOf(role);

                if (server.isUsernameBusy(username)) {
                    clientHandler.sendMsg("Указанная учетная запись уже занята");
                    return false;
                }

                clientHandler.setUsername(username);
                clientHandler.setRole(userRole);
                clientHandler.sendMsg("Вы подключились под ником: " + username + " (роль: " + userRole + ")");
                server.subscribe(clientHandler);
                clientHandler.sendMsg("/authok " + username);

                return true;
            } else {
                clientHandler.sendMsg("Некоректный логин / пароль");
                return false;
            }
        } catch (SQLException e) {
            clientHandler.sendMsg("Ошибка сервера при аутентификации");
            return false;
        }
    }

    @Override
    public boolean register(ClientHandler clientHandler, String login, String password, String username) {
        if (!validateData(clientHandler, login, password, username)) {
            return false;
        }

        try (PreparedStatement pstmt = connection.prepareStatement(REGISTER_QUERY)) {
            pstmt.setString(1, login);
            pstmt.setString(2, password);
            pstmt.setString(3, username);

            pstmt.executeUpdate();

            clientHandler.setUsername(username);
            clientHandler.setRole(UserRole.USER);
            clientHandler.sendMsg("Вы успешно зарегистрировались и подключились под ником: " + username + " (роль: USER)");
            server.subscribe(clientHandler);
            clientHandler.sendMsg("/regok " + username);

            return true;
        } catch (SQLException e) {
            clientHandler.sendMsg("Ошибка сервера при регистрации");

            return false;
        }
    }

    private boolean validateData(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3) {
            clientHandler.sendMsg("Логин должен состоять из 3+ символов");
            return false;
        }

        if (username.trim().length() < 3) {
            clientHandler.sendMsg("Имя пользователя должна состоять из 3+ символов");
            return false;
        }

        if (password.trim().length() < 3) {
            clientHandler.sendMsg("Пароль должен состоять из 3+ символов");
            return false;
        }

        return true;
    }
}