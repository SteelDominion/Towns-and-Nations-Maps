package org.leralix.tancommon.update;

import org.bukkit.plugin.Plugin;
import org.leralix.tancommon.TownsAndNationsMapCommon;
import org.leralix.tancommon.geometry.ChunkManager;
import org.leralix.tancommon.storage.NationDescription;
import org.leralix.tancommon.storage.NationDescriptionStorage;
import org.leralix.tancommon.storage.RegionDescription;
import org.leralix.tancommon.storage.RegionDescriptionStorage;
import org.leralix.tancommon.storage.TownDescription;
import org.leralix.tancommon.storage.TownDescriptionStorage;
import org.tan.api.TanAPI;
import org.tan.api.interfaces.territory.TanNation;
import org.tan.api.interfaces.territory.TanRegion;
import org.tan.api.interfaces.territory.TanTown;

public class UpdateChunks implements Runnable {

    private final ChunkManager chunkManager;
    private final Long updatePeriod;

    public UpdateChunks(ChunkManager chunkManager, long updatePeriod) {
        this.chunkManager = chunkManager;
        this.updatePeriod = updatePeriod;
    }

    public UpdateChunks(UpdateChunks copy) {
        this.chunkManager = copy.chunkManager;
        this.updatePeriod = copy.updatePeriod;
    }

    @Override
    public void run() {
        update();
    }


    public void update() {

        TanAPI tanAPI = TanAPI.getInstance();

        //Update town and regions descriptions
        for(TanTown townData : tanAPI.getTerritoryManager().getTowns()){
            TownDescription townDescription = new TownDescription(townData);
            TownDescriptionStorage.add(townDescription);
            chunkManager.update(townData);
        }

        for(TanRegion regionData : tanAPI.getTerritoryManager().getRegions()){
            RegionDescription regionDescription = new RegionDescription(regionData);
            RegionDescriptionStorage.add(regionDescription);
            chunkManager.update(regionData);
        }

        for(TanNation nationData : tanAPI.getTerritoryManager().getNations()){
            NationDescription nationDescription = new NationDescription(nationData);
            NationDescriptionStorage.add(nationDescription);
            chunkManager.update(nationData);
        }


        Plugin plugin = TownsAndNationsMapCommon.getPlugin();
        if(updatePeriod > 0)
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new UpdateChunks(this), updatePeriod);

    }
}
