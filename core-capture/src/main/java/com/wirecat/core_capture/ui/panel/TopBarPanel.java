package com.wirecat.core_capture.ui.panel;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.function.Consumer;

public class TopBarPanel extends HBox {
    public final TextField searchField;
    public final HBox filterChips;
    public final CheckBox autoScrollToggle;
    public final Button aiBtn;

    public TopBarPanel(LineChart<Number, Number> spark,
                       List<String> protocols,
                       Consumer<String> onSearch,
                       Consumer<List<String>> onProtocolsChanged,
                       Consumer<Boolean> onAutoScrollChanged,
                       Runnable onAI) {
        setSpacing(18);
        setPadding(new Insets(12, 20, 12, 18));
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("topbar-panel");

        // App Title
        Label title = new Label("🐾 WIRECAT");
        title.getStyleClass().add("topbar-title");
        title.setMinWidth(120);

        // Filter chips (styled in CSS)
        filterChips = new HBox(6);
        filterChips.getStyleClass().add("filter-chips");
        for (String proto : protocols) {
            CheckBox cb = new CheckBox(proto);
            cb.setSelected(true);
            cb.getStyleClass().add("filter-chip");
            cb.setOnAction(e -> onProtocolsChanged.accept(getSelectedProtocols()));
            filterChips.getChildren().add(cb);
        }

        // Search
        searchField = new TextField();
        searchField.setPromptText("🔍 Search IP/Port...");
        searchField.getStyleClass().add("topbar-search");
        searchField.setPrefWidth(190);
        searchField.textProperty().addListener((o, oldVal, newVal) -> {
            if (onSearch != null) onSearch.accept(newVal);
        });

        // Auto-scroll
        autoScrollToggle = new CheckBox("Auto‑scroll");
        autoScrollToggle.getStyleClass().add("auto-scroll-toggle");
        autoScrollToggle.setSelected(true);
        autoScrollToggle.setMinWidth(110);
        autoScrollToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (onAutoScrollChanged != null) onAutoScrollChanged.accept(newVal);
        });

        // AI Button
        aiBtn = new Button("🤖 Ask AI");
        aiBtn.getStyleClass().add("ai-btn");
        aiBtn.setMinWidth(90);
        aiBtn.setPrefHeight(34);
        aiBtn.setOnAction(e -> { if (onAI != null) onAI.run(); });

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Sparkline chart
        spark.setPrefWidth(170);
        spark.setMinHeight(48);
        spark.setMaxHeight(54);

        getChildren().addAll(spark, title, spacer, searchField, filterChips, autoScrollToggle, aiBtn);
    }

    public List<String> getSelectedProtocols() {
        return filterChips.getChildren().stream()
                .filter(node -> ((CheckBox) node).isSelected())
                .map(node -> ((CheckBox) node).getText())
                .toList();
    }
}
