package com.kochzap.share;

        import java.util.Arrays;
        import java.util.HashMap;
        import java.util.LinkedList;
        import java.util.Map;

/**
 * Created by naw880 on 10/23/16.
 * Moved company list stuff, including default, to a dedicated class
 */

public class Companies {

    static private final Map<String, String> coName = new HashMap<String, String>();

    // Used https://en.wikipedia.org/wiki/Georgia-Pacific to get various Koch Bros companies
    // Like Fort James, James River, and Crown-Zellenbach
    // Then look the companies up in the GS1 company database here:
    // https://www.gs1us.org/resources/tools/gs1-company-database
    static private final String[] defaultCompanies = {
            "010322",
            "030400",
            "032300",
            "036500",
            "041184",
            "040200",
            "042000",
            "044113",
            "047563",
            "048387",
            "050093",
            "050101",
            "054561",
            "054654",
            "064626",
            "073310",
            "073525",
            "078731",
            "081999",
            "090581",
            "098776",
            "100010",
            "100304",
            "100365",
            "100420",
            "100733",
            "330400",
            "492530",
            "637858",
            "638456",
            "641438",
            "642014",
            "642125",
            "643462",
            "695140",
            "700617",
            "713002",
            "720189",
            "728131",
            "740439",
            "781147",
            "785459",
            "785923",
            "785927",
            "796433",
            "796845",
            "798527",
            "887814"
    };


    private static LinkedList<String> companies = new LinkedList<>();

    Companies() {
        init();
    }

    private static void init() {
        companies = new LinkedList<>(Arrays.asList(defaultCompanies));
        coName.put("030400", "Georgia Pacific Angel Soft, Sparkle");
        coName.put("036500", "Georgia Pacific Angel Soft");
        coName.put("042000", "Georgia Pacific Quilted Northern, Brawny, Dixie, Vanity Fair");
        coName.put("044113", "Georgia Pacific Vanity Fair");
        coName.put("064626", "Georgia Pacific Dixie");
        coName.put("073310", "Georgia Pacific Angel Soft, Soft and Gentle, Brawny, Mardi Gras, Sparkle");
        coName.put("078731", "Georgia Pacific Dixie, Mardi Gras");
        coName.put("090581", "Georgia Pacific Vanity Fair");
        coName.put("098776", "Georgia Pacific Vanity Fair");
        coName.put("100010", "Georgia Pacific Vanity Fair");
        coName.put("100304", "Georgia Pacific Angel Soft");
        coName.put("100365", "Georgia Pacific Angel Soft");
        coName.put("100420", "Georgia Pacific Brawny");
        coName.put("100733", "Georgia Pacific Angel Soft, Brawny");
        coName.put("400001", "Georgia Pacific Vanity Fair");
        coName.put("492530", "Georgia Pacific Vanity Fair");
        coName.put("638456", "Georgia Pacific Vanity Fair");
        coName.put("641438", "Georgia Pacific Vanity Fair");
        coName.put("642014", "Georgia Pacific Vanity Fair");
        coName.put("642125", "Georgia Pacific Vanity Fair");
        coName.put("643462", "Georgia Pacific Vanity Fair");
        coName.put("695140", "Georgia Pacific Vanity Fair");
        coName.put("713002", "Georgia Pacific Vanity Fair");
        coName.put("720189", "Georgia Pacific Vanity Fair");
        coName.put("728131", "Georgia Pacific Vanity Fair");
        coName.put("740439", "Georgia Pacific Vanity Fair");
        coName.put("781147", "Georgia Pacific Vanity Fair");
        coName.put("785459", "Georgia Pacific Vanity Fair");
        coName.put("785923", "Georgia Pacific Vanity Fair");
        coName.put("785927", "Georgia Pacific Mardi Gras");
        coName.put("796433", "Georgia Pacific Vanity Fair");
        coName.put("796845", "Georgia Pacific Vanity Fair");
        coName.put("798527", "Georgia Pacific Vanity Fair");
        coName.put("887814", "Georgia Pacific Mardi Gras");
    }

    public static boolean containscompany(String co) {
        return companies.contains(co);
    }
    public static boolean containsscan(String scan) {
        if (scan.isEmpty()) {
            return false;
        }
        if (scan.length() > 5) {

            if (coName.size() < 5) {
                init();
            }

            String co = scan.substring(0, 6);
            return companies.contains(co);
        }
        return false;
    }

    public static String companyFromScan(String scan) {
        if (scan.isEmpty()) {
            return null;
        }

        if (scan.length() > 5) {
            return scan.substring(0, 6);
        } else {
            return "";
        }
    }

    public static String nameFromScan(String scan) {
        if (scan.isEmpty()) {
            return "";
        }
        if (scan.length() > 5) {
            String co = scan.substring(0, 6);

            if (coName.size() < 5) {
                init();
            }

            String cn = coName.get(co);
            if (cn.isEmpty()) {
                return "";
            }
            return cn;
        } else {
            return "";
        }
    }
}