// Original work Copyright (c) 2015, 2017, Igor Dimitrijevic
// Modified work Copyright (c) 2017-2018 OpenBW Team

//////////////////////////////////////////////////////////////////////////
//
// This file is part of the BWEM Library.
// BWEM is free software, licensed under the MIT/X11 License.
// A copy of the license is provided with the library in the LICENSE file.
// Copyright (c) 2015, 2017, Igor Dimitrijevic
//
//////////////////////////////////////////////////////////////////////////

package bwem.map;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.WalkPosition;

import java.util.List;

public interface MapData {
    /**
     * Returns the size of the map in tiles.
     */
    TilePosition getTileSize();

    /**
     * Returns the size of the map in walktiles.
     */
    WalkPosition getWalkSize();

    /**
     * Returns the size of the map in pixels.
     */
    Position getPixelSize();

    /**
     * Returns the center of the map in pixels.
     */
    Position getCenter();

    /**
     * Returns the internal container of the starting Locations.<br>
     * Note: these correspond to BWAPI::getStartLocations().
     */
    List<TilePosition> getStartingLocations();

    /**
     * Tests whether the specified position is inside the map.
     *
     * @param tilePosition the specified position
     */
    boolean isValid(TilePosition tilePosition);

    /**
     * Tests whether the specified position is inside the map.
     *
     * @param walkPosition the specified position
     */
    boolean isValid(WalkPosition walkPosition);

    /**
     * Tests whether the specified position is inside the map.
     *
     * @param position the specified position
     */
    boolean isValid(Position position);
}
