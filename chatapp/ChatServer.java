package chatapp;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Map<String, PrintWriter> clients = new HashMap<>();
    private static Set<String> groups = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Welcome to the chat! Please enter a username:");

                String input;
                while ((input = in.readLine()) != null) {
                    if (username == null) {
                        if (clients.containsKey(input)) {
                            out.println("Username already exists");
                        } else {
                            username = input;
                            clients.put(username, out);
                            clientWriters.add(out);
                            broadcastActiveUsers();
                            broadcast(username + " has joined the chat.");
                        }
                    } else {
                        if (input.startsWith("/")) {
                            handleCommand(input);
                        } else {
                            broadcast("[" + getCurrentTime() + "] " + username + ": " + input);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cleanUp();
            }
        }

        private void handleCommand(String command) {
            if (command.startsWith("/create")) {
                String groupName = command.substring(8);
                groups.add(groupName);
                broadcast("Groups: " + String.join(", ", groups));
                broadcast("Group " + groupName + " created.");
            } else if (command.startsWith("/join")) {
                String groupName = command.substring(6);
                // Handle group joining logic
                broadcast(username + " joined group " + groupName);
            } else if (command.startsWith("/leave")) {
                String groupName = command.substring(7);
                // Handle group leaving logic
                broadcast(username + " left group " + groupName);
            } else if (command.startsWith("/delete")) {
                String groupName = command.substring(8);
                groups.remove(groupName);
                broadcast("Groups: " + String.join(", ", groups));
                broadcast("Group " + groupName + " deleted.");
            } else if (command.startsWith("/groupmsg")) {
                String[] parts = command.split(" ", 4);
                String groupName = parts[1];
                String message = parts[3];
                String username = parts[2];
                broadcast("/groupmsg "+groupName+" "+username+": "+ message);
            }
        }

        private void broadcast(String message) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }

        private void broadcastActiveUsers() {
            StringBuilder userList = new StringBuilder("Active users: ");
            for (String user : clients.keySet()) {
                userList.append(user).append(", ");
            }
            String message = userList.substring(0, userList.length() - 2);
            broadcast(message);
        }

        private void cleanUp() {
            if (username != null) {
                clients.remove(username);
                clientWriters.remove(out);
                broadcast(username + " has left the chat.");
                broadcastActiveUsers();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static String getCurrentTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            return sdf.format(new Date());
        }
    }
}
