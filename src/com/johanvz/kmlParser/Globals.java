package com.johanvz.kmlParser;

import okhttp3.MediaType;

public interface Globals {
    String KMLSTART =
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

    String KMLEND =
            "        </Folder>\n" +
                    "    </Document>\n" +
                    "</kml>";
    String KMLELEMENT =
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

    enum FileTypes {KML, KMZ, CSV}

    String urlUL = "http://localhost:8080/api/clientul";
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
}
