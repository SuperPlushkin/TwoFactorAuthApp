package ru.superplushkin.twofactorauthapp.subclasses;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base32;
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm;
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig;
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TOTPGenerator {
    private static final int TIME_STEP = 30;
    private static final Base32 BASE32 = new Base32();
    private static final short MAX_DIGITS = 10;
    private static final short MIN_DIGITS = 4;
    private static volatile long lastTimeUpdate = 0;
    private static volatile long cachedRemainingTime = 0;
    private static final Object timeLock = new Object();

    public static String generateCode(String secretKey, String algorithm, int digits) {
        return generateCodeForTime(secretKey, algorithm, digits, new Date());
    }
    public static String generateCodeForTime(String base32Secret, String algorithm, int digits, Date date) {
        try {
            byte[] decodedKey = BASE32.decode(cleanSecretKey(base32Secret));

            HmacAlgorithm hmacAlgorithm;
            switch (algorithm.toUpperCase()) {
                case "SHA256":
                    hmacAlgorithm = HmacAlgorithm.SHA256;
                    break;
                case "SHA512":
                    hmacAlgorithm = HmacAlgorithm.SHA512;
                    break;
                case "SHA1":
                default:
                    hmacAlgorithm = HmacAlgorithm.SHA1;
                    break;
            }

            var config = new TimeBasedOneTimePasswordConfig(TIME_STEP, TimeUnit.SECONDS, digits, hmacAlgorithm);
            var generator = new TimeBasedOneTimePasswordGenerator(decodedKey, config);
            return generator.generate(date);
        } catch (Exception e) {
            return "ERROR";
        }
    }

    public static long getTimeRemainingFromGenerator() {
        long currentTime = System.currentTimeMillis();
        synchronized (timeLock) {
            if (currentTime - lastTimeUpdate > 100) {
                long currentTimeSeconds = currentTime / 1000;
                cachedRemainingTime = TIME_STEP - (currentTimeSeconds % TIME_STEP);
                lastTimeUpdate = currentTime;
            }
            return cachedRemainingTime;
        }
    }
    public static int getTimerProgress() {
        long remaining = getTimeRemainingFromGenerator();
        long elapsed = TIME_STEP - remaining;
        return (int) ((elapsed * 100) / TIME_STEP);
    }

    private static String cleanSecretKey(String secretKey) {
        if (secretKey == null) return "";
        return secretKey.replaceAll("\\s", "").toUpperCase();
    }
    public static boolean isValidSecretKey(String secretKey) {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            return false;
        }

        try {
            String cleanedKey = cleanSecretKey(secretKey);
            if (!cleanedKey.matches("^[A-Z2-7]+=*$")) {
                return false;
            }

            byte[] decoded = BASE32.decode(cleanedKey);
            return decoded != null && decoded.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidAlgorithm(String algorithm) {
        if (algorithm == null) return false;
        String algoUpper = algorithm.toUpperCase();
        return algoUpper.equals("SHA1") || algoUpper.equals("SHA256") || algoUpper.equals("SHA512");
    }
    public static boolean isValidDigits(int digits) {
        return digits >= MIN_DIGITS && digits <= MAX_DIGITS;
    }

    public static String formatCode(String code) {
        int mid = code.length() / 2;
        return String.format("%s %s", code.substring(0, mid), code.substring(mid));
    }

    public static short getMaxDigits(){
        return MAX_DIGITS;
    }
    public static short getMinDigits(){
        return MIN_DIGITS;
    }
}