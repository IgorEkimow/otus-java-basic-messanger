package ru.otus.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;
    private List<ClientHandler> clients;
    private AuthenticatedProvider authenticatedProvider;

    public Server(int port, String url, String user, String password) {
        this.port = port;
        this.clients = new CopyOnWriteArrayList<>();
        this.authenticatedProvider = new DatabaseAuthenticatedProvider(this, url, user, password);
    }

    public void start() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер успешно запущен на порту: " + port);

            authenticatedProvider.initialize();

            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        broadcastMessage("admin", "Подключился пользователь " + clientHandler.getUsername() + " (роль: " + clientHandler.getRole() + ")");
        clients.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("admin", "Пользователь " + clientHandler.getUsername() + " покинул чат");
        clients.remove(clientHandler);
    }

    public void broadcastMessage(String sender, String message) {
        for(ClientHandler c : clients) {
            c.sendMsg(ConsoleColors.PURPLE_BOLD + sender + ": " + ConsoleColors.CYAN + message + ConsoleColors.RESET);
        }
    }

    public void sendPrivateMsg(ClientHandler from, String to, String message) {
        boolean isFound = false;

        for(ClientHandler client : clients) {
            if(client.getUsername().equals(to)) {
                client.sendMsg("[Личное сообщение от " + from.getUsername() + "]: " + message);
                from.sendMsg("[Личное сообщение для " + to + "]: " + message);
                isFound = true;

                break;
            }
        }

        if(!isFound) {
            from.sendMsg("Пользователь с ником '" + to + "' не найден в чате");
        }
    }

    public void kickUser(ClientHandler admin, String usernameToKick) {
        if (admin.getRole() != UserRole.ADMIN) {
            admin.sendMsg("У вас не достаточно прав для удаления пользователей из чата");
            return;
        }

        if (usernameToKick.equals(admin.getUsername())) {
            admin.sendMsg("Вы не можете удалить самого себя из чата");
            return;
        }

        ClientHandler targetClient = null;
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(usernameToKick)) {
                targetClient = client;
                break;
            }
        }

        if (targetClient == null) {
            admin.sendMsg("Пользователь с ником '" + usernameToKick + "' не найден в чате");
            return;
        }

        broadcastMessage("admin", "Пользователь " + usernameToKick + " был удален из чата администратором " + admin.getUsername());

        targetClient.sendMsg("Вы были удалены из чата администратором " + admin.getUsername());
        targetClient.sendMsg("/exitok");

        try {
            targetClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isUsernameBusy(String username){
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)){
                return true;
            }
        }

        return false;
    }

    public AuthenticatedProvider getAuthenticatedProvider() {
        return authenticatedProvider;
    }
}