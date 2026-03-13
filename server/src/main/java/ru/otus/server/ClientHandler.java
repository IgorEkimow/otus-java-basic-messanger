package ru.otus.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private UserRole role;
    private boolean isAuthenticate;

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        System.out.println("Client connected, port: " + socket.getPort());

        new Thread(() -> {
            try {
                while (!isAuthenticate) {
                    sendMsg("Перед работой с чатом необходимо выполнить аутентификацию: " + ConsoleColors.GREEN_BOLD + "/auth login password\n" + ConsoleColors.RESET
                            + "или зарегистрироваться: " + ConsoleColors.GREEN_BOLD + "/reg login password username" + ConsoleColors.RESET);

                    String message = in.readUTF();

                    if(message.startsWith("/")) {
                        if(message.equals("/exit")) {
                            sendMsg("/exitok");
                            break;
                        }

                        if(message.startsWith("/auth")) {
                            String[] token = message.trim().split(" ");

                            if(token.length != 3) {
                                sendMsg(ConsoleColors.YELLOW + "Неверный формат команды /auth" + ConsoleColors.RESET);
                                continue;
                            }

                            if(server.getAuthenticatedProvider().authenticate(this, token[1], token[2])) {
                                isAuthenticate = true;
                                break;
                            }

                            continue;
                        }

                        if(message.startsWith("/reg")) {
                            String[] token = message.trim().split(" ");

                            if(token.length != 4) {
                                sendMsg(ConsoleColors.YELLOW + "Неверный формат команды /reg" + ConsoleColors.RESET);
                                continue;
                            }

                            if(server.getAuthenticatedProvider().register(this, token[1], token[2], token[3])) {
                                isAuthenticate = true;
                                break;
                            }
                        }
                    }
                }

                if(isAuthenticate) {
                    if (role == UserRole.ADMIN) {
                        sendMsg("-----------------------------------------------------------------------------------------");
                        sendMsg(ConsoleColors.RED_BOLD + "/kick username - удалить пользователя из чата (только для пользователей с ролью ADMIN)" + ConsoleColors.RESET);
                        sendMsg("-----------------------------------------------------------------------------------------");
                    }

                    while (true) {
                        String message = in.readUTF();

                        if(message.startsWith("/")) {
                            if(message.equals("/exit")) {
                                sendMsg("/exitok");

                                break;
                            } else if(message.startsWith("/w ")) {
                                String[] parts = message.split(" ", 3);

                                if(parts.length >= 3) {
                                    String nickname = parts[1];
                                    String privateMessage = parts[2];
                                    server.sendPrivateMsg(this, nickname, privateMessage);
                                }
                            } else if(message.startsWith("/kick ")) {
                                if (role == UserRole.ADMIN) {
                                    String[] parts = message.split(" ", 2);

                                    if (parts.length == 2) {
                                        String usernameToKick = parts[1];
                                        server.kickUser(this, usernameToKick);
                                    } else {
                                        sendMsg("Выполнена команда: /kick username");
                                    }
                                } else {
                                    sendMsg("У вас не достаточно прав для использования этой команды");
                                }
                            }
                        } else {
                            server.broadcastMessage(username, message);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMsg(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void disconnect() {
        System.out.println("Client disconnected, username: " + username);

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
    }
}