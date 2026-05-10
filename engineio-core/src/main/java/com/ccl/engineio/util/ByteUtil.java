package com.ccl.engineio.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 字节数组工具类
 *
 * <p>提供字节数组与字符串之间的转换、拼接、Hex 编码解码等常用操作</p>
 *
 * @author coffee377
 * @since 4.0.0-alpha.0
 */
public final class ByteUtil {

    private ByteUtil() {
    }

    /**
     * 将字符串转换为 UTF-8 编码的字节数组
     *
     * @param str 原始字符串
     * @return UTF-8 编码的字节数组
     */
    public static byte[] toBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将 UTF-8 编码的字节数组转换为字符串
     *
     * @param bytes UTF-8 编码的字节数组
     * @return 解码后的字符串
     */
    public static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 判断字节数组是否为空
     *
     * @param bytes 待检查的字节数组
     * @return 数组为 null 或长度为 0 时返回 true
     */
    public static boolean isEmpty(byte[] bytes) {
        return bytes == null || bytes.length == 0;
    }

    /**
     * 拼接多个字节数组
     *
     * @param arrays 待拼接的字节数组序列
     * @return 拼接后的新字节数组
     */
    public static byte[] concat(byte[]... arrays) {
        int totalLength = Arrays.stream(arrays).mapToInt(arr -> arr.length).sum();
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    /**
     * 将字节数组转换为 Hex 字符串
     *
     * @param bytes 字节数组
     * @return 小写 Hex 字符串，输入为 null 时返回 null
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 将 Hex 字符串转换为字节数组
     *
     * @param hex Hex 编码字符串（偶数长度，仅含 0-9 a-f A-F）
     * @return 解码后的字节数组
     * @throws IllegalArgumentException 当 Hex 字符串为 null 或长度为奇数时
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
