package com.blbilink.neoLibrary.utils;

public class YmlUtil {

    /**
     * 比较两个版本号，判断 version1 是否比 version2 更新。
     * 例如: isVersionNewer("1.2.1", "1.2") -> true
     * 例如: isVersionNewer("1.3", "1.2.1") -> true
     * 例如: isVersionNewer("1.2", "1.2.0") -> false
     *
     * @param version1 第一个版本号
     * @param version2 第二个版本号
     * @return 如果 version1 比 version2 新，则返回 true，否则返回 false
     */
    public static boolean isVersionNewer(String version1, String version2) {
        // 进行健壮性检查
        if (version1 == null || version1.isBlank()) return false;
        if (version2 == null || version2.isBlank()) return true; // 任何有效版本都比空版本新

        String[] arr1 = version1.split("\\.");
        String[] arr2 = version2.split("\\.");

        int maxLength = Math.max(arr1.length, arr2.length);
        for (int i = 0; i < maxLength; i++) {
            int num1 = 0;
            if (i < arr1.length) {
                try {
                    num1 = Integer.parseInt(arr1[i]);
                } catch (NumberFormatException e) {
                    // 如果版本部分不是数字，当作0处理，并可以记录一个警告
                }
            }

            int num2 = 0;
            if (i < arr2.length) {
                try {
                    num2 = Integer.parseInt(arr2[i]);
                } catch (NumberFormatException e) {
                    // 同上
                }
            }

            if (num1 > num2) {
                return true;
            } else if (num1 < num2) {
                return false;
            }
        }

        // 如果所有部分都相同，则版本相等，version1 不是更新的
        return false;
    }
}