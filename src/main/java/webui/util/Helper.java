package webui.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Helper {
    private static final Logger LOG = LoggerFactory.getLogger(Helper.class.getSimpleName());


    public static Properties loadConfig(String filePath) throws IOException {
        Properties result = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            result.load(fis);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        return result;
    }

    public static JsonObject readJson(String fileName) {
        JsonObject jsonObject = new JsonObject();

        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(new FileReader(fileName));
            jsonObject = jsonElement.getAsJsonObject();
        } catch (IOException ioe) {
            LOG.error("Could not parse file", ioe);
        }

        return jsonObject;
    }
}
