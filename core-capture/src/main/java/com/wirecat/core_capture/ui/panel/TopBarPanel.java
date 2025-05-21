package com.wirecat.core_capture.ui.panel;

import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        setSpacing(10);
        setPadding(new Insets(12, 18, 12, 18));
        getStyleClass().add("topbar-panel");

        Label title = new Label("WIRECAT");
        title.getStyleClass().add("topbar-title");

        filterChips = new HBox(7);
        for (String proto : protocols) {
            CheckBox cb = new CheckBox(proto);
            cb.setSelected(true);
            cb.getStyleClass().add("filter-chip");
            cb.setOnAction(e -> onProtocolsChanged.accept(getSelectedProtocols()));
            filterChips.getChildren().add(cb);
        }

        searchField = new TextField();
        searchField.setPromptText("🔍 Search IP/Port…");
        searchField.setPrefWidth(180);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((o, oldVal, newVal) -> {
            if (onSearch != null) onSearch.accept(newVal);
        });

        autoScrollToggle = new CheckBox("Auto‑scroll");
        autoScrollToggle.setSelected(true);
        autoScrollToggle.getStyleClass().add("auto-scroll-toggle");
        autoScrollToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (onAutoScrollChanged != null) onAutoScrollChanged.accept(newVal);
        });

        aiBtn = new Button("🤖 Ask AI");
        aiBtn.getStyleClass().add("ai-btn");
        aiBtn.setOnAction(e -> { if (onAI != null) onAI.run(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(spark, title, spacer, searchField, filterChips, autoScrollToggle, aiBtn);
    }

    public List<String> getSelectedProtocols() {
        return filterChips.getChildren().stream()
                .filter(node -> ((CheckBox) node).isSelected())
                .map(node -> ((CheckBox) node).getText())
                .toList();
    }
}
