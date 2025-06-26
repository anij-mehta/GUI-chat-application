package chatapp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatClient {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static JTextArea textArea;
    private static JTextField textField;
    private static JList<String> userList;
    private static DefaultListModel<String> userListModel;
    private static DefaultListModel<String> groupListModel;
    private static JList<String> groupList;
    private static Map<String, JFrame> groupChatFrames = new HashMap<>();
    private static String username;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Chat Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        textArea = new JTextArea(20, 50);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(new Color(230, 230, 250));
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane textScrollPane = new JScrollPane(textArea);
        textScrollPane.setBorder(BorderFactory.createTitledBorder("Chat Room"));

        textField = new JTextField(30);
        JButton sendButton = new JButton("Send");
        sendButton.setBackground(Color.BLACK);
        sendButton.setForeground(Color.BLACK);
        sendButton.setFont(new Font("Arial", Font.BOLD, 16));
        sendButton.setPreferredSize(new Dimension(100, 40));
        sendButton.setBorder(BorderFactory.createRaisedBevelBorder());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(textField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBackground(new Color(240, 248, 255));
        userList.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createTitledBorder("Active Users"));

        JPanel groupPanel = new JPanel();
        groupPanel.setLayout(new BorderLayout());
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setBackground(new Color(240, 248, 255));
        groupList.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane groupScrollPane = new JScrollPane(groupList);
        groupScrollPane.setBorder(BorderFactory.createTitledBorder("Available Groups"));

        JPanel groupInputPanel = new JPanel();
        groupInputPanel.setLayout(new BorderLayout());
        JButton createGroupButton = new JButton("Create Group");
        JButton joinGroupButton = new JButton("Join Group");
        JButton leaveGroupButton = new JButton("Leave Group");
        JButton deleteGroupButton = new JButton("Delete Group");
        groupInputPanel.add(deleteGroupButton, BorderLayout.NORTH);
        groupInputPanel.add(createGroupButton, BorderLayout.EAST);
        groupInputPanel.add(joinGroupButton, BorderLayout.CENTER);
        groupInputPanel.add(leaveGroupButton, BorderLayout.WEST);

        groupPanel.add(groupScrollPane, BorderLayout.CENTER);
        groupPanel.add(groupInputPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayout(2, 1));
        rightPanel.add(userScrollPane);
        rightPanel.add(groupPanel);

        frame.add(textScrollPane, BorderLayout.CENTER);
        frame.add(rightPanel, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        textField.addActionListener(e -> sendMessage());

        createGroupButton.addActionListener(e -> createGroup());
        joinGroupButton.addActionListener(e -> joinGroup());
        leaveGroupButton.addActionListener(e -> leaveGroup());
        deleteGroupButton.addActionListener(e -> deleteGroup());

        frame.setVisible(true);
        requestUsername(frame);
    }

    private static void requestUsername(JFrame frame) {
        while (true) {
            username = JOptionPane.showInputDialog(frame, "Enter your username:", "Username Required", JOptionPane.PLAIN_MESSAGE);
            if (username != null && !username.trim().isEmpty()) {
                connectToServer(frame);
                break;
            } else {
                JOptionPane.showMessageDialog(frame, "Username cannot be empty.", "Invalid Username", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void connectToServer(JFrame frame) {
        try {
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(username);

            String response = in.readLine();
            if (response.startsWith("Username already exists")) {
                socket.close();
                JOptionPane.showMessageDialog(frame, response, "Username Taken", JOptionPane.WARNING_MESSAGE);
                requestUsername(frame);
            } else {
                frame.setTitle("Chat Application - " + username);
                new Thread(new IncomingReader()).start();
                textArea.append("[" + getCurrentTime() + "] " + username + " connected.\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage() {
        String message = textField.getText();
        if (!message.trim().isEmpty()) {
            out.println(message);
            textField.setText("");
        }
    }

    private static void createGroup() {
        String groupName = JOptionPane.showInputDialog(null, "Enter group name:", "Create Group", JOptionPane.PLAIN_MESSAGE);
        if (groupName != null && !groupName.trim().isEmpty()) {
            out.println("/create " + groupName);
        } else {
            JOptionPane.showMessageDialog(null, "Group name cannot be empty.", "Invalid Group Name", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void deleteGroup() {
        String groupName = groupList.getSelectedValue();
        if (groupName != null) {
            out.println("/delete " + groupName);
        } else {
            JOptionPane.showMessageDialog(null, "Please select a group to delete.", "No Group Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void joinGroup() {
        String groupName = groupList.getSelectedValue();
        if (groupName != null) {
            out.println("/join " + groupName);
            openGroupChatWindow(groupName);
        } else {
            JOptionPane.showMessageDialog(null, "Please select a group to join.", "No Group Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void openGroupChatWindow(String groupName) {
        if (!groupChatFrames.containsKey(groupName)) {
            JFrame groupChatFrame = new JFrame("Group Chat - " + groupName);
            groupChatFrame.setSize(500, 400);
            groupChatFrame.setLayout(new BorderLayout());

            JTextArea groupTextArea = new JTextArea();
            groupTextArea.setEditable(false);
            groupTextArea.setLineWrap(true);
            groupTextArea.setWrapStyleWord(true);
            JScrollPane groupTextScrollPane = new JScrollPane(groupTextArea);
            groupChatFrame.add(groupTextScrollPane, BorderLayout.CENTER);

            JTextField groupTextField = new JTextField();
            JButton groupSendButton = new JButton("Send");

            JPanel groupInputPanel = new JPanel(new BorderLayout());
            groupInputPanel.add(groupTextField, BorderLayout.CENTER);
            groupInputPanel.add(groupSendButton, BorderLayout.EAST);
            groupChatFrame.add(groupInputPanel, BorderLayout.SOUTH);

            groupSendButton.addActionListener(e -> {
                String message = groupTextField.getText();
                if (!message.trim().isEmpty()) {
                    out.println("/groupmsg " + groupName+" "+username+" " + message + "\n");
                    groupTextField.setText("");
                }
            });

            groupChatFrame.setVisible(true);
            groupChatFrames.put(groupName, groupChatFrame);

            groupChatFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    out.println("/leave " + groupName);
                    groupChatFrames.remove(groupName);
                }
            });
        }
    }

    private static void leaveGroup() {
        String groupName = groupList.getSelectedValue();
        if (groupName != null) {
            out.println("/leave " + groupName);
            JFrame groupChatFrame = groupChatFrames.remove(groupName);
            if (groupChatFrame != null) {
                groupChatFrame.dispose();
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please select a group to leave.", "No Group Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    private static class IncomingReader implements Runnable {
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Active users: ")) {
                        updateActiveUsers(message.substring(14));
                    } else if (message.startsWith("Groups: ")) {
                        updateGroups(message.substring(8));
                    } else if (message.startsWith("/groupmsg")) {
                        String[] parts = message.split(" ", 3);
                        String groupName = parts[1];
                        String groupMessage = parts[2];
                        if (groupChatFrames.containsKey(groupName)) {
                            JTextArea groupTextArea = (JTextArea) ((JScrollPane) groupChatFrames.get(groupName).getContentPane().getComponent(0)).getViewport().getView();
                            groupTextArea.append("[" + groupName + "] " + groupMessage + "\n");
                        }
                    } else {
                        textArea.append(message + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void updateActiveUsers(String users) {
            userListModel.clear();
            for (String user : users.split(", ")) {
                userListModel.addElement(user);
            }
        }

        private void updateGroups(String groups) {
            groupListModel.clear();
            for (String group : groups.split(", ")) {
                groupListModel.addElement(group);
            }
        }
    }
}
