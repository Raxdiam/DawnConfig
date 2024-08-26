package com.raxdiam.dawn.serializer;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.electronwill.nightconfig.core.serde.SerdeException;
import com.raxdiam.dawn.ConfigData;
import com.raxdiam.dawn.annotation.Config;
import com.raxdiam.dawn.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.electronwill.nightconfig.core.Config.*;

public class NightConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {
    private final Class<T> configClass;
    private final Path configPath;
    private final ObjectSerializer serializer = ObjectSerializer.standard();
    private final ObjectDeserializer deserializer = ObjectDeserializer.standard();

    public NightConfigSerializer(Config definition, Class<T> configClass) {
        if (!isInsertionOrderPreserved()) {
            setInsertionOrderPreserved(true);
        }

        this.configClass = configClass;
        this.configPath = Utils.getConfigFolder().resolve(definition.name() + ".toml");
    }

    @Override
    public void serialize(T config) throws SerializationException {
        try (var configFile = CommentedFileConfig.of(configPath)) {
            writeConfig(config, configFile);
        } catch (SerdeException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public T deserialize() throws SerializationException {
        try (var configFile = CommentedFileConfig.of(configPath)) {
            var config = createDefault();
            if (!Files.exists(configPath)) {
                writeConfig(config, configFile);
                return config;
            }
            configFile.load();
            deserializer.deserializeFields(configFile, config);
            return config;
        } catch (SerdeException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public T createDefault() {
        return Utils.constructUnsafely(configClass);
    }

    private void writeConfig(T config, CommentedFileConfig configFile) {
        serializer.serializeFields(config, configFile);
        configFile.save();
    }
}
