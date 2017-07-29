package com.johanvz.kmlParser;

import com.jfoenix.controls.JFXDecorator;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        SharedElements.setMainStage(primaryStage);

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(Main.class.getResource("/Main.fxml"));
        AnchorPane root = fxmlLoader.load();

        JFXDecorator jfxDecorator = new JFXDecorator(primaryStage, root);
        jfxDecorator.setCustomMaximize(true);

        Scene scene = new Scene(jfxDecorator, 1280, 800);
        SharedElements.setMainScene(scene);

        final ObservableList<String> sheets = scene.getStylesheets();
        sheets.addAll(
                Main.class.getResource("/css/jfoenix-fonts.css").toExternalForm(),
                Main.class.getResource("/css/jfoenix-design.css").toExternalForm(),
                Main.class.getResource("/css/main.css").toExternalForm()
        );

        primaryStage.setScene(scene);
        primaryStage.show();

    }


    public static void main(String[] args) {
        launch(args);
    }
}
