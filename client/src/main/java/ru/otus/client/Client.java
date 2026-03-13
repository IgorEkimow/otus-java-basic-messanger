package ru.otus.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String host;
    private int port;

    Scanner sc;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;

        try {
            sc = new Scanner(System.in);
            socket = new Socket(host, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readUTF();

                        if(message.startsWith("/")) {
                            if(message.equals("/exitok")) {
                                break;
                            }

                            if(message.startsWith("/authok")) {
                                System.out.println("Удалось успешно войти в чат с именем пользователя: " + message.split(" ")[1]);
                            }

                            if(message.startsWith("/regok")) {
                                System.out.println("Удалось успешно зарегистрироваться и войти в чат с именем пользователя: " + message.split(" ")[1]);
                            }

                            continue;
                        }

                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            }).start();

            System.out.println("Добро пожаловать в чат!");
            System.out.println("=======================================================");
            System.out.println("Доступные команды:");
            System.out.println("-------------------------------------------------------");
            System.out.println("Обычный ввод - общее сообщение всем участникам чата");
            System.out.println("/w никнейм сообщение - личное сообщение участнику чата");
            System.out.println("/exit - выход из чата");
            System.out.println("-------------------------------------------------------");

            while (true) {
                String message = sc.nextLine();
                out.writeUTF(message);

                if(message.equals("/exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disconnect() {
        try {
            if(in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if(out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if(socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(sc != null) {
            sc.close();
        }
    }
}