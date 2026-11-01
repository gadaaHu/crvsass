package com.qcafr.gatherapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Holds every piece of text that is region-specific and jurisdiction-specific
 * so the app can be deployed to any Ethiopian civil-registration zone without code changes.
 *
 * Configures:
 * - Country (e.g. "Ethiopia")
 * - Region or City (e.g. "Dire Dawa", "Addis Ababa", "Oromia", etc. - optional in place of registration)
 * - Agency Name (e.g. "Dire Dawa CRVS Agency")
 * - Legal Proclamation
 * - Training Title
 * - Place of Registration defaults: Wereda, Kebele, and quick suggestion chips.
 */
data class RegionConfig(
    /** Short region/city name shown on pills, headers, etc.  e.g. "Dire Dawa" */
    val regionName: String = "Dire Dawa",
    /** Country label, used in certificate footer and place of registration. e.g. "Ethiopia" */
    val country: String = "Ethiopia",
    /** Full agency name shown on certificate seal/header.    e.g. "Dire Dawa CRVS Agency" */
    val agencyName: String = "Dire Dawa CRVS Agency",
    /** Proclamation reference shown on legal rows.           e.g. "Proclamation No. 1370/2024" */
    val proclamation: String = "Proclamation No. 1370/2024",
    /** Short label for the assessment suite training title.  e.g. "CRVS Registrar Training" */
    val trainingLabel: String = "CRVS Registrar Training",
    /** Default Wereda / Subcity / Zone for Place of Registration. e.g. "Sabian Wereda" or "Wereda 01" */
    val defaultWereda: String = "Wereda 01",
    /** Default Kebele for Place of Registration.             e.g. "Kebele 01" */
    val defaultKebele: String = "Kebele 01",
    /** Comma-separated list of Kebele / Wereda chips for quick suggestions. */
    val kebeleSuggestions: String = "Kebele 01, Kebele 02, Kebele 03, Kebele 04, Sabian, Genda Kore, Melka"
) {
    /** Upper-case agency name for use in PDF seals / certificate headings. */
    val agencyNameUpper: String get() = agencyName.uppercase()

    /** "City, Country" formatted string for use in footers / signature blocks. */
    val place: String get() = if (regionName.isNotBlank()) "$regionName, $country" else country

    /** "City CRVS" short label used in hero-card pills on the Home screen. */
    val regionCrvs: String get() = if (regionName.isNotBlank()) "$regionName CRVS" else "CRVS Agency"

    /**
     * Formats full Place of Registration:
     * "[Kebele], [Wereda], [Region/City], [Country]"
     * Region/City is optional if omitted or empty.
     */
    fun formatPlaceOfRegistration(
        kebele: String? = null,
        wereda: String? = null,
        regOrCity: String? = null,
        ctry: String? = null
    ): String {
        val parts = mutableListOf<String>()
        val k = (kebele ?: defaultKebele).trim()
        val w = (wereda ?: defaultWereda).trim()
        val r = (regOrCity ?: regionName).trim()
        val c = (ctry ?: country).trim()
        if (k.isNotBlank()) parts.add(k)
        if (w.isNotBlank()) parts.add(w)
        if (r.isNotBlank()) parts.add(r)
        if (c.isNotBlank()) parts.add(c)
        return if (parts.isNotEmpty()) parts.joinToString(", ") else place
    }

    /** List of kebele/wereda suggestion chips parsed from [kebeleSuggestions] */
    fun getKebeleList(): List<String> = kebeleSuggestions
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

class RegionPreferenceManager(private val context: Context) {

    companion object {
        private val KEY_REGION    = stringPreferencesKey("region_name")
        private val KEY_COUNTRY   = stringPreferencesKey("region_country")
        private val KEY_AGENCY    = stringPreferencesKey("region_agency")
        private val KEY_PROC      = stringPreferencesKey("region_proclamation")
        private val KEY_TRAINING  = stringPreferencesKey("region_training_label")
        private val KEY_WEREDA    = stringPreferencesKey("region_default_wereda")
        private val KEY_KEBELE    = stringPreferencesKey("region_default_kebele")
        private val KEY_SUGGEST   = stringPreferencesKey("region_kebele_suggestions")
        private val KEY_POST_TEST_UNLOCKED = booleanPreferencesKey("is_post_test_unlocked")

        @Volatile private var INSTANCE: RegionPreferenceManager? = null

        fun getInstance(context: Context): RegionPreferenceManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RegionPreferenceManager(context.applicationContext).also { INSTANCE = it }
            }

        /** Built-in presets that admins can select from with realistic Ethiopian jurisdiction defaults. */
        val PRESETS: List<RegionConfig> = listOf(
            RegionConfig(
                regionName        = "Afar",
                country           = "Ethiopia",
                agencyName        = "Afar CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Semera Wereda",
                defaultKebele     = "Kebele 01",
                kebeleSuggestions = "Semera, Asaita, Dubti, Awash 7 Kilo, Logiya, Mille"
            ),
            RegionConfig(
                regionName        = "Amhara",
                country           = "Ethiopia",
                agencyName        = "Amhara CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Bahir Dar Wereda",
                defaultKebele     = "Kebele 01",
                kebeleSuggestions = "Kebele 01, Kebele 02, Bahir Dar, Gondar, Dessie, Debre Birhan, Woldiya"
            ),
            RegionConfig(
                regionName        = "Benishangul-Gumuz",
                country           = "Ethiopia",
                agencyName        = "Benishangul-Gumuz CRVS Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Assosa Wereda",
                defaultKebele     = "Assosa 01",
                kebeleSuggestions = "Assosa 01, Assosa 02, Assosa 03, Bambasi, Kurmuk, Menge"
            ),
            RegionConfig(
                regionName        = "Central Ethiopia",
                country           = "Ethiopia",
                agencyName        = "Central Ethiopia CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Hosanna Wereda",
                defaultKebele     = "Kebele 01",
                kebeleSuggestions = "Hosanna, Butajira, Worabe, Hadiya, Gurage, Silte, Halaba"
            ),
            RegionConfig(
                regionName        = "Gambela",
                country           = "Ethiopia",
                agencyName        = "Gambela CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Gambela Wereda",
                defaultKebele     = "Kebele 01",
                kebeleSuggestions = "Kebele 01, Kebele 02, Kebele 03, Kebele 04, Kebele 05, Itang"
            ),
            RegionConfig(
                regionName        = "Harari",
                country           = "Ethiopia",
                agencyName        = "Harari CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Harar Wereda",
                defaultKebele     = "Abadir",
                kebeleSuggestions = "Abadir, Amir Nur, Shenkor, Jin'Eala, Sofi, Erer, Dire Teyara"
            ),
            RegionConfig(
                regionName        = "Oromia",
                country           = "Ethiopia",
                agencyName        = "Oromia CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Adama Wereda",
                defaultKebele     = "Ganda 01",
                kebeleSuggestions = "Ganda 01, Ganda 02, Adama, Bishoftu, Jimma, Shashemene, Ambo, Nekemte"
            ),
            RegionConfig(
                regionName        = "Sidama",
                country           = "Ethiopia",
                agencyName        = "Sidama CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Hawassa Wereda",
                defaultKebele     = "Tabor",
                kebeleSuggestions = "Tabor, Menahariya, Misrak, Hayk Dar, Bahil Adarash, Mehal Ketema"
            ),
            RegionConfig(
                regionName        = "Somali",
                country           = "Ethiopia",
                agencyName        = "Somali CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Jigjiga Wereda",
                defaultKebele     = "Kebele 01",
                kebeleSuggestions = "Kebele 01, Kebele 02, Kebele 03, Kebele 04, Kebele 05, Kebele 06"
            ),
            RegionConfig(
                regionName        = "South Ethiopia",
                country           = "Ethiopia",
                agencyName        = "South Ethiopia CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Wolaita Sodo Wereda",
                defaultKebele     = "Kebele 01",
                kebeleSuggestions = "Wolaita Sodo, Arba Minch, Dilla, Jinka, Sawla, Karat, Gidole"
            ),
            RegionConfig(
                regionName        = "South West Ethiopia Peoples' Region",
                country           = "Ethiopia",
                agencyName        = "South West Ethiopia Peoples' CRVS Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Bonga Wereda",
                defaultKebele     = "Kebele 01",
                kebeleSuggestions = "Bonga, Mizan Aman, Tepi, Tercha, Keffa, Bench Sheko, Sheka, Dawro"
            ),
            RegionConfig(
                regionName        = "Tigray",
                country           = "Ethiopia",
                agencyName        = "Tigray CRVS Regional Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Mekelle Wereda",
                defaultKebele     = "Hawelti",
                kebeleSuggestions = "Hawelti, Kedamay Weyane, Ayder, Hadnet, Semien, Quiha, Axum, Adigrat"
            ),
            RegionConfig(
                regionName        = "Addis Ababa",
                country           = "Ethiopia",
                agencyName        = "Addis Ababa CRVS Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Bole Subcity",
                defaultKebele     = "Wereda 03",
                kebeleSuggestions = "Bole, Kirkos, Arada, Yeka, Lideta, Nifas Silk, Kolfe, Gullele, Akaki"
            ),
            RegionConfig(
                regionName        = "Dire Dawa",
                country           = "Ethiopia",
                agencyName        = "Dire Dawa CRVS Agency",
                proclamation      = "Proclamation No. 1370/2024",
                trainingLabel     = "CRVS Registrar Training",
                defaultWereda     = "Sabian Wereda",
                defaultKebele     = "Kebele 02",
                kebeleSuggestions = "Kebele 01, Kebele 02, Kebele 03, Kebele 04, Sabian, Genda Kore, Melka"
            )
        )
    }

    /** Emits the current [RegionConfig] whenever any field changes. */
    val regionConfigFlow: Flow<RegionConfig> = context.dataStore.data.map { prefs ->
        RegionConfig(
            regionName        = prefs[KEY_REGION]   ?: "Dire Dawa",
            country           = prefs[KEY_COUNTRY]  ?: "Ethiopia",
            agencyName        = prefs[KEY_AGENCY]   ?: "Dire Dawa CRVS Agency",
            proclamation      = prefs[KEY_PROC]     ?: "Proclamation No. 1370/2024",
            trainingLabel     = prefs[KEY_TRAINING] ?: "CRVS Registrar Training",
            defaultWereda     = prefs[KEY_WEREDA]   ?: "Sabian Wereda",
            defaultKebele     = prefs[KEY_KEBELE]   ?: "Kebele 02",
            kebeleSuggestions = prefs[KEY_SUGGEST]  ?: "Kebele 01, Kebele 02, Kebele 03, Kebele 04, Sabian, Genda Kore, Melka"
        )
    }

    suspend fun saveRegion(config: RegionConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REGION]   = config.regionName
            prefs[KEY_COUNTRY]  = config.country
            prefs[KEY_AGENCY]   = config.agencyName
            prefs[KEY_PROC]     = config.proclamation
            prefs[KEY_TRAINING] = config.trainingLabel
            prefs[KEY_WEREDA]   = config.defaultWereda
            prefs[KEY_KEBELE]   = config.defaultKebele
            prefs[KEY_SUGGEST]  = config.kebeleSuggestions
        }
    }

    /** Flow emitting whether the Post-Test is unlocked for examinees by an Admin/Manager */
    val isPostTestUnlockedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_POST_TEST_UNLOCKED] ?: false
    }

    suspend fun savePostTestUnlocked(unlocked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POST_TEST_UNLOCKED] = unlocked
        }
    }
}
