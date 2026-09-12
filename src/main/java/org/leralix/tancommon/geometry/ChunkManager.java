package org.leralix.tancommon.geometry;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.leralix.tancommon.markers.CommonMarkerRegister;
import org.leralix.tancommon.storage.PolygonCoordinate;
import org.leralix.tancommon.storage.TileFlags;
import org.leralix.tancommon.storage.NationDescriptionStorage;
import org.leralix.tancommon.storage.RegionDescriptionStorage;
import org.leralix.tancommon.storage.TownDescriptionStorage;
import org.tan.api.interfaces.chunk.TanClaimedChunk;
import org.tan.api.interfaces.territory.TanTerritory;
import org.tan.api.interfaces.territory.TanNation;
import org.tan.api.interfaces.territory.TanRegion;
import org.tan.api.interfaces.territory.TanTown;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class ChunkManager {

    private final PolygonBuilder polygonBuilder;
    private final CommonMarkerRegister commonMarkerRegister;

    enum direction {XPLUS, ZPLUS, XMINUS, ZMINUS}

    public ChunkManager(CommonMarkerRegister markerRegister, PolygonBuilder polygonBuilder) {
        this.commonMarkerRegister = markerRegister;
        this.polygonBuilder = polygonBuilder;
    }

    public void update(TanTown town){
        String infoWindowPopup = TownDescriptionStorage.get(town.getID()).getChunkDescription();
        updateTerritory(town, infoWindowPopup);
    }
    public void update(TanRegion region) {
        String infoWindowPopup = RegionDescriptionStorage.get(region.getID()).getChunkDescription();
        updateTerritory(region, infoWindowPopup);
    }
    public void update(TanNation nation) {
        String infoWindowPopup = NationDescriptionStorage.get(nation.getID()).getChunkDescription();
        updateTerritory(nation, infoWindowPopup);
    }

    private void updateTerritory(TanTerritory territory, String infoWindowPopup) {

        int claimedPolyIndex = 0; /* Index of polygon for when a town has multiple shapes. */

        Collection<TanClaimedChunk> townClaimedChunks = territory.getClaimedChunks();
        if(townClaimedChunks.isEmpty())
            return;

        HashMap<String, TileFlags> worldNameClaimedShapeMap = new HashMap<>();
        LinkedList<TanClaimedChunk> claimedChunksToDraw = new LinkedList<>();
        World currentClaimWorld = null;
        TileFlags currentClaimShape = null;

        //Registering all the claimed chunks to draw
        for (TanClaimedChunk townClaimedChunk : townClaimedChunks) {
            World world = Bukkit.getWorld(townClaimedChunk.getWorldUUID());
            if(world == null){
                continue;
            }
            if (world != currentClaimWorld) {
                String worldName = world.getName();
                currentClaimShape = worldNameClaimedShapeMap.get(worldName);
                if (currentClaimShape == null) {
                    currentClaimShape = new TileFlags();
                    worldNameClaimedShapeMap.put(worldName, currentClaimShape);
                }
                currentClaimWorld = world;
            }
            if (currentClaimShape == null) {
                currentClaimShape = new TileFlags();
            }
            currentClaimShape.setFlag(townClaimedChunk.getX(), townClaimedChunk.getZ(), true);
            claimedChunksToDraw.addLast(townClaimedChunk);
        }

        //Drawing all the claimed chunks
        while(claimedChunksToDraw != null) {
            LinkedList<TanClaimedChunk> ourTownBlocks = null;
            LinkedList<TanClaimedChunk> townBlockLeftToDraw = null;
            TileFlags ourShape = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for(TanClaimedChunk claimedChunk : claimedChunksToDraw) {
                int tbX = claimedChunk.getX();
                int tbZ = claimedChunk.getZ();
                World world = Bukkit.getWorld(claimedChunk.getWorldUUID());
                if(ourShape == null && world != currentClaimWorld) {
                    currentClaimWorld = world;
                    currentClaimShape = worldNameClaimedShapeMap.get(currentClaimWorld.getName());
                }
                /* If we need to start shape, and this block is not part of one yet */
                if((ourShape == null) && currentClaimShape.getFlag(tbX, tbZ)) {
                    ourShape = new TileFlags();  /* Create map for shape */
                    ourTownBlocks = new LinkedList<>();
                    floodFillTarget(currentClaimShape, ourShape, tbX, tbZ);   /* Copy shape */
                    ourTownBlocks.add(claimedChunk); /* Add it to our node list */
                    minx = tbX; minz = tbZ;
                }
                /* If shape found, and we're in it, add to our node list */
                else if((ourShape != null) && (world == currentClaimWorld) &&
                        (ourShape.getFlag(tbX, tbZ))) {
                    ourTownBlocks.add(claimedChunk);
                    if(tbX < minx) {
                        minx = tbX; minz = tbZ;
                    }
                    else if((tbX == minx) && (tbZ < minz)) {
                        minz = tbZ;
                    }
                }
                else {  /* Else, keep it in the list for the next polygon */
                    if(townBlockLeftToDraw == null)
                        townBlockLeftToDraw = new LinkedList<>();
                    townBlockLeftToDraw.add(claimedChunk);
                }
            }
            claimedChunksToDraw = townBlockLeftToDraw; /* Replace list (null if no more to process) */
            if(ourShape != null) {
                 claimedPolyIndex = traceClaimedTerritoryOutline(territory, claimedPolyIndex, infoWindowPopup, currentClaimWorld.getName(), ourShape, minx, minz);
            }
        }

        int occupiedPolyIndex = 0; /* Index of polygon for when a town has multiple shapes. */
        Collection<TanClaimedChunk> townOccupiedChunks = territory.getOccupiedChunks();
        if(townOccupiedChunks.isEmpty())
            return;


        HashMap<String, TileFlags> worldNameOccupiedShapeMap = new HashMap<>();
        LinkedList<TanClaimedChunk> occupiedChunksToDraw = new LinkedList<>();
        World currentOccupiedWorld = null;
        TileFlags currentOccupiedShape = null;

        //Registering all the occupied chunks to draw
        for (TanClaimedChunk townOccupiedChunk : townOccupiedChunks) {
            World world = Bukkit.getWorld(townOccupiedChunk.getWorldUUID());
            if(world == null){
                continue;
            }
            if (world != currentOccupiedWorld) {
                String worldName = world.getName();
                currentOccupiedShape = worldNameOccupiedShapeMap.get(worldName);
                if (currentOccupiedShape == null) {
                    currentOccupiedShape = new TileFlags();
                    worldNameOccupiedShapeMap.put(worldName, currentOccupiedShape);
                }
                currentOccupiedWorld = world;
            }
            if (currentOccupiedShape == null) {
                currentOccupiedShape = new TileFlags();
            }
            currentOccupiedShape.setFlag(townOccupiedChunk.getX(), townOccupiedChunk.getZ(), true);
            occupiedChunksToDraw.addLast(townOccupiedChunk);
        }

        
        //Drawing all the occupied chunks
        while(occupiedChunksToDraw != null) {
            LinkedList<TanClaimedChunk> ourTownBlocks = null;
            LinkedList<TanClaimedChunk> townBlockLeftToDraw = null;
            TileFlags ourShape = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for(TanClaimedChunk occupiedChunk : occupiedChunksToDraw) {
                int tbX = occupiedChunk.getX();
                int tbZ = occupiedChunk.getZ();
                System.out.println("Chunk Loaded: X:"+tbX+" Z:"+tbZ);
                World world = Bukkit.getWorld(occupiedChunk.getWorldUUID());
                if(ourShape == null && world != currentOccupiedWorld) {
                    currentOccupiedWorld = world;
                    currentOccupiedShape = worldNameOccupiedShapeMap.get(currentOccupiedWorld.getName());
                }

                /* If we need to start shape, and this block is not part of one yet */
                if((ourShape == null) && currentOccupiedShape.getFlag(tbX, tbZ)) {
                    ourShape = new TileFlags();  /* Create map for shape */
                    ourTownBlocks = new LinkedList<>();
                    floodFillTarget(currentOccupiedShape, ourShape, tbX, tbZ);   /* Copy shape */
                    ourTownBlocks.add(occupiedChunk); /* Add it to our node list */
                    minx = tbX; minz = tbZ;
                }
                /* If shape found, and we're in it, add to our node list */
                else if((ourShape != null) && (world == currentOccupiedWorld) &&
                        (ourShape.getFlag(tbX, tbZ))) {
                    ourTownBlocks.add(occupiedChunk);
                    if(tbX < minx) {
                        minx = tbX; minz = tbZ;
                    }
                    else if((tbX == minx) && (tbZ < minz)) {
                        minz = tbZ;
                    }
                }
                else {  /* Else, keep it in the list for the next polygon */
                    if(townBlockLeftToDraw == null)
                        townBlockLeftToDraw = new LinkedList<>();
                    townBlockLeftToDraw.add(occupiedChunk);
                }
            }
            occupiedChunksToDraw = townBlockLeftToDraw; /* Replace list (null if no more to process) */
            if(ourShape != null) {
                 occupiedPolyIndex = traceOccupiedTerritoryOutline(territory, occupiedPolyIndex, infoWindowPopup, currentOccupiedWorld.getName(), ourShape, minx, minz);
            }
        }

    }

    private void floodFillTarget(TileFlags src, TileFlags dest, int x, int y) {
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[] { x, y });

        while (!stack.isEmpty()) {
            int[] nxt = stack.pop();
            x = nxt[0];
            y = nxt[1];
            if (src.getFlag(x, y)) { /* Set in src */
                src.setFlag(x, y, false); /* Clear source */
                dest.setFlag(x, y, true); /* Set in destination */
                if (src.getFlag(x + 1, y))
                    stack.push(new int[] { x + 1, y });
                if (src.getFlag(x - 1, y))
                    stack.push(new int[] { x - 1, y });
                if (src.getFlag(x, y + 1))
                    stack.push(new int[] { x, y + 1 });
                if (src.getFlag(x, y - 1))
                    stack.push(new int[] { x, y - 1 });
            }
        }
    }

    private int traceClaimedTerritoryOutline(TanTerritory territoryData, int polyIndex, String infoWindowPopup, String worldName, TileFlags ourShape, int minx, int minz) {
        String polyid = territoryData.getID() + "_" + polyIndex;

        PolygonCoordinate polygonCoordinate = polygonBuilder.buildPolygon(ourShape, minx, minz);
        Collection<PolygonCoordinate> holes = polygonBuilder.getHoles(ourShape, polygonCoordinate);

        commonMarkerRegister.registerNewArea(polyid, territoryData, false, worldName, polygonCoordinate, infoWindowPopup, holes);

        polyIndex++;
        return polyIndex;
    }

    private int traceOccupiedTerritoryOutline(TanTerritory territoryData, int polyIndex, String infoWindowPopup, String worldName, TileFlags ourShape, int minx, int minz) {
        String polyid = territoryData.getID() + "_" + polyIndex;

        PolygonCoordinate polygonCoordinate = polygonBuilder.buildPolygon(ourShape, minx, minz);
        Collection<PolygonCoordinate> holes = polygonBuilder.getHoles(ourShape, polygonCoordinate);

        commonMarkerRegister.registerNewOccupiedArea(polyid, territoryData, false, worldName, polygonCoordinate, infoWindowPopup, holes);

        polyIndex++;
        return polyIndex;
    }


}
