package main.java.org.matejko.plugin;

import org.bukkit.Location;

public class GateData {

    private String gateId;
    private Location corner1;
    private Location corner2;
    private Location gateDestination;

    public GateData(String gateId, Location corner1, Location corner2, Location gateDestination) {
        this.gateId = gateId;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.gateDestination = gateDestination;
    }

    public String getGateId() {
        return gateId;
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    // Change this method to getGateDestination instead of getDestination
    public Location getGateDestination() {
        return gateDestination;
    }
}
