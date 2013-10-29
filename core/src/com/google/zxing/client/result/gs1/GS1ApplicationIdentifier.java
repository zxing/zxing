/*
 * Copyright 2013 Melchior Rabe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.result.gs1;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The enum listing all the application identifiers defined by GS1.
 * 
 * @author Melchior Rabe
 * 
 */
public enum GS1ApplicationIdentifier {
	SSCC(0, "SSCC", false, new NumericWithCheckDigitFormatter(18)),
	GTIN(1, "GTIN", false, new NumericWithCheckDigitFormatter(14)),
	CONTENT_GTIN(2, "CONTENT", false, new NumericWithCheckDigitFormatter(14)),
	BATCH_LOT(10, "BATCH/LOT", true, new AlphaNumericFormatter(20)),
	PRODUCTION_DATE(11, "PROD DATE", false, new DateFormatter()),
	DUE_DATE(12, "DUE DATE", false, new DateFormatter()),
	PACK_DATE(13, "PACK DATE", false, new DateFormatter()),
	BEST_BEFORE(15, "BEST BEFORE or SELL BY", false, new DateFormatter()),
	EXPIRY_DATE(17, "USE BY OR EXPIRY", false, new DateFormatter()),
	VARIANT(20, "VARIANT", false, new NumericFormatter(2)),
	SERIAL(21, "SERIAL", true, new AlphaNumericFormatter(20)),
	ADDITIONAL_ID(240, "ADDITIONAL ID", true, new AlphaNumericFormatter(30)),
	CUSTOMER_PART_NUMBER(
			241,
			"CUST. PART NO.",
			true,
			new AlphaNumericFormatter(30)),
	MTO_VARIANT(242, "MTO VARIANT", true, new NumericFormatter(6, false)),
	PCN(243, "PCN", true, new AlphaNumericFormatter(20)),
	SECONDARY_SERIAL(250, "SECONDARY SERIAL", true, new AlphaNumericFormatter(
			30)),
	REF_2_SOURCE(251, "REF. TO SOURCE", true, new AlphaNumericFormatter(30)),
	GDTI(
			253,
			"GDTI",
			true,
			new NumericWithCheckDigitPlusSerialFormatter(13, 17)),
	GLN_EXTENSION(
			254,
			"GLN EXTENSION COMPONENT",
			true,
			new AlphaNumericFormatter(20)),
	GCN(255, "GCN", true, new NumericWithCheckDigitPlusSerialFormatter(13, 12)),
	COUNT_OF_ITEMS(30, "VAR. COUNT", true, new NumericFormatter(8, false)),
	NET_WEIGHT_KG(310, "NET WEIGHT (kg)", false, new DecimalFormatter(6)),
	LENGTH_M(311, "LENGTH (m)", false, new DecimalFormatter(6)),
	WIDTH_M(312, "WIDTH (m)", false, new DecimalFormatter(6)),
	HEIGHT_M(313, "HEIGHT (m)", false, new DecimalFormatter(6)),
	AREA_M2(314, "AREA (m²)", false, new DecimalFormatter(6)),
	NET_VOLUME_L(315, "NET VOLUME (l)", false, new DecimalFormatter(6)),
	NET_VOLUME_M3(316, "NET VOLUME (m³)", false, new DecimalFormatter(6)),
	NET_WEIGHT_LB(320, "NET WEIGHT", false, new DecimalFormatter(6)),
	WIDTH_Y_LOG(346, "WIDTH (y), log", false, new DecimalFormatter(6)),
	HEIGHT_I_LOG(347, "HEIGHT (i), log", false, new DecimalFormatter(6)),
	HEIGHT_F_LOG(348, "HEIGHT (f), log", false, new DecimalFormatter(6)),
	HEIGHT_Y_LOG(349, "HEIGHT (y), log", false, new DecimalFormatter(6)),
	AREA_I2(350, "AREA (i²)", false, new DecimalFormatter(6)),
	AREA_F2(351, "AREA (f²)", false, new DecimalFormatter(6)),
	AREA_Y2(352, "AREA (y²)", false, new DecimalFormatter(6)),
	AREA_I2_LOG(353, "AREA (i²), log", false, new DecimalFormatter(6)),
	AREA_F2_LOG(354, "AREA (f²), log", false, new DecimalFormatter(6)),
	AREA_Y2_LOG(355, "AREA (y²), log", false, new DecimalFormatter(6)),
	NET_WEIGHT_T(356, "NET WEIGHT (t)", false, new DecimalFormatter(6)),
	NET_VOLUME_OZ(357, "NET VOLUME (oz)", false, new DecimalFormatter(6)),
	NET_VOLUME_Q(360, "NET VOLUME (q)", false, new DecimalFormatter(6)),
	NET_VOLUME_G(361, "NET VOLUME (g)", false, new DecimalFormatter(6)),
	NET_VOLUME_Q_LOG(362, "VOLUME (q), log", false, new DecimalFormatter(6)),
	NET_VOLUME_G_LOG(363, "VOLUME (g), log", false, new DecimalFormatter(6)),
	VOLUME_I3(364, "VOLUME (i³)", false, new DecimalFormatter(6)),
	VOLUME_F3(365, "VOLUME (f³)", false, new DecimalFormatter(6)),
	VOLUME_Y3(366, "VOLUME (y³)", false, new DecimalFormatter(6)),
	VOLUME_I3_LOG(367, "VOLUME (i³), log", false, new DecimalFormatter(6)),
	VOLUME_F3_LOG(368, "VOLUME (f³),log", false, new DecimalFormatter(6)),
	VOLUME_Y3_LOG(369, "VOLUME (y³), log", false, new DecimalFormatter(6)),
	AMOUNT_LOCAL(390, "AMOUNT LOCAL", true, new DecimalFormatter(15, false)),
	COUNT(37, "COUNT", true, new NumericFormatter(8, false)),
	AMOUNT_ISO(391, "AMOUNT ISO", true, new DecimalWithIsoFormatter(15, false)),
	PRICE(392, "PRICE", true, new DecimalFormatter(15, false)),
	PRICE_ISO(393, "PRICE ISO", true, new DecimalWithIsoFormatter(15, false)),
	ORDER_NUMBER(400, "ORDER NUMBER", true, new AlphaNumericFormatter(30)),
	GINC(401, "GINC", true, new AlphaNumericFormatter(30)),
	GSIN(402, "GSIN", true, new NumericWithCheckDigitFormatter(17)),
	ROUTE(403, "ROUTE", true, new AlphaNumericFormatter(30)),
	SHIP_TO_LOC(410, "SHIP TO LOC", false, new NumericWithCheckDigitFormatter(13)),
	BILL_TO_LOC(411, "BILL TO", false, new NumericWithCheckDigitFormatter(13)),
	PURCHASED_FROM_LOC(412, "PURCHASE FROM", false, new NumericWithCheckDigitFormatter(13)),
	SHIP_FOR_LOC(413, "SHIP FOR LOC", false, new NumericWithCheckDigitFormatter(13)),
	LOC_NO(414, "LOC No", false, new NumericWithCheckDigitFormatter(13)),
	PAY_TO(415, "PAY TO", false, new NumericWithCheckDigitFormatter(13)),
	SHIP_TO_POST(420, "SHIP TO POST", true, new AlphaNumericFormatter(20)),
	SHIP_TO_POST_ISO(
			421,
			"SHIP TO POST",
			true,
			new AlphaNumericWithIsoFormatter(9)),
	ORIGIN(422, "ORIGIN", true, new IsoCountryFormatter(1)),
	COUNTRY_INITIAL_PROCESS(
			423,
			"COUNTRY - INITIAL PROCESS.",
			true,
			new IsoCountryFormatter(5)),
	COUNTRY_PROCESS(424, "COUNTRY - PROCESS.", true, new IsoCountryFormatter(1)),
	COUNTRY_DISASSEMBLY(
			425,
			"COUNTRY - DISASSEMBLY",
			true,
			new IsoCountryFormatter(1)),
	COUNTRY_FULL_PROCESS(
			426,
			"COUNTRY – FULL PROCESS",
			true,
			new IsoCountryFormatter(1)),
	ORIGIN_SUBDIVISION(
			427,
			"ORIGIN SUBDIVISION",
			true,
			new AlphaNumericFormatter(3)),
	NSN(7001, "NSN", true, new NumericFormatter(13)),
	MEAT_CUT(7002, "MEAT CUT", true, new AlphaNumericFormatter(30)),
	EXPIRY_TIME(7003, "EXPIRY TIME", true, new DateTimeFormatter(false)),
	ACTIVE_POTENCY(7004, "ACTIVE POTENCY", true, new NumericFormatter(4, false)),
	PROCESSOR(703, "PROCESSOR #s", true, new ProcessorFormatter()),
	NHRN_PZN(710, "NHRN PZN", true, new AlphaNumericFormatter(20)),
	NHRN_CIP(711, "NHRN CIP", true, new AlphaNumericFormatter(20)),
	NHRN_CN(712, "NHRN CN", true, new AlphaNumericFormatter(20)),
	DIMENSIONS(8001, "DIMENSIONS", true, new RollProductFormatter()),
	GMT_NO(8002, "CMT No", true, new AlphaNumericFormatter(20)),
	GRAI(8003, "GRAI", true, new NumericWithCheckDigitPlusSerialFormatter(14,
			16)),
	GIAI(8004, "GIAI", true, new AlphaNumericFormatter(30)),
	PRICE_PER_UNIT(8005, "PRICE PER UNIT", true, new NumericFormatter(6)),
	GCTIN(8006, "GCTIN", true, new TradeItemComponentFormatter()),
	IBAN(8007, "IBAN", true, new IBANFormatter()),
	PRODUCTION_TIME(8008, "PROD TIME", true, new DateTimeFormatter(true)),
	CPID(8010, "CPID", true, new AlphaNumericFormatter(30)),
	CPID_SERIAL(8011, "CPID SERIAL", true, new NumericFormatter(12, false)),
	GSRN_PROVIDER(
			8017,
			"GSRN - PROVIDER",
			true,
			new NumericWithCheckDigitFormatter(18)),
	GSRN_RECIPIENT(
			8018,
			"GSRN - RECIPIENT",
			true,
			new NumericWithCheckDigitFormatter(18)),
	SRIN(8019, "SRIN", true, new NumericFormatter(10, false)),
	REF_NO(8020, "REF No", true, new AlphaNumericFormatter(25)),
	GS1_128_EXT_CODE_1(8101, "GS1-128 EXT CODE 1", true, new NumericFormatter(
			10)),
	GS1_128_EXT_CODE_2(
			8100,
			"GS1-128 EXT CODE 2",
			true,
			new NumericFormatter(6)),
	GS1_128_EXT_CODE_3(
			8102,
			"GS1-128 EXT CODE 3",
			true,
			new NumericFormatter(2)),
	COUPON_CODE_NORTH_AMERICA(
			8110,
			"COUPON CODE NORTH AMERICA",
			true,
			new AlphaNumericFormatter(70)),
	PRODUCT_URL(8200, "PRODUCT URL", true, new AlphaNumericFormatter(70)),
	INTERNAL(90, "INTERNAL", true, new AlphaNumericFormatter(30)),
	INTERNAL_91(91, "INTERNAL 91", true, new AlphaNumericFormatter(30)),
	INTERNAL_92(92, "INTERNAL 92", true, new AlphaNumericFormatter(30)),
	INTERNAL_93(93, "INTERNAL 93", true, new AlphaNumericFormatter(30)),
	INTERNAL_94(94, "INTERNAL 94", true, new AlphaNumericFormatter(30)),
	INTERNAL_95(95, "INTERNAL 95", true, new AlphaNumericFormatter(30)),
	INTERNAL_96(96, "INTERNAL 96", true, new AlphaNumericFormatter(30)),
	INTERNAL_97(97, "INTERNAL 97", true, new AlphaNumericFormatter(30)),
	INTERNAL_98(98, "INTERNAL 98", true, new AlphaNumericFormatter(30)),
	INTERNAL_99(99, "INTERNAL 99", true, new AlphaNumericFormatter(30)),

	;

	private final int id;
	private final boolean fnc1;
	private final GS1Formatter formatter;
	private final String dataTitle;

	private static final Map<String, GS1ApplicationIdentifier> ID_MAP = new HashMap<String, GS1ApplicationIdentifier>();
	private static int MAX_LENGTH = 0;
	static {
		for (GS1ApplicationIdentifier id : GS1ApplicationIdentifier.values()) {
			ID_MAP.put(id.getIdentifier(), id);
			if (id.getIdentifier().length() > MAX_LENGTH) {
				MAX_LENGTH = id.getIdentifier().length();
			}
		}
	}

	private GS1ApplicationIdentifier(int id, String dataTitle, boolean fnc1,
			GS1Formatter formatter) {
		this.id = id;
		this.dataTitle = dataTitle;
		this.fnc1 = fnc1;
		this.formatter = formatter;
	}

	/**
	 * Returns the numeric value of the identifier. In case of an indexed id,
	 * only the fix part is returned. E.g. 310n.
	 * 
	 * @return The id.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the id formatted as string
	 * 
	 * @return The id
	 */
	public String getIdentifier() {
		if (id < 10) {
			return "0" + id;
		} else {
			return Integer.toString(id);
		}
	}

	/**
	 * Differentiates between ais that have an index and those that don't.
	 * 
	 * @return true if the ai is indexed
	 */
	public boolean isIndexedAI() {
		return (formatter instanceof IndexedAI);
	}

	/**
	 * Returns whether the value is succeeded by the FNC1 group separator or
	 * not.
	 * 
	 * @return true if FNC1 is needed, false otherwise
	 */
	public boolean needsFnc1() {
		return fnc1;
	}

	/**
	 * Returns the GS1 data title, a string to identify the ai in the human
	 * readable part.
	 * 
	 * @return The data title
	 */
	public String getDataTitle() {
		return dataTitle;
	}

	/**
	 * Returns the maximum length of the value.
	 * 
	 * @return the length
	 */
	public int getMaxLength() {
		return formatter.getMaxLength();
	}

	/**
	 * Returns the index of the value if the ai is indexed. An empty string is
	 * rerturned for non-indexed ais.
	 * 
	 * @param value
	 *            The value that contains the index.
	 * @return The index.
	 */
	public String getIndex(String value) {
		if (formatter instanceof IndexedAI) {
			return ((IndexedAI) formatter).getIndex(value);
		}
		return "";
	}

	/**
	 * Returns the ai including the index for the given value.
	 * 
	 * @param value
	 *            The value containing the index.
	 * @return The ai with the index appended.
	 */
	public String getIndexedIdentifier(String value) {
		return isIndexedAI() ? getIdentifier() + " " + getIndex(value)
				: getIdentifier();
	}

	/**
	 * Formats the value according to the formatting rules of the ai.
	 * 
	 * @param value
	 *            The value
	 * @return The formatted value.
	 */
	public String formatValue(String value) {
		return formatter.format(value);
	}

	/**
	 * Returns a Set of strings containing all ais.
	 * 
	 * @return The set
	 */
	public static Set<String> getIdentifiers() {
		return ID_MAP.keySet();
	}

	/**
	 * Returns the maximum number of digits an ai can have. Can be used for
	 * parsers.
	 * 
	 * @return the maximum length
	 */
	public static int getMaxIdLength() {
		return MAX_LENGTH;
	}

	/**
	 * Returns the ai for a given string or null if the string does not
	 * represent an ai-
	 * 
	 * @param identifier
	 *            The string
	 * @return the enum
	 */
	public static GS1ApplicationIdentifier getIdentifier(String identifier) {
		return ID_MAP.get(identifier);
	}
}