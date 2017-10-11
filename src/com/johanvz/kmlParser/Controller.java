package com.johanvz.kmlParser;

import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.TextFieldEditorBuilder;
import com.jfoenix.controls.cells.editors.base.GenericEditableTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import com.johanvz.kmlParser.model.Client;
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.event.GMapMouseEvent;
import com.lynden.gmapsfx.javascript.event.UIEventType;
import com.lynden.gmapsfx.javascript.object.*;
import com.lynden.gmapsfx.service.geocoding.GeocoderStatus;
import com.lynden.gmapsfx.service.geocoding.GeocodingResult;
import com.lynden.gmapsfx.service.geocoding.GeocodingService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

public class Controller implements Initializable, MapComponentInitializedListener {
    private static final String PREFIX = "( ";
    private static final String POSTFIX = " )";
    private static KMLDocument document;
    private static GeocodingService geocodingService;
    private static GoogleMap map;
    private static StringProperty address = new SimpleStringProperty();
    private static final ArrayList<Marker> markers = new ArrayList<>();
    private static Controller controller;
    private static File convertInputFile = null;
    private static File convertOutputFile = null;

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
    public JFXDialog addPlaceMarkDialog;
    public JFXTextField addPlaceMarkDialogName;
    public JFXTextField addPlaceMarkDialogCoordinate;
    public JFXTextField addPlaceMarkDialogDescription;
    public JFXDialog uploadDialog;
    public JFXTextField ulUserID;
    public JFXPasswordField ulUserPW;

    // static content for other classes
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
                        },
                        event -> sYesNoDialog.close()
                );
            } else {
                Controller.document = new KMLDocument(inputFile);
                setupTreeTable();
                mapView.addMapInializedListener(this);
            }
        }
    }

    private void setupTreeTable() {
        setupCellValueFactory(name, KMLDocument.Placemark::getName);
        setupCellValueFactory(description, KMLDocument.Placemark::getDescription);
        setupCellValueFactory(coordinate, KMLDocument.Placemark::getCoordinate);
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

        RecursiveTreeItem<KMLDocument.Placemark> recursiveTreeItem = new RecursiveTreeItem<>(document.getData(), RecursiveTreeObject::getChildren);

        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTableView.setRoot(recursiveTreeItem);
        treeTableView.setShowRoot(false);
        treeTableView.setEditable(true);
        rowCount.textProperty()
                .bind(Bindings.createStringBinding(() -> PREFIX + treeTableView.getCurrentItemsCount() + POSTFIX,
                        treeTableView.currentItemsCountProperty()));

        searchField.textProperty()
                .addListener(setupSearchField(treeTableView));

        treeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            markers.forEach(e -> {
                e.setVisible(false);
                map.removeMarker(e);
                e = null;
            });

            TreeTableView.TreeTableViewSelectionModel<KMLDocument.Placemark> selectionModel = treeTableView.getSelectionModel();
            ObservableList<TreeTablePosition<KMLDocument.Placemark, ?>> selectedList = selectionModel.getSelectedCells();
            final LatLong[] latLong = {null};
            selectedList.forEach(e -> {
                KMLDocument.Placemark placemark = e.getTreeItem().getValue();
                String stringCoord = placemark.getCoordinate().get();
                String[] splits = stringCoord.split(",");
                if (splits.length >= 2) {
                    String latitude = splits[0];
                    String longtitude = splits[1];
                    double dLatitude = Double.parseDouble(latitude);
                    double dLongtitude = Double.parseDouble(longtitude);

                    MarkerOptions markerOptions = new MarkerOptions();
                    latLong[0] = new LatLong(dLongtitude, dLatitude);
                    markerOptions.position(latLong[0]);
                    Marker marker = new Marker(markerOptions);
                    map.addMarker(marker);

                    markers.add(marker);

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
            if (latLong[0] != null) map.setCenter(latLong[0]);
            System.gc();
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
        map.addMouseEventHandler(UIEventType.rightclick, (GMapMouseEvent event) -> {
            LatLong latLong = event.getLatLong();
            double latitude = latLong.getLatitude();
            double longitude = latLong.getLongitude();
            addPlaceMarkDialogCoordinate.setText(Double.toString(longitude).concat(",").concat(Double.toString(latitude)));
            addPlaceMarkDialog.show(centerBorderPane);
        });
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
                warningDialog.requestFocus();
                latLong = new LatLong(results[0].getGeometry().getLocation().getLatitude(), results[0].getGeometry().getLocation().getLongitude());
            } else {
                latLong = new LatLong(results[0].getGeometry().getLocation().getLatitude(), results[0].getGeometry().getLocation().getLongitude());
            }

            map.setCenter(latLong);
            markers.forEach(e -> {
                e.setVisible(false);
                map.removeMarker(e);
                e = null;
            });

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLong);
            Marker marker = new Marker(markerOptions);
            map.addMarker(marker);
            map.setZoom(18);

            markers.add(marker);
            System.gc();

        });
    }

    public void showConvertFilesDialog() {
        convertDialog.show(sCenterBorderPane);
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
        if (optInputKML.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Keyhole Markup Language", "*.kml"));
        } else if (optInputKMZ.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Keyhole Markup Language Zipped", "*.kmz"));
        } else if (optInputCSV.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma-separated values", "*.csv*"));
        }

        File inputFile = fileChooser.showOpenDialog(mainStage);
        if (inputFile != null) {
            convertInputFile = inputFile;
        }
    }

    public void convertSelectOutput() {
        Stage mainStage = SharedElements.getMainStage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file to convert to");
        if (optOutputKML.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Keyhole Markup Language", "*.kml"));
        } else if (optOutputKMZ.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Keyhole Markup Language Zipped", "*.kmz"));
        } else if (optOutputCSV.isSelected()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma-separated values", "*.csv*"));
        }

        File outputFile = fileChooser.showSaveDialog(mainStage);
        if (outputFile != null) {
            convertOutputFile = outputFile;
        }
    }

    public void convertFiles() {
        if (convertInputFile == null) {
            Controller.showWarningDialog("Error", "Please select input source file");
            return;
        }
        if (convertOutputFile == null) {
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

        if (inputType == outputType) {
            Controller.showWarningDialog("Don't be silly",
                    "Please select different file types" +
                            " as input/output appear to be the same!");
            return;
        }

        Document document = null;
        switch (inputType) {
            case KML:
                document = FileHandler.openFromKML(convertInputFile);
                break;
            case KMZ:
                document = FileHandler.openFromKMZ(convertInputFile);
                break;
            case CSV:
                document = FileHandler.openFromCSV(convertInputFile);
                break;
        }

        if (document == null) return;

        switch (outputType) {
            case KML:
                FileHandler.saveToKML(convertOutputFile, document);
                break;
            case KMZ:
                FileHandler.saveToKMZ(convertOutputFile, document);
                break;
            case CSV:
                ObservableList<KMLDocument.Placemark> data = FXCollections.observableArrayList();
                KMLDocument.loadFromDocument(data, document);
                FileHandler.saveDocumentToCSV(data, convertOutputFile);
                break;
        }

        convertDialog.close();
        convertInputFile = null;
        convertOutputFile = null;
    }

    public void treeListener(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.DELETE) {

            //messy, could be optimised?

            ObservableList<TreeTablePosition<KMLDocument.Placemark, ?>> items = treeTableView.getSelectionModel().getSelectedCells();
            ArrayList<TreeItem<KMLDocument.Placemark>> copies = new ArrayList<>(items.size());

            items.forEach(e -> copies.add(e.getTreeItem()));
            treeTableView.getSelectionModel().clearSelection();
            copies.forEach(e -> {
                //TreeItem<KMLDocument.Placemark> placemark = e.getTreeItem();
                //placemark.getParent().getChildren().remove(placemark);
                //placemark.getValue().getReference().getParentNode().removeChild(placemark.getValue().getReference());
                //System.out.println(recursiveTreeItem.getChildren().remove(e));
                for (int i = 0; i < document.getData().size(); i++) {
                    if (document.getData().get(i).getCoordinate().equals(e.getValue().getCoordinate()))
                        document.getData().remove(i);
                }
            });
            treeTableView.currentItemsCountProperty().setValue(document.getData().size());
            treeTableView.refresh();
            System.gc();
        }
    }

    public void closeAddPlaceMarkDialog() {
        addPlaceMarkDialog.close();
    }

    public void addPlaceMark() {
        if (document == null) {
            showWarningDialog("Error", "Please ensure KML has been loaded!");
            return;
        }

        Document documentM = document.getKmlDocument();
        NodeList nodeList = documentM.getElementsByTagName("Placemark");
        Node lastNode = nodeList.item(nodeList.getLength() - 1);

        Element newPlacemark = documentM.createElement("Placemark");
        Element newName = documentM.createElement("name");
        newName.setTextContent(addPlaceMarkDialogName.getText());
        newPlacemark.appendChild(newName);

        Element newDescription = documentM.createElement("description");
        newDescription.setTextContent(addPlaceMarkDialogDescription.getText());
        newPlacemark.appendChild(newDescription);

        Element newStyle = documentM.createElement("Style");
        Element newIconStyle = documentM.createElement("scale");
        Element newScale = documentM.createElement("scale");
        newScale.setTextContent("0.9");
        Element newIcon = documentM.createElement("Icon");
        Element newHref = documentM.createElement("href");
        newHref.setTextContent("http://maps.google.com/mapfiles/kml/pushpin/blue-pushpin.png");

        newIcon.appendChild(newHref);
        newIconStyle.appendChild(newScale);
        newIconStyle.appendChild(newIcon);
        newStyle.appendChild(newIconStyle);
        newPlacemark.appendChild(newStyle);

        Element newPoint = documentM.createElement("Point");
        Element newExtrude = documentM.createElement("extrude");
        newExtrude.setTextContent("0");
        newPoint.appendChild(newExtrude);
        Element newAltitudeMode = documentM.createElement("altitudeMode");
        newAltitudeMode.setTextContent("clampToGround");
        newPoint.appendChild(newAltitudeMode);
        Element newCoordinates = documentM.createElement("coordinates");
        newCoordinates.setTextContent(addPlaceMarkDialogCoordinate.getText());
        newPoint.appendChild(newCoordinates);


        newPlacemark.appendChild(newPoint);
        lastNode.getParentNode().appendChild(newPlacemark);

        document.getData().add(new KMLDocument.Placemark(
                newName.getTextContent(),
                newDescription.getTextContent(),
                newCoordinates.getTextContent(),
                newPlacemark,
                newName,
                newDescription,
                newCoordinates

        ));
        treeTableView.currentItemsCountProperty().setValue(document.getData().size());
        treeTableView.refresh();
        addPlaceMarkDialog.close();
    }

    public void uploadSelectedClients() {
        if(ulUserID.getText().length() < 6) {
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent(
                    "Username too short",
                    "CLOSE",
                    3000,
                    true,
                    b -> snackBar.close()
            ));
            return;
        }
        if(ulUserPW.getText().length() < 6) {
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent(
                    "Password too short",
                    "CLOSE",
                    3000,
                    true,
                    b -> snackBar.close()
            ));
            return;
        }

        snackBar.fireEvent(new JFXSnackbar.SnackbarEvent(
                "Uploading..",
                "CLOSE",
                3000,
                true,
                b -> snackBar.close()
        ));

        String username = ulUserID.getText();
        String password = ulUserPW.getText();


        TreeTableView.TreeTableViewSelectionModel<KMLDocument.Placemark> selectionModel = treeTableView.getSelectionModel();
        ObservableList<TreeTablePosition<KMLDocument.Placemark, ?>> selectedList = selectionModel.getSelectedCells();
        Connector connector = new Connector(username, password);

        new Thread(() -> {
            selectedList.forEach(e -> {
                KMLDocument.Placemark placemark = e.getTreeItem().getValue();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                String response = connector.postClient(
                        new Client(placemark.getName().get(),
                                placemark.getDescription().get(),
                                placemark.getCoordinate().get())
                );

                if(!response.contains("success")) {
                    System.out.println("ERROR, SOMETHING WENT WRONG");
                    System.out.println(response);
                }
            });
            closeUploadDialog();
        }).start();

    }

    public void closeUploadDialog() {
        uploadDialog.close();
    }

    public void showUploadDialog() {
        uploadDialog.show(centerBorderPane);
    }
}
