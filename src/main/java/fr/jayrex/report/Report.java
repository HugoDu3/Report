package fr.jayrex.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.io.ByteStreams;

import fr.jayrex.report.commands.ReportCommand;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public final class Report extends Plugin {

    @Getter
    private Configuration config;
    @Getter
    private Configuration data;

    @Override
    public final void onEnable() {


        config = loadConfig("config.yml");
        data = loadConfig("data.yml");


        getProxy().getPluginManager().registerCommand(this, new ReportCommand(this));
    }

    @Override
    public final void onDisable() {
        config = null;
        saveConfig("data.yml", data);
        data = null;
    }


    @SneakyThrows
    public Configuration loadConfig(@NonNull String fileName) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), fileName);
        if (!configFile.exists()) {
            configFile.createNewFile();
            @Cleanup
            InputStream is = getResourceAsStream(fileName);
            @Cleanup
            OutputStream os = new FileOutputStream(configFile);
            ByteStreams.copy(is, os);
        }
        return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
    }

    @SneakyThrows
    public void saveConfig(@NonNull String fileName, Configuration config) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), fileName);
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
    }

}
