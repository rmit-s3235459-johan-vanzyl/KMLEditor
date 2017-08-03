package com.johanvz.kmlParser;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CSVDocument {

    public CSVDocument(File inputFile, File outputFile) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile.getAbsolutePath()));
            FileWriter fileWriter = new FileWriter(outputFile.getAbsolutePath());

            fileWriter.append(Globals.KMLSTART);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                String name = parts[0].replace("\"", "");
                String description = parts[1].replace("\"", "");
                String coordinate = (line.length() > 2) ? parts[2].replace("\"", "") : "NULL";

                fileWriter.append(Globals.KMLELEMENT
                        .replace("{name}", name)
                        .replace("{description}", description)
                        .replace("{coordinates}", coordinate));
            }

            bufferedReader.close();

            fileWriter.append(Globals.KMLEND);
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
