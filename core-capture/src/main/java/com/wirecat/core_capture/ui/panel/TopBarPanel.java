package com.wirecat.core_capture.ui.panel;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.function.Consumer;

public class TopBarPanel extends HBox {
    public final TextField searchField;
    public final HBox filterChips;
    public final CheckBox autoScrollToggle;
    public final Button aiBtn;

    public TopBarPanel(
            List<String> protocols,
            Consumer<String> onSearch,
            Consumer<List<String>> onProtocolsChanged,
            Consumer<Boolean> onAutoScrollChanged,
            Runnable onAI
    ) {
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(8, 14, 8, 14));
        setSpacing(10);
        setStyle("-fx-background-color: #22292b; -fx-border-color: #33383c; -fx-border-width: 0 0 2 0;");

        Label title = new Label("WireCat");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 1.14em; -fx-text-fill: #70fa95;");

        searchField = new TextField();
        searchField.setPromptText("Search IP/Port...");
        searchField.setPrefWidth(170);
        searchField.setMinHeight(28);
        searchField.setStyle("-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-border-color: #47ff6f; -fx-background-color: #262b2d; -fx-text-fill: #e0e0e0; -fx-font-size: 13px;");
        searchField.textProperty().addListener((o, oldVal, newVal) -> {
            if (onSearch != null) onSearch.accept(newVal);
        });

        filterChips = new HBox(5);
        filterChips.setAlignment(Pos.CENTER_LEFT);
        for (String proto : protocols) {
            CheckBox cb = new CheckBox(proto);
            cb.setSelected(true);
            cb.getStyleClass().add("filter-chip");
            cb.setMinHeight(24);
            cb.setStyle("-fx-background-radius: 11px; -fx-padding: 1 9 1 9;");
            cb.setOnAction(e -> onProtocolsChanged.accept(getSelectedProtocols()));
            filterChips.getChildren().add(cb);
        }

        autoScrollToggle = new CheckBox("Auto‑scroll");
        autoScrollToggle.setSelected(true);
        autoScrollToggle.setStyle("-fx-font-size: 12px; -fx-text-fill: #b8ffca;");
        autoScrollToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (onAutoScrollChanged != null) onAutoScrollChanged.accept(newVal);
        });

        aiBtn = new Button("🤖 Ask AI");
        aiBtn.setStyle("-fx-background-color: #70fa95; -fx-text-fill: #23272b; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 0 16 0 16; -fx-font-size: 13px;");
        aiBtn.setMinHeight(28);
        aiBtn.setOnAction(e -> { if (onAI != null) onAI.run(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().setAll(title, searchField, filterChips, spacer, autoScrollToggle, aiBtn);
        setMinHeight(44);
        setMaxHeight(44);
    }

    public List<String> getSelectedProtocols() {
        return filterChips.getChildren().stream()
                .filter(node -> ((CheckBox) node).isSelected())
                .map(node -> ((CheckBox) node).getText())
                .toList();
    }
}
