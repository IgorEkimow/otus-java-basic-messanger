package ru.otus.server;

public class ServerApp {
    public static void main(String[] args) {
        Server server = new Server(8189, "jdbc:postgresql://localhost:5432/messanger_db", "messanger_user", "T2qXVgS}s{");

        server.start();
    }
}