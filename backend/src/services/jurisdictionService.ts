/**
 * Ethiopian CRVS Jurisdictions Service
 * Synchronized 1-to-1 with Android Mobile App presets and jurisdictions.
 */

export interface Jurisdiction {
  code_prefix: string;
  region_name: string;
  amharic_name: string;
  agency_name_en: string;
  agency_name_am: string;
  signatory_title_en: string;
  signatory_title_am: string;
  default_wereda: string;
  default_kebele: string;
}

export const ETHIOPIAN_JURISDICTIONS: Jurisdiction[] = [
  {
    code_prefix: 'DD',
    region_name: 'Dire Dawa',
    amharic_name: 'የድሬዳዋ አስተዳደር',
    agency_name_en: 'Dire Dawa Administration CRVS Agency',
    agency_name_am: 'የድሬዳዋ አስተዳደር ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Dire Dawa CRVS Agency Director',
    signatory_title_am: 'የድሬዳዋ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Sabian Wereda',
    default_kebele: 'Kebele 02'
  },
  {
    code_prefix: 'AA',
    region_name: 'Addis Ababa',
    amharic_name: 'የአዲስ አበባ አስተዳደር',
    agency_name_en: 'Addis Ababa City Administration CRVS Agency',
    agency_name_am: 'የአዲስ አበባ ከተማ አስተዳደር ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Addis Ababa CRVS Agency Director',
    signatory_title_am: 'የአዲስ አበባ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Bole Subcity',
    default_kebele: 'Wereda 03'
  },
  {
    code_prefix: 'AF',
    region_name: 'Afar',
    amharic_name: 'የአፋር ክልል',
    agency_name_en: 'Afar CRVS Regional Agency',
    agency_name_am: 'የአፋር ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Afar CRVS Regional Agency Director',
    signatory_title_am: 'የአፋር ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Semera Wereda',
    default_kebele: 'Kebele 01'
  },
  {
    code_prefix: 'AM',
    region_name: 'Amhara',
    amharic_name: 'የአማራ ክልል',
    agency_name_en: 'Amhara CRVS Regional Agency',
    agency_name_am: 'የአማራ ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Amhara CRVS Regional Agency Director',
    signatory_title_am: 'የአማራ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Bahir Dar Wereda',
    default_kebele: 'Kebele 01'
  },
  {
    code_prefix: 'BG',
    region_name: 'Benishangul-Gumuz',
    amharic_name: 'የቤንሻንጉል ጉሙዝ ክልል',
    agency_name_en: 'Benishangul-Gumuz CRVS Agency',
    agency_name_am: 'የቤንሻንጉል ጉሙዝ ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Benishangul-Gumuz CRVS Agency Director',
    signatory_title_am: 'የቤንሻንጉል ጉሙዝ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Assosa Wereda',
    default_kebele: 'Assosa 01'
  },
  {
    code_prefix: 'CE',
    region_name: 'Central Ethiopia',
    amharic_name: 'የማዕከላዊ ኢትዮጵያ ክልል',
    agency_name_en: 'Central Ethiopia CRVS Regional Agency',
    agency_name_am: 'የማዕከላዊ ኢትዮጵያ ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Central Ethiopia CRVS Regional Agency Director',
    signatory_title_am: 'የማዕከላዊ ኢትዮጵያ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Hosanna Wereda',
    default_kebele: 'Kebele 01'
  },
  {
    code_prefix: 'GA',
    region_name: 'Gambela',
    amharic_name: 'የጋምቤላ ክልል',
    agency_name_en: 'Gambela CRVS Regional Agency',
    agency_name_am: 'የጋምቤላ ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Gambela CRVS Regional Agency Director',
    signatory_title_am: 'የጋምቤላ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Gambela Wereda',
    default_kebele: 'Kebele 01'
  },
  {
    code_prefix: 'HA',
    region_name: 'Harari',
    amharic_name: 'የሐረሪ ክልል',
    agency_name_en: 'Harari CRVS Regional Agency',
    agency_name_am: 'የሐረሪ ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Harari CRVS Regional Agency Director',
    signatory_title_am: 'የሐረሪ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Harar Wereda',
    default_kebele: 'Abadir'
  },
  {
    code_prefix: 'OR',
    region_name: 'Oromia',
    amharic_name: 'የኦሮሚያ ክልል',
    agency_name_en: 'Oromia CRVS Regional Agency',
    agency_name_am: 'የኦሮሚያ ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Oromia CRVS Regional Agency Director',
    signatory_title_am: 'የኦሮሚያ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Adama Wereda',
    default_kebele: 'Ganda 01'
  },
  {
    code_prefix: 'SI',
    region_name: 'Sidama',
    amharic_name: 'የሲዳማ ክልል',
    agency_name_en: 'Sidama CRVS Regional Agency',
    agency_name_am: 'የሲዳማ ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Sidama CRVS Regional Agency Director',
    signatory_title_am: 'የሲዳማ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Hawassa Wereda',
    default_kebele: 'Tabor'
  },
  {
    code_prefix: 'SO',
    region_name: 'Somali',
    amharic_name: 'የሶማሌ ክልል',
    agency_name_en: 'Somali CRVS Regional Agency',
    agency_name_am: 'የሶማሌ ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Somali CRVS Regional Agency Director',
    signatory_title_am: 'የሶማሌ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Jigjiga Wereda',
    default_kebele: 'Kebele 01'
  },
  {
    code_prefix: 'SE',
    region_name: 'South Ethiopia',
    amharic_name: 'የደቡብ ኢትዮጵያ ክልል',
    agency_name_en: 'South Ethiopia CRVS Regional Agency',
    agency_name_am: 'የደቡብ ኢትዮጵያ ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'South Ethiopia CRVS Regional Agency Director',
    signatory_title_am: 'የደቡብ ኢትዮጵያ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Wolaita Sodo Wereda',
    default_kebele: 'Kebele 01'
  },
  {
    code_prefix: 'SW',
    region_name: "South West Ethiopia Peoples' Region",
    amharic_name: 'የደቡብ ምዕራብ ኢትዮጵያ ክልል',
    agency_name_en: 'South West Ethiopia CRVS Agency',
    agency_name_am: 'የደቡብ ምዕራብ ኢትዮጵያ ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'South West Ethiopia CRVS Agency Director',
    signatory_title_am: 'የደቡብ ምዕራብ ኢትዮጵያ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Bonga Wereda',
    default_kebele: 'Kebele 01'
  },
  {
    code_prefix: 'TG',
    region_name: 'Tigray',
    amharic_name: 'የትግራይ ክልል',
    agency_name_en: 'Tigray CRVS Regional Agency',
    agency_name_am: 'የትግራይ ክልል ወሳኝ ኩነቶች ኤጀንሲ',
    signatory_title_en: 'Tigray CRVS Regional Agency Director',
    signatory_title_am: 'የትግራይ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር',
    default_wereda: 'Mekelle Wereda',
    default_kebele: 'Kedamay Weyane'
  }
];

export class JurisdictionService {
  /**
   * Returns list of all registered Ethiopian jurisdictions
   */
  public static getAllJurisdictions(): Jurisdiction[] {
    return ETHIOPIAN_JURISDICTIONS;
  }

  /**
   * Intelligently resolves jurisdiction based on participant code prefix,
   * wereda/kebele string, or region name hint.
   */
  public static resolveJurisdiction(placeOrCode?: string | null): Jurisdiction {
    const defaultJurisdiction = ETHIOPIAN_JURISDICTIONS[0]; // Dire Dawa fallback
    if (!placeOrCode || typeof placeOrCode !== 'string') {
      return defaultJurisdiction;
    }

    const clean = placeOrCode.trim();

    // 1. Check code prefix (e.g. "CE-USER", "AA-001", "OR-REG-01")
    for (const jur of ETHIOPIAN_JURISDICTIONS) {
      if (clean.toUpperCase().startsWith(`${jur.code_prefix}-`)) {
        return jur;
      }
    }

    // 2. Check full name or Amharic name in string
    const lower = clean.toLowerCase();
    for (const jur of ETHIOPIAN_JURISDICTIONS) {
      if (lower.includes(jur.region_name.toLowerCase()) || clean.includes(jur.amharic_name)) {
        return jur;
      }
    }

    // 3. Check for specific keywords (e.g. "Hosanna" -> Central Ethiopia, "Sabian" -> Dire Dawa)
    if (lower.includes('hosanna') || lower.includes('butajira') || lower.includes('worabe') || clean.includes('ማዕከላዊ')) {
      return ETHIOPIAN_JURISDICTIONS.find(j => j.code_prefix === 'CE')!;
    }
    if (lower.includes('bole') || lower.includes('kirkos') || lower.includes('arada') || clean.includes('አዲስ አበባ')) {
      return ETHIOPIAN_JURISDICTIONS.find(j => j.code_prefix === 'AA')!;
    }
    if (lower.includes('adama') || lower.includes('jimma') || lower.includes('bishoftu') || clean.includes('ኦሮሚያ')) {
      return ETHIOPIAN_JURISDICTIONS.find(j => j.code_prefix === 'OR')!;
    }
    if (lower.includes('bahir dar') || lower.includes('gondar') || lower.includes('dessie') || clean.includes('አማራ')) {
      return ETHIOPIAN_JURISDICTIONS.find(j => j.code_prefix === 'AM')!;
    }
    if (lower.includes('hawassa') || lower.includes('tabor') || clean.includes('ሲዳማ')) {
      return ETHIOPIAN_JURISDICTIONS.find(j => j.code_prefix === 'SI')!;
    }
    if (lower.includes('mekelle') || clean.includes('ትግራይ')) {
      return ETHIOPIAN_JURISDICTIONS.find(j => j.code_prefix === 'TG')!;
    }

    return defaultJurisdiction;
  }
}

