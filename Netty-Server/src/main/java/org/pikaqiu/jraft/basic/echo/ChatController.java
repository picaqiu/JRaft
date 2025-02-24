package org.pikaqiu.jraft.basic.echo;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatController {
    @FXML
    private VBox chatContainer;
    @FXML
    private TextField inputField;
    @FXML
    private Label statusLabel;

    private NettyEchoUIClient nettyClient;
    private Stage primaryStage;

    public void setStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void initialize() {
        // 输入框动画
        inputField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                inputField.setStyle("-fx-border-color: #00BFFF;");
            } else {
                inputField.setStyle("-fx-border-color: #D3D3D3;");
            }
        });

        // 添加Enter键监听（兼容不同操作系统）
        inputField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
                event.consume(); // 阻止默认换行行为
            }
        });

        // 允许Shift+Enter换行
        inputField.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isShiftDown()) {
                inputField.appendText("\n");
            }
        });
    }

    public void connectToServer(String host, int port) {
        nettyClient = new NettyEchoUIClient(host, port, new NettyEchoUIClient.MessageCallback() {
            @Override
            public void onMessage(String message) {
                appendMessage(message, false);
            }

            @Override
            public void onStatusChange(String status, boolean isError) {
                Platform.runLater(() -> {
                    statusLabel.setText(status);
                    statusLabel.setTextFill(isError ? Color.RED : Color.GREEN);

                    // 连接成功时添加动画
                    if (!isError) {
                        Timeline blink = new Timeline(
                                new KeyFrame(Duration.seconds(0.5), e -> statusLabel.setOpacity(0.5)),
                                new KeyFrame(Duration.seconds(1), e -> statusLabel.setOpacity(1))
                        );
                        blink.setCycleCount(2);
                        blink.play();
                    }
                });
            }
        });

        new Thread(() -> nettyClient.connect()).start();
    }

    @FXML
    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            appendMessage(msg, true);
            nettyClient.sendMessage(msg);
            inputField.clear();

            // 发送动画
            ScaleTransition st = new ScaleTransition(Duration.millis(200), inputField);
            st.setFromX(1.0);
            st.setToX(0.9);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        }
    }

    private void appendMessage(String text, boolean isSelf) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox(10);
            messageBox.setAlignment(isSelf ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            messageBox.setPadding(new Insets(5));

            // 头像
            ImageView avatar = createAvatar(isSelf);

            // 消息气泡
            Label message = new Label(text);
            message.getStyleClass().addAll("bubble", isSelf ? "bubble-self" : "bubble-other");
            message.setMaxWidth(400);
            message.setWrapText(true);

            // 时间标签
            Label timeLabel = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            timeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");

            VBox container = new VBox(2);
            container.getChildren().addAll(message, timeLabel);

            if (isSelf) {
                messageBox.getChildren().addAll(container, avatar);
            } else {
                messageBox.getChildren().addAll(avatar, container);
            }

            // 入场动画
            FadeTransition ft = new FadeTransition(Duration.millis(300), messageBox);
            ft.setFromValue(0);
            ft.setToValue(1);

            TranslateTransition tt = new TranslateTransition(Duration.millis(300), messageBox);
            tt.setFromX(isSelf ? 20 : -20);
            tt.setToX(0);

            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.play();

            chatContainer.getChildren().add(messageBox);
        });
    }

    private ImageView createAvatar(boolean isSelf) {
        ImageView avatar = new ImageView(
                new Image(getClass().getResourceAsStream(
                        isSelf ? "/user_icon.png" : "/server_icon.png"))
        );
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);

        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);

        // 悬停显示用户名
        Tooltip tooltip = new Tooltip(isSelf ? "我" : "服务器");
        Tooltip.install(avatar, tooltip);

        return avatar;
    }

    public void shutdown() {
        if (nettyClient != null) {
            nettyClient.disconnect();
        }
    }
}

