package ru.otus.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;
    private List<ClientHandler> clients;

    public Server(int port) {
        this.port = port;
        clients = new CopyOnWriteArrayList<>();
    }

    public void start() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                subscribe(new ClientHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        broadcastMessage("Подключился пользователь " + clientHandler.getUsername());
        clients.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("Пользователь " + clientHandler.getUsername() + " покинул чат");
        clients.remove(clientHandler);
    }

    public void broadcastMessage(String message) {
        for(ClientHandler c : clients) {
            c.sendMsg(message);
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
}