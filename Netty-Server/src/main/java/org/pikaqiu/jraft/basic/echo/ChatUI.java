package org.pikaqiu.jraft.basic.echo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatUI extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat-view.fxml"));
        Parent root = loader.load();

        ChatController controller = loader.getController();
        controller.setStage(stage); // 传递Stage引用

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Modern Chat");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> controller.shutdown());
        stage.show();

        controller.connectToServer("localhost", 9999);
    }

    public static void main(String[] args) {
        launch();
    }
}
