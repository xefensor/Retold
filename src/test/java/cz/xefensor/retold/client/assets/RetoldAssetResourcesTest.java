package cz.xefensor.retold.client.assets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks resource contracts without starting Minecraft or depending on generation artifacts. */
class RetoldAssetResourcesTest {
    private static final String ASSETS = "assets/retold/";
    private static final List<Path> RESOURCE_ROOTS = List.of(
            Path.of("src/main/resources"), Path.of("src/generated/resources")
    );
    private static final Set<String> INVENTORY_CONTEXTS = Set.of("gui", "ground", "fixed", "on_shelf");
    private static final Map<String, List<Integer>> ATLAS_SIZES = Map.of(
            "block/aender_sign", List.of(32, 32),
            "block/aender_hanging_sign", List.of(32, 32),
            "entity/boat/aender", List.of(128, 64),
            "entity/chest_boat/aender", List.of(128, 128),
            "entity/earth_guardian", List.of(128, 64),
            "entity/gale_core", List.of(32, 32),
            "entity/equipment/humanoid/steel", List.of(64, 32),
            "entity/equipment/humanoid_leggings/steel", List.of(64, 32),
            "entity/equipment/humanoid_baby/steel", List.of(64, 64)
    );

    @Test
    void localModelParentsTexturesAndItemReferencesArePackaged() throws IOException {
        for (String path : resources("models", ".json")) {
            JsonObject model = json(path);
            Set<String> ancestors = new HashSet<>();
            String current = path;
            while (current != null) {
                assertTrue(ancestors.add(current), "Model parent cycle at " + current);
                JsonObject ancestor = json(current);
                String parent = ancestor.has("parent") ? ancestor.get("parent").getAsString() : "";
                current = localReference(parent, "models/", ".json");
                if (current != null) {
                    requireResource(current, path);
                }
            }
            if (model.has("textures")) {
                for (JsonElement texture : model.getAsJsonObject("textures").asMap().values()) {
                    String reference = texture.getAsString();
                    if (!reference.equals("retold:block/aender_portal")) {
                        requireLocalReference(reference, "textures/", ".png", path);
                    }
                }
            }
        }
        for (String folder : List.of("items", "blockstates")) {
            for (String path : resources(folder, ".json")) {
                checkModelReferences(json(path), path);
            }
        }
        // This texture is produced by a registered atlas source rather than a packaged PNG.
        JsonElement portalSources = json("assets/minecraft/atlases/blocks.json").get("sources");
        assertTrue(portalSources.getAsJsonArray().asList().stream().anyMatch(element -> {
            JsonObject source = element.getAsJsonObject();
            return source.has("target") && source.get("target").getAsString().equals("retold:block/aender_portal")
                    && source.get("type").getAsString().equals("retold:aender_portal_recolor")
                    && source.get("source").getAsString().equals("minecraft:block/nether_portal");
        }), "The generated Aender portal sprite must retain its atlas source");
    }

    @Test
    void steelEquipmentLayersResolveToAtlasesWithNativeUvDimensions() throws IOException {
        JsonObject layers = json(ASSETS + "equipment/steel.json").getAsJsonObject("layers");
        assertEquals(Set.of("humanoid", "humanoid_leggings", "humanoid_baby"), layers.keySet());
        for (String layerType : layers.keySet()) {
            assertFalse(layers.getAsJsonArray(layerType).isEmpty(), "Empty Steel layer " + layerType);
            for (JsonElement element : layers.getAsJsonArray(layerType)) {
                String texture = element.getAsJsonObject().get("texture").getAsString();
                String resource = localReference(texture, "textures/entity/equipment/" + layerType + "/", ".png");
                assertNotNull(resource, "Steel must use Retold's equipment atlas for " + layerType);
                BufferedImage image = image(resource);
                assertEquals(64, image.getWidth(), resource);
                assertEquals(layerType.equals("humanoid_baby") ? 64 : 32, image.getHeight(), resource);
                checkAlpha(image, resource, true);
                // Front-face regions from the 26.2 HumanoidModel armor meshes. Baby limbs have
                // independent UVs; an adult atlas stretched to 64x64 leaves some of them invisible.
                int[][] faces = switch (layerType) {
                    case "humanoid" -> new int[][]{{8, 8, 8, 8}, {20, 20, 8, 12}, {44, 20, 4, 12}, {4, 20, 4, 12}};
                    case "humanoid_leggings" -> new int[][]{{20, 20, 8, 12}, {4, 20, 4, 12}};
                    case "humanoid_baby" -> new int[][]{{8, 8, 9, 8}, {3, 20, 6, 5}, {33, 20, 2, 5}, {33, 28, 2, 5},
                            {21, 20, 3, 4}, {21, 27, 3, 4}, {3, 28, 3, 1}, {3, 32, 3, 1}, {2, 39, 6, 2}};
                    default -> throw new AssertionError("Unexpected equipment layer " + layerType);
                };
                for (int[] face : faces) {
                    assertTrue(hasOpaquePixel(image, face), resource + " has an empty armor face at " + face[0] + ", " + face[1]);
                }
            }
        }
    }

    @Test
    void refreshedPixelTexturesHaveNativeDimensionsAndBinaryAlpha() throws IOException {
        for (String path : resources("textures", ".png")) {
            String texture = path.substring((ASSETS + "textures/").length(), path.length() - 4);
            if (!isRefreshedTexture(texture)) {
                continue;
            }
            BufferedImage image = image(path);
            List<Integer> size = ATLAS_SIZES.getOrDefault(texture,
                    texture.endsWith("_spear_in_hand") ? List.of(32, 32) : List.of(16, 16));
            assertEquals(size.getFirst(), image.getWidth(), path);
            assertEquals(size.getLast(), image.getHeight(), path);
            checkAlpha(image, path, texture.startsWith("item/"));
            // Static material faces must not accidentally keep metadata from the old animation strips.
            assertNull(getClass().getClassLoader().getResource(path + ".mcmeta"), path);
        }
        for (String texture : ATLAS_SIZES.keySet()) {
            requireResource(ASSETS + "textures/" + texture + ".png", "Native atlas dimensions");
        }
    }

    @Test
    void spearsUseInventorySpritesAndFullLengthHeldModelsInEveryDisplayContext() throws IOException {
        Set<String> contexts = Set.of("gui", "ground", "fixed", "on_shelf", "head",
                "thirdperson_lefthand", "thirdperson_righthand", "firstperson_lefthand", "firstperson_righthand");
        for (String spear : List.of("flint_spear", "steel_spear")) {
            JsonObject definition = json(ASSETS + "items/" + spear + ".json");
            JsonObject selector = definition.getAsJsonObject("model");
            assertEquals("minecraft:select", selector.get("type").getAsString(), spear);
            assertEquals("minecraft:display_context", selector.get("property").getAsString(), spear);
            for (String context : contexts) {
                JsonObject selected = selector.getAsJsonObject("fallback");
                for (JsonElement element : selector.getAsJsonArray("cases")) {
                    JsonObject entry = element.getAsJsonObject();
                    JsonElement when = entry.get("when");
                    boolean matches = when.isJsonArray()
                            ? when.getAsJsonArray().asList().stream().anyMatch(value -> value.getAsString().equals(context))
                            : when.getAsString().equals(context);
                    if (matches) {
                        selected = entry.getAsJsonObject("model");
                        break;
                    }
                }
                String expected = "retold:item/" + spear + (INVENTORY_CONTEXTS.contains(context) ? "" : "_in_hand");
                assertEquals("minecraft:model", selected.get("type").getAsString(), spear + ": " + context);
                assertEquals(expected, selected.get("model").getAsString(), spear + ": " + context);
            }
            assertEquals(1.95, definition.get("swap_animation_scale").getAsDouble(), 0.0001, spear);
            JsonObject held = json(ASSETS + "models/item/" + spear + "_in_hand.json");
            assertEquals("minecraft:item/spear_in_hand", held.get("parent").getAsString(), spear);
            assertEquals("retold:item/" + spear + "_in_hand", held.getAsJsonObject("textures").get("layer0").getAsString(), spear);
        }
    }

    private static boolean isRefreshedTexture(String texture) {
        String name = texture.substring(texture.lastIndexOf('/') + 1);
        return ATLAS_SIZES.containsKey(texture) || name.startsWith("aender") || name.startsWith("stripped_aender")
                || name.startsWith("steel_") || name.startsWith("flint_") || name.equals("dev_aender_portal_frame")
                || name.equals("nether_reactor_core") || name.equals("earth_guardian_spawn_egg") || name.equals("gale_core_spawn_egg")
                || name.equals("raw_aenderite");
    }

    private static void checkAlpha(BufferedImage image, String path, boolean cutout) {
        boolean transparent = false;
        boolean opaque = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                assertTrue(alpha == 0 || alpha == 255, path + " has filtered alpha at " + x + ", " + y);
                transparent |= alpha == 0;
                opaque |= alpha == 255;
            }
        }
        assertTrue(opaque, "Empty texture " + path);
        if (cutout) {
            assertTrue(transparent, "Cutout texture has an opaque background: " + path);
        }
    }

    private static boolean hasOpaquePixel(BufferedImage image, int[] region) {
        for (int y = region[1]; y < region[1] + region[3]; y++) {
            for (int x = region[0]; x < region[0] + region[2]; x++) {
                if (image.getRGB(x, y) >>> 24 == 255) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void checkModelReferences(JsonElement element, String owner) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> checkModelReferences(child, owner));
        } else if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if ((entry.getKey().equals("model") || entry.getKey().equals("base")) && entry.getValue().isJsonPrimitive()) {
                    requireLocalReference(entry.getValue().getAsString(), "models/", ".json", owner);
                } else {
                    checkModelReferences(entry.getValue(), owner);
                }
            }
        }
    }

    private static void requireLocalReference(String reference, String folder, String extension, String owner) {
        String path = localReference(reference, folder, extension);
        if (path != null) {
            requireResource(path, owner);
        }
    }

    private static String localReference(String reference, String folder, String extension) {
        if (!reference.startsWith("retold:")) {
            return null;
        }
        String name = reference.substring("retold:".length());
        assertTrue(name.matches("[a-z0-9_.-]+(/[a-z0-9_.-]+)*") && !name.contains(".."), "Invalid Retold identifier " + reference);
        return ASSETS + folder + name + extension;
    }

    private static void requireResource(String path, String owner) {
        assertNotNull(RetoldAssetResourcesTest.class.getClassLoader().getResource(path), owner + " references missing " + path);
    }

    private static InputStream open(String path) {
        InputStream stream = RetoldAssetResourcesTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing packaged resource " + path);
        return stream;
    }

    private static JsonObject json(String path) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(open(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static BufferedImage image(String path) throws IOException {
        try (InputStream stream = open(path)) {
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, "Invalid PNG " + path);
            return image;
        }
    }

    private static List<String> resources(String folder, String extension) throws IOException {
        Set<String> paths = new HashSet<>();
        for (Path root : RESOURCE_ROOTS) {
            Path directory = root.resolve(ASSETS + folder);
            if (Files.isDirectory(directory)) {
                try (Stream<Path> files = Files.walk(directory)) {
                    files.filter(path -> path.toString().endsWith(extension))
                            .map(root::relativize)
                            .map(path -> path.toString().replace('\\', '/'))
                            .forEach(paths::add);
                }
            }
        }
        assertFalse(paths.isEmpty(), "No resources discovered in " + folder);
        return paths.stream().sorted().toList();
    }
}
