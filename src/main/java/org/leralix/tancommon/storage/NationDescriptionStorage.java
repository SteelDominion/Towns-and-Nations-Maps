package org.leralix.tancommon.storage;

import java.util.HashMap;
import java.util.Map;

public class NationDescriptionStorage {

    private NationDescriptionStorage() {
        throw new IllegalStateException("Utility class");
    }

    private static final Map<String, NationDescription> regionDescriptionData = new HashMap<>();

    public static void add(NationDescription data){
        regionDescriptionData.put(data.getID(), data);
    }
    public static NationDescription get(String id){
        return regionDescriptionData.get(id);
    }

}
