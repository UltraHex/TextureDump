package mezz.texturedump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ProgressManager;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mezz.texturedump.dumpers.ModStatsDumper;
import mezz.texturedump.dumpers.ResourceWriter;
import mezz.texturedump.dumpers.TextureImageDumper;
import mezz.texturedump.dumpers.TextureInfoDumper;
import mezz.texturedump.util.Log;

@Mod(modid = TextureDump.MOD_ID, name = TextureDump.NAME, version = TextureDump.VERSION)
public class TextureDump {

    public static final String MOD_ID = "texturedump";
    public static final String NAME = "TextureDump";
    public static final String VERSION = "@VERSION@";
    private boolean mainMenuOpened = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /*
     * broken
     * @Mod.EventHandler
     * public void onLoadComplete(FMLLoadCompleteEvent event) {
     * if (event.getSide() == Side.CLIENT) {
     * // Reload when resources change
     * Minecraft minecraft = Minecraft.getMinecraft();
     * IReloadableResourceManager reloadableResourceManager = (IReloadableResourceManager)
     * minecraft.getResourceManager();
     * reloadableResourceManager.registerReloadListener(resourceManager -> {
     * if (mainMenuOpened) { // only reload when the player requests it in-game
     * System.out.println("debug yeah fuck dump");
     * try {
     * dumpTextureMaps();
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * }
     * });
     * }
     * }
     */

    @SideOnly(Side.CLIENT)
    private static void dumpTextureMaps() throws IOException {
        Path outputFolder = Paths.get("texture_dump");
        outputFolder = Files.createDirectories(outputFolder);

        TextureManager textureManager = Minecraft.getMinecraft()
            .getTextureManager();

        try {
            Path mipmapsDir = createSubDirectory(outputFolder, "mipmaps");
            Path resourceDir = createSubDirectory(outputFolder, "resources");
            Path modStatsDir = createSubDirectory(outputFolder, "modStats");
            Path texturesDir = createSubDirectory(outputFolder, "textures");
            Path textureInfoDir = createSubDirectory(outputFolder, "textureInfo");
            ResourceWriter.writeResources(resourceDir);

            @SuppressWarnings("unchecked")
            Map<ResourceLocation, ITextureObject> textureObjects = textureManager.mapTextureObjects;
            ProgressManager.ProgressBar progressBar = ProgressManager
                .push("Dumping TextureMaps", textureObjects.size());
            for (Map.Entry<ResourceLocation, ITextureObject> entry : textureObjects.entrySet()) {
                ITextureObject textureObject = entry.getValue();
                if (textureObject instanceof TextureMap) {
                    String name = entry.getKey()
                        .toString()
                        .replace(':', '_')
                        .replace('/', '_');
                    progressBar.step(name);
                    dumpTextureMap(
                        (TextureMap) textureObject,
                        name,
                        outputFolder,
                        mipmapsDir,
                        resourceDir,
                        modStatsDir,
                        texturesDir,
                        textureInfoDir);
                } else {
                    progressBar.step(textureObject.toString());
                }
            }
            ProgressManager.pop(progressBar);
        } catch (IOException e) {
            Log.error("Failed to dump texture maps.", e);
        }
    }

    private static void dumpTextureMap(TextureMap map, String name, Path outputFolder, Path mipmapsDir,
        Path resourceDir, Path modStatsDir, Path texturesDir, Path textureInfoDir) {
        try {
            ModStatsDumper modStatsDumper = new ModStatsDumper();
            Path modStatsPath = modStatsDumper.saveModStats(name, map, modStatsDir);

            List<Path> textureImagePaths = TextureImageDumper.saveGlTextures(name, map.getGlTextureId(), texturesDir);
            int mipmapLevels = textureImagePaths.size();
            List<Path> textureInfoJsPaths = TextureInfoDumper
                .saveTextureInfoDataFiles(name, map, mipmapLevels, textureInfoDir);

            ResourceWriter.writeFiles(
                name,
                outputFolder,
                mipmapsDir,
                textureImagePaths,
                textureInfoJsPaths,
                modStatsPath,
                resourceDir,
                mipmapLevels);
        } catch (IOException e) {
            Log.error(String.format("Failed to dump texture map: %s.", name), e);
        }
    }

    public static Path createSubDirectory(Path outputFolder, String subFolderName) throws IOException {
        Path subFolder = outputFolder.resolve(subFolderName);
        return Files.createDirectories(subFolder);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onMainMenuOpen(GuiOpenEvent event) {
        if (!mainMenuOpened && event.gui instanceof GuiMainMenu) {
            mainMenuOpened = true;
            try {
                dumpTextureMaps();
            } catch (IOException e) {
                Log.error("Failed to dump texture maps with error.", e);
            }
        }
    }
}
