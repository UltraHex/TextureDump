package mezz.texturedump.dumpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import cpw.mods.fml.common.ProgressManager;
import mezz.texturedump.TextureDump;
import mezz.texturedump.util.Log;

public class ResourceWriter {

    public static void writeFiles(String name, Path outputFolder, Path mipmapsDir, List<Path> textureImagePaths,
        List<Path> textureInfoJsPaths, Path modStatsPath, Path resourceDir, int mipmapLevels) throws IOException {
        ProgressManager.ProgressBar progressBar = ProgressManager.push(
            "Writing TextureMap resources to files",
            mipmapLevels);

        for (int level = 0; level < mipmapLevels; level++) {
            final Path htmlFile;
            if (level == 0) {
                htmlFile = outputFolder.resolve(name + ".html");
            } else {
                htmlFile = mipmapsDir.resolve(name + "_mipmap_" + level + ".html");
            }
            progressBar.step(htmlFile.getFileName()
                .toString());

            Path textureInfoJsFile = textureInfoJsPaths.get(level);
            Path textureImageFile = textureImagePaths.get(level);

            String webPage = getResourceAsString("page.html").replaceAll(
                    "\\[statisticsFile]",
                    fileToRelativeHtmlPath(modStatsPath, outputFolder))
                .replaceAll("\\[textureImage]", fileToRelativeHtmlPath(textureImageFile, outputFolder))
                .replaceAll("\\[textureInfo]", fileToRelativeHtmlPath(textureInfoJsFile, outputFolder))
                .replaceAll("\\[resourceDir]", fileToRelativeHtmlPath(resourceDir, outputFolder));

            FileWriter htmlFileWriter = new FileWriter(htmlFile.toFile());
            htmlFileWriter.write(webPage);
            htmlFileWriter.close();

            Log.info("Exported html to: {}", htmlFile.toString());
        }

        ProgressManager.pop(progressBar);
    }

    private static String fileToRelativeHtmlPath(Path file, Path outputFolder) {
        String path = outputFolder.relativize(file)
            .toString();
        return FilenameUtils.separatorsToUnix(path);
    }

    public static void writeResources(Path resourceDir) throws IOException {
        writeFileFromResource(resourceDir, "fastdom.min.js");
        writeFileFromResource(resourceDir, "texturedump.js");
        writeFileFromResource(resourceDir, "texturedump.css");
        writeFileFromResource(resourceDir, "texturedump.backgrounds.css");
        IResourceManager resourceManager = Minecraft.getMinecraft()
            .getResourceManager();
        final IResource resource = resourceManager.getResource(new ResourceLocation(TextureDump.MODID, "bg.png"));
        final InputStream inputStream = resource.getInputStream();
        final OutputStream outputStream = Files.newOutputStream(resourceDir.resolve("bg.png"));
        IOUtils.copy(inputStream, outputStream);
    }

    private static void writeFileFromResource(Path outputFolder, String s) throws IOException {
        FileWriter fileWriter = new FileWriter(outputFolder.resolve(s)
            .toFile());
        fileWriter.write(getResourceAsString(s));
        fileWriter.close();
    }

    private static String getResourceAsString(String resourceName) throws IOException {
        IResourceManager resourceManager = Minecraft.getMinecraft()
            .getResourceManager();
        final IResource resource = resourceManager.getResource(new ResourceLocation(TextureDump.MODID, resourceName));
        final InputStream inputStream = resource.getInputStream();
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, Charset.defaultCharset());
        String string = writer.toString();
        inputStream.close();
        return string;
    }
}
