package com.github.siom79.japicmp.mcp.service;

import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.JApiClass;
import japicmp.output.markdown.MarkdownOutputGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class JApiCmpToolService {

    private static final Logger log = LoggerFactory.getLogger(JApiCmpToolService.class);
    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";

    @Tool(description = "Compares two Maven artifacts for API compatibility using japicmp. Returns a Markdown report.")
    public String compareJavaApi(
            @ToolParam(description = "GroupId of the old artifact (e.g. com.example)") String oldGroupId,
            @ToolParam(description = "ArtifactId of the old artifact") String oldArtifactId,
            @ToolParam(description = "Version of the old artifact (e.g. 1.0.0)") String oldVersion,
            @ToolParam(description = "GroupId of the new artifact") String newGroupId,
            @ToolParam(description = "ArtifactId of the new artifact") String newArtifactId,
            @ToolParam(description = "Version of the new artifact") String newVersion,
            @ToolParam(description = "Only show modified API elements (default: false)") boolean onlyModified,
            @ToolParam(description = "Only show binary incompatible changes (default: false)") boolean onlyBinaryIncompatible) {

        log.info("Comparing {}:{}:{} vs {}:{}:{}",
                oldGroupId, oldArtifactId, oldVersion,
                newGroupId, newArtifactId, newVersion);

        byte[] oldBytes = downloadJar(oldGroupId, oldArtifactId, oldVersion);
        byte[] newBytes = downloadJar(newGroupId, newArtifactId, newVersion);

        return compare(
                oldBytes, oldGroupId, oldArtifactId, oldVersion,
                newBytes, newGroupId, newArtifactId, newVersion,
                onlyModified, onlyBinaryIncompatible);
    }

    private byte[] downloadJar(String groupId, String artifactId, String version) {
        String url = MAVEN_CENTRAL
                + groupId.replace(".", "/") + "/"
                + artifactId + "/"
                + version + "/"
                + artifactId + "-" + version + ".jar";
        log.debug("Downloading {}", url);
        try (BufferedInputStream in = new BufferedInputStream(URI.create(url).toURL().openStream());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            log.debug("Downloaded {} bytes from {}", baos.size(), url);
            return baos.toByteArray();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Artifact not found on Maven Central: " + url, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download artifact: " + url, e);
        }
    }

    private String compare(
            byte[] oldBytes, String oldGroupId, String oldArtifactId, String oldVersion,
            byte[] newBytes, String newGroupId, String newArtifactId, String newVersion,
            boolean onlyModified, boolean onlyBinaryIncompatible) {

        JarArchiveComparatorOptions comparatorOptions = new JarArchiveComparatorOptions();
        comparatorOptions.getIgnoreMissingClasses().setIgnoreAllMissingClasses(true);
        JarArchiveComparator comparator = new JarArchiveComparator(comparatorOptions);

        List<JApiCmpArchive> oldArchives = Collections.singletonList(
                new JApiCmpArchive(oldBytes, oldVersion, oldGroupId + ":" + oldArtifactId + ":" + oldVersion));
        List<JApiCmpArchive> newArchives = Collections.singletonList(
                new JApiCmpArchive(newBytes, newVersion, newGroupId + ":" + newArtifactId + ":" + newVersion));

        List<JApiClass> jApiClasses = comparator.compare(oldArchives, newArchives);

        Options options = Options.newDefault();
        options.setHtmlOutputFile(Optional.of(""));
        options.setOldArchives(oldArchives);
        options.setNewArchives(newArchives);
        options.setOutputOnlyModifications(onlyModified);
        options.setOutputOnlyBinaryIncompatibleModifications(onlyBinaryIncompatible);

        return new MarkdownOutputGenerator(options, jApiClasses).generate();
    }
}
