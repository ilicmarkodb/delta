package io.delta.kernel.defaults.internal.expressions;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;
import io.delta.kernel.expressions.CollationIdentifier;
import io.delta.kernel.internal.util.Tuple2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.delta.kernel.defaults.internal.expressions.CollationFactory.Collation.DEFAULT_COLLATION;
import static io.delta.kernel.defaults.internal.expressions.DefaultExpressionUtils.STRING_COMPARATOR;
import static io.delta.kernel.expressions.CollationIdentifier.ICU_COLLATOR_VERSION;
import static io.delta.kernel.expressions.CollationIdentifier.PROVIDER_ICU;

public class CollationFactory {
  private static final Map<CollationIdentifier, Collation> collationMap = new ConcurrentHashMap<>();

  public static Collation fetchCollation(CollationIdentifier collationIdentifier) {

    if (collationIdentifier.equals(CollationIdentifier.DEFAULT_COLLATION_IDENTIFIER)) {
      return DEFAULT_COLLATION;
    } else if (collationMap.containsKey(collationIdentifier)) {
      return collationMap.get(collationIdentifier);
    } else {
      Collation collation;
      if (collationIdentifier.getProvider().equals(CollationIdentifier.PROVIDER_SPARK)) {
        collation = UTF8CollationFactory.fetchCollation(collationIdentifier);
      } else if (collationIdentifier.getProvider().equals(PROVIDER_ICU)) {
        collation = ICUCollationFactory.fetchCollation(collationIdentifier);
      } else {
        throw new IllegalArgumentException(String.format("Invalid collation provider: %s.", collationIdentifier.getProvider()));
      }
      collationMap.put(collationIdentifier, collation);
      return collation;
    }
  }

  private static class UTF8CollationFactory {
    private static Collation fetchCollation(CollationIdentifier collationIdentifier) {
      if (collationIdentifier.equals(CollationIdentifier.DEFAULT_COLLATION_IDENTIFIER)) {
        return new Collation(CollationIdentifier.DEFAULT_COLLATION_IDENTIFIER, STRING_COMPARATOR);
      } else {
        // TODO
        throw new IllegalArgumentException(String.format("Invalid collation identifier: %s.", collationIdentifier.getProvider()));
      }
    }
  }

  private static class ICUCollationFactory {
    /**
     * Bit 17 in collation ID having value 0 for case-sensitive and 1 for case-insensitive
     * collation.
     */
    private enum CaseSensitivity {
      CS, CI
    }

    /**
     * Bit 16 in collation ID having value 0 for accent-sensitive and 1 for accent-insensitive
     * collation.
     */
    private enum AccentSensitivity {
      AS, AI
    }

    /**
     * Array of locale names, each locale ID corresponds to the index in this array.
     */
    private static final String[] ICULocaleNames;

    /**
     * Mapping of locale names to corresponding `ULocale` instance.
     */
    private static final Map<String, ULocale> ICULocaleMap = new HashMap<>();

    /**
     * Used to parse user input collation names which are converted to uppercase.
     */
    private static final Map<String, String> ICULocaleMapUppercase = new HashMap<>();

    private static Collation fetchCollation(CollationIdentifier collationIdentifier) {
      if (collationIdentifier.getVersion().isPresent() &&
              !collationIdentifier.getVersion().get().equals(ICU_COLLATOR_VERSION)) {
        throw new IllegalArgumentException(String.format("Invalid collation version: %s.", collationIdentifier.getVersion().get()));
      }

      String locale = getICULocale(collationIdentifier);

      Tuple2<CaseSensitivity, AccentSensitivity> caseAndAccentSensitivity = getICUCaseAndAccentSensitivity(collationIdentifier, locale);
      CaseSensitivity caseSensitivity = caseAndAccentSensitivity._1;
      AccentSensitivity accentSensitivity = caseAndAccentSensitivity._2;

      Collator collator = getICUCollator(locale, caseSensitivity, accentSensitivity);

      return new Collation(
              collationIdentifier,
              collator::compare);
    }

    private static String getICULocale(CollationIdentifier collationIdentifier) {
      String collationName = collationIdentifier.getName();

      // Search for the longest locale match because specifiers are designed to be different from
      // script tag and country code, meaning the only valid locale name match can be the longest
      // one.
      int lastPos = -1;
      for (int i = 1; i <= collationName.length(); i++) {
        String localeName = collationName.substring(0, i);
        if (ICULocaleMapUppercase.containsKey(localeName)) {
          lastPos = i;
        }
      }
      if (lastPos == -1) {
        throw new IllegalArgumentException(String.format("Invalid collation name: %s.", collationIdentifier.toStringWithoutVersion()));
      } else {
        return collationName.substring(0, lastPos);
      }
    }

    private static Tuple2<CaseSensitivity, AccentSensitivity> getICUCaseAndAccentSensitivity(CollationIdentifier collationIdentifier, String locale) {
      String collationName = collationIdentifier.getName();

      // Try all combinations of AS/AI and CS/CI.
      CaseSensitivity caseSensitivity;
      AccentSensitivity accentSensitivity;
      if (collationName.equals(locale) ||
              collationName.equals(locale + "_AS") ||
              collationName.equals(locale + "_CS") ||
              collationName.equals(locale + "_AS_CS") ||
              collationName.equals(locale + "_CS_AS")
      ) {
        caseSensitivity = CaseSensitivity.CS;
        accentSensitivity = AccentSensitivity.AS;
      } else if (collationName.equals(locale + "_CI") ||
              collationName.equals(locale + "_AS_CI") ||
              collationName.equals(locale + "_CI_AS")) {
        caseSensitivity = CaseSensitivity.CI;
        accentSensitivity = AccentSensitivity.AS;
      } else if (collationName.equals(locale + "_AI") ||
              collationName.equals(locale + "_CS_AI") ||
              collationName.equals(locale + "_AI_CS")) {
        caseSensitivity = CaseSensitivity.CS;
        accentSensitivity = AccentSensitivity.AI;
      } else if (collationName.equals(locale + "_AI_CI") ||
              collationName.equals(locale + "_CI_AI")) {
        caseSensitivity = CaseSensitivity.CI;
        accentSensitivity = AccentSensitivity.AI;
      } else {
        throw new IllegalArgumentException(String.format("Invalid collation name: %s.", collationIdentifier.toStringWithoutVersion()));
      }

      return new Tuple2<>(caseSensitivity, accentSensitivity);
    }

    private static Collator getICUCollator(String locale, CaseSensitivity caseSensitivity, AccentSensitivity accentSensitivity) {
      ULocale.Builder builder = new ULocale.Builder();
      builder.setLocale(ICULocaleMap.get(locale));
      // Compute unicode locale keyword for all combinations of case/accent sensitivity.
      if (caseSensitivity == CaseSensitivity.CS &&
              accentSensitivity == AccentSensitivity.AS) {
        builder.setUnicodeLocaleKeyword("ks", "level3");
      } else if (caseSensitivity == CaseSensitivity.CS &&
              accentSensitivity == AccentSensitivity.AI) {
        builder
                .setUnicodeLocaleKeyword("ks", "level1")
                .setUnicodeLocaleKeyword("kc", "true");
      } else if (caseSensitivity == CaseSensitivity.CI &&
              accentSensitivity == AccentSensitivity.AS) {
        builder.setUnicodeLocaleKeyword("ks", "level2");
      } else if (caseSensitivity == CaseSensitivity.CI &&
              accentSensitivity == AccentSensitivity.AI) {
        builder.setUnicodeLocaleKeyword("ks", "level1");
      }
      ULocale resultLocale = builder.build();
      Collator collator = Collator.getInstance(resultLocale);
      // Freeze ICU collator to ensure thread safety.
      collator.freeze();
      return collator;
    }

    static {
      ICULocaleMap.put("UNICODE", ULocale.ROOT);
      // ICU-implemented `ULocale`s which have corresponding `Collator` installed.
      ULocale[] locales = Collator.getAvailableULocales();
      // Build locale names in format: language["_" optional script]["_" optional country code].
      // Examples: en, en_USA, sr_Cyrl_SRB
      for (ULocale locale : locales) {
        // Skip variants.
        if (locale.getVariant().isEmpty()) {
          String language = locale.getLanguage();
          // Require non-empty language as first component of locale name.
          assert (!language.isEmpty());
          StringBuilder builder = new StringBuilder(language);
          // Script tag.
          String script = locale.getScript();
          if (!script.isEmpty()) {
            builder.append('_');
            builder.append(script);
          }
          // 3-letter country code.
          String country = locale.getISO3Country();
          if (!country.isEmpty()) {
            builder.append('_');
            builder.append(country);
          }
          String localeName = builder.toString();
          // Verify locale names are unique.
          assert (!ICULocaleMap.containsKey(localeName));
          ICULocaleMap.put(localeName, locale);
        }
      }
      // Construct uppercase-normalized locale name mapping.
      for (String localeName : ICULocaleMap.keySet()) {
        String localeUppercase = localeName.toUpperCase();
        // Locale names are unique case-insensitively.
        assert (!ICULocaleMapUppercase.containsKey(localeUppercase));
        ICULocaleMapUppercase.put(localeUppercase, localeName);
      }
      // Construct locale name to ID mapping. Locale ID is defined as index in `ICULocaleNames`.
      ICULocaleNames = ICULocaleMap.keySet().toArray(new String[0]);
      Arrays.sort(ICULocaleNames);
      // Maximum number of locale IDs as defined by binary layout.
      assert (ICULocaleNames.length <= (1 << 12));
    }
  }

  public static class Collation {

    public static Collation DEFAULT_COLLATION = UTF8CollationFactory.fetchCollation(CollationIdentifier.DEFAULT_COLLATION_IDENTIFIER);

    public Collation(CollationIdentifier collationIdentifier, Comparator<String> collationComparator) {
      this.collationIdentifier = collationIdentifier;
      this.collationComparator = collationComparator;
    }

    public Comparator<String> getCollationComparator() {
      return collationComparator;
    }

    private final CollationIdentifier collationIdentifier;
    private final Comparator<String> collationComparator;
  }
}
