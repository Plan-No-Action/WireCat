package com.wirecat.core_capture;

import javafx.application.Application;
import javafx.stage.Stage;

public class WireCatApp extends Application {

    @Override
    public void start(Stage stage) {
        new SettingsView().show(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}