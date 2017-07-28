package com.johanvz.kmlParser;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class KMLDocument {
    private File file;
    private final ObservableList<Placemark> data = FXCollections.observableArrayList();
    private Document kmlDocument;
    private NodeList nodeList;

    public KMLDocument(File file) {
        this.file = file;
        loadFile();
    }

    private void loadFile() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            kmlDocument = documentBuilder.parse(file);
            kmlDocument.getDocumentElement().normalize();

            nodeList = kmlDocument.getElementsByTagName("Placemark");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element element = (Element) node;
                NodeList names = element.getElementsByTagName("name");
                if(names.getLength() > 0) {
                    NodeList description = element.getElementsByTagName("description");
                    NodeList coordinates = element.getElementsByTagName("coordinates");

                    Node name = names.item(0);
                    Node descriptionNode;
                    Node coordinateNode;

                    if(description.getLength() > 0) {
                        descriptionNode = description.item(0);
                    } else {
                        Element newDescription = kmlDocument.createElement("description");
                        newDescription.setTextContent("NULL");
                        node.appendChild(newDescription);
                        descriptionNode = newDescription;
                    }

                    if(coordinates.getLength() > 0) {
                        coordinateNode = coordinates.item(0);
                    } else {
                        Element newCoordinate = kmlDocument.createElement("coordinates");
                        newCoordinate.setTextContent("NULL");
                        node.appendChild(newCoordinate);
                        coordinateNode = newCoordinate;
                    }

                    data.add(new Placemark(
                            name.getTextContent(),
                            descriptionNode.getTextContent(),
                            coordinateNode.getTextContent(),
                            name,
                            descriptionNode,
                            coordinateNode
                    ));
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public ObservableList<Placemark> getData() {
        return data;
    }

    public Document getKmlDocument() {
        return kmlDocument;
    }

    public NodeList getNodeList() {
        return nodeList;
    }

    static final class Placemark extends RecursiveTreeObject<Placemark> {
        final StringProperty name;
        final StringProperty description;
        final StringProperty coordinate;
        final Node nodeName, nodeDescription, nodeCoordinate;

        Placemark(String name, String description, String coordinate, Node nodeName, Node nodeDescription, Node nodeCoordinate) {
            this.name = new SimpleStringProperty(name);
            this.description = new SimpleStringProperty(description);
            this.coordinate = new SimpleStringProperty(coordinate);
            this.nodeName = nodeName;
            this.nodeDescription = nodeDescription;
            this.nodeCoordinate = nodeCoordinate;
        }

        StringProperty getName() {
            return name;
        }

        public void setName(String name) {
            this.name.set(name);
        }

        StringProperty getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description.set(description);
        }

        StringProperty getCoordinate() {
            return coordinate;
        }

        public void setCoordinate(String coordinate) {
            this.coordinate.set(coordinate);
        }


        public Node getNodeName() {
            return nodeName;
        }

        public Node getNodeDescription() {
            return nodeDescription;
        }

        public Node getNodeCoordinate() {
            return nodeCoordinate;
        }
    }
}
