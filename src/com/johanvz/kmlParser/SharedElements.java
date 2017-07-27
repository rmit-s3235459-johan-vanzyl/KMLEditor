package com.johanvz.kmlParser;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class SharedElements {
    private static Stage mainStage;
    private static Scene mainScene;
    private static StackPane mainStackPane;

    public static Stage getMainStage() {
        return mainStage;
    }

    public static void setMainStage(Stage mainStage) {
        if(SharedElements.mainStage == null) SharedElements.mainStage = mainStage;
    }

    public static Scene getMainScene() {
        return mainScene;
    }

    public static void setMainScene(Scene mainScene) {
        if(SharedElements.mainScene == null) SharedElements.mainScene = mainScene;
    }

    public static StackPane getMainStackPane() {
        return mainStackPane;
    }

    public static void setMainStackPane(StackPane mainStackPane) {
        SharedElements.mainStackPane = mainStackPane;
    }
}
