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
    public JFXDialog yesNoCancelDialog;
    public Label yesNoCanceDialogHeader;
    public Label yesNoCanceDialogBody;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        snackBar.registerSnackbarContainer(root);
        mapView.addMapInializedListener(this);
        address.bind(addressTextField.textProperty());
    }

    public void importKML() {
        Stage mainStage = SharedElements.getMainStage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select KML to import");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Keyhole Markup Language", "*.kml"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(mainStage);
        if (selectedFile != null) {
            Controller.document = new KMLDocument(selectedFile);
            setupTreeTable();
        }

        mapView.addMapInializedListener(this);
        kmlLoaded = true;
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

    public void saveKML() {
        if (document == null) {
            warningDialogHeader.setText("Error");
            warningDialogBody.setText("Please ensure KML has been loaded!");
            warningDialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
            warningDialog.show(centerBorderPane);
            return;
        }

        Stage mainStage = SharedElements.getMainStage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save work to KML file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Keyhole Markup Language", "*.kml"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showSaveDialog(mainStage);


        if (selectedFile != null) {
            Document xmlDocument = document.getKmlDocument();

            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                if (selectedFile.exists()) {
                    while (!selectedFile.delete()) {
                        warningDialogHeader.setText("Error");
                        warningDialogBody.setText("Could not delete file \"" + selectedFile.getAbsolutePath() + "\". Please ensure it is closed!");
                        warningDialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
                        warningDialog.show(centerBorderPane);
                    }
                }
                if (selectedFile.createNewFile()) {
                    snackBar.fireEvent(new JFXSnackbar.SnackbarEvent(
                            "Saved successfully",
                            "CLOSE",
                            3000,
                            true,
                            b -> snackBar.close()
                    ));
                } else {
                    snackBar.fireEvent(new JFXSnackbar.SnackbarEvent(
                            "Unsuccessful",
                            "CLOSE",
                            3000,
                            true,
                            b -> snackBar.close()
                    ));
                }
                Result output = new StreamResult(selectedFile);
                Source input = new DOMSource(xmlDocument);
                transformer.transform(input, output);
            } catch (IOException | TransformerException e) {
                e.printStackTrace();
            }
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

    public void toCSV() {
        if (!kmlLoaded) {
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent(
                    "Please load KML first",
                    "CLOSE",
                    3000,
                    true,
                    b -> snackBar.close()
            ));
            return;
        }

        Stage mainStage = SharedElements.getMainStage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save work to CSV file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showSaveDialog(mainStage);
        if(selectedFile != null) {
            try {
                StringBuilder stringBuilder = new StringBuilder();

                for (KMLDocument.Placemark placemark : document.getData()) {
                    stringBuilder.append('"').append(placemark.getName().get()).append('"');
                    stringBuilder.append(',');
                    stringBuilder.append('"').append(placemark.getDescription().get()).append('"');
                    stringBuilder.append(',');
                    stringBuilder.append('"').append(placemark.getCoordinate().get()).append('"');
                    stringBuilder.append('\n');
                }

                FileWriter fileWriter = new FileWriter(selectedFile.getAbsoluteFile());

                String toWrite = stringBuilder.toString().replace("&", "&amp;");

                fileWriter.append(toWrite);
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void CSVtoKML() {
        Stage mainStage = SharedElements.getMainStage();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV to convert");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File inputFile = fileChooser.showOpenDialog(mainStage);
        if(inputFile != null) {
            String SoutputFile = inputFile.getAbsolutePath().replace(".csv", ".kml");
            File outputFile = new File(SoutputFile);
            while(outputFile.exists()) {
                SoutputFile = SoutputFile.replace(".kml", " copy.kml");
                outputFile = new File(SoutputFile);
            }

            new CSVDocument(inputFile, outputFile);
        }

    }

    public void CSVtoKMZ() {

    }
}
