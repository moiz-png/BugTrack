package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.model.ExportFormat;
import com.bugtrack.ui.model.ReportType;
import com.bugtrack.ui.model.ReportView;
import com.bugtrack.ui.model.UserView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.Dialogs;
import com.bugtrack.ui.util.Formats;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

/**
 * Controller for the reports screen: generate a report as a chart and a table, export it
 * to CSV or PDF, and save or reopen it as a serialized snapshot.
 */
public class ReportsController {

    @FXML
    private ComboBox<ReportType> typeCombo;
    @FXML
    private Label reportTitle;
    @FXML
    private Label reportMeta;
    @FXML
    private BarChart<String, Number> chart;
    @FXML
    private TableView<Map.Entry<String, Long>> valuesTable;
    @FXML
    private TableColumn<Map.Entry<String, Long>, String> itemColumn;
    @FXML
    private TableColumn<Map.Entry<String, Long>, Long> valueColumn;
    @FXML
    private Button csvButton;
    @FXML
    private Button pdfButton;
    @FXML
    private Button saveSnapshotButton;

    private ReportView current;

    @FXML
    private void initialize() {
        typeCombo.getItems().setAll(ReportType.values());
        typeCombo.setValue(ReportType.BUGS_BY_STATUS);
        itemColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getKey()));
        valueColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getValue()));
        valuesTable.setPlaceholder(new Label("Generate a report to see its numbers."));
        onGenerate();
    }

    @FXML
    private void onGenerate() {
        ReportType type = typeCombo.getValue();
        UserView viewer = Session.user();
        if (type == null) {
            return;
        }
        Async.run(() -> AppContext.backend().buildReport(type, viewer), report -> show(report, false));
    }

    private void show(ReportView report, boolean fromSnapshot) {
        current = report;
        reportTitle.setText(report.title());
        reportMeta.setText((fromSnapshot ? "Snapshot · " : "") + "Generated " + Formats.dateTime(report.generatedAt())
                + " for " + report.generatedFor());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        report.values().forEach((item, value) -> series.getData().add(new XYChart.Data<>(item, value)));
        chart.getData().clear();
        chart.getData().add(series);
        valuesTable.getItems().setAll(new ArrayList<>(report.values().entrySet()));

        csvButton.setDisable(false);
        pdfButton.setDisable(false);
        saveSnapshotButton.setDisable(false);
    }

    @FXML
    private void onExportCsv() {
        export(ExportFormat.CSV);
    }

    @FXML
    private void onExportPdf() {
        export(ExportFormat.PDF);
    }

    private void export(ExportFormat format) {
        if (current == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export report as " + format.name());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.description(), format.pattern()));
        chooser.setInitialFileName(fileName(current) + format.extension());
        File target = chooser.showSaveDialog(chart.getScene().getWindow());
        if (target == null) {
            return;
        }
        ReportView report = current;
        Async.run(() -> {
            AppContext.backend().exportReport(report, format, target);
            return target;
        }, file -> Dialogs.info("Report exported", "Saved to " + file.getAbsolutePath()));
    }

    @FXML
    private void onSaveSnapshot() {
        if (current == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save report snapshot");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BugTrack snapshot", "*.ser"));
        chooser.setInitialFileName(fileName(current) + ".ser");
        File target = chooser.showSaveDialog(chart.getScene().getWindow());
        if (target == null) {
            return;
        }
        ReportView report = current;
        Async.run(() -> {
            AppContext.backend().saveSnapshot(report, target);
            return target;
        }, file -> Dialogs.info("Snapshot saved",
                "Saved to " + file.getAbsolutePath() + ". Open it later to see this report exactly as it is now."));
    }

    @FXML
    private void onOpenSnapshot() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open report snapshot");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BugTrack snapshot", "*.ser"));
        File source = chooser.showOpenDialog(chart.getScene().getWindow());
        if (source == null) {
            return;
        }
        Async.run(() -> AppContext.backend().loadSnapshot(source), report -> {
            typeCombo.setValue(report.type());
            show(report, true);
        });
    }

    private static String fileName(ReportView report) {
        return "bugtrack-" + report.type().name().toLowerCase().replace('_', '-') + "-" + LocalDate.now();
    }
}
