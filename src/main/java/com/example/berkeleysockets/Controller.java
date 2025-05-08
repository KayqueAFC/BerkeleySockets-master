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

        configurarInterface();
    }

    private void configurarInterface() {
        // Auto-scroll: sempre rola para o final quando a altura muda
        vboxMensagens.heightProperty().addListener((obs, oldVal, newVal) ->
                scrollMensagens.setVvalue(1.0)
        );

        // Ajuste dinâmico da altura do campo de mensagem
        campoMensagem.textProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                double height = Math.min(campoMensagem.getBoundsInLocal().getHeight(), 100);
                campoMensagem.setPrefHeight(height);
            });
        });

        // Enviar mensagem ao pressionar Enter
        campoMensagem.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();  // Impede que ENTER insira nova linha
                enviarMensagem();
            }
        });

        // Enviar mensagem ao clicar no botão
        botaoEnviar.setOnAction(event -> enviarMensagem());
    }

    private void enviarMensagem() {
        String mensagem = campoMensagem.getText().trim();
        if (!mensagem.isEmpty()) {
            adicionarMensagem(mensagem, Pos.CENTER_RIGHT, "bubble-right");

            if (server != null) {
                server.enviarMensagemCliente(mensagem,null);
            } else {
                showErrorAlert("Erro: servidor não inicializado!");
            }

            campoMensagem.clear();
            campoMensagem.setPrefHeight(40);
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
            balao.maxWidthProperty().bind(scrollMensagens.widthProperty().multiply(0.75)); // Corrigido tamanho dinâmico
            balao.setPadding(new Insets(8));

            Label remetente = new Label(posicao == Pos.CENTER_RIGHT ? "Você" : "Cliente");
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
            alert.setTitle("Erro no Cliente");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
