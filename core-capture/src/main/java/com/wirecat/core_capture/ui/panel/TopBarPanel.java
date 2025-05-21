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
        setPadding(new Insets(7, 16, 7, 16));
        setSpacing(9);
        getStyleClass().add("topbar-panel");

        Label title = new Label("WireCat");
        title.getStyleClass().add("topbar-title");

        searchField = new TextField();
        searchField.setPromptText("Search IP/Port...");
        searchField.setPrefWidth(160);
        searchField.setMinHeight(26);
        searchField.getStyleClass().add("topbar-search");
        searchField.textProperty().addListener((o, oldVal, newVal) -> {
            if (onSearch != null) onSearch.accept(newVal);
        });

        filterChips = new HBox(4);
        filterChips.setAlignment(Pos.CENTER_LEFT);
        for (String proto : protocols) {
            CheckBox cb = new CheckBox(proto);
            cb.setSelected(true);
            cb.getStyleClass().add("filter-chip");
            cb.setMinHeight(22);
            cb.setOnAction(e -> onProtocolsChanged.accept(getSelectedProtocols()));
            filterChips.getChildren().add(cb);
        }

        autoScrollToggle = new CheckBox("Auto‑scroll");
        autoScrollToggle.setSelected(true);
        autoScrollToggle.getStyleClass().add("topbar-autoscroll");
        autoScrollToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (onAutoScrollChanged != null) onAutoScrollChanged.accept(newVal);
        });

        aiBtn = new Button("🤖 Ask AI");
        aiBtn.getStyleClass().add("ai-btn");
        aiBtn.setMinHeight(26);
        aiBtn.setOnAction(e -> { if (onAI != null) onAI.run(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().setAll(title, searchField, filterChips, spacer, autoScrollToggle, aiBtn);
        setMinHeight(40);
        setMaxHeight(40);
    }

    public List<String> getSelectedProtocols() {
        return filterChips.getChildren().stream()
                .filter(node -> ((CheckBox) node).isSelected())
                .map(node -> ((CheckBox) node).getText())
                .toList();
    }
}
