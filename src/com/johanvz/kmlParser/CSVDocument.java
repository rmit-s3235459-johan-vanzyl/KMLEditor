package com.johanvz.kmlParser;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CSVDocument {
    private static final String KMLSTART =
            "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                    "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\"\n" +
                    "     xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" xmlns:kmlx=\"http://www.google.com/kml/ext/2.2\"\n" +
                    "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                    "    <Document>\n" +
                    "        <Style id=\"point\">\n" +
                    "            <IconStyle>\n" +
                    "                <scale>0.9</scale>\n" +
                    "                <Icon>\n" +
                    "                    <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n" +
                    "                </Icon>\n" +
                    "            </IconStyle>\n" +
                    "        </Style>\n" +
                    "        <Folder>\n" +
                    "            <name>Waypoints</name>";
    private static final String KMLEND =
            "        </Folder>\n" +
                    "    </Document>\n" +
                    "</kml>";
    private static final String KMLELEMENT =
            "\n            <Placemark>\n" +
                    "                <name>{name}</name>\n" +
                    "                <description>{description}</description>\n" +
                    "                <Style>\n" +
                    "                    <IconStyle>\n" +
                    "                        <scale>0.9</scale>\n" +
                    "                        <Icon>\n" +
                    "                            <href>http://maps.google.com/mapfiles/kml/pushpin/blue-pushpin.png</href>\n" +
                    "                        </Icon>\n" +
                    "                    </IconStyle>\n" +
                    "                </Style>\n" +
                    "                <Point>\n" +
                    "                    <extrude>0</extrude>\n" +
                    "                    <altitudeMode>clampToGround</altitudeMode>\n" +
                    "                    <coordinates>{coordinates}</coordinates>\n" +
                    "                </Point>\n" +
                    "            </Placemark>\n";

    public CSVDocument(File inputFile, File outputFile) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile.getAbsolutePath()));
            FileWriter fileWriter = new FileWriter(outputFile.getAbsolutePath());

            fileWriter.append(KMLSTART);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                String name = parts[0].replace("\"", "");
                String description = parts[1].replace("\"", "");
                String coordinate = (line.length() > 2) ? parts[2].replace("\"", "") : "NULL";

                fileWriter.append(KMLELEMENT
                        .replace("{name}", name)
                        .replace("{description}", description)
                        .replace("{coordinates}", coordinate));
            }

            bufferedReader.close();

            fileWriter.append(KMLEND);
            fileWriter.flush();
            fileWriter.close();

            // kmz
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsolutePath().replace(".kml", ".kmz"));
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            ZipEntry zipEntry = new ZipEntry(outputFile.getName());

            zipOutputStream.putNextEntry(zipEntry);
            FileInputStream fileInputStream = new FileInputStream(outputFile.getAbsolutePath());

            int length;
            byte[] buffer = new byte[1024];
            while((length = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }

            fileInputStream.close();
            zipOutputStream.closeEntry();
            zipOutputStream.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
