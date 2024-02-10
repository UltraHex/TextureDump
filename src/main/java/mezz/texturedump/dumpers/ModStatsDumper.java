package mezz.texturedump.dumpers;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import com.google.gson.stream.JsonWriter;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.ProgressManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mezz.texturedump.util.Log;

@SideOnly(Side.CLIENT)
public class ModStatsDumper {

	private static final Map<String, ModContainer> modContainersForLowercaseIds = new HashMap<>();

	public ModStatsDumper() {
		for (Map.Entry<String, ModContainer> modEntry : Loader.instance().getIndexedModList().entrySet()) {
			String lowercaseId = modEntry.getKey().toLowerCase(Locale.ENGLISH);
			modContainersForLowercaseIds.put(lowercaseId, modEntry.getValue());
		}
	}

	private static void writeModStatisticsObject(JsonWriter jsonWriter, String resourceDomain, long pixelCount,
		long totalPixels) throws IOException {
		ModMetadata modInfo = getModMetadata(resourceDomain);
		String modName = modInfo != null ? modInfo.name : "";

		jsonWriter.beginObject()
			.name("resourceDomain")
			.value(resourceDomain)
			.name("pixelCount")
			.value(pixelCount)
			.name("percentOfTextureMap")
			.value(pixelCount * 100f / totalPixels)
			.name("modName")
			.value(modName)
			.name("url")
			.value(modInfo != null ? modInfo.url : null);

		jsonWriter.name("authors")
			.beginArray();
		{
			List<String> authorList = modInfo != null ? modInfo.authorList : Collections.emptyList();
			if (!authorList.isEmpty()) {
				for (String author : authorList) {
					jsonWriter.value(author.trim());
				}
			}
		}
		jsonWriter.endArray();

		jsonWriter.endObject();
	}

	private static ModMetadata getModMetadata(String resourceDomain) {
		ModContainer modContainer = modContainersForLowercaseIds.get(resourceDomain.toLowerCase(Locale.ENGLISH));
		if (modContainer == null) {
			ModMetadata modMetadata = new ModMetadata();
			modMetadata.name = resourceDomain.equals("minecraft") ? "Minecraft" : "unknown";
			return modMetadata;
		} else {
			return modContainer.getMetadata();
		}
	}

	public Path saveModStats(String name, TextureMap map, Path modStatsDir) throws IOException {
		@SuppressWarnings("unchecked")
		Map<String, Long> modPixelCounts = (Map<String, Long>) map.mapUploadedSprites.values().stream()
				.collect(Collectors.groupingBy(
						sprite -> new ResourceLocation(((TextureAtlasSprite)sprite).getIconName()).getResourceDomain(),
					Collectors.summingLong(sprite -> (long) ((TextureAtlasSprite) sprite).getIconWidth()
						* ((TextureAtlasSprite) sprite).getIconHeight()))
				);

		final long totalPixels = modPixelCounts.values().stream().mapToLong(longValue -> longValue).sum();

		final String filename = name + "_mod_statistics";
		Path output = modStatsDir.resolve(filename + ".js");

		List<Map.Entry<String, Long>> sortedEntries = modPixelCounts.entrySet().stream()
				.sorted(Collections.reverseOrder(Comparator.comparing(Map.Entry::getValue)))
				.collect(Collectors.toList());

		ProgressManager.ProgressBar progressBar = ProgressManager.push("Dumping Mod TextureMap Statistics", sortedEntries.size());
		FileWriter fileWriter = new FileWriter(output.toFile());
		fileWriter.write("var modStatistics = \n//Start of Data\n");
		JsonWriter jsonWriter = new JsonWriter(fileWriter);
		jsonWriter.setIndent("    ");
		jsonWriter.beginArray();
		{
			for (Map.Entry<String, Long> modPixels : sortedEntries) {
				String resourceDomain = modPixels.getKey();
				progressBar.step(resourceDomain);
				long pixelCount = modPixels.getValue();
				writeModStatisticsObject(jsonWriter, resourceDomain, pixelCount, totalPixels);
			}
		}
		jsonWriter.endArray();
		jsonWriter.close();
		fileWriter.close();

		Log.info("Saved mod statistics to {}.", output.toString());
		ProgressManager.pop(progressBar);
		return output;

	}
}
