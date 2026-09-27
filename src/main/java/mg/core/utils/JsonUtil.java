package mg.core.utils;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class JsonUtil {
    private static final Gson gson = new Gson();

    public static boolean isValidJson(String str) {
        if (str == null) {
            return false;
        }
        String trimmed = str.trim();
        if ((!trimmed.startsWith("{") || !trimmed.endsWith("}"))
                && (!trimmed.startsWith("[") || !trimmed.endsWith("]"))) {
            return false;
        }
        try {
            JsonParser.parseString(trimmed);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    public static String formatToJson(Object data) {
        if (data == null) {
            return "null";
        }
        if (data instanceof String) {
            String strData = (String) data;
            if (isValidJson(strData)) {
                return strData;
            }
        }
        return gson.toJson(data);
    }
}