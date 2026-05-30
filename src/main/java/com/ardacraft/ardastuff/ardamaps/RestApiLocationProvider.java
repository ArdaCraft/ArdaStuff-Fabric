package com.ardacraft.ardastuff.ardamaps;

import com.duom.ardamaps.api.LocationSource;
import com.duom.ardamaps.core.consumers.HuskHomesApiHook;
import com.duom.ardamaps.core.data.location.LocationServer;
import com.google.gson.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import net.william278.huskhomes.api.FabricHuskHomesAPI;
import net.william278.huskhomes.position.Warp;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Built-in {@link LocationSource} that fetches location data from the ArdaCraft WordPress REST API.
 * This class fetches paginated location data and maps it to {@link LocationServer} objects.
 *
 * <p>This implementation is registered as the default source during ArdaMaps initialization.
 * It is intended to be externalized into a separate companion mod in the future; at that point,
 * ArdaMaps itself will ship with no default source and require an explicit registration.</p>
 *
 * <p>Register via {@link com.duom.ardamaps.api.ArdaMapsApi#setLocationSource} if you wish to
 * override or replicate this behaviour from your own mod.</p>
 */
@SuppressWarnings("LoggingSimilarMessage")
@Environment(EnvType.SERVER)
public class RestApiLocationProvider implements LocationSource {

    /** Logger instance for the mod. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiLocationProvider.class);
    
    /** Base URL for the paginated location REST API endpoint. Page number is appended at runtime. */
    private static final String LOCATION_API_URL =
            "https://wp.ardacraft.me/wp-json/wp/v2/location-region?per_page=100&page=";

    /** Base URL for the paginated build-type REST API endpoint. Page number is appended at runtime. */
    private static final String BUILD_TYPE_API_URL =
            "https://wp.ardacraft.me/wp-json/wp/v2/buildtype?per_page=100&page=";

    /** Gson Parser instance */
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Matches the full ardacraft.me origin (http or https, with or without www.)
     * so it can be stripped to produce a relative path.
     * e.g. {@code https://www.ardacraft.me/location/rivendell} -> {@code /location/rivendell}
     */
    private static final Pattern WEBSITE_DOMAIN_PATTERN =
            Pattern.compile("https?://(?:www\\.)?ardacraft\\.me");

    /**
     * Matches the singular {@code /region/} path segment and replaces it with the
     * plural {@code /regions/} form expected by the internal link router.
     */
    private static final Pattern WEBSITE_REGION_PATH_PATTERN =
            Pattern.compile("/region/");

    /**
     * Fetches all locations from the REST API, page by page, and invokes the callback
     * once all pages have been collected and post-processed.
     *
     * @param callback Invoked with the full list of {@link LocationServer} once complete.
     */
    @Override
    public void refreshLocations(Consumer<List<LocationServer>> callback) {

        LOGGER.info("Fetching locations from REST API: {}", LOCATION_API_URL + "<page>");
        CompletableFuture.supplyAsync(() -> {

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            Map<Integer, String> buildTypeMap = fetchBuildTypes(client);
            List<LocationServer> locations = fetchLocations(client, buildTypeMap);

            LOGGER.info("{} locations fetched", locations.size());
            postProcessLocations(locations, callback);

            return null;

        }).exceptionally(ex -> {

            LOGGER.error("Failed to fetch locations from REST API", ex);
            callback.accept(new ArrayList<>());

            return null;
        });
    }

    /**
     * Fetches all build types from the WP REST API, paginating until exhausted.
     * Returns an empty map if the request fails, so location fetching can continue.
     *
     * @param client The {@link HttpClient} to use for requests.
     * @return A {@link Map} of build type ID to name, never {@code null}.
     */
    private static Map<Integer, String> fetchBuildTypes(HttpClient client) {

        Map<Integer, String> buildTypeMap = new HashMap<>();
        int page = 1;

        LOGGER.info("Fetching build types from REST API: {}", BUILD_TYPE_API_URL + "<page>");

        while (true) {

            LOGGER.info("\t- Fetching page {}", page);
            String url = BUILD_TYPE_API_URL + page;

            try {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 400) {
                    LOGGER.info("Build type API returned 400 at page {} - stopping pagination", page);
                    break;
                }

                if (response.statusCode() != 200) {
                    LOGGER.warn("Build type API returned unexpected status {} at page {} - skipping build types", response.statusCode(), page);
                    break;
                }

                JsonArray array = GSON.fromJson(response.body(), JsonArray.class);

                if (array == null || array.isEmpty()) {
                    LOGGER.info("Build type API returned empty page {} - stopping pagination", page);
                    break;
                }

                for (JsonElement element : array) {

                    if (!element.isJsonObject()) continue;

                    JsonObject entry = element.getAsJsonObject();
                    JsonElement idEl = entry.get("id");
                    JsonElement nameEl = entry.get("name");

                    if (idEl == null || nameEl == null) continue;
                    if (!idEl.isJsonPrimitive() || !nameEl.isJsonPrimitive()) continue;

                    buildTypeMap.put(idEl.getAsInt(), nameEl.getAsString());
                }

                page++;

            } catch (Exception e) {
                LOGGER.warn("Failed to fetch build types at page {} - proceeding with {} types already loaded", page, buildTypeMap.size(), e);
                break;
            }
        }

        LOGGER.info("Finished fetching {} build types from REST API", buildTypeMap.size());
        return buildTypeMap;
    }

    /**
     * Main loop: fetches all location-region entries from the REST API, paginating until exhausted.
     *
     * @param client       The {@link HttpClient} to use for requests.
     * @param buildTypeMap A map of build type ID to name, fetched from the REST API before parsing locations.
     * @return A list of parsed {@link LocationServer} objects, never {@code null}.
     */
    private static List<LocationServer> fetchLocations(HttpClient client, Map<Integer, String> buildTypeMap) {

        List<LocationServer> locations = new ArrayList<>();
        int page = 1;

        LOGGER.info("Fetching Locations from REST API: {}", LOCATION_API_URL + "<page>");

        // Loop until we hit an empty page or an error, which indicates we've fetched all available data
        while (true) {

            LOGGER.info("\t- Fetching page {}", page);

            String url = LOCATION_API_URL + page;

            try {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 400) {
                    LOGGER.info("REST API returned 400 at page {} - stopping pagination", page);
                    break;
                }

                if (response.statusCode() != 200) {
                    LOGGER.error("REST API returned unexpected status {} at page {}", response.statusCode(), page);
                    break;
                }

                JsonArray array = GSON.fromJson(response.body(), JsonArray.class);

                if (array == null || array.isEmpty()) {
                    LOGGER.info("REST API returned empty page {} - stopping pagination", page);
                    break;
                }

                parseJsonLocationArray(buildTypeMap, array, locations, page);

                page++;

            } catch (Exception e) {
                LOGGER.error("Failed to fetch locations from REST API at page {}", page, e);
                break;
            }
        }

        return locations;
    }

    /**
     * Post-processes all parsed locations using HuskHomes warp data to resolve
     * world identifiers and precise coordinates.
     *
     * @param locations The list of parsed LocationServer objects to post-process. This list is modified in-place.
     * @param callback  The callback to invoke with the post-processed list once complete.
     */
    private static void postProcessLocations(List<LocationServer> locations, Consumer<List<LocationServer>> callback) {

        if (HuskHomesApiHook.getInstance() == null) {
            LOGGER.warn("HuskHomes API not available, skipping warp resolution");
            callback.accept(locations);
            return;
        }

        FabricHuskHomesAPI.getInstance().getWarps().thenAccept((List<Warp> warpList) -> {

            LOGGER.info("Syncing {} warps for REST API location resolution", warpList.size());

            for (LocationServer location : locations) {
                if (location.getWarp() != null && !location.getWarp().isEmpty()) {
                    resolveWarp(warpList, location);
                }
            }

        }).thenRun(() -> callback.accept(locations));
    }

    /**
     * Parses a JSON array of location-region entries, converting each valid entry into a {@link LocationServer}
     * and adding it to the provided list. Invalid entries are skipped with an error log.
     *
     * @param buildTypeMap A map of build type ID to name, used to resolve type names during parsing.
     * @param array        The JSON array containing location-region entries from the REST API.
     * @param locations    The list to which parsed LocationServer objects will be added.
     * @param page         The current page number being parsed, used for logging context.
     */
    private static void parseJsonLocationArray(Map<Integer, String> buildTypeMap, JsonArray array, List<LocationServer> locations, int page) {

        for (JsonElement element : array) {
            try {
                locations.add(parseEntry(element.getAsJsonObject(), buildTypeMap));

            } catch (Exception e) {
                LOGGER.error("Failed to parse location entry on page {}", page, e);
            }
        }
    }

    /**
     * Resolves the warp for a location, updating its world identifier and position.
     *
     * @param warpList The list of available warps fetched from the HuskHomes API.
     * @param location The LocationServer object to resolve. This object is modified in-place.
     */
    private static void resolveWarp(List<Warp> warpList, LocationServer location) {

        String warpString = location.getWarp().trim();

        for (Warp warp : warpList) {
            if (warp.getName().equalsIgnoreCase(warpString)) {
                location.setWorld(warp.getWorld().getName());
                location.setPosition(new Vec3d(warp.getX(), warp.getY(), warp.getZ()));
                break;
            }
        }
    }

    /**
     * Parses a single WP REST API location-region entry into a {@link LocationServer}.
     *
     * @param obj The JSON object representing one location-region post.
     * @return The parsed {@link LocationServer}, or {@code null} if the entry is invalid.
     */
    private static LocationServer parseEntry(JsonObject obj, Map<Integer, String> buildTypeMap) {

        LocationServer location = new LocationServer();

        String id = obj.has("slug") ? obj.get("slug").getAsString() : "";
        location.setId(id);

        // name - from title.rendered
        String name = getNestedString(obj, "title", "rendered");

        location.setName(name);

        JsonObject acf = obj.has("acf") && obj.get("acf").isJsonObject()
                ? obj.getAsJsonObject("acf") : new JsonObject();

        // status
        location.setStatus(getString(acf, "status", ""));

        // warp - strip leading "/warp " prefix
        String rawWarp = getString(acf, "warp", "");
        location.setWarp(rawWarp.replaceAll("(?i)^[\\\\/]warp\\s+", "").trim());

        // long description
        location.setDescription(getDescription(acf, "short_description", ""));

        // Canon - true only when lore field equals "Canon" (case-insensitive)
        String lore = getString(acf, "lore", "");
        location.setCanon("Canon".equalsIgnoreCase(lore));

        // external_url
        location.setExternalUrl(getString(acf, "external_url", ""));

        // regions - from region_source.formatted_value[*].post_name
        location.setRegions(parseRegions(acf));

        // types - resolved from buildtype IDs using the prefetched buildTypeMap
        location.setTypes(parseTypes(acf, buildTypeMap));

        // world defaults to overworld; position resolved later via warp post-processing
        location.setWorld("minecraft:overworld");
        location.setPosition(parsePositions(acf));

        return location;
    }

    /**
     * Decodes an html / unicode string
     * @param htmlString the html string to decode
     * @return the decoded string
     */
    private static String decodeHtmlString(String htmlString) {

        if (htmlString == null || htmlString.isBlank()) return "";

        // Decode HTML entities (named, decimal and hex numeric) using jsoup, which is already bundled at runtime
        return Parser.unescapeEntities(htmlString, false);
    }

    /**
     * Safely extracts a nested string value from a JSON object using an outer and inner key.
     * For example, to extract "rendered" from "title.rendered", you would call getNestedString(obj, "title", "rendered").
     *
     * @param obj      The JSON object to extract the value from.
     * @param outerKey The key for the nested JSON object.
     * @param innerKey The key for the string value within the nested JSON object.
     * @return The string value associated with the inner key in the nested object, or null if not available.
     */
    @SuppressWarnings("SameParameterValue")
    private static String getNestedString(JsonObject obj, String outerKey, String innerKey) {
        JsonObject nested = getNestedObject(obj, outerKey);
        if (nested == null) return null;
        return getString(nested, innerKey, null);
    }

    /**
     * Processes a description text, making all ardacraft.me links relative and
     * normalising the singular {@code /region/} path segment to its plural form {@code /regions/}.
     *
     * <p>Step 1 – strip origin: {@code https?://(?:www.)?ardacraft.me} is removed so that
     * {@code https://www.ardacraft.me/location/rivendell} becomes {@code /location/rivendell}.</p>
     * <p>Step 2 – fix path: {@code /region/} is replaced with {@code /regions/} to match
     * the internal link format expected by {@link com.duom.ardamaps.core.data.conversion.HtmlConverter}.</p>
     *
     * @param obj          The JSON object to extract the value from.
     * @param key          The key to look up in the JSON object.
     * @param defaultValue The value to return if the key is missing, null, or not a primitive string.
     * @return The string value associated with the key, or the default value if not available.
     */
    @SuppressWarnings("SameParameterValue")
    private static String getDescription(JsonObject obj, String key, String defaultValue) {

        var description = getString(obj, key, defaultValue);

        if (description != null && !description.isBlank()) {

            // Step 1: strip the ardacraft.me origin to produce relative links
            description = WEBSITE_DOMAIN_PATTERN.matcher(description).replaceAll("");

            // Step 2: normalise /region/ -> /regions/
            description = WEBSITE_REGION_PATH_PATTERN.matcher(description).replaceAll("/regions/");
        }

        return description;
    }

    /**
     * Safely extracts a string value from a JSON object by key, returning a default value if the key is missing,
     *
     * @param obj          The JSON object to extract the value from.
     * @param key          The key to look up in the JSON object.
     * @param defaultValue The value to return if the key is missing, null, or not a primitive string.
     * @return The string value associated with the key, or the default value if not available.
     */
    private static String getString(JsonObject obj, String key, String defaultValue) {

        if (obj == null || !obj.has(key)) return defaultValue;

        JsonElement el = obj.get(key);

        if (el.isJsonNull() || !el.isJsonPrimitive()) return defaultValue;

        return decodeHtmlString(el.getAsString());
    }

    /**
     * Extracts region slugs from {@code acf.region_source.formatted_value[*].post_name}.
     *
     * @param acf The Advanced Custom Fields JSON object containing the region source data.
     * @return A list of region slugs associated with the location, never null but possibly empty.
     */
    private static List<String> parseRegions(JsonObject acf) {

        List<String> regions = new ArrayList<>();

        JsonObject regionSource = getNestedObject(acf, "region_source");
        if (regionSource == null) return regions;

        JsonElement fv = regionSource.get("formatted_value");
        if (fv == null || !fv.isJsonArray()) return regions;

        for (JsonElement el : fv.getAsJsonArray()) {

            if (!el.isJsonObject()) continue;
            String postName = getString(el.getAsJsonObject(), "post_name", null);

            if (postName != null && !postName.isBlank()) regions.add(postName.trim());
        }

        return regions;
    }

    /**
     * Resolves build type names from their IDs using the prefetched buildTypeMap.
     * If no types are found, defaults to "Region" to ensure the location is categorized.
     *
     * @param obj          The JSON object containing the types array.
     * @param buildTypeMap A map of build type ID - name, fetched from the REST API before parsing locations.
     * @return A list of build type names associated with the location, never null but possibly empty.
     */
    private static List<String> parseTypes(JsonObject obj, Map<Integer, String> buildTypeMap) {

        var key = "types";
        var outputList = new ArrayList<String>();

        if (obj == null || !obj.has(key)) return outputList;

        JsonElement el = obj.get(key);

        if (el.isJsonNull() || !el.isJsonArray()) return outputList;

        JsonArray array = el.getAsJsonArray();

        // We are expecting an array of integers, but validate each element to avoid parsing errors
        for (JsonElement jsonElement : array) {

            if (!jsonElement.isJsonPrimitive()) continue;

            var primitive = jsonElement.getAsJsonPrimitive();

            if (!primitive.isNumber()) continue;

            int id = primitive.getAsInt();
            String name = buildTypeMap.getOrDefault(id, Integer.toString(id));
            outputList.add(name.toUpperCase());
        }

        // Regions returned by the API do not have set types. Manually define it.
        if (outputList.isEmpty()) outputList.add("REGION");

        return outputList;
    }

    /**
     * Extracts Minecraft coordinates from {@code acf.minecraft_x} and {@code acf.minecraft_z}.
     *
     * @param acf The Advanced Custom Fields JSON object containing the coordinate data.
     * @return A Vec3d representing the coordinates, with Y set to 0. Defaults to (0, 0, 0) if values are missing or invalid.
     */
    private static Vec3d parsePositions(JsonObject acf) {

        String x = getString(acf, "minecraft_x", "0");
        String z = getString(acf, "minecraft_z", "0");

        double xCoordinate = 0;
        double zCoordinate = 0;

        if (x != null && !x.isBlank()) {

            try {

                xCoordinate = Double.parseDouble(x);

            } catch (NumberFormatException numberFormatException) {

                LOGGER.warn("Invalid minecraft_x value '{}', defaulting to 0", x);
            }
        }

        if (z != null && !z.isBlank()) {

            try {

                zCoordinate = Double.parseDouble(z);

            } catch (NumberFormatException numberFormatException) {

                LOGGER.warn("Invalid minecraft_z value '{}', defaulting to 0", z);
            }
        }

        return new Vec3d(xCoordinate, 0, zCoordinate);
    }

    /**
     * Safely extracts a nested JSON object from a parent JSON object by key.
     *
     * @param obj The parent JSON object to extract the nested object from.
     * @param key The key to look up in the parent JSON object for the nested object.
     * @return The nested JsonObject associated with the key, or null if the key is missing or not a JSON object.
     */
    private static JsonObject getNestedObject(JsonObject obj, String key) {

        if (obj == null || !obj.has(key)) return null;

        JsonElement el = obj.get(key);

        return el.isJsonObject() ? el.getAsJsonObject() : null;
    }
}
