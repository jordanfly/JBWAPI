package bwapi;

import bwapi.ClientData.ForceData;
import java.util.Set;
import java.util.stream.Collectors;

public class Force {
    private final Game game;

    private final int id;
    private final String name;

    Force(final ForceData forceData, int id, final Game game) {
        this.game = game;
        this.id = id;
        this.name = forceData.getName();
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Player> getPlayers() {
        return game.getPlayers().stream()
                .filter(p -> equals(p.getForce()))
                .collect(Collectors.toSet());
    }

    public boolean equals(final Object that) {
        if (!(that instanceof Force)) {
            return false;
        }
        return getID() == ((Force) that).getID();
    }

    public int hashCode() {
        return getID();
    }
}
