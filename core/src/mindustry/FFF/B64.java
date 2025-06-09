package mindustry.FFF;

import arc.util.serialization.Base64Coder;

import java.nio.charset.StandardCharsets;

public class B64 {
    public static String write(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        String base64 = new String(Base64Coder.encode(bytes));
        return base64;
    }

    public static String read(String text) {
        byte[] bytes = Base64Coder.decode(text);
        String decodedB64 = new String(bytes, StandardCharsets.UTF_8);
        return decodedB64;
    }
}
