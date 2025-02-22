/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024-2025 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xpdustry.avs.util.bundle;

import java.text.MessageFormat;
import java.util.Locale;

import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.logging.Logger;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;


public class L10NBundle {
  private static boolean loaded = false;
  /** Store a cache to make the next requests, with the same locale, faster */
  protected static final ObjectMap<Locale, Bundle> cache = new ObjectMap<>();
  protected static final Logger logger = new Logger("Bundle");
  protected static Locale defaultLocale;
  protected static Bundle defaultBundle;
  protected static MessageFormat defaultFormatter;
  
  public static final Seq<Bundle> bundles = new Seq<>();
  
  /** Will use the default bundle to get the key, when not found in other bundles. */
  public static boolean useDefaultWhenKeyNotFound = true;
  /** If enabled, will use a cache to avoid searching again the right bundle for the locale. */
  public static boolean useCache = true;
  
  public static boolean isLoaded() {
    return loaded;
  }
  
  public static void clearCache() {
    if (useCache) cache.clear();
  }
  
  public static void clearBundles() {
    clearCache();
    bundles.clear();
  }
  
  public static void load(Class<? extends mindustry.mod.Mod> main, String defaultLocale) {
    load(mindustry.Vars.mods.getMod(main).root.child("bundles"), defaultLocale);
  }

  /** {@link #applyBundles()} must be called after, to finalize bundle loading. */
  public static void load(Fi directory, String defaultLocale) {
    load(directory, Strings.string2Locale(defaultLocale));
  }
  
  /**
   * Load the bundles in the specified directory. <br>
   * See {@link #loadBundles(Fi)} for file naming. <br>
   * {@link #applyBundles()} must be called after, to finalize bundle loading.
   */
  public static void load(Fi directory, Locale defaultLocale_) {
    loaded = false;

    clearBundles();
    bundles.add(new RouterBundle()); // router router
    defaultLocale = defaultLocale_ == Locale.ROOT ? Locale.getDefault() : defaultLocale_;
    defaultBundle = null;
    appendBundles(directory);

    loaded = true;
  }
  
  /** See {@link #loadBundles(Fi)} for file naming */
  public static void appendBundles(Fi directory) {
    logger.debug("avs.bundle.loading.folder", directory.toString());
    appendBundles(loadBundles(directory));
  }
  
  /** 
   * Add custom bundles to the list. <br>
   * If a locale is the same as an existing one, they will be merged. <br>
   * {@link #applyBundles()} must be called after, to finalize bundle loading.
   * 
   * @param list the bundle list
   * @param applyBundles set default bundle and apply hierarchy, after
   * 
   * @apiNote For bundles loading, normal logging messages can be used, 
   *          in case of default bundle isn't loaded yet.
   */
  public static void appendBundles(Seq<Bundle> list) {
    if (list.isEmpty()) return;
    logger.debug("avs.bundle.loading.list", list.size);
    
    // Clear the cache before, to avoid bundle confusions
    clearCache();

    // Merge existing bundles
    list.each(b -> {
      Bundle tmp = bundles.find(bb -> b.equals(bb));
      if (tmp != null) tmp.merge(b);
      else bundles.add(b);
    });
    
    // Sort bundles
    bundles.sort(b -> b.locale.hashCode());
    
    logger.debug("avs.bundle.loading.done");
  }
  
  /** {@link #applyBundles()} must be called after, to finalize bundle loading. */
  public static void appendBundle(Bundle bundle) {
    if (bundle == null) return;
    logger.debug("avs.bundle.loading.one", bundle.locale);
    
    // Clear the cache before, to avoid bundle confusions
    clearCache();

    // Merge if existing
    Bundle tmp = bundles.find(b -> bundle.equals(b));
    if (tmp != null) tmp.merge(bundle);
    else bundles.add(bundle);
    
    // Sort bundles
    bundles.sort(b -> b.locale.hashCode());

    logger.debug("avs.bundle.loading.done");
  }
  
  /** Search and set the default bundle, and apply the bundle hierarchy. */
  public static void applyBundles() {
    if (defaultBundle == null || !defaultBundle.locale.equals(defaultLocale)) 
      defaultBundle = findBundle(bundles, defaultLocale);
    if (defaultBundle == null) {
      logger.warn("avs.bundle.default-not-found", defaultLocale);
      defaultLocale = Locale.getDefault();
      defaultBundle = findBundle(bundles, defaultLocale);
      if (defaultBundle == null) defaultBundle = new Bundle(defaultLocale);
    }
    
    // Set hierarchy
    logger.debug("avs.bundle.loading.hierarchy.aplying");
    setBundleHierarchy(bundles, defaultBundle);
  }
  
  /**
   * Walk into the directory, load the bundle files and associate their with the locale code in the file name. <br>
   * Files format must start with a file prefix (or nothing), followed by '_' or '-', 
   * the locale code in format 'xx', 'xx_XX' or 'xx_XX_XX' and must have the '.properties' file extension. <br><br>
   * 
   * @apiNote Bundle files with no locale code, or with no language in the code, in the file name, will be ignored. <br>
   *          If you want to have a default bundle for the entire plugin (and as default for all client),
   *          <strong>please change the config variable</strong> to the locale code you want. <br>
   *          This method doens't care about the default locale and bundle, it just loading them.
   */
  protected static Seq<Bundle> loadBundles(Fi directory) {
    Seq<Bundle> loaded = new Seq<>();
    if (!directory.exists()) return loaded;

    // Load bundle files
    directory.walk(fi -> {
      if (!fi.extEquals("properties")) return;
      
      try {
        String name = fi.nameWithoutExtension().replace("-", "_");
        int cindex = name.indexOf('_');
        String code = cindex == -1 ? "" : name.substring(cindex+1);
        Locale locale = Strings.string2Locale(code);
        
        // Check if language is empty, to ignore and warn
        if (locale.getLanguage().isEmpty()) {
          logger.warn("avs.bundle.warn.bundle-locale.msg1");
          logger.warn("avs.bundle.warn.bundle-locale.msg2");
          return;
        }
        
        Bundle bundle = new Bundle(locale);
        bundle.load(fi);
        loaded.add(bundle);
        
        if (bundle.isEmpty()) logger.warn("avs.bundle.warn.empty", Strings.locale2String(locale));
        logger.debug("avs.bundle.loading.file.loaded", Strings.locale2String(locale));
      
      } catch (arc.util.ArcRuntimeException e) {
        logger.err("avs.bundle.loading.file.error", fi.toString());
        logger.err("avs.general-error", e.toString());
      }
    });

    return loaded;
  }
  
  protected static void setBundleHierarchy(Seq<Bundle> bundles, Bundle defaultBundle) {
    Seq<Bundle> visited = new Seq<>(bundles.size);

    try {
      // First unset parents
      bundles.each(b -> b.parent = null);
      
      bundles.each(b -> {
        if (visited.contains(b)) return;

        Bundle[] founds = findCandidateBundles(bundles, b.locale);
        int i = 0;
        logger.debugNormal("Founds: @", Strings.listToSentence(logger, Seq.with(founds), 
                           bb -> bb == null ? "null" : bb.locale.toString()));

        for (; i<founds.length; i++) {
          if (founds[i] == null) break;
          if (i > 0) founds[i-1].setParent(founds[i]);
          visited.add(founds[i]);
        }
  
        // Set the default to the last parent. Will be used in last resort to get the key.
        if (useDefaultWhenKeyNotFound && i > 0 && 
            !b.locale.getLanguage().equals(defaultBundle.locale.getLanguage()) && founds[i-1].parent == null)
          founds[i-1].setParent(defaultBundle);
      });
      
    } catch (IllegalStateException e) {
      logger.err("avs.bundle.loading.hierarchy.error");
      logger.err("avs.general-error", e.toString());
    }
  }
  
  public static Bundle getBundle(String locale) {
    return getBundle(Strings.string2Locale(locale));
  }
  
  public static Bundle getBundle(Locale locale) { return getBundle(locale, L10NBundle::getDefaultBundle); }  
  /**
   * Try to get the bundle. <br> 
   * First, search the variant specific, after the country specific, 
   * after the language specific, else the {@code def}.
   *
   * @apiNote The first call, with a specific locale, will be slow. <br>
   *          But the result will be cached to make further calls, with the same locale, faster.
   *
   * @return a found bundle, the default one if not found, or a dummy one if bundles are not loaded.
   */
  public static Bundle getBundle(Locale locale, arc.func.Prov<Bundle> def) {
    if (!isLoaded()) return new Bundle(locale);
    if (locale == null || locale == Locale.ROOT) return def.get();
    
    Bundle bundle = useCache ? cache.get(locale) : null;
    if (bundle != null) return bundle; 
      
    bundle = findBundle(bundles, locale);
    if (bundle == null) {
      logger.debug("avs.bundle.not-found", Strings.locale2String(locale));
      bundle = def.get();
    }
    
    if (useCache) cache.put(locale, bundle);
    return bundle;
  }
  
  /**
   * 1- Search by language, country and variant. <br>
   * 2- If not found, search just with language and country. <br>
   * 3- If still not found, search only with language. <br>
   * 4- Else, return {@code null}.
   */
  protected static Bundle findBundle(Seq<Bundle> bundles, Locale locale) {
    String l = locale.getLanguage();
    String c = locale.getCountry();
    String v = locale.getVariant();
    Bundle found = null;
    
    if (found == null && v.length() > 0) 
      found = bundles.find(b -> b.locale.getLanguage().equals(l) &&
                                b.locale.getCountry().equals(c) &&
                                b.locale.getVariant().equals(v));

    if (found == null && c.length() > 0)
      found = bundles.find(b -> b.locale.getLanguage().equals(l) &&
                                b.locale.getCountry().equals(c));

    if (found == null && l.length() > 0)
      found = bundles.find(b -> b.locale.getLanguage().equals(l));

    return found;
  }
  
  /** 
   * Pretty same as {@link #findBundle(Seq, Locale)}, but do all steps.
   * 
   * @return a table of 3 indexes
   */
  protected static Bundle[] findCandidateBundles(Seq<Bundle> bundles, Locale locale) {
    String l = locale.getLanguage();
    String c = locale.getCountry();
    String v = locale.getVariant();
    Bundle found = null;
    Bundle[] candidates = new Bundle[3];
    int i = 0;
        
    if (v.length() > 0) {
      found = bundles.find(b -> b.locale.getLanguage().equals(l) &&
                                b.locale.getCountry().equals(c) &&
                                b.locale.getVariant().equals(v));
      if (found != null) candidates[i++] = found;
    }
    
    if (c.length() > 0) {
      found = bundles.find(b -> b.locale.getLanguage().equals(l) &&
                                b.locale.getCountry().equals(c));
      if (found != null) candidates[i++] = found;
    }

    if (l.length() > 0) {
      found = bundles.find(b -> b.locale.getLanguage().equals(l));
      if (found != null) candidates[i++] = found;
    }
    
    return candidates;
  }

  /**
   * @return The default bundle. <br>
   *         If it's null, this will search a bundle with {@link #defaultLocale}.
   *         And if still null, this will return a dummy one with the {@link #defaultLocale}. <br>
   *         In short, this method ensure that the default bundle and locale are never null.
   * 
   * @apiNote if bundles are not loaded, this return a dummy bundle
   */
  public static Bundle getDefaultBundle() {
    Locale locale = getDefaultLocale();
    
    if (defaultBundle == null) {
      if (!isLoaded()) return new Bundle(locale);
      else defaultBundle = getBundle(locale, () -> new Bundle(locale));
    }
    return defaultBundle;
  }
  
  public static boolean isDefaultBundleSet() {
    return defaultBundle != null;
  }
  
  /** Set the new default locale. */
  public static void setDefaultLocale(Locale locale) {
    defaultLocale = locale == Locale.ROOT ? Locale.getDefault() : locale;
    applyBundles();
  }
  
  /** 
   * @return default locale and ensure that {@link #defaultLocale} is never null 
   * by using {@link Locale#getDefault()}. 
   */
  public static Locale getDefaultLocale() {
    if (defaultLocale == null) defaultLocale = Locale.getDefault();
    return defaultLocale;
  }
  
  public static MessageFormat getDefaultFormatter() {
    if (defaultFormatter == null) defaultFormatter = new MessageFormat("", getDefaultLocale());
    return defaultFormatter;
  }

  // region has

  public static boolean has(String key) {
    return getDefaultBundle().has(key);
  }

  public static boolean has(String key, String locale) {
    return getBundle(locale).has(key);
  }

  public static boolean has(String key, Locale locale) {
    return getBundle(locale).has(key);
  }

  // endregion
  // region get

  public static String get(String key) {
    return getDefaultBundle().get(key);
  }
  
  public static String getOrNull(String key) {
    return getDefaultBundle().getOrNull(key);
  }

  public static String get(String key, String locale) {
    return getBundle(locale).get(key);
  }

  public static String get(String key, Locale locale) {
    return getBundle(locale).get(key);
  }
  
  public static String getOrNull(String key, Locale locale) {
    return getBundle(locale).getOrNull(key);
  }
  

  // endregion
  // region format

  public static String format(String key, Object... values) {
    return getDefaultBundle().format(key, values);
  }
  
  public static String format(String key, MessageFormat formatter, Object... values) {
    return getDefaultBundle().format(formatter, key, values);
  }
  
  public static String formatColor(String key, Object... values) {
    return getDefaultBundle().formatColor(getDefaultFormatter(), key, "&fb&lb", "&fr", values);
  }
  
  public static String formatColor(String key, String leftColor, String rightColor, Object... values) {
    return getDefaultBundle().formatColor(getDefaultFormatter(), key, leftColor, rightColor, values);
  }

  public static String format(String key, String locale, Object... values) {
    return getBundle(locale).format(key, values);
  }

  public static String format(String key, Locale locale, Object... values) {
    return getBundle(locale).format(key, values);
  }
  
  public static String format(String key, MessageFormat formatter, Locale locale, Object... values) {
    return getBundle(locale).format(formatter, key, values);
  }
  
  public static String formatColor(String key, Locale locale, Object... values) {
    return getBundle(locale).formatColor(getDefaultFormatter(), key, "&fb&lb", "&fr", values);
  }
  
  public static String formatColor(String key, Locale locale, String leftColor, String rightColor, Object... values) {
    return getBundle(locale).formatColor(getDefaultFormatter(), key, leftColor, rightColor, values);
  }
  
  // endregion
}