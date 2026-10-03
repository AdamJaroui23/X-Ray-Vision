package com.adamjaroui.xrayvision.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes {@link XRayConfig} as pretty printed JSON inside the Fabric config directory
 * ({@code config/xrayvision.json}). Gson ships with Minecraft, so this adds no extra dependency.
 */
public final class XRayConfigManager {
	public static final String FILE_NAME = "xrayvision.json";

	private static final Logger LOGGER = LoggerFactory.getLogger("X-Ray Vision/Config");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private static XRayConfig config;
	private static Path configFile;

	private XRayConfigManager() {
	}

	/**
	 * Loads the configuration from disk, creating the file with default values when it is missing or
	 * unreadable. Never throws: a broken file falls back to the defaults so the game always starts.
	 */
	public static synchronized XRayConfig load(Path configDir) {
		configFile = configDir.resolve(FILE_NAME);
		XRayConfig loaded = null;

		if (Files.isRegularFile(configFile)) {
			try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, XRayConfig.class);
			} catch (IOException | JsonParseException | RuntimeException e) {
				LOGGER.error("Could not read {}, falling back to default settings", configFile, e);
				loaded = null;
			}
		}

		if (loaded == null) {
			loaded = new XRayConfig();
		}

		loaded.validate();
		loaded.configVersion = XRayConfig.CURRENT_VERSION;
		config = loaded;
		save();
		return config;
	}

	/**
	 * @return the loaded configuration; a default instance is created when {@link #load(Path)} has not run yet
	 */
	public static synchronized XRayConfig get() {
		if (config == null) {
			config = new XRayConfig();
			config.validate();
		}

		return config;
	}

	/**
	 * Writes the current configuration to disk. Failures are logged and swallowed: losing a settings
	 * file must never crash the client.
	 */
	public static synchronized void save() {
		if (configFile == null || config == null) {
			return;
		}

		try {
			Path parent = configFile.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Could not write {}", configFile, e);
		}
	}

	/**
	 * @return the absolute path of the configuration file, or {@code null} before the first load
	 */
	public static Path getConfigFile() {
		return configFile;
	}
}
