package com.ardacraft.ardastuff.ardamaps;

import com.duom.ardamaps.api.ArdaMapsApi;
import com.duom.ardamaps.api.ArdaMapsApiEntrypoint;
import com.duom.ardamaps.api.locations.ApiLocation;
import com.duom.ardamaps.api.locations.ILocationsApi;
import com.duom.ardamaps.core.data.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ArdaMaps API Entrypoint implementation for ArdaStuff.
 */
public class ArdaMapsHook implements ArdaMapsApiEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArdaMapsHook.class);

    /**
     * Callback when ArdaMaps is ready to accept API calls.
     * Set the WP-REST API location provider as the source for location data in ArdaMaps.
     */
    @Override
    public void onApiReady(ArdaMapsApi ardaMapsApi) {

        ILocationsApi locationsApi = ardaMapsApi.getLocationsApi();
        locationsApi.setLocationSource(
                () -> RestApiLocationProvider.refreshLocations().thenApply(ArdaMapsHook::toApiLocations));
        LOGGER.info("Registered ArdaStuff REST API location source with ArdaMaps");
    }

    private static List<ApiLocation> toApiLocations(List<RestApiLocationProvider.WpLocation> locations) {

        return locations.stream()
                .map(ArdaMapsHook::toApiLocation)
                .toList();
    }

    private static ApiLocation toApiLocation(RestApiLocationProvider.WpLocation location) {

        return new ApiLocation(
                location.id(),
                location.name(),
                location.world(),
                location.types(),
                location.warp(),
                new Vec3d(location.x(), location.y(), location.z()),
                location.pathfinder(),
                location.status(),
                location.regions(),
                location.canon(),
                location.description(),
                location.externalUrl());
    }
}
