package com.johanvz.kmlParser;

import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.TextFieldEditorBuilder;
import com.jfoenix.controls.cells.editors.base.GenericEditableTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.*;
import com.lynden.gmapsfx.service.geocoding.GeocoderStatus;
import com.lynden.gmapsfx.service.geocoding.GeocodingResult;
import com.lynden.gmapsfx.service.geocoding.GeocodingService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.function.Function;

public class Controller implements Initializable, MapComponentInitializedListener {
    private static final String PREFIX = "( ";
    private static final String POSTFIX = " )";
    private static KMLDocument document;
    private static GeocodingService geocodingService;
    private static GoogleMap map;
    private static StringProperty address = new SimpleStringProperty();
    private static Marker marker;
    private static boolean kmlLoaded = false;

    public Label rowCount;
    public JFXTextField searchField;
    public JFXTreeTableView<KMLDocument.Placemark> treeTableView;
    public JFXTreeTableColumn<KMLDocument.Placemark, String> name;
    public JFXTreeTableColumn<KMLDocument.Placemark, String> description;
    public JFXTreeTableColumn<KMLDocument.Placemark, String> coordinate;
    public JFXDialog warningDialog;
    public Label warningDialogHeader;
    public Label warningDialogBody;
    public JFXSnackbar snackBar;
    public GoogleMapView mapView;
    public AnchorPane root;
    public StackPane centerBorderPane;
    public TextField addressTextField;
    public JFXDialog yesNoDialog;
    public Label yesNoDialogHeader;
    public Label yesNoDialogDialogBody;
    public JFXButton yesNoDialogYes;
    public JFXButton yesNoDialogNo;
    public JFXDialog convertDialog;
    public JFXRadioButton optInputKML;
    public JFXRadioButton optInputKMZ;
    public JFXRadioButton optInputCSV;
    public JFXRadioButton optOutputKML;
    public JFXRadioButton optOutputKMZ;
    public JFXRadioButton optOutputCSV;

    private static JFXDialog sWarningDialog;
    private static Label sWarningDialogHeader;
    private static Label sWarningDialogBody;
    private static StackPane sCenterBorderPane;
    private static JFXSnackbar sSnackBar;
    private static JFXDialog sYesNoDialog;
    private static Label sYesNoDialogHeader;
    private static Label sYesNoDialogDialogBody;
    private static JFXButton sYesNoDialogYes;
    private static JFXButton sYesNoDialogNo;
    private static Controller controller;
    private static File convertInputFile = null;
    private static File convertOutputFile = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        snackBar.registerSnackbarContainer(root);
        mapView.addMapInializedListener(this);
        address.bind(addressTextField.textProperty());

        sWarningDialog = warningDialog;
        sWarningDialogHeader = warningDialogHeader;
        sWarningDialogBody = warningDialogBody;
        sCenterBorderPane = centerBorderPane;
        sSnackBar = snackBar;
        sYesNoDialogHeader = yesNoDialogHeader;
        sYesNoDialogDialogBody = yesNoDialogDialogBody;
        sYesNoDialogYes = yesNoDialogYes;
        sYesNoDialogNo = yesNoDialogNo;
        sYesNoDialog = yesNoDialog;

        controller = this;

    }

    public void openFile() {
        Stage mainStage = SharedElements.getMainStage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file to open");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Keyhole Markup Language", "*.kml"),
                new FileChooser.ExtensionFilter("Keyhole Markup Language Zipped", "*.kmz"),
                new FileChooser.ExtensionFilter("Comma-separated values", "*.csv*"));
        File inputFile = fileChooser.showOpenDialog(mainStage);
        if (inputFile != null) {

            if (inputFile.getName().substring(inputFile.getName().length() - 3).equals("csv")) {
                Controller.showYesNoDialog(
                        "Warning",
                        "This will only work when csv columns are in the following format |Name|Description|Coordinate|" +
                                " \nDo you wish to continue?",
                        event -> {
                            sYesNoDialog.close();
                            Controller.document = new KMLDocument(inputFile);
                            setupTreeTable();
                            mapView.addMapInializedListener(controller);
                            kmlLoaded = true;
                        },
                        event -> sYesNoDialog.close()
                );
            } else {
                Controller.document = new KMLDocument(inputFile);
                setupTreeTable();
                mapView.addMapInializedListener(this);
                kmlLoaded = true;
            }
        }
    }

    private void setupTreeTable() {
        setupCellValueFactory(name, KMLDocument.Placemark::getName);
        setupCellValueFactory(description, KMLDocument.Placemark::getDescription);
        setupCellValueFactory(coordinate, KMLDocument.Placemark::getCoordinate);

        // add editors
        name.setCellFactory((TreeTableColumn<KMLDocument.Placemark, String> param) -> new GenericEditableTreeTableCell<>(
                new TextFieldEditorBuilder()));
        name.setOnEditCommit((TreeTableColumn.CellEditEvent<KMLDocument.Placemark, String> t) -> {
            KMLDocument.Placemark placemark = t.getTreeTableView().getTreeItem(t.getTreeTablePosition().getRow()).getValue();

            placemark.getName().set(t.getNewValue());
            placemark.getNodeName().setTextContent(t.getNewValue());
        });

        description.setCellFactory((TreeTableColumn<KMLDocument.Placemark, String> param) -> new GenericEditableTreeTableCell<>(
                new TextFieldEditorBuilder()));
        description.setOnEditCommit((TreeTableColumn.CellEditEvent<KMLDocument.Placemark, String> t) -> {
            KMLDocument.Placemark placemark = t.getTreeTableView().getTreeItem(t.getTreeTablePosition().getRow()).getValue();

            placemark.getDescription().set(t.getNewValue());
            placemark.getNodeDescription().setTextContent(t.getNewValue());
        });

        coordinate.setCellFactory((TreeTableColumn<KMLDocument.Placemark, String> param) -> new GenericEditableTreeTableCell<>(
                new TextFieldEditorBuilder()));
        coordinate.setOnEditCommit((TreeTableColumn.CellEditEvent<KMLDocument.Placemark, String> t) -> {
            KMLDocument.Placemark placemark = t.getTreeTableView().getTreeItem(t.getTreeTablePosition().getRow()).getValue();

            placemark.getCoordinate().set(t.getNewValue());
            placemark.getNodeCoordinate().setTextContent(t.getNewValue());
        });

        treeTableView.setRoot(new RecursiveTreeItem<>(document.getData(), RecursiveTreeObject::getChildren));
        treeTableView.setShowRoot(false);
        treeTableView.setEditable(true);
        rowCount.textProperty()
                .bind(Bindings.createStringBinding(() -> PREFIX + treeTableView.getCurrentItemsCount() + POSTFIX,
                        treeTableView.currentItemsCountProperty()));

        searchField.textProperty()
                .addListener(setupSearchField(treeTableView));

        treeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            TreeTableView.TreeTableViewSelectionModel<KMLDocument.Placemark> selectionModel = treeTableView.getSelectionModel();

            TreeItem<KMLDocument.Placemark> placemark = selectionModel.getSelectedItem();

            String stringCoord = placemark.getValue().getCoordinate().get();
            String[] splits = stringCoord.split(",");
            if (splits.length >= 2) {
                String latitude = splits[0];
                String longtitude = splits[1];
                double dLatitude = Double.parseDouble(latitude);
                double dLongtitude = Double.parseDouble(longtitude);

                LatLong latLong = new LatLong(dLongtitude, dLatitude);
                map.setCenter(latLong);

                if (marker != null) {
                    marker.setVisible(false);
                    map.removeMarker(marker);
                }

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLong);
                marker = new Marker(markerOptions);
                map.addMarker(marker);

            } else {
                snackBar.fireEvent(new JFXSnackbar.SnackbarEvent(
                        "Contains invalid coordinate",
                        "CLOSE",
                        3000,
                        true,
                        b -> snackBar.close()
                ));
            }
        });
    }

    private ChangeListener<String> setupSearchField(final JFXTreeTableView<KMLDocument.Placemark> tableView) {
        return (o, oldVal, newVal) ->
                tableView.setPredicate(personProp -> {
                    final KMLDocument.Placemark placemark = personProp.getValue();
                    return placemark.name.get().contains(newVal)
                            || placemark.description.get().contains(newVal)
                            || placemark.coordinate.get().contains(newVal);
                });
    }

    private <T> void setupCellValueFactory(JFXTreeTableColumn<KMLDocument.Placemark, T> column, Function<KMLDocument.Placemark, ObservableValue<T>> mapper) {
        column.setCellValueFactory((TreeTableColumn.CellDataFeatures<KMLDocument.Placemark, T> param) -> {
            if (column.validateValue(param)) {
                return mapper.apply(param.getValue().getValue());
            } else {
                return column.getComputedValue(param);
            }
        });
    }

    public void saveAS() {
        if (document == null) {
            showWarningDialog("Error", "Please ensure KML has been loaded!");
            return;
        }

        Stage mainStage = SharedElements.getMainStage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as...");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Keyhole Markup Language", "*.kml"),
                new FileChooser.ExtensionFilter("Keyhole Markup Language Zipped", "*.kmz"),
                new FileChooser.ExtensionFilter("Comma-separated values", "*.csv*"));
        File selectedFile = fileChooser.showSaveDialog(mainStage);

        if (selectedFile == null) return;

        switch (selectedFile.getName().substring(selectedFile.getName().length() - 3)) {
            case "kml":
                FileHandler.saveToKML(selectedFile, document.getKmlDocument());
                break;
            case "kmz":
                FileHandler.saveToKMZ(selectedFile, document.getKmlDocument());
                break;
            case "csv":
                FileHandler.saveDocumentToCSV(document.getData(), selectedFile);
                break;
        }

    }

    public void closeDialog() {
        warningDialog.close();
    }

    @Override
    public void mapInitialized() {
        geocodingService = new GeocodingService();
        MapOptions mapOptions = new MapOptions();

        mapOptions.center(new LatLong(-37.829003, 147.619331))
                .mapType(MapTypeIdEnum.ROADMAP)
                .overviewMapControl(false)
                .panControl(false)
                .rotateControl(false)
                .scaleControl(false)
                .streetViewControl(false)
                .zoomControl(false)
                .zoom(12);

        map = mapView.createMap(mapOptions);
    }

    public void searchAddress() {
        geocodingService.geocode(address.get(), (GeocodingResult[] results, GeocoderStatus status) -> {

            LatLong latLong;

            if (status == GeocoderStatus.ZERO_RESULTS) {
                warningDialogHeader.setText("Error");
                warningDialogBody.setText("No matching address found");
                warningDialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
                warningDialog.show(centerBorderPane);
                return;
            } else if (results.length > 1) {
                warningDialogHeader.setText("Error");
                warningDialogBody.setText("Multiple results found, showing the first one.");
                warningDialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
                warningDialog.show(centerBorderPane);
                latLong = new LatLong(results[0].getGeometry().getLocation().getLatitude(), results[0].getGeometry().getLocation().getLongitude());
            } else {
                latLong = new LatLong(results[0].getGeometry().getLocation().getLatitude(), results[0].getGeometry().getLocation().getLongitude());
            }

            map.setCenter(latLong);

        });
    }

    public void showConvertFilesDialog() {
        convertDialog.show(sCenterBorderPane);
    }

    public void CSVtoKML() {
        Stage mainStage = SharedElements.getMainStage();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV to convert");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File inputFile = fileChooser.showOpenDialog(mainStage);
        if (inputFile != null) {
            String SoutputFile = inputFile.getAbsolutePath().replace(".csv", ".kml");
            File outputFile = new File(SoutputFile);
            while (outputFile.exists()) {
                SoutputFile = SoutputFile.replace(".kml", " copy.kml");
                outputFile = new File(SoutputFile);
            }

            new CSVDocument(inputFile, outputFile);
        }
    }

    public static void showWarningDialog(String headerDialog, String bodyDialog) {
        if (sWarningDialogHeader == null ||
                sWarningDialogBody == null ||
                sWarningDialog == null ||
                sCenterBorderPane == null) return;
        sWarningDialogHeader.setText(headerDialog);
        sWarningDialogBody.setText(bodyDialog);
        sWarningDialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
        sWarningDialog.show(sCenterBorderPane);
    }

    public static void showSnackBar(String message) {
        if (sSnackBar == null) return;

        sSnackBar.fireEvent(new JFXSnackbar.SnackbarEvent(
                message,
                "CLOSE",
                3000,
                true,
                b -> sSnackBar.close()
        ));
    }

    public static void showYesNoDialog(String header, String body,
                                       EventHandler<ActionEvent> yesEvent,
                                       EventHandler<ActionEvent> noEvent) {
        sYesNoDialogHeader.setText(header);
        sYesNoDialogDialogBody.setText(body);

        sYesNoDialogYes.setOnAction(yesEvent);
        sYesNoDialogNo.setOnAction(noEvent);
        sYesNoDialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
        sYesNoDialog.show(sCenterBorderPane);
    }

    public void closeConvertDialog() {
        convertDialog.close();
    }

    public void convertSelectInput() {
        Stage mainStage = SharedElements.getMainStage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file to open");
        if(optInputKML.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Keyhole Markup Language", "*.kml"));
        } else if (optInputKMZ.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Keyhole Markup Language Zipped", "*.kmz"));
        } else if (optInputCSV.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma-separated values", "*.csv*"));
        }

        File inputFile = fileChooser.showOpenDialog(mainStage);
        if(inputFile != null) {
            convertInputFile = inputFile;
        }
    }

    public void convertSelectOutput() {
        Stage mainStage = SharedElements.getMainStage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file to convert to");
        if(optOutputKML.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Keyhole Markup Language", "*.kml"));
        } else if (optOutputKMZ.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Keyhole Markup Language Zipped", "*.kmz"));
        } else if (optOutputCSV.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma-separated values", "*.csv*"));
        }

        File outputFile = fileChooser.showSaveDialog(mainStage);
        if(outputFile != null) {
            convertOutputFile = outputFile;
        }
    }

    public void convertFiles() {
        if(convertInputFile == null) {
            Controller.showWarningDialog("Error", "Please select input source file");
            return;
        }
        if(convertOutputFile == null) {
            Controller.showWarningDialog("Error", "Please select output source file");
            return;
        }

        Globals.FileTypes inputType;
        Globals.FileTypes outputType;
        switch (convertInputFile.getName().substring(convertInputFile.getName().length() - 3)) {
            case "kml":
                inputType = Globals.FileTypes.KML;
                break;
            case "kmz":
                inputType = Globals.FileTypes.KMZ;
                break;
            case "csv":
                inputType = Globals.FileTypes.CSV;
                break;
            default:
                Controller.showSnackBar("Unknown Error Occurred");
                return;
        }
        switch (convertOutputFile.getName().substring(convertOutputFile.getName().length() - 3)) {
            case "kml":
                outputType = Globals.FileTypes.KML;
                break;
            case "kmz":
                outputType = Globals.FileTypes.KMZ;
                break;
            case "csv":
                outputType = Globals.FileTypes.CSV;
                break;
            default:
                Controller.showSnackBar("Unknown Error Occurred");
                return;
        }

        if(inputType == outputType) {
            Controller.showWarningDialog("Don't be silly",
                    "Please select different file types" +
                    " as input/output appear to be the same!");
            return;
        }

        System.out.println("Good to go");

    }
}
