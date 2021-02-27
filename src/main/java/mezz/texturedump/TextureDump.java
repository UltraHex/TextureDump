package mezz.texturedump;

import java.io.File;
import java.util.Map;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mezz.texturedump.dumpers.ModStatsDumper;
import mezz.texturedump.dumpers.TextureImageDumper;
import mezz.texturedump.dumpers.TextureInfoDumper;
import mezz.texturedump.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(
		modid = TextureDump.MODID,
		name = TextureDump.NAME,
		version = TextureDump.VERSION
)
public class TextureDump {
	public static final String MODID = "texturedump";
	public static final String NAME = "TextureDump";
	public static final String VERSION = "@VERSION@";
	private boolean mainMenuOpened = false;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	/* broken
	@Mod.EventHandler
	public void onLoadComplete(FMLLoadCompleteEvent event) {
		if (event.getSide() == Side.CLIENT) {
			// Reload when resources change
			Minecraft minecraft = Minecraft.getMinecraft();
			IReloadableResourceManager reloadableResourceManager = (IReloadableResourceManager) minecraft.getResourceManager();
			reloadableResourceManager.registerReloadListener(resourceManager -> {
				if (mainMenuOpened) { // only reload when the player requests it in-game
					System.out.println("debug yeah fuck dump");
					try {
						dumpTextureMaps();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	 */

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onMainMenuOpen(GuiOpenEvent event) {
		if (!mainMenuOpened && event.gui instanceof GuiMainMenu) {
			mainMenuOpened = true;
			dumpTextureMaps();
		}
	}

	@SideOnly(Side.CLIENT)
	private static void dumpTextureMaps() {
		TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
		textureManager.mapTextureObjects.forEach((key, textureObject) -> {
			if (textureObject instanceof TextureMap) {
				String name = key.toString().replace(':', '_').replace('/', '_');
				dumpTextureMap((TextureMap) textureObject, name);
			}
		});
	}

	@SideOnly(Side.CLIENT)
	private static void dumpTextureMap(TextureMap map, String name) {
		int mip = map.mipmapLevels;
		File outputFolder = new File("texture_dump");
		if (!outputFolder.exists()) {
			if (!outputFolder.mkdir()) {
				Log.error("Failed to create directory " + outputFolder);
				return;
			}
		}

		TextureImageDumper.saveGlTexture(name, map.getGlTextureId(), mip, outputFolder);
		TextureInfoDumper.saveTextureInfo(name, map, mip, outputFolder);

		ModStatsDumper modStatsDumper = new ModStatsDumper();
		modStatsDumper.saveModStats(name, map, outputFolder);
	}

}
