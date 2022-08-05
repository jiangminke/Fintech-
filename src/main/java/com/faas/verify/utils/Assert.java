package com.faas.verify.utils;

public class Assert {
    public static boolean hasLength(String str) {
        return str != null && !str.isEmpty();
    }
    public static boolean hasLength(CharSequence str) {
        return str != null && str.length() > 0;
    }
    public static boolean hasText(CharSequence str) {
        return hasLength(str) && containsText(str);
    }

    public static boolean hasText(String str) {
        return hasLength(str) && containsText(str);
    }
    private static boolean containsText(CharSequence str) {
        int strLen = str.length();

        for(int i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }

        return false;
    }
    public static void hasText(String text, String message) {
        if (!hasText(text)) {
            throw new IllegalArgumentException(message);
        }
    }
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
