export const crvsAssessmentSurvey = {
  code: 'CRVS_DIRE_DAWA_1370',
  title_am: 'የቅድመ-ስልጠና እና የድህረ-ስልጠና የእውቀት መመዘኛ - የድሬዳዋ አስተዳደር የሲአርቪኤስ (CRVS) መዝጋቢዎች ስልጠና',
  title_en: 'Pre & Post Training Knowledge Assessment - Dire Dawa CRVS Registrar Training',
  description_am: 'አዲሱ የሲአርቪኤስ ዲጂታል ምዝገባ ሥርዓት · የሲቪል እና ቤተሰብ ምዝገባ አዋጅ ቁጥር 1370/2017 (እ.ኤ.አ. 2025) አጠቃላይ ትውውቅ',
  description_en: 'New CRVS Digital Registration System & Civil and Family Registration Proclamation No. 1370/2017 (2025)',
  total_points: 29,
  passing_score: 18,
  sections: [
    {
      code: 'SECTION_A',
      title_am: 'ክፍል ሀ – የሲቪል እና ቤተሰብ ምዝገባ አዋጅ ቁጥር 1370/2017 (ጥያቄ 1–8)',
      title_en: 'Part A - Civil & Family Registration Proclamation No. 1370/2017 (Q1–8)',
      description_am: 'ከአማራጮቹ ትክክለኛውን አንድ መልስ ይምረጡ (እያንዳንዱ 1 ነጥብ)።',
      description_en: 'Select the single best answer for each question (1 point each).',
      order_index: 1,
      questions: [
        {
          question_number: 1,
          type: 'SINGLE_CHOICE',
          prompt_am: 'አዋጅ ቁጥር 1370/2017 (እ.ኤ.አ. 2025) የሚጠራው፦',
          prompt_en: 'Proclamation No. 1370/2017 (2025) is titled:',
          points: 1,
          order_index: 1,
          options: [
            { option_key: 'ሀ', label_am: 'የወሳኝ ኩነቶች ምዝገባና የብሔራዊ መታወቂያ ካርድ አዋጅ', label_en: 'Vital Events Registration and National ID Card Proclamation', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'የሲቪል እና ቤተሰብ ምዝገባ አዋጅ', label_en: 'Civil and Family Registration Proclamation', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የዲጂታል መታወቂያ አዋጅ', label_en: 'Digital Identification Proclamation', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'የኢሚግሬሽንና ዜግነት አገልግሎት አዋጅ', label_en: 'Immigration and Citizenship Service Proclamation', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 2,
          type: 'SINGLE_CHOICE',
          prompt_am: 'አዋጅ ቁጥር 1370/2017 የሚተካው ወይም ክፍተቱን የሚሞላው፦',
          prompt_en: 'Proclamation No. 1370/2017 replaces or fills the gaps of:',
          points: 1,
          order_index: 2,
          options: [
            { option_key: 'ሀ', label_am: 'አዋጅ ቁጥር 760/2004 (እ.ኤ.አ. 2012)ን', label_en: 'Proclamation No. 760/2004 (2012)', is_correct: true, order_index: 1 },
            { option_key: 'ለ', label_am: 'አዋጅ ቁጥር 1284/2015 (እ.ኤ.አ. 2023)ን', label_en: 'Proclamation No. 1284/2015 (2023)', is_correct: false, order_index: 2 },
            { option_key: 'ሐ', label_am: 'አዋጅ ቁጥር 1263/2014 (እ.ኤ.አ. 2021)ን', label_en: 'Proclamation No. 1263/2014 (2021)', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'የተሻሻለውን የቤተሰብ ሕግ ብቻ', label_en: 'Revised Family Code only', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 3,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ከሚከተሉት ውስጥ በአዋጁ መሠረት የምዝገባ መርህ የሆነው የትኛው ነው?',
          prompt_en: 'Which of the following is a fundamental registration principle under the Proclamation?',
          points: 1,
          order_index: 3,
          options: [
            { option_key: 'ሀ', label_am: 'ለደህንነት ሲባል አንድ ኩነት ከአንድ በላይ በሆኑ መዝገቦች ሊመዘገብ ይችላል', label_en: 'For security, an event may be recorded in multiple registries', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'ምዝገባ በፈቃደኝነት ላይ የተመሠረተ ነው', label_en: 'Registration is entirely voluntary', is_correct: false, order_index: 2 },
            { option_key: 'ሐ', label_am: 'እያንዳንዱ ወሳኝ ኩነት ትክክለኛና የተሟላ ሆኖ አንድ ጊዜ ብቻ ይመዘገባል', label_en: 'Every vital event is registered accurately, completely, and only once', is_correct: true, order_index: 3 },
            { option_key: 'መ', label_am: 'የዜጎች ኩነቶች ብቻ ይመዘገበሉ', label_en: 'Only events of citizens are registered', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 4,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ልደት በስንት ቀን ውስጥ መመዝገብ አለበት?',
          prompt_en: 'Within how many days must a birth be registered?',
          points: 1,
          order_index: 4,
          options: [
            { option_key: 'ሀ', label_am: 'በ30 ቀን ውስጥ', label_en: 'Within 30 days', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'በ60 ቀን ውስጥ', label_en: 'Within 60 days', is_correct: false, order_index: 2 },
            { option_key: 'ሐ', label_am: 'በ90 ቀን ውስጥ', label_en: 'Within 90 days', is_correct: true, order_index: 3 },
            { option_key: 'መ', label_am: 'በ1 ዓመት ውስጥ', label_en: 'Within 1 year', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 5,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ሞት በስንት ቀን ውስጥ መመዝገብ አለበት?',
          prompt_en: 'Within how many days must a death be registered?',
          points: 1,
          order_index: 5,
          options: [
            { option_key: 'ሀ', label_am: 'በ10 ቀን ውስጥ', label_en: 'Within 10 days', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'በ30 ቀን ውስጥ', label_en: 'Within 30 days', is_correct: false, order_index: 2 },
            { option_key: 'ሐ', label_am: 'በ50 ቀን ውስጥ', label_en: 'Within 50 days', is_correct: true, order_index: 3 },
            { option_key: 'መ', label_am: 'በ90 ቀን ውስጥ', label_en: 'Within 90 days', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 6,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ሌሎች ወሳኝ ኩነቶች (ለምሳሌ ጋብቻ፣ ፍቺ) በስንት ቀን ውስጥ መመዝገብ አለባቸው?',
          prompt_en: 'Within how many days must other vital events (e.g. marriage, divorce) be registered?',
          points: 1,
          order_index: 6,
          options: [
            { option_key: 'ሀ', label_am: 'በ15 ቀን ውስጥ', label_en: 'Within 15 days', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'በ30 ቀን ውስጥ', label_en: 'Within 30 days', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'በ60 ቀን ውስጥ', label_en: 'Within 60 days', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'በ90 ቀን ውስጥ', label_en: 'Within 90 days', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 7,
          type: 'SINGLE_CHOICE',
          prompt_am: 'አዋጁ “የቤተሰብ ምዝገባ”ን ያስተዋውቃል። ይህ ማለት፦',
          prompt_en: 'The proclamation introduces "Family Registration". This signifies:',
          points: 1,
          order_index: 7,
          options: [
            { option_key: 'ሀ', label_am: 'የቤተሰብ ኃላፊን ብቻ መመዝገብ', label_en: 'Registering only head of household', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'በአንድ ቤተሰብ ውስጥ ያሉ ሰዎችን ግንኙነት (በደም፣ በጋብቻ ወይም በጉዲፈቻ) መመዝገብ', label_en: 'Recording relationships of family members (by blood, marriage, or adoption)', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የቤተሰብ ንግዶችን መመዝገብ', label_en: 'Registering family businesses', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'ከልደት የምስክር ወረቀት ይልቅ የቤተሰብ መታወቂያ መስጠት', label_en: 'Issuing a family ID card instead of birth certificates', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 8,
          type: 'SINGLE_CHOICE',
          prompt_am: 'አንድ ልደት ከተወሰነው ጊዜ በኋላ ተዘግይቶ ቢቀርብ መዝጋቢው ምን ማድረግ አለበት?',
          prompt_en: 'If a birth registration is presented past the legal deadline, what should the registrar do?',
          points: 1,
          order_index: 8,
          options: [
            { option_key: 'ሀ', label_am: 'ምዝገባውን አለመቀበል', label_en: 'Reject the registration', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'በጊዜው እንደተደረገ አድርጎ መመዝገብ', label_en: 'Record it as on-time without remark', is_correct: false, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የመዘግየቱን ምክንያትና ማስረጃ ገምግሞ፣ እንደ ዘገየ ምዝገባ (አግባብ ያለውን ክፍያ በማስከፈል) መመዝገብ', label_en: 'Evaluate the delay reason & evidence, process as late registration with applicable fee', is_correct: true, order_index: 3 },
            { option_key: 'መ', label_am: 'በሁሉም ሁኔታ ቤተሰቡን ወደ ፍርድ ቤት መላክ', label_en: 'Send the family to court in all cases', is_correct: false, order_index: 4 }
          ]
        }
      ]
    },
    {
      code: 'SECTION_B',
      title_am: 'ክፍል ለ – አዲሱ የሲአርቪኤስ ዲጂታል ምዝገባ ሥርዓት (ጥያቄ 9–16)',
      title_en: 'Part B - New CRVS Digital Registration System (Q9–16)',
      description_am: 'ከአማራጮቹ ትክክለኛውን አንድ መልስ ይምረጡ (እያንዳንዱ 1 ነጥብ)።',
      description_en: 'Select the single best answer for each question (1 point each).',
      order_index: 2,
      questions: [
        {
          question_number: 9,
          type: 'SINGLE_CHOICE',
          prompt_am: 'የዲጂታል የሲአርቪኤስ ምዝገባ ሥርዓት ዋና ዓላማ ምንድን ነው?',
          prompt_en: 'What is the primary purpose of the digital CRVS registration system?',
          points: 1,
          order_index: 1,
          options: [
            { option_key: 'ሀ', label_am: 'የምስክር ወረቀቶችን አስፈላጊነት መተካት', label_en: 'Eliminate the necessity of paper certificates', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'ወሳኝ ኩነቶችን በአንድ ማዕከላዊ የመረጃ ቋት በትክክል መመዝገብና የምስክር ወረቀት መስጠት', label_en: 'Accurately register vital events in a central database and issue verified certificates', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'ለጤና ቢሮ የስታቲስቲክስ መረጃ ብቻ ማከማቸት', label_en: 'Store statistical metrics exclusively for the health bureau', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'በጤና ተቋማት ውስጥ ታካሚዎችን መከታተል', label_en: 'Track inpatient hospital admissions', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 10,
          type: 'SINGLE_CHOICE',
          prompt_am: 'አዲስ የልደት መዝገብ ከማስገባት በፊት መዝጋቢው መጀመሪያ ማድረግ ያለበት፦',
          prompt_en: 'Before submitting a new birth registration entry, the registrar must first:',
          points: 1,
          order_index: 2,
          options: [
            { option_key: 'ሀ', label_am: 'የምስክር ወረቀቱን ማተም', label_en: 'Print out the certificate', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'ኩነቱ ከዚህ በፊት አለመመዝገቡን በሥርዓቱ ውስጥ መፈለግና ማረጋገጥ', label_en: 'Search and verify in the system that the event was not previously recorded', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'ቤተሰቡ ክፍያ እንዲከፍል መጠየቅ', label_en: 'Request upfront payment from the family', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'የቆዩ የወረቀት መዝገቦችን ማጥፋት', label_en: 'Destroy historical physical registers', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 11,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ለሞት መዝገብ የግድ አስፈላጊ የሆነው ከሚከተሉት የትኛው ነው?',
          prompt_en: 'Which of the following is mandatory for a death registration record?',
          points: 1,
          order_index: 3,
          options: [
            { option_key: 'ሀ', label_am: 'የሟች ሃይማኖት', label_en: 'The deceased person religion', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'የሞት ቀን፣ ቦታና መንስኤ/ሁኔታ፣ እንዲሁም የአሳዋቂው ዝርዝር መረጃ', label_en: 'Date, place, cause/circumstances of death, and informant details', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የሟች አሠሪ', label_en: 'The deceased employer details', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'የቤተሰብ ገቢ', label_en: 'Family annual income', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 12,
          type: 'SINGLE_CHOICE',
          prompt_am: 'መዝገብ ካስቀመጡ በኋላ የተሳሳተ የትውልድ ቀን እንዳስገቡ ቢገነዘቡ ትክክለኛው እርምጃ ምንድን ነው?',
          prompt_en: 'If you realize an erroneous birth date was saved, what is the appropriate action?',
          points: 1,
          order_index: 4,
          options: [
            { option_key: 'ሀ', label_am: 'በትክክለኛው ቀን አዲስ መዝገብ መመዝገብ', label_en: 'Create a brand new record with the correct date', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'በድጋፍ ሰነዶችና በተፈቀደ ሥልጣን የተደገፈውን የሥርዓቱን የማስተካከያ ሂደት መከተል', label_en: 'Follow formal amendment workflow supported by documents and authorization', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የሥራ ባልደረባ የራሱን መለያ ተጠቅሞ መዝገቡን እንዲተካ መጠየቅ', label_en: 'Ask a colleague to overwrite using their account credentials', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'መዝገቡን እንዳለ ትቶ በወረቀት የምስክር ወረቀቱ ላይ ማስተካከል', label_en: 'Leave the system record as is and pen-correct the physical paper', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 13,
          type: 'SINGLE_CHOICE',
          prompt_am: 'የሥርዓት መግቢያ መለያዎችን (የተጠቃሚ ስምና የይለፍ ቃል) ለመያዝ ትክክለኛው መንገድ የትኛው ነው?',
          prompt_en: 'What is the correct protocol for managing system login credentials?',
          points: 1,
          order_index: 5,
          options: [
            { option_key: 'ሀ', label_am: 'ሥራ ሲበዛ ለታማኝ የሥራ ባልደረባ ማጋራት', label_en: 'Share with a trusted coworker during peak load hours', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'ለመጠባበቂያ በቢሮ ግድግዳ ላይ መጻፍ', label_en: 'Write on office whiteboards as a backup', is_correct: false, order_index: 2 },
            { option_key: 'ሐ', label_am: 'በሚስጥር መያዝና ከእያንዳንዱ የሥራ ክፍለ ጊዜ በኋላ መውጣት (log out)', label_en: 'Keep strictly confidential and log out after every work session', is_correct: true, order_index: 3 },
            { option_key: 'መ', label_am: 'ለመላው ቢሮ አንድ የጋራ መለያ መጠቀም', label_en: 'Use one common shared account for the entire office', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 14,
          type: 'SINGLE_CHOICE',
          prompt_am: 'በቢሮዎ የበይነመረብ ግንኙነት ቢቋረጥ ምን ማድረግ አለብዎት?',
          prompt_en: 'If your office internet connection is disconnected, what should you do?',
          points: 1,
          order_index: 6,
          options: [
            { option_key: 'ሀ', label_am: 'ምዝገባን በሙሉ አቁሞ ደንበኞችን መመለስ', label_en: 'Halt all registrations and turn away citizens', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'የተፈቀደውን ከመስመር-ውጭ/የወረቀት አማራጭ አሠራር ተጠቅሞ፣ ግንኙነቱ ሲመለስ ድግግሞሽ ሳይፈጠር መዝገቦችን ማስገባት', label_en: 'Use authorized offline/paper procedures and sync without duplicates when connection resumes', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'ኩነቶችን ከትውስታ በኋላ መመዝገብ', label_en: 'Rely on memory to enter events days later', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'ለደህንነት ሲባል መዝገቦችን ሁለት ጊዜ ማስገባት', label_en: 'Submit every entry twice as a safety precaution', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 15,
          type: 'SINGLE_CHOICE',
          prompt_am: 'የተመዘገቡ ሰዎች የግል መረጃ ሊገለጽ የሚችለው፦',
          prompt_en: 'Personal data of registered individuals may be disclosed to:',
          points: 1,
          order_index: 7,
          options: [
            { option_key: 'ሀ', label_am: 'በመስኮት ለሚጠይቅ ማንኛውም ሰው', label_en: 'Any passerby inquiring at the service window', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'በሕጉና በአሠራሩ በተፈቀደ መሠረት ለተፈቀደላቸው ሰዎች ወይም ተቋማት ብቻ', label_en: 'Only to authorized entities or persons as permitted by law and formal protocol', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'ለሚጠይቅ ማንኛውም አሠሪ', label_en: 'Any prospective employer requesting employee data', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'አገልግሎቱን ለማስተዋወቅ በማኅበራዊ ሚዲያ', label_en: 'Social media channels to promote civic awareness', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 16,
          type: 'SINGLE_CHOICE',
          prompt_am: 'በዲጂታል ሥርዓቱ ውስጥ ምዝገባ እንደተጠናቀቀ የሚቆጠረው፦',
          prompt_en: 'In the digital system, a registration is legally recognized as complete when:',
          points: 1,
          order_index: 8,
          options: [
            { option_key: 'ሀ', label_am: 'ቅጹ እንደ ረቂቅ (draft) ሲቀመጥ', label_en: 'The entry is saved in draft state', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'ሁሉም አስገዳጅ መስኮች ተረጋግጠው፣ መዝገቡ ገብቶ/ጸድቆ፣ የምስክር ወረቀቱ ለአመልካቹ ሲሰጥ', label_en: 'All mandatory fields are verified, record submitted/approved, and certificate issued to applicant', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'ደንበኛው ቢሮውን ለቆ ሲሄድ', label_en: 'The citizen leaves the building premises', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'ተቆጣጣሪው በስልክ ሲነገረው', label_en: 'The supervisor is orally informed over a phone call', is_correct: false, order_index: 4 }
          ]
        }
      ]
    },
    {
      code: 'SECTION_C',
      title_am: 'ክፍል ሐ – ተግባራዊ ሁኔታዎች (ጥያቄ 17–20)',
      title_en: 'Part C - Practical Scenarios (Q17–20)',
      description_am: 'አጭር መልስ ይጻፉ (2–4 ዓረፍተ ነገር)። እያንዳንዱ ጥያቄ 2 ነጥብ ነው።',
      description_en: 'Write concise scenario answers (2–4 sentences). Each question is worth 2 points.',
      order_index: 3,
      questions: [
        {
          question_number: 17,
          type: 'TEXT_OPEN',
          prompt_am: 'አንዲት እናት በቤት የተወለደ የ5 ወር ሕፃን ይዛ መጥታለች። የልደት ማሳወቂያ የላትም፤ መታወቂያም የላትም። በዲጂታል ሥርዓቱ ውስጥ የሚወስዷቸውን እርምጃዎችና ሕጉ የሚጠይቀውን ያብራሩ።',
          prompt_en: 'A mother brings a 5-month-old infant born at home without birth notification or ID. Explain the legal requirements and actions you will take in the digital system.',
          points: 2,
          rubric_am: '1 ነጥብ፦ ይህ ከ90 ቀን በላይ የዘገየ ምዝገባ መሆኑንና ትክክለኛ ምክንያትና ማስረጃ (ምስክሮች ወይም ሌላ አማራጭ ማስረጃ) እንደሚያስፈልግ ለመለየት፤ 1 ነጥብ፦ ሥርዓቱን በትክክል ለመጠቀም (መጀመሪያ መፈለግ፣ አስገዳጅ መስኮችን መሙላት፣ የዘገየ ምዝገባ ዝርዝርን መመዝገብ፣ የምስክር ወረቀት መስጠት፤ በደንቡ መሠረት ክፍያ ማስከፈል)።',
          rubric_en: '1 pt: Identify delay beyond 90 days requiring substantiated reason & witness/alternative evidence. 1 pt: Accurate system execution (search first, capture mandatory details, record late status & fee, issue cert).',
          order_index: 1
        },
        {
          question_number: 18,
          type: 'TEXT_OPEN',
          prompt_am: 'አንድ ቤተሰብ አንድ አዛውንት ከሞቱ ከ70 ቀን በኋላ ሞታቸውን አሳውቋል። ምን ያደርጋሉ፤ ምንስ ይመዘግባሉ?',
          prompt_en: 'A family notifies the death of an elder 70 days after passing. What procedure do you follow and what data do you register?',
          points: 2,
          rubric_am: '1 ነጥብ፦ የ50 ቀን ገደብ ማለፉንና የዘገየ ምዝገባ ሂደት እንደሚተገበር ለመለየት፤ 1 ነጥብ፦ የመዘግየቱን ምክንያት፣ የሞቱን መንስኤ/ሁኔታ፣ የአሳዋቂውን ዝርዝር መመዝገብና የምስክር ወረቀት መስጠትን ለመጥቀስ።',
          rubric_en: '1 pt: Identify exceeding the 50-day deadline, apply late death registration. 1 pt: Document reason for delay, place/cause, informant details, and issue certificate.',
          order_index: 2
        },
        {
          question_number: 19,
          type: 'TEXT_OPEN',
          prompt_am: 'የአንድ ልጅ ልደት በትንሽ የስም ፊደል ልዩነት በሥርዓቱ ውስጥ ሁለት ጊዜ የተመዘገበ ሆኖ አገኙት። ምን ያደርጋሉ?',
          prompt_en: 'You notice a child birth is entered twice in the system with a minor spelling discrepancy. What steps do you take?',
          points: 2,
          rubric_am: '1 ነጥብ፦ አዲስ መዝገብ ላለመፍጠር ወይም በራስ ፈቃድ መዝገብ ላለመሰረዝ፤ 1 ነጥብ፦ ለተቆጣጣሪው/ለሥርዓት አስተዳዳሪው አሳውቆ በተፈቀደ ሂደት እንዲጣራና እንዲዋሃድ/እንዲሰረዝ ለማድረግ።',
          rubric_en: '1 pt: Avoid creating another record or unilaterally deleting entries. 1 pt: Escalate to supervisor/admin for formal investigation and authorized merge/de-duplication.',
          order_index: 3
        },
        {
          question_number: 20,
          type: 'TEXT_OPEN',
          prompt_am: 'ከቤተሰብ ያልሆነ አንድ ሰው የጎረቤቱን የምዝገባ ዝርዝር እንዲያሳዩት ይጠይቅዎታል። እንዴት ምላሽ ይሰጣሉ፤ ለምን?',
          prompt_en: 'A non-family member requests to inspect a neighbor civil registration details. How do you respond and why?',
          points: 2,
          rubric_am: '1 ነጥብ፦ መረጃውን ላለመስጠት፤ 1 ነጥብ፦ መረጃው ሚስጥራዊ መሆኑንና በሕጉና በአሠራሩ መሠረት ለተፈቀደላቸው ብቻ እንደሚሰጥ ለማብራራት።',
          rubric_en: '1 pt: Strictly decline request. 1 pt: Clarify data confidentiality and legal mandate restricting disclosure to authorized individuals.',
          order_index: 4
        }
      ]
    },
    {
      code: 'SECTION_D',
      title_am: 'ክፍል መ – ቴክኒካዊ ምዘና ቼክሊስት (ጥያቄ 21–25)',
      title_en: 'Part D - Technical Assessment Checklist (Q21–25)',
      description_am: 'የሥርዓት አሠራር፣ የደህንነት መስፈርቶችና የዳታ ጥራት ቴክኒካዊ ቼክሊስት (እያንዳንዱ 1 ነጥብ)።',
      description_en: 'Technical checklist assessing operational procedures, data security, and quality controls (1 point each).',
      order_index: 4,
      questions: [
        {
          question_number: 21,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ቴክኒካዊ ቼክሊስት 1፦ አዲስ የልደት ወይም የሞት ምዝገባ ከመጀመርዎ በፊት በዲጂታል ሥርዓቱ ላይ መጀመሪያ መከናወን ያለበት አስገዳጅ ቴክኒካዊ ተግባር የትኛው ነው?',
          prompt_en: 'Technical Checklist 1: Prior to initiating a new birth or death registration, which mandatory technical action must be executed in the digital system?',
          points: 1,
          order_index: 1,
          options: [
            { option_key: 'ሀ', label_am: 'ወዲያውኑ አዲስ የምስክር ወረቀት ማተም', label_en: 'Directly print a blank certificate', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'በሥርዓቱ ውስጥ ቀድሞ የተመዘገበ እንዳይኖር ቅድመ-ፍለጋ (Duplicate Search) ማካሄድ', label_en: 'Conduct a deduplication search to ensure no prior duplicate record exists', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የሥርዓቱን የይለፍ ቃል መቀየር', label_en: 'Change system login password', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'ሁሉንም የቀደሙ መዝገቦች ወደ ውጭ ፋይል (Export) ማድረግ', label_en: 'Export all historical registry entries', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 22,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ቴክኒካዊ ቼክሊስት 2፦ የበይነ-መረብ (Internet) ግንኙነት በተቋረጠበት ወቅት ከመስመር ውጭ (Offline Mode) የተመዘገቡ መረጃዎችን በተመለከተ ትክክለኛው ቴክኒካዊ አሠራር የትኛው ነው?',
          prompt_en: 'Technical Checklist 2: Regarding offline records captured during internet network downtime, what is the correct technical procedure?',
          points: 1,
          order_index: 2,
          options: [
            { option_key: 'ሀ', label_am: 'የበይነ-መረብ ግንኙነት ሲመለስ መረጃዎቹ በራስ-ሰር ወይም በተጠቃሚ ትዕዛዝ ወደ ማዕከላዊ ሰርቨር ማመሳሰል (Sync) ማረጋገጥ', label_en: 'Once connectivity resumes, verify records are synchronized automatically or prompted to the central server', is_correct: true, order_index: 1 },
            { option_key: 'ለ', label_am: 'ከመስመር ውጭ የተመዘገቡ መረጃዎችን ሙሉ በሙሉ ሰርዞ አዲስ መመዝገብ', label_en: 'Delete offline records and re-enter everything from scratch', is_correct: false, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የበይነ-መረብ ግንኙነት እስኪመጣ ድረስ ምንም አይነት ምዝገባ አለማከናወን', label_en: 'Suspend all client registrations entirely until connection returns', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'ከመስመር ውጭ የተመዘገቡትን በወረቀት ብቻ አስቀርቶ ማቆየት', label_en: 'Keep offline records solely in physical paper forms without digitizing', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 23,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ቴክኒካዊ ቼክሊስት 3፦ የደጋፊ ሰነዶች ቅኝት (Document Scanning/Attachment) በሚከናወንበት ወቅት የዳታ ጥራት መስፈርትን የሚያሟላው የትኛው ነው?',
          prompt_en: 'Technical Checklist 3: When scanning or uploading supporting documents into the CRVS system, which standard fulfills data quality requirements?',
          points: 1,
          order_index: 3,
          options: [
            { option_key: 'ሀ', label_am: 'ማናቸውም ዓይነት ግልጽ ያልሆነ ወይም የደበዘዘ ምስል መጫን', label_en: 'Uploading any blurry or illegible preview', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'ሰነዱ ሙሉ በሙሉ የሚነበብ፣ ትክክለኛ ማዕዘን ያለውና በይፋ የተፈቀደ ፎርማት (PDF/JPG) መሆኑን ማረጋገጥ', label_en: 'Ensuring the document is completely legible, properly aligned, and in an authorized format (PDF/JPG)', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የደጋፊ ሰነድ ሳያያይዙ ምዝገባውን ያለተቆጣጣሪ ፈቃድ ማጠናቀቅ', label_en: 'Completing registration without uploading mandatory proof or supervisor bypass', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'የሰነዱን የመጀመሪያ ገጽ ብቻ መጫንና ሌላውን መተው', label_en: 'Uploading only the first cover page regardless of required pages', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 24,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ቴክኒካዊ ቼክሊስት 4፦ በሥርዓቱ ውስጥ የተጠቃሚ መለያ (Account Credentials) እና የይለፍ ቃል አያያዝ የደህንነት መመሪያ የትኛው ነው?',
          prompt_en: 'Technical Checklist 4: What is the mandatory security protocol regarding CRVS user account credentials and passwords?',
          points: 1,
          order_index: 4,
          options: [
            { option_key: 'ሀ', label_am: 'ለስራ ቅልጥፍና ሲባል የይለፍ ቃልን ለስራ ባልደረቦች ማጋራት', label_en: 'Sharing login credentials with colleagues to expedite service delivery', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'የይለፍ ቃልን በኮምፒውተር ስክሪን ወይም ጠረጴዛ ላይ ጽፎ መለጠፍ', label_en: 'Writing passwords on sticky notes attached to monitor or desk', is_correct: false, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የተጠቃሚ መለያ በጥብቅ ሚስጥራዊ ሆኖ መጠበቅ፣ ስራ ሲጠናቀቅ Logout ማድረግ እና የኦዲት መዝገብ (Audit Log) ተጠያቂነትን ማወቅ', label_en: 'Keeping credentials strictly confidential, logging out when leaving, and observing audit trail accountability', is_correct: true, order_index: 3 },
            { option_key: 'መ', label_am: 'የይለፍ ቃልን በጭራሽ አለመቀየር', label_en: 'Never changing default system-assigned passwords', is_correct: false, order_index: 4 }
          ]
        },
        {
          question_number: 25,
          type: 'SINGLE_CHOICE',
          prompt_am: 'ቴክኒካዊ ቼክሊስት 5፦ የዕለቱ የምዝገባ ስራ ሲጠናቀቅ መዝጋቢው ማከናወን ያለበት የመጨረሻ ቴክኒካዊ ተግባር ምንድን ነው?',
          prompt_en: 'Technical Checklist 5: At the close of daily operations, what final technical reconciliation task must the registrar execute?',
          points: 1,
          order_index: 5,
          options: [
            { option_key: 'ሀ', label_am: 'ኮምፒውተሩን ክፍት አድርጎ መሄድ', label_en: 'Leaving the workstation unlocked overnight', is_correct: false, order_index: 1 },
            { option_key: 'ለ', label_am: 'የተሰጡ ሰርተፊኬቶችን ቁጥር፣ የተሰበሰበ ክፍያና የዳታ ማመሳሰል (Daily Reconciliation & Sync) ሪፖርት ማረጋገጥ', label_en: 'Verifying certificate count, fee reconciliation, and confirming full server data synchronization', is_correct: true, order_index: 2 },
            { option_key: 'ሐ', label_am: 'የተመዘገቡትን ሪከርዶች ከዳታቤዝ ማጥፋት', label_en: 'Purging submitted records from the local client cache', is_correct: false, order_index: 3 },
            { option_key: 'መ', label_am: 'ምንም አይነት ማረጋገጫ ሳያደርጉ መውጣት', label_en: 'Exiting without checking pending transmission queues', is_correct: false, order_index: 4 }
          ]
        }
      ]
    },
    {
      code: 'FEEDBACK',
      title_am: 'የሥልጠና አስተያየት (ለድህረ-ሙከራ)',
      title_en: 'Training Feedback (Post-Test Only)',
      description_am: 'ከ1 (ደካማ) እስከ 5 (እጅግ በጣም ጥሩ) ደረጃ ይስጡ፦',
      description_en: 'Rate from 1 (Poor) to 5 (Excellent):',
      order_index: 5,
      questions: [
        {
          question_number: 26,
          type: 'RATING_SCALE',
          prompt_am: 'የአዋጅ ቁጥር 1370/2017 ትውውቅ ግልጽነት',
          prompt_en: 'Clarity of Proclamation No. 1370/2017 orientation',
          points: 0,
          order_index: 1
        },
        {
          question_number: 27,
          type: 'RATING_SCALE',
          prompt_am: 'ከዲጂታል ሥርዓቱ ጋር የተደረገ ተግባራዊ ልምምድ',
          prompt_en: 'Hands-on practical exercises with the digital system',
          points: 0,
          order_index: 2
        },
        {
          question_number: 28,
          type: 'RATING_SCALE',
          prompt_am: 'የአሰልጣኞችና የሥልጠና ቁሳቁሶች ጥራት',
          prompt_en: 'Quality of trainers and instructional materials',
          points: 0,
          order_index: 3
        },
        {
          question_number: 29,
          type: 'RATING_SCALE',
          prompt_am: 'ለዕለት ተዕለት ሥራዬ ያለው ጠቀሜታ',
          prompt_en: 'Relevance to daily field registration duties',
          points: 0,
          order_index: 4
        },
        {
          question_number: 30,
          type: 'TEXT_OPEN',
          prompt_am: 'አሁንም ድጋፍ የሚያስፈልግዎት ርዕሶች፦',
          prompt_en: 'Topics where you still need mentoring support:',
          points: 0,
          order_index: 5
        },
        {
          question_number: 31,
          type: 'TEXT_OPEN',
          prompt_am: 'ለማሻሻያ የሚሆኑ ሃሳቦች፦',
          prompt_en: 'Recommendations for future training enhancement:',
          points: 0,
          order_index: 6
        }
      ]
    }
  ]
};
