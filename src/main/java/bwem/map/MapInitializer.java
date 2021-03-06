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

import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.WalkPosition;
import bwem.area.TempAreaInfo;
import bwem.area.typedef.AreaId;
import bwem.tile.MiniTile;
import bwem.typedef.Altitude;
import bwem.unit.Mineral;
import bwem.unit.Neutral;
import bwem.unit.StaticBuilding;
import bwapi.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface MapInitializer {
    // This has to be called before any other function is called.
    // A good place to do this is in ExampleAIModule::onStart()
    void initialize();

    void initializeTerrainData(
            int mapTileWidth, int mapTileHeight, List<TilePosition> startingLocations);

    void initializeNeutralData(
            List<Unit> mineralPatches,
            List<Unit> vespeneGeysers,
            List<Unit> neutralUnits);

    void computeAltitude(TerrainData terrainData);

    List<Pair<WalkPosition, Altitude>> getSortedDeltasByAscendingAltitude(
            int mapWalkTileWidth, int mapWalkTileHeight, int altitudeScale);

    List<Pair<WalkPosition, Altitude>> getActiveSeaSideList(TerrainData terrainData);

    Altitude setAltitudesAndGetUpdatedHighestAltitude(
            Altitude currentHighestAltitude,
            TerrainData terrainData,
            List<Pair<WalkPosition, Altitude>> deltasByAscendingAltitude,
            List<Pair<WalkPosition, Altitude>> activeSeaSideList,
            int altitudeScale);

    void setHighestAltitude(Altitude altitude);

    void processBlockingNeutrals(List<Neutral> candidates);

    List<Neutral> getCandidates(List<StaticBuilding> staticBuildings, List<Mineral> minerals);

    List<WalkPosition> getOuterMiniTileBorderOfNeutral(Neutral pCandidate);

    List<WalkPosition> trimOuterMiniTileBorder(List<WalkPosition> border);

    List<WalkPosition> getDoors(List<WalkPosition> border);

    List<WalkPosition> getTrueDoors(List<WalkPosition> doors, Neutral pCandidate);

    void markBlockingStackedNeutrals(Neutral pCandidate, List<WalkPosition> trueDoors);

    void computeAreas(List<TempAreaInfo> tempAreaList, int areaMinMiniTiles);

    List<Pair<WalkPosition, MiniTile>> getSortedMiniTilesByDescendingAltitude();

    List<TempAreaInfo> computeTempAreas(
            List<Pair<WalkPosition, MiniTile>> miniTilesByDescendingAltitude);

    void replaceAreaIds(WalkPosition p, AreaId newAreaId);

    void createAreas(List<TempAreaInfo> tempAreaList, int areaMinMiniTiles);

    void setLowestAltitudeInTile(TilePosition t);

    List<Unit> filterPlayerUnits(Collection<Unit> units, Player player);

    List<Unit> filterNeutralPlayerUnits(Collection<Unit> units, Collection<Player> players);
}
