package com.example.berkeleysockets;

import javafx.geometry.Pos;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private ServerSocket serverSocket;
    private List<ClientHandler> clientHandlers = new ArrayList<>();
    private Controller controller;

    public Server(ServerSocket serverSocket, Controller controller) {
        this.serverSocket = serverSocket;
        this.controller = controller;
    }

    public void startServer() {
        new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Novo cliente conectado!");

                    ClientHandler clientHandler = new ClientHandler(socket, this, controller);
                    clientHandlers.add(clientHandler);

                    new Thread(clientHandler).start();
                }
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Erro no servidor: " + e.getMessage());
                }
                closeServer();
            }
        }).start();
    }

    public void enviarMensagemCliente(String mensagem, ClientHandler remetente) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler != remetente) { // NÃO reenvia para quem enviou
                try {
                    clientHandler.enviarMensagem(mensagem);
                } catch (IOException e) {
                    System.err.println("Erro ao enviar mensagem para cliente: " + e.getMessage());
                }
            }
        }
    }

    public void closeServer() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.closeConnection();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar servidor: " + e.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private Controller controller;
        private Server server;

        public ClientHandler(Socket socket, Server server, Controller controller) {
            try {
                this.socket = socket;
                this.server = server;
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                this.controller = controller;
            } catch (IOException e) {
                System.err.println("Erro ao criar handler do cliente: " + e.getMessage());
                closeConnection();
            }
        }

        @Override
        public void run() {
            try {
                while (socket.isConnected()) {
                    String mensagemCliente = bufferedReader.readLine();
                    if (mensagemCliente == null) {
                        System.out.println("[DEBUG] Cliente desconectado, encerrando thread.");
                        break;
                    }

                    System.out.println("[DEBUG] Mensagem recebida: " + mensagemCliente);

                    // Exibe mensagem no controlador
                    if (controller != null) {
                        controller.adicionarMensagem(mensagemCliente, Pos.CENTER_LEFT, "bubble-left");
                    } else {
                        System.err.println("[ERRO] Controller não inicializado.");
                    }


                    server.enviarMensagemCliente(mensagemCliente, this);
                }
            } catch (IOException e) {
                System.err.println("[ERRO] Cliente desconectado inesperadamente: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        public void enviarMensagem(String mensagem) throws IOException {
            bufferedWriter.write(mensagem);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }

        public void closeConnection() {
            try {
                if (bufferedReader != null) bufferedReader.close();
                if (bufferedWriter != null) bufferedWriter.close();
                if (socket != null) socket.close();
                clientHandlers.remove(this);
            } catch (IOException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
}
