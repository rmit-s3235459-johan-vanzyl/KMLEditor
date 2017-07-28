package com.johanvz.kmlParser;

import com.jfoenix.controls.*;
import com.jfoenix.controls.cells.editors.TextFieldEditorBuilder;
import com.jfoenix.controls.cells.editors.base.GenericEditableTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.function.Function;

public class Controller implements Initializable {
    private static final String PREFIX = "( ";
    private static final String POSTFIX = " )";
    private static KMLDocument document;
    public StackPane root;
    public Label rowCount;
    public JFXTextField searchField;
    public JFXTreeTableView<KMLDocument.Placemark> treeTableView;
    public JFXTreeTableColumn<KMLDocument.Placemark, String> name;
    public JFXTreeTableColumn<KMLDocument.Placemark, String> description;
    public JFXTreeTableColumn<KMLDocument.Placemark, String> coordinate;
    public BorderPane borderPane;
    public VBox centerBorderPane;
    public JFXDialog warningDialog;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

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
            warningDialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
            warningDialog.show(root);
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
                if(selectedFile.exists()) {
                    selectedFile.delete();
                }
                selectedFile.createNewFile();
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
}
