package io.confluent.developer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private final static String DEV_CONFIG_FILE = "configuration/dev.properties";

    public static Properties loadEnvProperties() throws IOException {
        Properties allProps = new Properties();
        FileInputStream input = new FileInputStream(DEV_CONFIG_FILE);
        allProps.load(input);
        input.close();

        return allProps;
    }
}
