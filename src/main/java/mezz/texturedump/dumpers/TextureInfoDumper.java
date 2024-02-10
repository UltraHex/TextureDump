package mezz.texturedump.dumpers;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;

import com.google.gson.stream.JsonWriter;

import cpw.mods.fml.common.ProgressManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TextureInfoDumper {

	public static List<Path> saveTextureInfoDataFiles(String name, TextureMap map, int mipmapLevels, Path outputFolder)
		throws IOException {
		//Set<String> animatedTextures = map.listAnimatedSprites.stream()
		//		.map(TextureAtlasSprite::getIconName)
		//		.collect(Collectors.toSet());

		HashSet<String> animatedTextures = new HashSet<String>();

		for (Object listAnimatedSprite : map.listAnimatedSprites) {
			TextureAtlasSprite sprite = (TextureAtlasSprite)listAnimatedSprite;
			animatedTextures.add(sprite.getIconName());
		}

		ProgressManager.ProgressBar progressBar = ProgressManager.push("Dumping TextureMap info to file", mipmapLevels);

		List<Path> dataFiles = new ArrayList<>();
		for (int level = 0; level < mipmapLevels; level++) {
			final String filename = name + "_mipmap_" + level;
			Path dataFile = outputFolder.resolve(filename + ".js");
			progressBar.step(filename);

			StringWriter out = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(out);
			jsonWriter.setIndent("    ");

			@SuppressWarnings("unchecked")
			Collection<TextureAtlasSprite> values = map.mapUploadedSprites.values();
			ProgressManager.ProgressBar progressBar2 = ProgressManager.push("Mipmap Level " + level, values.size());
			jsonWriter.beginArray();
			{
				for (TextureAtlasSprite sprite : values) {
					String iconName =  sprite.toString() + sprite.getIconName();
					if (iconName.indexOf(':') == -1) {
						iconName = "minecraft:" + iconName;
					}
					progressBar2.step(iconName);
					boolean animated = animatedTextures.contains(iconName);
					jsonWriter.beginObject()
						.name("name")
						.value(iconName)
						.name("animated")
						.value(animated)
						.name("x")
						.value(sprite.getOriginX() / (1L << level))
						.name("y")
						.value(sprite.getOriginY() / (1L << level))
						.name("width")
						.value(sprite.getIconWidth() / (1L << level))
						.name("height")
						.value(sprite.getIconHeight() / (1L << level))
						.endObject();
				}
			}
			jsonWriter.endArray();
			jsonWriter.close();
			out.close();

			FileWriter fileWriter;
			fileWriter = new FileWriter(dataFile.toFile());
			fileWriter.write("var textureData = \n//Start of Data\n" + out);
			fileWriter.close();

			dataFiles.add(dataFile);

			ProgressManager.pop(progressBar2);
		}

		ProgressManager.pop(progressBar);

		return dataFiles;
	}
}
