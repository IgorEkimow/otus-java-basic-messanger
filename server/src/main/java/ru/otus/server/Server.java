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

    public Server(int port) {
        this.port = port;
        this.clients = new CopyOnWriteArrayList<>();
        this.authenticatedProvider = new InMemoryAuthenticatedProvider(this);
    }

    public void start() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        broadcastMessage("Admin", "Подключился пользователь " + clientHandler.getUsername());
        clients.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("Admin", "Пользователь " + clientHandler.getUsername() + " покинул чат");
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