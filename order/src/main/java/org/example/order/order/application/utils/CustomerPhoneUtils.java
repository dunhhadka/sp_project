package org.example.order.order.application.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public final class CustomerPhoneUtils {

    private static final Pattern VN_PHONE_PREFIX_PATTERN = Pattern.compile("^(" +
            //viettel
            "086|8486|\\+8486" +
            "|096|8496|\\+8496" +
            "|097|8497|\\+8497" +
            "|098|8498|\\+8498" +
            "|032|8432|\\+8432" +
            "|033|8433|\\+8433" +
            "|034|8434|\\+8434" +
            "|035|8435|\\+8435" +
            "|036|8436|\\+8436" +
            "|037|8437|\\+8437" +
            "|038|8438|\\+8438" +
            "|039|8439|\\+8439" +
            //vina
            "|081|8481|\\+8481" +
            "|082|8482|\\+8482" +
            "|083|8483|\\+8483" +
            "|084|8484|\\+8484" +
            "|085|8485|\\+8485" +
            "|088|8488|\\+8488" +
            "|091|8491|\\+8491" +
            "|094|8494|\\+8494" +
            //mobi
            "|070|8470|\\+8470" +
            "|076|8476|\\+8476" +
            "|077|8477|\\+8477" +
            "|078|8478|\\+8478" +
            "|079|8479|\\+8479" +
            "|089|8489|\\+8489" +
            "|090|8490|\\+8490" +
            "|093|8493|\\+8493" +
            //vietnammobile
            "|056|8456|\\+8456" +
            "|058|8458|\\+8458" +
            "|092|8492|\\+8492" +
            //Gmobile
            "|059|8459|\\+8459" +
            "|099|8499|\\+8499" +
            //itelecom
            "|087|8487|\\+8487" +
            //reddi
            "|055|8455|\\+8455" +
            ").+");

    public static boolean isValid(String phone) {
        return isStrictValid(phone) || isWeakValid(phone);
    }

    private static boolean isStrictValid(String phone) {
        var vnPhoneMarcher = VN_PHONE_PREFIX_PATTERN.matcher(phone);
        if (vnPhoneMarcher.matches()) {
            var prefix = vnPhoneMarcher.group(1);
            var other = phone.substring(prefix.length());
            return other.length() == 7 && isDigits(other);
        }
        return false;
    }

    private static boolean isWeakValid(String phone) {
        if (phone.startsWith("+")) {
            phone = phone.substring(1);
        }
        return isDigits(phone) && phone.length() >= 7 && phone.length() <= 13;
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String normalize(String phone) {
        if (StringUtils.isBlank(phone)) return null;
        if (phone.startsWith("84")) return "+" + phone;
        if (phone.startsWith("0") && VN_PHONE_PREFIX_PATTERN.matcher(phone).matches()) {
            return "+84" + phone.substring(1);
        }
        return phone;
    }
}
