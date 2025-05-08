package com.example.berkeleysockets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML private Button botaoEnviar;
    @FXML private TextArea campoMensagem;
    @FXML private VBox vboxMensagens;
    @FXML private ScrollPane scrollMensagens;

    private Server server;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            server = new Server(new ServerSocket(1234), this);
            System.out.println("Servidor iniciado na porta 1234");
            server.startServer();
        } catch (IOException e) {
            showErrorAlert("Falha ao iniciar servidor: " + e.getMessage());
        }

        // Configura auto-scroll
        vboxMensagens.heightProperty().addListener((obs, oldVal, newVal) ->
                scrollMensagens.setVvalue(newVal.doubleValue())
        );

        // Ajuste dinâmico da altura do campo de mensagem
        campoMensagem.textProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                double height = Math.min(campoMensagem.getBoundsInLocal().getHeight(), 100);
                campoMensagem.setPrefHeight(height);
            });
        });

        // Configura ações de envio
        botaoEnviar.setOnAction(event -> enviarMensagem());

        // Enviar mensagem ao pressionar Enter (correção do erro original)
        campoMensagem.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                enviarMensagem();
                event.consume(); // Evita que o Enter adicione nova linha
            }
        });
    }

    private void enviarMensagem() {
        String mensagem = campoMensagem.getText().trim();
        if (!mensagem.isEmpty()) {
            adicionarMensagem(mensagem, Pos.CENTER_RIGHT, "bubble-right");
            server.enviarMensagemCliente(mensagem);
            campoMensagem.clear();
            campoMensagem.setPrefHeight(40); // Reseta a altura após enviar
        }
    }

    public void adicionarMensagem(String mensagem, Pos posicao, String estilo) {
        Platform.runLater(() -> {
            HBox container = new HBox();
            container.setAlignment(posicao);
            container.setPadding(new Insets(5, 10, 15, 10));

            Text texto = new Text(mensagem);
            texto.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

            TextFlow balao = new TextFlow(texto);
            balao.getStyleClass().add(estilo);
            balao.setMaxWidth(scrollMensagens.getWidth() * 0.7);
            balao.setPadding(new Insets(8));

            Label remetente = new Label(posicao == Pos.CENTER_RIGHT ? "Você" : "Suporte");
            remetente.getStyleClass().addAll("sender-label",
                    posicao == Pos.CENTER_RIGHT ? "sender-client" : "sender-server");

            VBox vboxMensagem = new VBox(2, remetente, balao);
            vboxMensagem.setAlignment(posicao);
            container.getChildren().add(vboxMensagem);

            vboxMensagens.getChildren().add(container);
        });
    }

    private void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}