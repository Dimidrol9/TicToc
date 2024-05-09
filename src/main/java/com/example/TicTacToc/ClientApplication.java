package com.example.TicTacToc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApplication extends Application {

    @Override
    public void start(Stage Stage)throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientController.class.getResource("client-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage.getTitle();
        Stage.setScene(scene);
        Stage.show();


    }
    public static void main(String[] args) {
        launch(args);
    }


}
