import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
import java.util.logging.Logger;
import java.io.*;
import java.util.HashMap;

public class App {

    private static Logger logger = Logger.getLogger("MyLogger", null);

    public static void main(String[] args) {
        JSONParser parser = new JSONParser();

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(new File("login.properties"))));
            StringBuilder contents = new StringBuilder();
            char[] batch = new char[1024];
            String[] properties = { "host", "username", "password" };
            HashMap<String, String> result = new HashMap<>();

            while (reader.read(batch, 0, batch.length) != -1) {
                contents.append(batch);
            }

            String contentsStr = contents.toString().trim();

            reader.close();

            JSONObject loginProperties = (JSONObject) parser.parse(contentsStr);

            for (int i = 0; i < properties.length; i++) {
                Object loginPropertyValue = loginProperties.get(properties[i]);
                if (loginPropertyValue == null)
                    throw new IllegalArgumentException("Required property \'" + properties[i] + "\' is missing.");
                String loginPropertyValueStr = loginPropertyValue.toString();
                result.put(properties[i], loginPropertyValueStr);
            }

            FetchEmailServer fetcher = new FetchEmailServer(logger);

            fetcher.check(result.get("host"), result.get("username"), result.get("password"));

        } catch (IOException e) {
            logger.severe("The file \'login.properties\' was not found.");
        } catch (ParseException e) {
            logger.severe("The file's syntax does not meet JSON specifications. Error at position " + e.getPosition());
        } catch (IllegalArgumentException e) {
            logger.severe(e.getMessage());
        }
    }
}
