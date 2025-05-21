package com.wirecat.core_capture;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class PacketAnalysisView {
    private final TableView<PacketModel> packetTable = new TableView<>();
    private final ObservableList<PacketModel> packetData = FXCollections.observableArrayList();
    private final CaptureService captureService = new CaptureService();
    private final AIAnalysisService aiService = new AIAnalysisService();

    public void show(Stage stage) {
        setupTable();
        setupToolbar(stage);
        BorderPane root = new BorderPane();
        root.setCenter(packetTable);
        stage.setTitle("WireCat - Packet Analysis");
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
        startCapture();
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {
        TableColumn<PacketModel, String> srcCol = new TableColumn<>("Source");
        srcCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSrc()));
        TableColumn<PacketModel, String> dstCol = new TableColumn<>("Destination");
        dstCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDst()));
        TableColumn<PacketModel, String> protoCol = new TableColumn<>("Protocol");
        protoCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProto()));

        packetTable.getColumns().addAll(srcCol, dstCol, protoCol);
        packetTable.setItems(packetData);
    }

    private void setupToolbar(Stage stage) {
        ToolBar toolbar = new ToolBar();
        Button startBtn = new Button("Start");
        startBtn.setOnAction(e -> startCapture());
        Button stopBtn = new Button("Stop");
        stopBtn.setOnAction(e -> captureService.stopCapture());
        toolbar.getItems().addAll(startBtn, stopBtn);
        ((BorderPane) packetTable.getParent()).setTop(toolbar);
    }

    private void startCapture() {
        packetData.clear();
        captureService.setOnPacketCaptured(packet -> {
            aiService.analyzeAsync(packet, updated -> {
                Platform.runLater(() -> {
                    int index = packetData.indexOf(updated);
                    if (index >= 0) packetData.set(index, updated);
                    else packetData.add(updated);
                });
            });
        });
        captureService.startCapture("eth0", "", 50);
    }
}
