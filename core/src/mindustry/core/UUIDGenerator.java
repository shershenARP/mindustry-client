package mindustry.core;

import arc.Core;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static mindustry.Vars.ui;

public class UUIDGenerator {

    // Статический метод для генерации полного UUID
    public static String generateFullUUID(byte[] shortUUID) throws NoSuchAlgorithmException {
        // Получаем хеш SHA-256 от короткого UUID
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(shortUUID);

        // Берём первые 8 байт от хеша и соединяем их с оригинальными 8 байтами
        byte[] fullUUID = new byte[16];
        System.arraycopy(shortUUID, 0, fullUUID, 0, 8); // копируем первые 8 байт
        System.arraycopy(hash, 0, fullUUID, 8, 8); // генерируем оставшиеся 8 байт

        // Преобразуем в строку Base64
        return Base64.getEncoder().encodeToString(fullUUID); // Используем стандартный Base64
    }

    // Метод main для вызова в статическом контексте
    public static void main(String[] args) {
        try {
            // Получаем короткий UUID из Core.settings
            String shortUUIDString = Core.settings.getString("uuid");
            byte[] shortUUID = Base64.getDecoder().decode(shortUUIDString); // Преобразуем его в массив байтов

            // Генерируем полный UUID
            String fullUUID = generateFullUUID(shortUUID);

            // Выводим полный UUID
            ui.showInfo("Full UUID: " + fullUUID);
        } catch (NoSuchAlgorithmException e) {
            // Обработка исключения, если алгоритм не найден
            e.printStackTrace();
        }
    }
}
