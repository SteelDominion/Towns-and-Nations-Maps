package org.leralix.tancommon.storage;

import org.leralix.tancommon.TownsAndNationsMapCommon;
import org.tan.api.interfaces.territory.TanNation;
import org.tan.api.interfaces.territory.TanTerritory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class NationDescription {

    private final String uuid;
    private final String name;
    private final int daysSinceCreation;
    private final String description;
    private final int numberOfClaims;
    private final int numberOfTowns;
    private final String capitalName;
    private final String nationName;
    private final List<String> townListName;


    public NationDescription(TanNation nationData){


        Date today = new Date();
        Date creationDate = new Date(nationData.getCreationDate());

        long diffInDays = today.getTime() - creationDate.getTime();
        int nbDays = (int) (diffInDays / (1000 * 60 * 60 * 24));

        Collection<TanTerritory> vasals = nationData.getVassals();

        int numberOfChunks = nationData.getNumberOfClaimedChunk();
        int nbTowns = 0;
        String townCaptialName = nationData.getCapital().getName();

        List<String> townNames = new ArrayList<>();
        for(TanTerritory regionData : vasals){
            for(TanTerritory townData : regionData.getVassals()){
                nbTowns += 1;
                townNames.add(townData.getName());
            }
        }

        this.uuid = nationData.getID();
        this.name = nationData.getName();
        this.daysSinceCreation = nbDays;
        this.description = nationData.getDescription();
        this.numberOfClaims = numberOfChunks;
        this.numberOfTowns = nbTowns;
        this.capitalName = townCaptialName;
        this.nationName = name;
        this.townListName = townNames;
    }

    public String getID() {
        return uuid;
    }



    public String getChunkDescription(){

        String description = TownsAndNationsMapCommon.getPlugin().getConfig().getString("nation_infowindow", "Config not found - nation");

        description = description.replace("%NATION_NAME%", this.name);
        description =  description.replace("%DAYS_SINCE_CREATION%", String.valueOf(this.daysSinceCreation));
        description  = description.replace("%DESCRIPTION%", this.description);
        description  = description.replace("%NUMBER_CLAIMS%", String.valueOf(this.numberOfClaims));
        description  = description.replace("%NUMBER_OF_TOWNS%", String.valueOf(this.numberOfTowns));
        description  = description.replace("%NATION_CAPITAL%", capitalName);
        description  = description.replace("%TOWN_LIST%", getMemberList());

        return description;
    }

    private StringBuilder getMemberList() {
        StringBuilder memberList = new StringBuilder();
        for(String member : townListName){
            memberList.append(member).append(", ");
        }
        return memberList;
    }


}
