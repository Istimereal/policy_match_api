package app.utils;

import app.exceptions.ApiException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static void main(String[] args) {
        System.out.println(getPropertyValue("db.name", "properties-from-pom.properties"));
    }

    public static String getPropertyValue(String propName, String resourceName)  {
        try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(resourceName)) {
            Properties prop = new Properties();
            prop.load(is);

            String value = prop.getProperty(propName);
            if (value != null) {
                return value.trim();  // Trim whitespace
            } else {
                throw new ApiException(500, String.format("Property %s not found in %s", propName, resourceName));
            }
        } catch (IOException ex) {
            throw new ApiException(500, String.format("Could not read property %s.", propName));
        }
    }

    public static String getPropertyValueSecurity(String propName, String ressourceName)  {
        // REMEMBER TO BUILD WITH MAVEN FIRST. Read the property file if not deployed (else read system vars instead)
        // Read from ressources/config.properties or from pom.xml depending on the ressourceName
        try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(ressourceName)) { //"config.properties" or "properties-from-pom.properties"
            Properties prop = new Properties();
            prop.load(is);
            return prop.getProperty(propName);
        } catch (IOException ex) {
            logger.error("Could not read property " + propName + " from " + ressourceName, ex);
            throw new ApiException(500, String.format("Could not read property %s. Did you remember to build the project with MAVEN?", propName));
        }
    }

    public ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Ignore unknown properties in JSON
        objectMapper.registerModule(new JavaTimeModule()); // Serialize and deserialize java.time objects
        objectMapper.writer(new DefaultPrettyPrinter());
        return objectMapper;
    }
}
