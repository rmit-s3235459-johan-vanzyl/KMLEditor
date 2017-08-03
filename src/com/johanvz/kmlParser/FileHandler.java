package com.johanvz.kmlParser;

import javafx.collections.ObservableList;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileHandler {
    public static void saveToKML(File outputFile, Document document) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            if (outputFile.exists()) {
                while (!outputFile.delete()) {
                    Controller.showWarningDialog("Error", "Could not delete file \"" +
                            outputFile.getAbsolutePath() + "\". Please ensure it is closed!");

                }
            }
            if (outputFile.createNewFile()) {
                Controller.showSnackBar("Saved successfully");
                Runtime.getRuntime().exec("explorer.exe /select," + outputFile.getAbsolutePath());
            } else {
                Controller.showSnackBar("Save unsuccessful");
            }
            Result output = new StreamResult(outputFile);
            Source input = new DOMSource(document);
            transformer.transform(input, output);

        } catch (IOException | TransformerException e) {
            e.printStackTrace();
            Controller.showWarningDialog("Unexpected Error Occurred", e.getMessage());
        }
    }

    public static void saveDocumentToCSV(ObservableList<KMLDocument.Placemark> placemarks, File outputFile) {
        if (outputFile.exists()) {
            while (!outputFile.delete()) {
                Controller.showWarningDialog("Error", "Could not delete file \"" +
                        outputFile.getAbsolutePath() + "\". Please ensure it is closed!");

            }
        }
        try {
            StringBuilder stringBuilder = new StringBuilder();

            for (KMLDocument.Placemark placemark : placemarks) {
                stringBuilder.append('"').append(placemark.getName().get()).append('"');
                stringBuilder.append(',');
                stringBuilder.append('"').append(placemark.getDescription().get()).append('"');
                stringBuilder.append(',');
                stringBuilder.append('"').append(placemark.getCoordinate().get()).append('"');
                stringBuilder.append('\n');
            }

            FileWriter fileWriter = new FileWriter(outputFile.getAbsoluteFile());

            String toWrite = stringBuilder.toString().replace("&", "&amp;");

            fileWriter.append(toWrite);
            fileWriter.flush();
            fileWriter.close();

            Controller.showSnackBar("Saved successfully");
            Runtime.getRuntime().exec("explorer.exe /select," + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Controller.showWarningDialog("Unexpected Error Occurred", e.getMessage());
        }

    }

    public static void saveToKMZ(File outputFile, Document document) {
        StreamResult result = new StreamResult(new StringWriter());
        Source source = new DOMSource(document);
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(source, result);

            FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsolutePath());
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            ZipEntry zipEntry = new ZipEntry(outputFile.getName().replace("kmz", "kml"));

            zipOutputStream.putNextEntry(zipEntry);

            byte[] xmlBytes = result.getWriter().toString().getBytes();
            zipOutputStream.write(xmlBytes);
            zipOutputStream.flush();
            zipOutputStream.close();

            Controller.showSnackBar("Saved successfully");
            Runtime.getRuntime().exec("explorer.exe /select," + outputFile.getAbsolutePath());

        } catch (TransformerException | IOException e) {
            e.printStackTrace();
            Controller.showWarningDialog("Unexpected Error Occurred", e.getMessage());
        }
    }

    public static Document openFromKML(File inputFile) {
        Document document = null;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.parse(inputFile);
            document.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            Controller.showWarningDialog("Unexpected Error Occurred", e.getMessage());
        }

        return document;
    }

    public static Document openFromKMZ(File inputFile) {
        Document document = null;
        try {
            ZipFile zipFile = new ZipFile(inputFile.getAbsolutePath());
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while(entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if(entry.getName().contains("kml")) {
                    InputStream stream = zipFile.getInputStream(entry);
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    document = documentBuilder.parse(stream);
                    document.getDocumentElement().normalize();
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            Controller.showWarningDialog("Unexpected Error Occurred", e.getMessage());
        }
        return document;
    }

    public static Document openFromCSV(File inputFile) {

        Document document = null;

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile.getAbsolutePath()));
            StringBuilder stringBuilder = new StringBuilder(Globals.KMLSTART);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                String name = parts[0].replace("\"", "");
                String description = parts[1].replace("\"", "");
                String coordinate = (line.length() > 2) ? parts[2].replace("\"", "") : "NULL";

                stringBuilder.append(Globals.KMLELEMENT
                            .replace("{name}", name)
                            .replace("{description}", description)
                            .replace("{coordinates}", coordinate));
            }

            bufferedReader.close();
            stringBuilder.append(Globals.KMLEND);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(stringBuilder.toString()));
            document = documentBuilder.parse(inputSource);
            document.getDocumentElement().normalize();

        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            Controller.showWarningDialog("Unexpected Error Occurred", e.getMessage());
        }

        return document;
    }
}
