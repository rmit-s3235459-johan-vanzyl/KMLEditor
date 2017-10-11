package com.johanvz.kmlParser.model;

public class Client {

    private String name;
    private String description;
    private String coordinate;

    public Client(String name, String description, String coordinate) {
        this.name = name;
        this.description = description;
        this.coordinate = coordinate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    public Client() {
    }

}
