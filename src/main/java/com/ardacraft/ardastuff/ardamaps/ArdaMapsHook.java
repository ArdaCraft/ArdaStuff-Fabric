package com.ardacraft.ardastuff.ardamaps;

import com.duom.ardamaps.api.ArdaMapsApi;
import com.duom.ardamaps.api.ArdaMapsApiEntrypoint;

/**
 * ArdaMaps API Entrypoint implementation for ArdaStuff.
 */
public class ArdaMapsHook implements ArdaMapsApiEntrypoint {

    /**
     * Callback when ArdaMaps is ready to accept API calls.
     * Set the WP-REST API location provider as the source for location data in ArdaMaps.
     */
    @Override
    public void onArdaMapsReady() {

        ArdaMapsApi.setLocationSource(new RestApiLocationProvider());
    }
}
