package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.Conversation;
import com.wirecat.core_capture.model.CapturedPacket;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class RightPanel extends VBox {
    private static final double PANEL_WIDTH = 250; // <= tweak to taste

    public RightPanel(
            ObservableList<Conversation> conversationList,
            Consumer<Conversation> onConversationSelect,
            ObservableList<CapturedPacket> packets,
            XYChart.Series<String, Number> protoSeries,
            Stage stage
    ) {
        setFillWidth(true);
        setPrefWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH);
        setMinWidth(PANEL_WIDTH);

        // Conversation Table
        ConversationPanel conversationPanel = new ConversationPanel(conversationList, onConversationSelect);
        conversationPanel.setMaxWidth(PANEL_WIDTH);
        conversationPanel.setPrefWidth(PANEL_WIDTH);

        // Scroll if too many convos
        ScrollPane convScroll = new ScrollPane(conversationPanel);
        convScroll.setFitToWidth(true);
        convScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        convScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        convScroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        convScroll.setMaxWidth(PANEL_WIDTH);
        convScroll.setPrefWidth(PANEL_WIDTH);
        convScroll.setMinWidth(PANEL_WIDTH);

        // Statistics Panel
        StatisticsPanel statisticsPanel = new StatisticsPanel(packets, protoSeries, stage);
        statisticsPanel.setMaxWidth(PANEL_WIDTH);
        statisticsPanel.setPrefWidth(PANEL_WIDTH);
        statisticsPanel.setMinWidth(PANEL_WIDTH);

        // Use SplitPane (vertical): conversations top, stats bottom
        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.getItems().addAll(convScroll, statisticsPanel);

        // 60% convos, 40% stats
        split.setDividerPositions(0.6);
        split.setMaxWidth(PANEL_WIDTH);
        split.setPrefWidth(PANEL_WIDTH);
        split.setMinWidth(PANEL_WIDTH);

        VBox.setVgrow(split, Priority.ALWAYS);
        getChildren().add(split);
    }
}
