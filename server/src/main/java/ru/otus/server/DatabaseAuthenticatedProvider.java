package ru.otus.server;

import java.sql.*;

public class DatabaseAuthenticatedProvider implements AuthenticatedProvider {
    private Server server;
    private Connection connection;
    private String url;
    private String user;
    private String password;

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
            String checkAdmin = "SELECT COUNT(*) FROM users WHERE login = 'admin'";
            ResultSet rs = stmt.executeQuery(checkAdmin);

            if (rs.next() && rs.getInt(1) == 0) {
                String insertAdmin = "INSERT INTO users (login, password, username, role) VALUES ('admin', 'admin', 'admin', 'ADMIN')";
                stmt.execute(insertAdmin);
                System.out.println("Создан пользователь администратор");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка создания пользователя администратора");
        }
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String query = "SELECT username, role FROM users WHERE login = ? AND password = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
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

        String query = "INSERT INTO users (login, password, username, role) VALUES (?, ?, ?, 'USER')";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
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
}