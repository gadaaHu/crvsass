// CRVS Interactive Client Application
const state = {
  currentLang: (typeof localStorage !== 'undefined' && localStorage.getItem('crvs_selected_lang')) || 'am', // 'am' or 'en'
  currentSurvey: null,
  activeTab: 'dashboard',
  analytics: null,
  submissions: [],
  // Wizard state
  wizard: {
    participant: null,
    testType: 'POST',
    currentQIndex: 0,
    flatQuestions: [],
    answers: {}, // question_id -> { selected_option_key, answer_text, rating_value }
    result: null
  }
};

const API_BASE = '/api';

// Security: Transparent Authorization Header Interceptor for Web Management
(function() {
  const originalFetch = window.fetch;
  window.fetch = function(url, options) {
    try {
      const token = localStorage.getItem('qcafr_token');
      if (token && typeof url === 'string' && url.includes('/api/')) {
        options = options || {};
        options.headers = options.headers || {};
        if (options.headers instanceof Headers) {
          if (!options.headers.has('Authorization')) {
            options.headers.set('Authorization', 'Bearer ' + token);
          }
        } else if (Array.isArray(options.headers)) {
          const hasAuth = options.headers.some(pair => pair[0] && pair[0].toLowerCase() === 'authorization');
          if (!hasAuth) {
            options.headers.push(['Authorization', 'Bearer ' + token]);
          }
        } else {
          if (!options.headers['Authorization'] && !options.headers['authorization']) {
            options.headers['Authorization'] = 'Bearer ' + token;
          }
        }
      }
    } catch (_) {}
    return originalFetch.call(this, url, options);
  };
})();

// DOM Elements
const elements = {
  tabButtons: document.querySelectorAll('.nav-item'),
  tabPanes: document.querySelectorAll('.tab-pane'),
  langBtns: { am: document.getElementById('btn-lang-am'), en: document.getElementById('btn-lang-en') },
  btnExportCsv: document.getElementById('btn-export-csv'),
  // KPIs
  kpiTotalSubs: document.getElementById('kpi-total-subs'),
  kpiSubsBreakdown: document.getElementById('kpi-subs-breakdown'),
  kpiPreAvg: document.getElementById('kpi-pre-avg'),
  kpiPostAvg: document.getElementById('kpi-post-avg'),
  kpiDelta: document.getElementById('kpi-delta'),
  bandListContainer: document.getElementById('band-list-container'),
  barPassPre: document.getElementById('bar-pass-pre'),
  barPassPost: document.getElementById('bar-pass-post'),
  labelPassPre: document.getElementById('label-pass-pre'),
  labelPassPost: document.getElementById('label-pass-post'),
  questionAccuracyTbody: document.getElementById('question-accuracy-tbody'),
  // Wizard elements
  screens: {
    demog: document.getElementById('wizard-screen-demog'),
    questions: document.getElementById('wizard-screen-questions'),
    review: document.getElementById('wizard-screen-review'),
    result: document.getElementById('wizard-screen-result')
  },
  stepperNav: {
    demog: document.getElementById('step-nav-demog'),
    questions: document.getElementById('step-nav-questions'),
    review: document.getElementById('step-nav-review')
  },
  participantForm: document.getElementById('participant-form'),
  btnStartQuestions: document.getElementById('btn-start-questions'),
  qProgressBar: document.getElementById('q-progress-fill'),
  qSectionBadge: document.getElementById('q-section-badge'),
  qNumberIndicator: document.getElementById('q-number-indicator'),
  qPointsIndicator: document.getElementById('q-points-indicator'),
  qPromptText: document.getElementById('q-prompt-text'),
  qPromptAlt: document.getElementById('q-prompt-alt'),
  qInputArea: document.getElementById('q-input-area'),
  btnSpeakQuestion: document.getElementById('btn-speak-question'),
  qRubricNotice: document.getElementById('q-rubric-notice'),
  qRubricText: document.getElementById('q-rubric-text'),
  btnQPrev: document.getElementById('btn-q-prev'),
  btnQNext: document.getElementById('btn-q-next'),
  qQuickStatus: document.getElementById('q-quick-status'),
  // Review elements
  reviewAnswersList: document.getElementById('review-answers-list'),
  revParticipantCode: document.getElementById('rev-participant-code'),
  revTestMode: document.getElementById('rev-test-mode'),
  revCompletionStat: document.getElementById('rev-completion-stat'),
  btnRevBack: document.getElementById('btn-rev-back'),
  btnSubmitFinal: document.getElementById('btn-submit-final'),
  // Results
  resTotalScore: document.getElementById('res-total-score'),
  resBandPill: document.getElementById('res-band-pill'),
  resPercentageText: document.getElementById('res-percentage-text'),
  resScoreSecA: document.getElementById('res-score-sec-a'),
  resScoreSecB: document.getElementById('res-score-sec-b'),
  resScoreSecC: document.getElementById('res-score-sec-c'),
  btnRestartAssessment: document.getElementById('btn-restart-assessment'),
  btnViewAllSubs: document.getElementById('btn-view-all-subs'),
  // Submissions Log
  submissionsTbody: document.getElementById('submissions-tbody'),
  filterTestType: document.getElementById('filter-test-type'),
  filterCodeSearch: document.getElementById('filter-code-search'),
  // Question Bank
  fullQuestionBankContainer: document.getElementById('full-question-bank-container'),
  // Modal
  submissionModal: document.getElementById('submission-modal'),
  btnCloseModal: document.getElementById('btn-close-modal'),
  modalTitle: document.getElementById('modal-title'),
  modalContent: document.getElementById('modal-content'),
  // Settings & Question Editor
  settingsSectionFilter: document.getElementById('settings-section-filter'),
  settingsQuestionsContainer: document.getElementById('settings-questions-container'),
  btnAddNewQuestion: document.getElementById('btn-add-new-question'),
  editQuestionModal: document.getElementById('edit-question-modal'),
  btnCloseEditModal: document.getElementById('btn-close-edit-modal'),
  btnCancelEdit: document.getElementById('btn-cancel-edit'),
  editQuestionForm: document.getElementById('edit-question-form'),
  editModalTitle: document.getElementById('edit-modal-title'),
  editQId: document.getElementById('edit-q-id'),
  editQSectionId: document.getElementById('edit-q-section-id'),
  editQPromptAm: document.getElementById('edit-q-prompt-am'),
  editQPromptEn: document.getElementById('edit-q-prompt-en'),
  editQType: document.getElementById('edit-q-type'),
  editQPoints: document.getElementById('edit-q-points'),
  editOptionsSection: document.getElementById('edit-options-section'),
  editOptionsList: document.getElementById('edit-options-list'),
  btnAddOptionRow: document.getElementById('btn-add-option-row'),
  editRubricSection: document.getElementById('edit-rubric-section'),
  editQRubricAm: document.getElementById('edit-q-rubric-am'),
  editQRubricEn: document.getElementById('edit-q-rubric-en')
};

// Initialize App
async function initApp() {
  // Apply stored/default language on load
  applyLanguageToUI(state.currentLang);
  elements.langBtns.am.classList.toggle('active', state.currentLang === 'am');
  elements.langBtns.en.classList.toggle('active', state.currentLang === 'en');
  setupEventListeners();
  await loadSurvey();
  await loadAnalytics();
  await loadSubmissions();
}

// Setup Event Listeners
function setupEventListeners() {
  // Tabs
  elements.tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const tabId = btn.getAttribute('data-tab');
      switchTab(tabId);
    });
  });

  // Language toggle
  elements.langBtns.am.addEventListener('click', () => setLanguage('am'));
  elements.langBtns.en.addEventListener('click', () => setLanguage('en'));

  // CSV Export
  elements.btnExportCsv.addEventListener('click', () => {
    if (state.currentSurvey) {
      window.location.href = `${API_BASE}/export/${state.currentSurvey.id}/csv`;
    }
  });

  // Dynamic Region -> Wereda & Kebele matcher
  const REGION_JURISDICTIONS = {
    'Dire Dawa': { wereda: 'Sabian Wereda', kebele: 'Kebele 02', prefix: 'DD', am: 'የድሬዳዋ አስተዳደር' },
    'Addis Ababa': { wereda: 'Bole Subcity', kebele: 'Wereda 03', prefix: 'AA', am: 'የአዲስ አበባ አስተዳደር' },
    'Afar': { wereda: 'Semera Wereda', kebele: 'Kebele 01', prefix: 'AF', am: 'የአፋር ክልል' },
    'Amhara': { wereda: 'Bahir Dar Wereda', kebele: 'Kebele 01', prefix: 'AM', am: 'የአማራ ክልል' },
    'Benishangul-Gumuz': { wereda: 'Assosa Wereda', kebele: 'Assosa 01', prefix: 'BG', am: 'የቤንሻንጉል ጉሙዝ ክልል' },
    'Central Ethiopia': { wereda: 'Hosanna Wereda', kebele: 'Kebele 01', prefix: 'CE', am: 'የማዕከላዊ ኢትዮጵያ ክልል' },
    'Gambela': { wereda: 'Gambela Wereda', kebele: 'Kebele 01', prefix: 'GM', am: 'የጋምቤላ ክልል' },
    'Harari': { wereda: 'Harar Wereda', kebele: 'Abadir', prefix: 'HR', am: 'የሐረሪ ክልል' },
    'Oromia': { wereda: 'Adama Wereda', kebele: 'Ganda 01', prefix: 'OR', am: 'የኦሮሚያ ክልል' },
    'Sidama': { wereda: 'Hawassa Wereda', kebele: 'Tabor', prefix: 'SD', am: 'የሲዳማ ክልል' },
    'Somali': { wereda: 'Jigjiga Wereda', kebele: 'Kebele 01', prefix: 'SM', am: 'የሶማሌ ክልል' },
    'South Ethiopia': { wereda: 'Wolaita Sodo Wereda', kebele: 'Kebele 01', prefix: 'SE', am: 'የደቡብ ኢትዮጵያ ክልል' },
    'South West Ethiopia Peoples\' Region': { wereda: 'Bonga Wereda', kebele: 'Kebele 01', prefix: 'SW', am: 'የደቡብ ምዕራብ ኢትዮጵያ ክልል' },
    'Tigray': { wereda: 'Mekelle Wereda', kebele: 'Hawelti', prefix: 'TG', am: 'የትግራይ ክልል' }
  };

  const selRegion = document.getElementById('select-region');
  const inpWereda = document.getElementById('input-wereda-kebele');
  const inpCode = document.getElementById('input-participant-code');
  const subHeading = document.getElementById('page-subheading');

  function matchRegionData(regionKey) {
    const data = REGION_JURISDICTIONS[regionKey];
    if (!data) return;
    if (inpWereda) inpWereda.value = data.wereda + ' / ' + data.kebele;
    if (inpCode && (!inpCode.value || inpCode.value.includes('-REG-'))) {
      inpCode.value = data.prefix + '-REG-01';
    }
    if (subHeading) {
      subHeading.textContent = state.currentLang === 'am' 
        ? data.am + ' የሲአርቪኤስ መዝጋቢዎች ስልጠና መመዘኛ ሥርዓት' 
        : regionKey + ' Administration CRVS Registrar Training Assessment System';
    }
  }

  if (selRegion) {
    selRegion.addEventListener('change', () => matchRegionData(selRegion.value));
  }

  // Post-Test Lock & Manager Unlock Logic
  const radioPostTest = document.getElementById('radio-test-post');
  const radioPre = document.getElementById('radio-test-pre');
  const spanPostLabel = document.getElementById('span-post-label');
  const lockNotice = document.getElementById('post-test-lock-notice');
  const btnAdminUnlock = document.getElementById('btn-admin-unlock-post');
  const labelPillPost = document.getElementById('label-pill-post');

  let postTestUnlocked = false;

  function applyPostTestLockState() {
    if (postTestUnlocked) {
      if (radioPostTest) radioPostTest.disabled = false;
      if (spanPostLabel) spanPostLabel.textContent = '🎯 Post-Test (ድህረ-ሙከራ)';
      if (lockNotice) lockNotice.style.display = 'none';
      if (labelPillPost) labelPillPost.style.opacity = '1';
      if (btnAdminUnlock) { btnAdminUnlock.textContent = '🔓 Post-Test Unlocked'; btnAdminUnlock.style.background = 'rgba(34,197,94,0.15)'; btnAdminUnlock.style.borderColor = 'rgba(34,197,94,0.4)'; btnAdminUnlock.style.color = '#4ade80'; }
    } else {
      if (radioPostTest) { radioPostTest.disabled = true; radioPostTest.checked = false; }
      if (radioPre) radioPre.checked = true;
      if (spanPostLabel) spanPostLabel.textContent = '🔒 ድህረ-ሙከራ (Locked)';
      if (lockNotice) lockNotice.style.display = 'block';
      if (labelPillPost) labelPillPost.style.opacity = '0.5';
      if (btnAdminUnlock) { btnAdminUnlock.textContent = '🔐 Manager Unlock'; btnAdminUnlock.style.background = 'rgba(99,102,241,0.12)'; btnAdminUnlock.style.borderColor = 'rgba(99,102,241,0.35)'; btnAdminUnlock.style.color = '#a5b4fc'; }
    }
  }

  // Initialize locked state on page load
  applyPostTestLockState();

  if (btnAdminUnlock) {
    btnAdminUnlock.addEventListener('click', () => {
      if (postTestUnlocked) {
        // Re-lock
        postTestUnlocked = false;
        applyPostTestLockState();
      } else {
        // Prompt manager pin/password
        const pin = prompt('ኃላፊ ወይም አስተዳዳሪ (Manager/Admin): ፈቃድ ኮድ ያስገቡ\n(Enter Manager authorization code to unlock Post-Test):\n\nDefault code: MANAGER2024', '');
        if (pin === null) return; // cancelled
        if (pin === 'MANAGER2024' || pin === 'ADMIN') {
          postTestUnlocked = true;
          applyPostTestLockState();
          // Show brief success toast
          const toast = document.createElement('div');
          toast.textContent = '✅ Post-Test unlocked by Manager';
          toast.style.cssText = 'position:fixed;bottom:24px;right:24px;background:#1e293b;border:1px solid #4ade80;color:#4ade80;padding:10px 18px;border-radius:10px;font-size:13px;font-weight:600;z-index:9999;box-shadow:0 4px 20px rgba(0,0,0,0.4);';
          document.body.appendChild(toast);
          setTimeout(() => toast.remove(), 3500);
        } else {
          alert('❌ ኮዱ ትክክለኛ አይደለም። ብቃት ያለው ኃላፊ ወይም አስተዳዳሪ ብቻ ድህረ-ሙከራ ሊከፍቱ ይችላሉ።\n(Incorrect code. Only authorized Managers or Admins can unlock Post-Test.)');
        }
      }
    });
  }

  // Prevent examinees from clicking locked post-test pill
  if (labelPillPost) {
    labelPillPost.addEventListener('click', (e) => {
      if (!postTestUnlocked) {
        e.preventDefault();
        alert('🔒 ድህረ-ሙከራ ተቆልፏል!\n\nተፈታኞች ቅድመ-ሙከራ (Pre-Test) ካጠናቀቁ በኋላ ኃላፊ (Manager) ሲፈቅዱ ብቻ ድህረ-ሙከራ (Post-Test) ሊወሰድ ይችላል።\n\n(Post-Test is locked. Complete Pre-Test first. Manager must authorize Post-Test.)');
      }
    });
  }

  // Wizard: Start Questions
  elements.btnStartQuestions.addEventListener('click', handleStartQuestions);

  // Wizard: Question Navigation
  elements.btnQPrev.addEventListener('click', () => navigateQuestion(-1));
  elements.btnQNext.addEventListener('click', () => navigateQuestion(1));

  // Speech TTS
  elements.btnSpeakQuestion.addEventListener('click', speakCurrentQuestion);

  // Review & Submit
  elements.btnRevBack.addEventListener('click', () => showWizardScreen('questions'));
  elements.btnSubmitFinal.addEventListener('click', handleSubmitSurvey);

  // Restart & View
  elements.btnRestartAssessment.addEventListener('click', resetWizard);
  elements.btnViewAllSubs.addEventListener('click', () => switchTab('submissions'));

  // Submissions Filters & PDF Export
  elements.filterTestType.addEventListener('change', loadSubmissions);
  elements.filterCodeSearch.addEventListener('input', debounce(loadSubmissions, 300));
  const btnExportAllPdf = document.getElementById('btn-export-all-submissions-pdf');
  if (btnExportAllPdf) {
    btnExportAllPdf.addEventListener('click', () => exportAllSubmissionsPdf());
  }

  // Modal Close
  elements.btnCloseModal.addEventListener('click', closeModal);
  elements.submissionModal.addEventListener('click', (e) => {
    if (e.target === elements.submissionModal) closeModal();
  });

  // Settings & Edit Modal Listeners
  if (elements.settingsSectionFilter) {
    elements.settingsSectionFilter.addEventListener('change', renderSettingsQuestions);
  }
  if (elements.btnAddNewQuestion) {
    elements.btnAddNewQuestion.addEventListener('click', openAddQuestionModal);
  }
  if (elements.btnCloseEditModal) {
    elements.btnCloseEditModal.addEventListener('click', closeEditModal);
  }
  if (elements.btnCancelEdit) {
    elements.btnCancelEdit.addEventListener('click', closeEditModal);
  }
  if (elements.editQuestionModal) {
    elements.editQuestionModal.addEventListener('click', (e) => {
      if (e.target === elements.editQuestionModal) closeEditModal();
    });
  }
  if (elements.editQType) {
    elements.editQType.addEventListener('change', (e) => {
      const type = e.target.value;
      elements.editOptionsSection.style.display = type === 'SINGLE_CHOICE' ? 'block' : 'none';
      elements.editRubricSection.style.display = type === 'TEXT_OPEN' ? 'block' : 'none';
    });
  }
  if (elements.btnAddOptionRow) {
    elements.btnAddOptionRow.addEventListener('click', () => {
      const rows = elements.editOptionsList.querySelectorAll('.edit-opt-row');
      const nextKey = ['ሀ', 'ለ', 'ሐ', 'መ', 'ሠ', 'ረ'][rows.length] || String.fromCharCode(65 + rows.length);
      addOptionRowToEditor(nextKey, '', '', false);
    });
  }
  if (elements.editQuestionForm) {
    elements.editQuestionForm.addEventListener('submit', handleSaveQuestion);
  }
}

// ── i18n Engine ──────────────────────────────────────────────────────────────
const I18N = {
  am: {
    nav_analytics: 'የትንተና ዳሽቦርድ', nav_wizard: 'የቀጥታ መመዘኛ ቅጽ',
    nav_submissions: 'የተላኩ ምላሾች እና ውጤቶች', nav_grading: 'ምላሾችን ማረም',
    nav_questions: 'የጥያቄዎች ባንክ', nav_settings: 'የጥያቄዎች ማስተካከያ',
    page_heading: 'የሲአርቪኤስ መመዘኛ ትንተና',
    page_subheading: 'የድሬዳዋ አስተዳደር የሲአርቪኤስ መዝጋቢዎች ስልጠና መመዘኛ ሥርዓት',
    btn_scan_qr: '📲 ባርኮድ ስካን', btn_download_apk: '📱 መተግበሪያውን አውርድ',
    btn_export_csv: 'መረጃውን በCSV አውርድ',
    kpi_total_subs: 'አጠቃላይ ተሳታፊዎች', kpi_pre_avg: 'የቅድመ-ስልጠና አማካይ',
    kpi_post_avg: 'የድህረ-ስልጠና አማካይ', kpi_delta: 'የእውቀት ዕድገት ምጣኔ',
    bands_title: 'የውጤት ደረጃዎች ስርጭት',
    pass_rates_title: 'የስልጠና ውጤታማነት እና የማለፊያ ምጣኔ',
    pre_pass_label: 'የቅድመ-ስልጠና ማለፊያ ምጣኔ', post_pass_label: 'የድህረ-ስልጠና ማለፊያ ምጣኔ',
    cert_pipeline_title: 'የምስክር ወረቀት ዝግጁነት እና የቁጥጥር አስተዳደር',
    cert_pipeline_desc: 'በአዋጅ ቁጥር 1370/2016 መሠረት (50% የማለፊያ ነጥብ)',
    cert_eligible_label: 'ማለፊያ መስፈርት ያሟሉ (≥ 50%)', cert_eligible_desc: 'የምስክር ወረቀት ለማግኘት ብቁ',
    cert_issued_label: 'የተሰጡ ይፋዊ የምስክር ወረቀቶች', cert_issued_desc: 'የተረጋገጡ እና የተፈረሙ',
    section_breakdown_title: 'የትምህርት ክፍሎች የብቃት ትንተና',
    section_breakdown_desc: 'የሕግ፣ የቴክኒክ፣ የተግባር እና የስነ-ምግባር እውቀት ደረጃን ይገመግማል',
    kebele_title: 'የቀበሌዎች እና ተቋማት ውጤት ደረጃ',
    kebele_desc: 'የቀበሌዎች ደረጃ በአማካይ ውጤት እና የማለፊያ ምጣኔ',
    role_title: 'የመዝጋቢዎች የሥራ ድርሻ ውጤት', role_desc: 'ውጤት በሙያዊ የሥራ መደብ',
    accuracy_title: 'የጥያቄዎች ትክክለኛ ምላሽ ትንተና',
    accuracy_desc: 'የተሳታፊዎችን ክፍተት እና የተካኑባቸውን አርእስቶች ይለያል',
    demog_title: 'የተሳታፊ እና የምዘና መረጃ',
    demog_desc: 'የጥያቄ ክፍለ-ጊዜውን ከመጀመርዎ በፊት የመዝጋቢውን መለያ መረጃ ያስገቡ።',
    lbl_participant_code: 'የተሳታፊ ኮድ *', lbl_wereda_kebele: 'ወረዳ / ቀበሌ / ተቋም',
    lbl_role: 'የሥራ ድርሻ', role_registrar: 'መዝጋቢ', role_health: 'የጤና ተቋም መዝጋቢ',
    role_supervisor: 'ተቆጣጣሪ', role_other: 'ሌላ', lbl_gender: 'ፆታ',
    gender_female: 'ሴት', gender_male: 'ወንድ', lbl_exp: 'በሥራ የቆዩበት ዓመት',
    lbl_mode: 'የሙከራ ዓይነት *', mode_pre: 'ቅድመ-ሙከራ (የመነሻ ደረጃ)',
    mode_post: 'ድህረ-ሙከራ (የእውቀት ብቃት)', btn_start_flow: 'መመዘኛውን ጀምር →',
    btn_listen: 'ጥያቄውን አድምጥ', btn_prev: '← ወደ ኋላ',
    btn_next: 'ቀጣይ ጥያቄ →', btn_review: 'መልሶችን ይከልሱ →', btn_submit: 'መልሶችን አስገባ',
    settings_title: 'የጥያቄዎች ማስተካከያ',
    settings_desc: 'የጥያቄዎች ጽሑፍ፣ የአማራጮች ዝርዝር፣ ትክክለኛ መልስ እና የነጥብ አሰጣጥ ማስተካከያ።',
    opt_all_sections: 'ሁሉም ክፍሎች', tab_signin: 'መግቢያ', tab_signup: 'መመዝገቢያ',
    lbl_username: 'የተጠቃሚ ስም', lbl_password: 'የይለፍ ቃል', btn_signin_submit: 'ይግቡ',
    lbl_fullname: 'ሙሉ ስም', lbl_account_role: 'ሚና',
    role_opt_examinee: '🎓 ተፈታኝ', role_opt_manager: '👥 የተጠቃሚዎች አስተዳዳሪ',
    role_opt_admin: '🛡️ ሥርዓት አስተዳዳሪ', btn_signup_submit: 'ይመዝገቡ',
    profile_desc: 'የተፈታኝ መገለጫ እና ኦፊሴላዊ የምስክር ወረቀት ማዕከል'
  },
  en: {
    nav_analytics: 'Analytics & Overview', nav_wizard: 'Ask Questions (Live Flow)',
    nav_submissions: 'Submissions & Scores', nav_grading: 'Grade Responses',
    nav_questions: 'Question Accuracy Bank', nav_settings: 'Settings & Question Editor',
    page_heading: 'CRVS Assessment Analytics',
    page_subheading: 'Dire Dawa Administration CRVS Registrar Training Assessment System',
    btn_scan_qr: '📲 Scan Barcode', btn_download_apk: '📱 Download APK',
    btn_export_csv: 'Export CSV', kpi_total_subs: 'Total Assessors',
    kpi_pre_avg: 'Pre-Test Mean', kpi_post_avg: 'Post-Test Mean', kpi_delta: 'Knowledge Gain',
    bands_title: 'Performance Distribution Bands',
    pass_rates_title: 'Training Effectiveness & Pass Rates',
    pre_pass_label: 'Pre-Test Pass Rate', post_pass_label: 'Post-Test Pass Rate',
    cert_pipeline_title: 'Certificate Readiness & Regulatory Governance',
    cert_pipeline_desc: 'Governed under Civil Proclamation No. 1370/2024 (50% passing threshold)',
    cert_eligible_label: 'Benchmark Satisfied (≥ 50%)', cert_eligible_desc: 'Eligible for certificate issuance',
    cert_issued_label: 'Official Certificates Issued', cert_issued_desc: 'Approved & serial signed',
    section_breakdown_title: 'Curriculum Section Competency Analysis',
    section_breakdown_desc: 'Evaluates training efficacy across legal, technical, practical, and ethics domains',
    kebele_title: 'Kebele & Facility Performance',
    kebele_desc: 'Regional ranking by average score and pass rate',
    role_title: 'Registrar Role & Cadre Performance', role_desc: 'Performance breakdown by professional designation',
    accuracy_title: 'Question Accuracy Analysis', accuracy_desc: 'Identifies comprehension gaps and topic mastery',
    demog_title: 'Participant & Assessment Setup',
    demog_desc: 'Enter registrar identification details before starting the question session.',
    lbl_participant_code: 'Participant Code *', lbl_wereda_kebele: 'Wereda / Kebele / Facility',
    lbl_role: 'Role', role_registrar: 'Registrar', role_health: 'Health Registrar',
    role_supervisor: 'Supervisor', role_other: 'Other', lbl_gender: 'Gender',
    gender_female: 'Female', gender_male: 'Male', lbl_exp: 'Years in Civil Registration',
    lbl_mode: 'Assessment Mode *', mode_pre: 'Pre-Test (Baseline)', mode_post: 'Post-Test (Mastery)',
    btn_start_flow: 'Start Assessment →', btn_listen: 'Listen Prompt',
    btn_prev: '← Previous', btn_next: 'Next Question →',
    btn_review: 'Review Answers →', btn_submit: 'Submit Answers',
    settings_title: 'Settings & Question Bank Editor',
    settings_desc: 'Live editing of question prompts, multiple choice options, correct answers, points, and rubrics.',
    opt_all_sections: 'All Sections', tab_signin: 'Sign In', tab_signup: 'Sign Up',
    lbl_username: 'Username', lbl_password: 'Password', btn_signin_submit: 'Sign In',
    lbl_fullname: 'Full Name', lbl_account_role: 'Account Role',
    role_opt_examinee: '🎓 Examinee', role_opt_manager: '👥 User Manager',
    role_opt_admin: '🛡️ Admin', btn_signup_submit: 'Create Account',
    profile_desc: 'Examinee Profile & Official Certificate Center'
  }
};

function applyLanguageToUI(lang) {
  const dict = I18N[lang] || I18N.am;
  // Update all data-i18n tagged elements
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (dict[key] !== undefined) el.textContent = dict[key];
  });
  // Update sidebar navigation labels
  const navMap = {
    'dashboard': dict.nav_analytics, 'interactive-wizard': dict.nav_wizard,
    'submissions': dict.nav_submissions, 'grade-responses': dict.nav_grading,
    'question-bank': dict.nav_questions, 'settings': dict.nav_settings
  };
  document.querySelectorAll('.nav-item').forEach(btn => {
    const tab = btn.getAttribute('data-tab');
    if (navMap[tab]) { const sp = btn.querySelector('span'); if (sp) sp.textContent = navMap[tab]; }
  });
}
// ─────────────────────────────────────────────────────────────────────────────

function setLanguage(lang) {
  state.currentLang = lang;
  try { localStorage.setItem('crvs_selected_lang', lang); } catch(_) {}
  elements.langBtns.am.classList.toggle('active', lang === 'am');
  elements.langBtns.en.classList.toggle('active', lang === 'en');
  applyLanguageToUI(lang);
  if (state.currentSurvey) {
    renderCurrentQuestion();
    renderQuestionAccuracyTable();
    renderQuestionBank();
    renderSettingsQuestions();
  }
  if (state.analytics) {
    renderAnalytics(state.analytics);
  }
}

function switchTab(tabId) {
  state.activeTab = tabId;
  elements.tabButtons.forEach(btn => btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId));
  elements.tabPanes.forEach(pane => pane.classList.toggle('active', pane.id === `tab-${tabId}`));
  if (tabId === 'settings') {
    renderSettingsQuestions();
  }
}

// Load Survey Data
async function loadSurvey() {
  try {
    const res = await fetch(`${API_BASE}/surveys/1`);
    const json = await res.json();
    if (json.success && json.data) {
      state.currentSurvey = json.data;
      flattenQuestions(json.data);
      renderQuestionBank();
      renderSettingsQuestions();
    }
  } catch (err) {
    console.error('Failed to load survey:', err);
  }
}

function flattenQuestions(survey) {
  const flat = [];
  if (survey.sections) {
    for (const sec of survey.sections) {
      if (sec.questions) {
        for (const q of sec.questions) {
          flat.push({
            ...q,
            sectionCode: sec.code,
            sectionTitleAm: sec.title_am,
            sectionTitleEn: sec.title_en
          });
        }
      }
    }
  }
  state.wizard.flatQuestions = flat;
}

// Load Analytics
async function loadAnalytics() {
  if (!state.currentSurvey) return;
  try {
    const res = await fetch(`${API_BASE}/analytics/${state.currentSurvey.id}`);
    const json = await res.json();
    if (json.success && json.data) {
      state.analytics = json.data;
      renderAnalytics();
    }
  } catch (err) {
    console.error('Failed to load analytics:', err);
  }
}

function renderAnalytics() {
  const a = state.analytics;
  if (!a) return;

  elements.kpiTotalSubs.textContent = a.total_submissions;
  elements.kpiSubsBreakdown.textContent = `${a.pre_test_count} Pre-Tests · ${a.post_test_count} Post-Tests`;
  elements.kpiPreAvg.textContent = `${a.avg_pre_score} / 24`;
  elements.kpiPostAvg.textContent = `${a.avg_post_score} / 24`;

  const deltaStr = a.avg_improvement_delta >= 0 ? `+${a.avg_improvement_delta}%` : `${a.avg_improvement_delta}%`;
  elements.kpiDelta.textContent = deltaStr;

  // Pass rates
  elements.labelPassPre.textContent = `${a.pass_rate_pre}%`;
  elements.barPassPre.style.width = `${a.pass_rate_pre}%`;
  elements.labelPassPost.textContent = `${a.pass_rate_post}%`;
  elements.barPassPost.style.width = `${a.pass_rate_post}%`;

  // Bands
  elements.bandListContainer.innerHTML = `
    <div class="band-item band-strong">
      <div class="band-info">
        <span class="band-name">Strong (ጠንካራ)</span>
        <span class="band-range">Score 17–20 / 24 (High Mastery)</span>
      </div>
      <div class="band-count">${a.performance_bands.strong}</div>
    </div>
    <div class="band-item band-satisfactory">
      <div class="band-info">
        <span class="band-name">Satisfactory (አጥጋቢ)</span>
        <span class="band-range">Score 13–16 / 24 (Adequate Competence)</span>
      </div>
      <div class="band-count">${a.performance_bands.satisfactory}</div>
    </div>
    <div class="band-item band-reinforcement">
      <div class="band-info">
        <span class="band-name">Needs Reinforcement (የማጠናከሪያ ድጋፍ)</span>
        <span class="band-range">Score 9–12 / 24 (Follow-up Mentoring)</span>
      </div>
      <div class="band-count">${a.performance_bands.needs_reinforcement}</div>
    </div>
    <div class="band-item band-intensive">
      <div class="band-info">
        <span class="band-name">Needs Intensive Support (ጠንከር ያለ ክትትል)</span>
        <span class="band-range">Score 0–8 / 24 (Urgent Re-training)</span>
      </div>
      <div class="band-count">${a.performance_bands.needs_intensive_support}</div>
    </div>
  `;

    // 1. Certification Pipeline
  if (a.certification_pipeline) {
    const pipe = a.certification_pipeline;
    const elEligible = document.getElementById('pipeline-eligible');
    const elApproved = document.getElementById('pipeline-approved');
    const elPending = document.getElementById('pipeline-pending');
    const elReinf = document.getElementById('pipeline-reinforcement');
    const badgeSummary = document.getElementById('badge-cert-summary');
    if (elEligible) elEligible.textContent = pipe.total_eligible;
    if (elApproved) elApproved.textContent = pipe.total_approved;
    if (elPending) elPending.textContent = pipe.pending_approval;
    if (elReinf) elReinf.textContent = pipe.needs_reinforcement;
    if (badgeSummary) badgeSummary.textContent = `${pipe.total_approved} Issued · ${pipe.pending_approval} Awaiting Review`;
  }

  // 2. Section Competency Breakdown
  const secContainer = document.getElementById('section-breakdown-container');
  if (secContainer && a.section_breakdown) {
    secContainer.innerHTML = a.section_breakdown.map(sec => {
      const impColor = sec.improvement_pct >= 0 ? '#34d399' : '#f87171';
      const impSign = sec.improvement_pct >= 0 ? `+${sec.improvement_pct}%` : `${sec.improvement_pct}%`;
      return `
        <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 14px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
            <div>
              <span class="badge badge-primary" style="font-size: 10px;">${sec.section_code}</span>
              <h4 style="margin: 4px 0 2px; font-size: 13px; color: #f1f5f9;">${state.currentLang === 'am' ? escapeHtml(sec.title_am) : escapeHtml(sec.title_en)}</h4>
              <span style="font-size: 11px; color: var(--text-dim);">Max: ${sec.max_points} pts</span>
            </div>
            <span class="badge" style="background: rgba(16,185,129,0.15); color: ${impColor}; font-weight: 700;">${impSign}</span>
          </div>
          
          <div style="display: flex; flex-direction: column; gap: 8px; margin-top: 10px;">
            <div>
              <div style="display: flex; justify-content: space-between; font-size: 11px; color: #94a3b8; margin-bottom: 3px;">
                <span>Pre-Test: ${sec.pre_avg_score} pts</span>
                <span>${sec.pre_avg_pct}%</span>
              </div>
              <div class="progress-bar-bg" style="height: 6px;">
                <div class="progress-bar-fill fill-amber" style="width: ${sec.pre_avg_pct}%;"></div>
              </div>
            </div>
            <div>
              <div style="display: flex; justify-content: space-between; font-size: 11px; color: #94a3b8; margin-bottom: 3px;">
                <span>Post-Test: ${sec.post_avg_score} pts</span>
                <span style="color: #34d399; font-weight: 600;">${sec.post_avg_pct}%</span>
              </div>
              <div class="progress-bar-bg" style="height: 6px;">
                <div class="progress-bar-fill fill-green" style="width: ${sec.post_avg_pct}%;"></div>
              </div>
            </div>
          </div>
        </div>
      `;
    }).join('');
  }

  // 3. Kebele / Facility League Table
  const facTbody = document.getElementById('facility-breakdown-tbody');
  if (facTbody && a.facility_breakdown) {
    if (!a.facility_breakdown.length) {
      facTbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color: var(--text-dim);">No kebele data recorded yet.</td></tr>';
    } else {
      facTbody.innerHTML = a.facility_breakdown.map((f, i) => {
        const passColor = f.pass_rate_pct >= 60 ? '#34d399' : (f.pass_rate_pct >= 50 ? '#fbbf24' : '#f87171');
        return `
          <tr>
            <td><strong>#${i + 1}</strong> &nbsp;${escapeHtml(f.facility)}</td>
            <td><span class="badge">${f.total_submissions}</span></td>
            <td><strong>${f.avg_score_pct}%</strong></td>
            <td><strong style="color: ${passColor};">${f.pass_rate_pct}%</strong></td>
            <td><span class="badge badge-success">🏆 ${f.certified_count}</span></td>
          </tr>
        `;
      }).join('');
    }
  }

  // 4. Role & Cadre Breakdown
  const roleContainer = document.getElementById('role-breakdown-container');
  if (roleContainer && a.role_breakdown) {
    roleContainer.innerHTML = a.role_breakdown.map(r => {
      return `
        <div style="background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); border-radius: 8px; padding: 10px 14px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
            <span style="font-weight: 600; font-size: 13px; color: #e2e8f0;">${escapeHtml(r.role)}</span>
            <span class="badge badge-primary">${r.total_count} examinees</span>
          </div>
          <div style="display: flex; justify-content: space-between; font-size: 11px; color: #94a3b8; margin-bottom: 4px;">
            <span>Avg Score: <strong>${r.avg_score_pct}%</strong></span>
            <span>Pass Rate: <strong style="color: ${r.pass_rate_pct >= 50 ? '#34d399' : '#f87171'}">${r.pass_rate_pct}%</strong></span>
          </div>
          <div class="progress-bar-bg" style="height: 6px;">
            <div class="progress-bar-fill fill-green" style="width: ${r.avg_score_pct}%;"></div>
          </div>
        </div>
      `;
    }).join('');
  }

  renderQuestionAccuracyTable();
}

function renderQuestionAccuracyTable() {
  if (!state.analytics?.question_accuracy) return;
  const isAm = state.currentLang === 'am';
  
  elements.questionAccuracyTbody.innerHTML = state.analytics.question_accuracy.map(q => {
    const prompt = isAm ? q.prompt_am : q.prompt_en;
    const badge = q.needs_revision 
      ? `<span class="badge badge-danger">Revise Topic (<60%)</span>`
      : `<span class="badge badge-success">Mastered</span>`;

    return `
      <tr>
        <td><strong>#${q.question_number}</strong></td>
        <td><span class="badge badge-primary">${q.section_code}</span></td>
        <td style="max-width: 380px;">${escapeHtml(prompt)}</td>
        <td><strong>${q.pre_accuracy_pct}%</strong></td>
        <td><strong style="color: ${q.post_accuracy_pct >= 60 ? '#34d399' : '#f87171'}">${q.post_accuracy_pct}%</strong></td>
        <td>${badge}</td>
      </tr>
    `;
  }).join('');
}

// Load Submissions
async function loadSubmissions() {
  if (!state.currentSurvey) return;
  try {
    const testType = elements.filterTestType.value;
    const search = elements.filterCodeSearch.value;

    let url = `${API_BASE}/submissions?survey_id=${state.currentSurvey.id}`;
    if (testType) url += `&test_type=${encodeURIComponent(testType)}`;
    if (search) url += `&participant_code=${encodeURIComponent(search)}`;

    const res = await fetch(url);
    const json = await res.json();
    if (json.success) {
      state.submissions = json.data;
      renderSubmissionsTable();
    }
  } catch (err) {
    console.error('Failed to load submissions:', err);
  }
}

function renderSubmissionsTable() {
  if (!state.submissions.length) {
    elements.submissionsTbody.innerHTML = `<tr><td colspan="10" style="text-align: center; color: var(--text-dim);">No submissions found matching criteria.</td></tr>`;
    return;
  }

  const currentUser = JSON.parse(localStorage.getItem('qcafr_user') || 'null');
  const isManagerOrAdmin = currentUser && (currentUser.role === 'ADMIN' || currentUser.role === 'USER_MANAGER');

  elements.submissionsTbody.innerHTML = state.submissions.map(s => {
    const bandBadge = s.performance_band === 'STRONG' ? 'badge-success'
      : s.performance_band === 'SATISFACTORY' ? 'badge-primary'
      : s.performance_band === 'NEEDS_REINFORCEMENT' ? 'badge-warning' : 'badge-danger';

    const testBadge = s.test_type === 'POST' ? 'badge-success' : 'badge-warning';
    const canCertify = s.score_percentage >= 50;
    const approvalStatus = s.approval_status || 'PENDING';

    let approvalBadge = '';
    if (approvalStatus === 'APPROVED') {
      approvalBadge = `<span class="badge badge-success" style="font-size:10px;">✓ APPROVED</span>`;
    } else if (approvalStatus === 'REJECTED') {
      approvalBadge = `<span class="badge badge-danger" style="font-size:10px;">✗ REJECTED</span>`;
    } else if (canCertify) {
      approvalBadge = `<span class="badge badge-warning" style="font-size:10px;">Awaiting Approval</span>`;
    } else {
      approvalBadge = `<span style="font-size:10px; color: var(--text-dim);">Score < 50%</span>`;
    }

    let actionButtons = `
      <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 11px;" onclick="viewSubmissionDetail(${s.id})">
        Inspect
      </button>
      <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 11px; margin-left: 3px; color: #818cf8;" onclick="exportSingleSubmissionPdf(${s.id})" title="Download PDF Report">
        📄 PDF
      </button>
    `;

    if (isManagerOrAdmin) {
      if (approvalStatus === 'APPROVED') {
        actionButtons += `
          <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 11px; margin-left: 3px; color: #34d399;" onclick="exportCertificatePdf(${s.id})" title="Download Certificate">
            🏆 Certificate
          </button>
        `;
      } else if (canCertify && approvalStatus !== 'REJECTED') {
        actionButtons += `
          <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 11px; margin-left: 3px; color: #34d399;" onclick="approveSubmissionAction(${s.id}, '${escapeHtml(s.participant_code)}')" title="Approve &amp; Issue Certificate">
            ✅ Approve
          </button>
          <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 11px; margin-left: 3px; color: #f59e0b;" onclick="rejectSubmissionAction(${s.id}, '${escapeHtml(s.participant_code)}')" title="Reject Submission">
            ❌ Reject
          </button>
        `;
      }
      actionButtons += `
        <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 11px; margin-left: 3px; color: #ef4444;" onclick="deleteSubmissionPrompt(${s.id}, '${escapeHtml(s.participant_code)}')" title="Delete Submission">
          🗑️
        </button>
      `;
    }

    return `
      <tr>
        <td>#${s.id}</td>
        <td><strong>${escapeHtml(s.participant_code)}</strong></td>
        <td>${escapeHtml(s.wereda_kebele || 'N/A')} · ${escapeHtml(s.role || '')}</td>
        <td><span class="badge ${testBadge}">${s.test_type}</span></td>
        <td><strong>${s.total_score}</strong> / ${s.max_score}</td>
        <td><strong>${s.score_percentage}%</strong></td>
        <td><span class="badge ${bandBadge}">${s.performance_band}</span></td>
        <td>${approvalBadge}</td>
        <td>${new Date(s.submitted_at).toLocaleDateString()}</td>
        <td style="white-space: nowrap;">${actionButtons}</td>
      </tr>
    `;
  }).join('');
}

// Approve Submission — triggers certificate issuance
window.approveSubmissionAction = async function(id, code) {
  const currentUser = JSON.parse(localStorage.getItem('qcafr_user') || 'null');
  if (!currentUser || (currentUser.role !== 'ADMIN' && currentUser.role !== 'USER_MANAGER')) {
    alert('Only managers and admins can approve submissions.');
    return;
  }
  if (!confirm(`Approve submission for "${code}" (ID #${id}) and issue an official certificate?\n\nThis will generate a unique certificate number and send it to the examinee profile.`)) return;
  try {
    const res = await fetch(`${API_BASE}/submissions/${id}/approve`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ approved_by: currentUser.full_name || currentUser.username })
    });
    const json = await res.json();
    if (res.ok && json.success) {
      alert(`✅ Certificate issued!\nCertificate No.: ${json.data.certificate_number}\n\nExaminee "${code}" can now download their certificate from their profile.`);
      await loadSubmissions();
      // Offer to print certificate immediately
      if (confirm('Would you like to print/download the certificate now?')) {
        exportCertificatePdf(id);
      }
    } else {
      alert(`Failed to approve: ${json.error}`);
    }
  } catch (e) {
    alert(`Error: ${e.message}`);
  }
};

// Reject Submission
window.rejectSubmissionAction = async function(id, code) {
  const currentUser = JSON.parse(localStorage.getItem('qcafr_user') || 'null');
  if (!currentUser || (currentUser.role !== 'ADMIN' && currentUser.role !== 'USER_MANAGER')) {
    alert('Only managers and admins can reject submissions.');
    return;
  }
  if (!confirm(`Reject the submission for "${code}" (ID #${id})?\n\nNo certificate will be issued.`)) return;
  try {
    const res = await fetch(`${API_BASE}/submissions/${id}/reject`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rejected_by: currentUser.full_name || currentUser.username })
    });
    const json = await res.json();
    if (res.ok && json.success) {
      alert(`Submission #${id} for "${code}" has been rejected.`);
      await loadSubmissions();
    } else {
      alert(`Failed to reject: ${json.error}`);
    }
  } catch (e) {
    alert(`Error: ${e.message}`);
  }
};

// Export Official Certificate PDF (matching exact Birth/Civil Certificate template)
window.exportCertificatePdf = async function(submissionId) {
  try {
    let cert = null;
    const res = await fetch(`${API_BASE}/submissions/${submissionId}/certificate`);
    const json = await res.json();
    if (res.ok && json.success && json.data) {
      cert = json.data;
    } else {
      const subRes = await fetch(`${API_BASE}/submissions/${submissionId}`);
      const subJson = await subRes.json();
      if (subRes.ok && subJson.success && subJson.data) {
        const s = subJson.data;
        cert = {
          certificate_number: s.certificate_number || `CRVS-2026-${Math.random().toString(36).substring(2, 10).toUpperCase()}`,
          full_name: s.participant_code || s.participant_id || 'CRVS Candidate',
          participant_code: s.participant_code || `P-${submissionId}`,
          wereda_kebele: s.wereda_kebele || 'Dire Dawa Administration',
          role: s.role || 'Certified Vital Events Registrar',
          gender: s.gender || 'Female',
          survey_title_en: s.survey_title_en || 'Dire Dawa CRVS Registrar Competency Assessment',
          survey_title_am: s.survey_title_am || 'የድሬዳዋ ወሳኝ ሁነቶች ምዝገባ ብቃት ምዘና',
          test_type: s.test_type || 'POST-TEST',
          score_percentage: s.score_percentage != null ? s.score_percentage : 85,
          total_score: s.total_score != null ? s.total_score : 20,
          max_score: s.max_score != null ? s.max_score : 24,
          performance_band: s.performance_band || 'STRONG',
          issued_by: s.approved_by || 'Dire Dawa CRVS Agency Director',
          issued_at: s.submitted_at || new Date().toISOString(),
          submission_id: s.id || submissionId
        };
      } else {
        alert(json.error || 'Certificate not found. Please approve the submission first.');
        return;
      }
    }
    const printWin = window.open('', '_blank', 'width=900,height=1200');
    if (!printWin) { alert('Pop-ups blocked. Please allow pop-ups for this site.'); return; }

    const issuedDate = new Date(cert.issued_at || Date.now());
    const dateStr = issuedDate.toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const fullDateStr = dateStr;
    const timeStr = issuedDate.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });

    const docNo = cert.certificate_number || `CRVS-2026-${Math.random().toString(36).substring(2, 10).toUpperCase()}`;
    const participantCode = cert.participant_code || 'REG-DD-001';
    const rawFullName = (cert.full_name || cert.participant_code || 'CRVS Candidate').trim();
    const nameParts = rawFullName.split(/\s+/);
    let surname = participantCode;
    let forenames = rawFullName;
    if (nameParts.length > 1) {
      surname = nameParts[nameParts.length - 1];
      forenames = nameParts.slice(0, -1).join(' ');
    } else {
      surname = participantCode;
      forenames = rawFullName;
    }

    const placeOfRegistration = cert.wereda_kebele || 'Dire Dawa City Administration';
    const rawGender = String(cert.gender || 'Female');
    const gender = (rawGender.toUpperCase().startsWith('M') || rawGender.includes('ወንድ')) ? 'Male (ወንድ)' : 'Female (ሴት)';
    const role = cert.role || 'Certified Vital Events Registrar';
    const surveyTitleEn = cert.survey_title_en || 'Dire Dawa CRVS Registrar Competency Assessment';
    const testType = (cert.test_type || 'POST-TEST').toUpperCase();
    const scorePct = Math.round(Number(cert.score_percentage) || 0);
    const totalScore = Number(cert.total_score) || 0;
    const maxScore = Number(cert.max_score) || 24;
    const performanceBand = cert.performance_band || (scorePct >= 75 ? 'STRONG' : (scorePct >= 50 ? 'SATISFACTORY' : 'NEEDS_REINFORCEMENT'));
    const issuingAuthority = 'Dire Dawa City Administration CRVS Agency (የድሬዳዋ አስተዳደር ወሳኝ ሁነቶች ምዝገባ ኤጀንሲ)';
    const placeOfIssuance = 'Dire Dawa, Ethiopia';
    const issuingOfficer = cert.issued_by || 'Dire Dawa CRVS Agency Director';
    const recordId = `CRVS-REC-${cert.submission_id || submissionId}`;
    const credentialNumber = docNo;

    printWin.document.write(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>CRVS Official Certificate - ${docNo}</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700;800;900&family=Playfair+Display:ital,wght@0,600;0,700;1,600&family=Inter:wght@400;500;600;700&family=Noto+Sans+Ethiopic:wght@400;600;700&display=swap" rel="stylesheet">
  <style>
    @page {
      size: A4 portrait;
      margin: 0;
    }
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
      color-adjust: exact !important;
    }
    body {
      background: #e2e8f0;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px 0 40px;
      font-family: 'Inter', 'Noto Sans Ethiopic', sans-serif;
      color: #0c2340;
    }
    .no-print {
      position: sticky;
      top: 12px;
      z-index: 9999;
      background: #003366;
      color: #ffffff;
      padding: 12px 28px;
      border-radius: 10px;
      box-shadow: 0 6px 20px rgba(0,0,0,0.3);
      display: flex;
      align-items: center;
      gap: 18px;
      margin-bottom: 22px;
      font-weight: 600;
      font-size: 14px;
    }
    .no-print button {
      background: #f59e0b;
      color: #001f3f;
      border: none;
      padding: 9px 24px;
      font-size: 14px;
      font-weight: 800;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s;
      box-shadow: 0 2px 8px rgba(0,0,0,0.2);
    }
    .no-print button:hover {
      background: #fbbf24;
      transform: translateY(-1px);
    }
    .cert-sheet {
      width: 210mm;
      height: 297mm;
      position: relative;
      background-color: #ffffff;
      background-image: url('/certificate_template.png');
      background-size: 100% 100%;
      background-position: center;
      background-repeat: no-repeat;
      overflow: hidden;
      box-shadow: 0 16px 40px rgba(0,0,0,0.25);
    }
    .cert-container {
      position: absolute;
      top: 22mm;
      bottom: 20mm;
      left: 20mm;
      right: 20mm;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      text-align: center;
    }
    .cert-top-header {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .emblem-svg {
      width: 50px;
      height: 50px;
      margin-bottom: 4px;
    }
    .gov-name {
      font-family: 'Cinzel', serif;
      font-size: 11.5px;
      font-weight: 800;
      letter-spacing: 2px;
      color: #003366;
      text-transform: uppercase;
    }
    .gov-amharic {
      font-family: 'Noto Sans Ethiopic', sans-serif;
      font-size: 10.5px;
      font-weight: 700;
      color: #004c99;
      margin-top: 1px;
    }
    .agency-name {
      font-family: 'Cinzel', serif;
      font-size: 10px;
      font-weight: 700;
      letter-spacing: 1.2px;
      color: #1e3a8a;
      margin-top: 2px;
      text-transform: uppercase;
    }
    .agency-amharic {
      font-family: 'Noto Sans Ethiopic', sans-serif;
      font-size: 9px;
      color: #2563eb;
    }
    .meta-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      width: 100%;
      margin-top: 6px;
      padding: 3px 8px;
      border-top: 1px dashed rgba(0, 76, 153, 0.35);
      border-bottom: 1px dashed rgba(0, 76, 153, 0.35);
      font-size: 9px;
      color: #1e3a8a;
    }
    .meta-item {
      display: flex;
      align-items: center;
      gap: 5px;
    }
    .meta-val {
      font-family: 'Courier New', monospace;
      font-weight: 800;
      color: #002b66;
      background: rgba(0, 82, 156, 0.08);
      padding: 1px 6px;
      border-radius: 4px;
      border: 1px solid rgba(0, 82, 156, 0.25);
    }
    .title-box {
      margin-top: 6px;
    }
    .title-h1 {
      font-family: 'Cinzel', serif;
      font-size: 23px;
      font-weight: 900;
      letter-spacing: 3px;
      color: #003366;
      text-transform: uppercase;
      line-height: 1.15;
    }
    .title-h1-am {
      font-family: 'Noto Sans Ethiopic', sans-serif;
      font-size: 14.5px;
      font-weight: 700;
      color: #00529c;
      margin-top: 2px;
    }
    .proclamation-tag {
      font-size: 8px;
      font-weight: 600;
      letter-spacing: 1px;
      text-transform: uppercase;
      color: #64748b;
      margin-top: 2px;
    }
    .certifies-prompt {
      margin-top: 6px;
      font-size: 10px;
      font-style: italic;
      color: #475569;
      letter-spacing: 0.5px;
    }
    .recipient-name-box {
      margin: 3px 0 5px;
      padding: 2px 10px;
    }
    .recipient-name {
      font-family: 'Playfair Display', serif;
      font-size: 26px;
      font-weight: 700;
      color: #002244;
      letter-spacing: 0.8px;
      line-height: 1.2;
    }
    .recipient-name-underline {
      width: 55%;
      height: 2px;
      margin: 3px auto 0;
      background: linear-gradient(90deg, transparent, #00529c, #d97706, #00529c, transparent);
    }
    .trainee-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 6px;
      width: 100%;
      margin: 6px 0;
      background: rgba(240, 246, 252, 0.7);
      border: 1px solid rgba(0, 82, 156, 0.2);
      border-radius: 6px;
      padding: 4px 8px;
    }
    .grid-cell {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
    }
    .grid-label {
      font-size: 7.5px;
      text-transform: uppercase;
      font-weight: 700;
      color: #475569;
      letter-spacing: 0.5px;
    }
    .grid-val {
      font-size: 9.5px;
      font-weight: 700;
      color: #0f172a;
      margin-top: 1px;
    }
    .cert-narrative {
      font-size: 9.5px;
      line-height: 1.45;
      color: #334155;
      margin: 4px 0;
      padding: 0 6px;
    }
    .program-highlight {
      font-family: 'Cinzel', serif;
      font-weight: 700;
      color: #003366;
      font-size: 10.5px;
    }
    .program-highlight-am {
      font-family: 'Noto Sans Ethiopic', sans-serif;
      font-weight: 700;
      color: #00529c;
      font-size: 10px;
    }
    .score-badge-row {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 10px;
      margin: 5px 0;
    }
    .badge-pill {
      background: #ffffff;
      border: 1.5px solid #00529c;
      border-radius: 999px;
      padding: 3px 12px;
      font-size: 9px;
      font-weight: 700;
      color: #00529c;
      box-shadow: 0 2px 6px rgba(0, 82, 156, 0.08);
    }
    .badge-pill.strong {
      background: #ecfdf5;
      border-color: #059669;
      color: #047857;
    }
    .badge-pill.gold {
      background: #fffbeb;
      border-color: #d97706;
      color: #b45309;
    }
    .sig-seal-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      width: 100%;
      margin-top: 10px;
      padding: 0 10px;
    }
    .sig-column {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 32%;
    }
    .sig-script {
      font-family: 'Playfair Display', italic, serif;
      font-style: italic;
      font-size: 16px;
      color: #003366;
      height: 20px;
      display: flex;
      align-items: flex-end;
    }
    .sig-rule {
      width: 100%;
      border-bottom: 1.5px solid #003366;
      margin: 3px 0;
    }
    .sig-officer-title {
      font-size: 8px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: #003366;
    }
    .sig-officer-name {
      font-size: 7.5px;
      color: #64748b;
      margin-top: 1px;
    }
    .seal-column {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 28%;
    }
    .seal-badge-svg {
      width: 76px;
      height: 76px;
    }
    .cert-footer-security {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;
      margin-top: 6px;
      padding-top: 4px;
      border-top: 1px solid rgba(0, 76, 153, 0.2);
    }
    .barcode-display {
      font-family: 'Courier New', monospace;
      letter-spacing: 5px;
      font-weight: 800;
      font-size: 9px;
      color: #002b66;
    }
    .security-notice {
      font-size: 7px;
      color: #64748b;
      letter-spacing: 0.5px;
      text-transform: uppercase;
    }
    @media print {
      body { background: transparent; padding: 0; }
      .no-print { display: none !important; }
      .cert-sheet { box-shadow: none; }
    }
  </style>
</head>
<body>

  <div class="no-print">
    <span>🏛️ Dire Dawa CRVS Competency Certificate</span>
    <button onclick="window.print()">🖨️ Print / Save as PDF</button>
  </div>

  <div class="cert-sheet">
    <div class="cert-container">

      <!-- TOP OFFICIAL HEADER -->
      <div class="cert-top-header">
        <!-- Ethiopian Coat of Arms / Legal Scale Emblem SVG -->
        <svg class="emblem-svg" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="50" cy="50" r="46" stroke="#004c99" stroke-width="2.5" fill="#f0f6fc"/>
          <circle cx="50" cy="50" r="41" stroke="#d97706" stroke-width="1" stroke-dasharray="2 2"/>
          <!-- Scale of justice beam -->
          <line x1="28" y1="36" x2="72" y2="36" stroke="#003366" stroke-width="3" stroke-linecap="round"/>
          <circle cx="50" cy="36" r="4" fill="#d97706"/>
          <line x1="50" y1="36" x2="50" y2="74" stroke="#003366" stroke-width="3"/>
          <line x1="36" y1="74" x2="64" y2="74" stroke="#003366" stroke-width="3.5" stroke-linecap="round"/>
          <!-- Scale Pans -->
          <line x1="32" y1="37" x2="26" y2="52" stroke="#004c99" stroke-width="1.5"/>
          <line x1="32" y1="37" x2="38" y2="52" stroke="#004c99" stroke-width="1.5"/>
          <path d="M22 52 Q32 58 42 52 Z" fill="#003366"/>
          <line x1="68" y1="37" x2="62" y2="52" stroke="#004c99" stroke-width="1.5"/>
          <line x1="68" y1="37" x2="74" y2="52" stroke="#004c99" stroke-width="1.5"/>
          <path d="M58 52 Q68 58 78 52 Z" fill="#003366"/>
          <!-- Central Star -->
          <polygon points="50,20 53,26 60,26 55,30 57,36 50,32 43,36 45,30 40,26 47,26" fill="#d97706"/>
        </svg>

        <div class="gov-name">Federal Democratic Republic of Ethiopia</div>
        <div class="gov-amharic">የኢትዮጵያ ፌደራላዊ ዲሞክራሲያዊ ሪፐብሊክ</div>
        <div class="agency-name">Dire Dawa City Administration Vital Events Registration Agency</div>
        <div class="agency-amharic">የድሬዳዋ አስተዳደር ወሳኝ ኩነቶች ምዝገባ ኤጀንሲ</div>
      </div>

      <!-- METADATA BAR -->
      <div class="meta-bar">
        <div class="meta-item">
          <span>Certificate No / የምስክር ወረቀት ቁጥር:</span>
          <span class="meta-val">${docNo}</span>
        </div>
        <div class="meta-item">
          <span>Date of Issue / የተሰጠበት ቀን:</span>
          <span class="meta-val">${fullDateStr}</span>
        </div>
        <div class="meta-item">
          <span>Record ID:</span>
          <span class="meta-val">${recordId}</span>
        </div>
      </div>

      <!-- TITLE -->
      <div class="title-box">
        <div class="title-h1">Certificate of Competency</div>
        <div class="title-h1-am">የሙያ ብቃት ማረጋገጫ ምስክር ወረቀት</div>
        <div class="proclamation-tag">Issued Under Civil & Family Registration Proclamation No. 1370/2024</div>
      </div>

      <!-- RECIPIENT BLOCK -->
      <div>
        <div class="certifies-prompt">This official certificate is proudly conferred upon / ይህ የምስክር ወረቀት የተሰጠው ለ:</div>
        <div class="recipient-name-box">
          <div class="recipient-name">${rawFullName}</div>
          <div class="recipient-name-underline"></div>
        </div>

        <!-- TRAINEE SYSTEM INFO GRID -->
        <div class="trainee-grid">
          <div class="grid-cell">
            <span class="grid-label">Participant ID / መለያ</span>
            <span class="grid-val">${participantCode}</span>
          </div>
          <div class="grid-cell">
            <span class="grid-label">Duty Station / ቀበሌ</span>
            <span class="grid-val">${placeOfRegistration}</span>
          </div>
          <div class="grid-cell">
            <span class="grid-label">Designation / የሥራ መደብ</span>
            <span class="grid-val">${role}</span>
          </div>
          <div class="grid-cell">
            <span class="grid-label">Gender / ጾታ</span>
            <span class="grid-val">${gender}</span>
          </div>
        </div>
      </div>

      <!-- NARRATIVE TEXT -->
      <div class="cert-narrative">
        for demonstrating proven mastery and successfully passing the comprehensive technical competency assessment for:
        <br>
        <span class="program-highlight">${surveyTitleEn}</span><br>
        <span class="program-highlight-am">(${surveyTitleAm})</span>
        <br>
        confirming readiness to register vital events (Birth, Death, Marriage, and Divorce), enforce data validation rules,
        safeguard citizen confidentiality, and operate the National Digital CRVS Platform.
      </div>

      <!-- SCORE BADGES -->
      <div class="score-badge-row">
        <div class="badge-pill gold">
          Final Score: <strong>${totalScore} / ${maxScore} pts (${scorePct}%)</strong>
        </div>
        <div class="badge-pill strong">
          Proficiency: <strong>${performanceBand} (ብቁ)</strong>
        </div>
        <div class="badge-pill">
          Mode: <strong>${testType} (≥50% Benchmark Satisfied)</strong>
        </div>
      </div>

      <!-- SIGNATURES & OFFICIAL SEAL -->
      <div class="sig-seal-row">
        <!-- Lead Coordinator -->
        <div class="sig-column">
          <div class="sig-script">Mulugeta T.</div>
          <div class="sig-rule"></div>
          <div class="sig-officer-title">Training Coordinator</div>
          <div class="sig-officer-name">CRVS Capacity Building Directorate</div>
        </div>

        <!-- Blue Official Seal -->
        <div class="seal-column">
          <svg class="seal-badge-svg" viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="60" cy="60" r="56" stroke="#004c99" stroke-width="2.5" fill="#f0f7fc"/>
            <circle cx="60" cy="60" r="50" stroke="#004c99" stroke-width="1.2" stroke-dasharray="3 2"/>
            <circle cx="60" cy="60" r="38" stroke="#004c99" stroke-width="1.5"/>
            <path id="sealUpperText" d="M 22 60 A 38 38 0 0 1 98 60" fill="none"/>
            <text font-size="6.8" font-weight="bold" fill="#003366" letter-spacing="1">
              <textPath href="#sealUpperText" startOffset="50%" text-anchor="middle">
                DIRE DAWA CRVS AGENCY
              </textPath>
            </text>
            <path id="sealLowerText" d="M 98 60 A 38 38 0 0 1 22 60" fill="none"/>
            <text font-size="6.5" font-weight="bold" fill="#003366" letter-spacing="1">
              <textPath href="#sealLowerText" startOffset="50%" text-anchor="middle">
                ★ OFFICIAL SEAL ★
              </textPath>
            </text>
            <polygon points="60,46 63,55 72,55 65,60 67,69 60,64 53,69 55,60 48,55 57,55" fill="#d97706"/>
            <text x="60" y="78" font-size="6" font-weight="bold" text-anchor="middle" fill="#003366">VERIFIED</text>
          </svg>
        </div>

        <!-- Executive Director -->
        <div class="sig-column">
          <div class="sig-script">${issuingOfficer.split(' ').slice(-1)[0]}</div>
          <div class="sig-rule"></div>
          <div class="sig-officer-title">Executive Director</div>
          <div class="sig-officer-name">${issuingOfficer}</div>
        </div>
      </div>

      <!-- SECURITY FOOTER -->
      <div class="cert-footer-security">
        <div class="barcode-display">||| | ||||| || |||| ||| ||||| | || ||||| |</div>
        <div class="security-notice">
          Document No: ${docNo} &nbsp;·&nbsp; Tamper-Evident Digital Record: ${recordId} &nbsp;·&nbsp; Certified via Dire Dawa CRVS Agency
        </div>
      </div>

    </div>
  </div>

  <script>
    window.onload = function() {
      const img = new Image();
      img.src = '/certificate_template.png';
      img.onload = function() {
        setTimeout(function() { window.print(); }, 400);
      };
      img.onerror = function() {
        setTimeout(function() { window.print(); }, 400);
      };
    };
  </script>
</body>
</html>`);
printWin.document.close();
  } catch (err) {
    alert('Error generating certificate: ' + err.message);
  }
};

// Delete Submission Handler
window.deleteSubmissionPrompt = async function(id, code) {
  if (!confirm(`Are you sure you want to delete the exam submission for participant "${code}" (ID #${id})?\n\nThis will remove the score record and allow the examinee to retake the exam.`)) {
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/submissions/${id}`, { method: 'DELETE' });
    const json = await res.json();
    if (res.ok && json.success) {
      alert(`Submission #${id} (${code}) was successfully deleted.`);
      await loadSubmissions();
      await loadAnalytics();
    } else {
      alert(`Failed to delete submission: ${json.error || 'Server error'}`);
    }
  } catch (e) {
    alert(`Error deleting submission: ${e.message}`);
  }
};


// Export All Submissions Master PDF
window.exportAllSubmissionsPdf = function() {
  const subs = state.submissions;
  if (!subs || !subs.length) {
    alert('No submissions available to export.');
    return;
  }

  const printWin = window.open('', '_blank', 'width=950,height=750');
  if (!printWin) {
    alert('Pop-up was blocked. Please allow pop-ups for this page to download or print the PDF.');
    return;
  }

  const total = subs.length;
  const preCount = subs.filter(s => s.test_type === 'PRE').length;
  const postCount = subs.filter(s => s.test_type === 'POST').length;
  const avgScore = Math.round(subs.reduce((acc, s) => acc + s.score_percentage, 0) / total);
  const passedCount = subs.filter(s => s.score_percentage >= 60).length;
  const passRate = Math.round((passedCount / total) * 100);

  const rowsHtml = subs.map((s, idx) => `
    <tr style="background: ${idx % 2 === 0 ? '#f8fafc' : '#ffffff'};">
      <td style="padding: 7px 9px; border-bottom: 1px solid #e2e8f0; font-weight: bold;">${idx + 1}</td>
      <td style="padding: 7px 9px; border-bottom: 1px solid #e2e8f0; font-weight: bold; color: #1e1b4b;">${escapeHtml(s.participant_code)}</td>
      <td style="padding: 7px 9px; border-bottom: 1px solid #e2e8f0;">${escapeHtml(s.wereda_kebele || 'Dire Dawa')}</td>
      <td style="padding: 7px 9px; border-bottom: 1px solid #e2e8f0;">${escapeHtml(s.role || 'Registrar')}</td>
      <td style="padding: 7px 9px; border-bottom: 1px solid #e2e8f0; text-align: center;">
        <span style="font-weight: 700; padding: 2px 6px; border-radius: 4px; font-size: 11px; ${s.test_type === 'POST' ? 'background: #dcfce7; color: #15803d;' : 'background: #fef3c7; color: #b45309;'}">
          ${s.test_type}
        </span>
      </td>
      <td style="padding: 7px 9px; border-bottom: 1px solid #e2e8f0; text-align: center; font-weight: bold;">${s.total_score} / ${s.max_score}</td>
      <td style="padding: 7px 9px; border-bottom: 1px solid #e2e8f0; text-align: center; font-weight: bold; color: ${s.score_percentage >= 60 ? '#15803d' : '#b45309'};">${s.score_percentage}%</td>
      <td style="padding: 7px 9px; border-bottom: 1px solid #e2e8f0; font-size: 11px;">${s.performance_band}</td>
      <td style="padding: 7px 9px; border-bottom: 1px solid #e2e8f0; font-size: 11px; color: #64748b;">${new Date(s.submitted_at).toLocaleDateString()}</td>
    </tr>
  `).join('');

  printWin.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>CRVS Examinee Performance Master Roster - PDF Export</title>
      <style>
        @page { size: A4 portrait; margin: 12mm; }
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; color: #0f172a; margin: 0; padding: 12px; }
        .header { display: flex; align-items: center; justify-content: space-between; border-bottom: 2px solid #4338ca; padding-bottom: 10px; margin-bottom: 14px; }
        .title { font-size: 18px; font-weight: 800; color: #1e1b4b; margin: 0; }
        .sub { font-size: 11px; color: #64748b; margin-top: 3px; }
        .kpi-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; margin-bottom: 14px; }
        .kpi-box { background: #f1f5f9; border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px; text-align: center; }
        .kpi-val { font-size: 16px; font-weight: 800; color: #4338ca; }
        .kpi-lbl { font-size: 9.5px; color: #64748b; text-transform: uppercase; font-weight: 600; margin-top: 2px; }
        table { width: 100%; border-collapse: collapse; font-size: 11px; }
        th { background: #1e1b4b; color: white; padding: 8px; text-align: left; font-size: 10.5px; }
        .footer { margin-top: 24px; display: flex; justify-content: space-between; font-size: 10.5px; color: #64748b; border-top: 1px solid #e2e8f0; padding-top: 12px; }
        .sig-line { width: 180px; border-top: 1px dashed #64748b; margin-top: 30px; text-align: center; font-size: 9.5px; }
        @media print { .no-print { display: none; } }
      </style>
    </head>
    <body>
      <div class="no-print" style="margin-bottom: 14px; background: #e0e7ff; padding: 10px 14px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center;">
        <span><strong>CRVS PDF Print Preview:</strong> In the print dialog, choose 'Save as PDF'.</span>
        <button onclick="window.print()" style="padding: 7px 16px; background: #4338ca; color: white; border: none; border-radius: 6px; font-weight: bold; cursor: pointer;">Save as PDF / Print</button>
      </div>

      <div class="header">
        <div>
          <h1 class="title">Dire Dawa City Administration CRVS Agency</h1>
          <div class="sub">የድሬዳዋ አስተዳደር ወሳኝ ሁነቶች ምዝገባ ኤጀንሲ · Examinee Master Performance Roster</div>
          <div class="sub">Assessment Curriculum: Proclamation 1370 / 2024 · Exported: ${new Date().toLocaleString()}</div>
        </div>
      </div>

      <div class="kpi-row">
        <div class="kpi-box"><div class="kpi-val">${total}</div><div class="kpi-lbl">Total Examinees</div></div>
        <div class="kpi-box"><div class="kpi-val">${preCount}</div><div class="kpi-lbl">Pre-Tests</div></div>
        <div class="kpi-box"><div class="kpi-val">${postCount}</div><div class="kpi-lbl">Post-Tests</div></div>
        <div class="kpi-box"><div class="kpi-val">${avgScore}%</div><div class="kpi-lbl">Average Score</div></div>
        <div class="kpi-box"><div class="kpi-val">${passRate}%</div><div class="kpi-lbl">Pass Rate (≥60%)</div></div>
      </div>

      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Participant Code</th>
            <th>Wereda / Kebele</th>
            <th>Role</th>
            <th style="text-align: center;">Test</th>
            <th style="text-align: center;">Score</th>
            <th style="text-align: center;">%</th>
            <th>Performance Band</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>
          ${rowsHtml}
        </tbody>
      </table>

      <div class="footer">
        <div>
          <div>Certified official record of Dire Dawa CRVS Registration System</div>
          <div>Director / Assessment Manager Verification:</div>
          <div class="sig-line">Signature & Date</div>
        </div>
        <div style="text-align: right;">
          <div>Official Seal & Authentication</div>
          <div class="sig-line" style="margin-left: auto;">Registrar General Seal</div>
        </div>
      </div>

      <script>
        window.onload = function() { window.print(); }
      </script>
    </body>
    </html>
  `);
  printWin.document.close();
};

// Export Single Submission PDF Certificate / Score Report
window.exportSingleSubmissionPdf = async function(id) {
  // Always use the authentic Official Birth Certificate template
  exportCertificatePdf(id);
};

// View Submission Detail Modal
window.viewSubmissionDetail = async function(id) {
  try {
    const res = await fetch(`${API_BASE}/submissions/${id}`);
    const json = await res.json();
    if (json.success && json.data) {
      renderSubmissionModal(json.data);
    }
  } catch (e) {
    alert('Failed to load submission: ' + e.message);
  }
};

function renderSubmissionModal(sub) {
  elements.modalTitle.textContent = `Submission #${sub.id} - ${sub.participant_code} (${sub.test_type}-TEST)`;
  
  const isAm = state.currentLang === 'am';
  let html = `
    <div style="display: flex; justify-content: flex-end; margin-bottom: 12px;">
      <button class="btn btn-primary" onclick="exportSingleSubmissionPdf(${sub.id})" style="padding: 6px 14px; font-size: 12px; font-weight: bold;">
        📄 Download PDF Score Card
      </button>
    </div>
    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 20px; background: rgba(0,0,0,0.25); padding: 14px; border-radius: 8px;">
      <div><span>Role:</span> <strong>${escapeHtml(sub.role || 'N/A')}</strong></div>
      <div><span>Facility:</span> <strong>${escapeHtml(sub.wereda_kebele || 'N/A')}</strong></div>
      <div><span>Experience:</span> <strong>${sub.experience_years ?? 'N/A'} yrs</strong></div>
      <div><span>Score:</span> <strong>${sub.total_score} / ${sub.max_score} (${sub.score_percentage}%)</strong></div>
      <div><span>Classification:</span> <strong>${sub.performance_band}</strong></div>
      <div><span>Date:</span> <strong>${new Date(sub.submitted_at).toLocaleString()}</strong></div>
    </div>
    <h4 style="margin-bottom: 12px;">Answers Breakdown</h4>
    <div style="display: flex; flex-direction: column; gap: 10px;">
  `;

  if (sub.answers) {
    for (const a of sub.answers) {
      const prompt = isAm ? a.prompt_am : a.prompt_en;
      const isCorrect = a.is_correct;
      const indicator = isCorrect ? '✓ Correct' : '✗ Incorrect';
      const indicatorColor = isCorrect ? '#34d399' : '#f87171';

      html += `
        <div style="padding: 12px; background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); border-radius: 8px;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
            <strong>Q${a.question_number}: ${escapeHtml(prompt)}</strong>
            <span style="color: ${indicatorColor}; font-weight: 700;">${indicator} (${a.score_awarded} / ${a.max_points} pts)</span>
          </div>
          <div style="font-size: 13px; color: var(--text-muted);">
            <div>Selected / Answer: <strong style="color: #fff;">${escapeHtml(a.selected_option_key || a.answer_text || 'None')}</strong></div>
            ${a.correct_option_key ? `<div>Official Correct Option: <strong style="color: #34d399;">${a.correct_option_key}</strong></div>` : ''}
          </div>
        </div>
      `;
    }
  }

  html += `</div>`;
  elements.modalContent.innerHTML = html;
  elements.submissionModal.classList.add('active');
}

function closeModal() {
  elements.submissionModal.classList.remove('active');
}

// =================== INTERACTIVE WIZARD (ASK QUESTIONS FLOW) ===================

function showWizardScreen(screenName) {
  Object.keys(elements.screens).forEach(key => {
    elements.screens[key].classList.toggle('active', key === screenName);
  });

  elements.stepperNav.demog.classList.toggle('active', screenName === 'demog');
  elements.stepperNav.questions.classList.toggle('active', screenName === 'questions');
  elements.stepperNav.review.classList.toggle('active', screenName === 'review' || screenName === 'result');
}

function handleStartQuestions() {
  const code = document.getElementById('input-participant-code').value.trim();
  if (!code) {
    alert('Please enter a participant code.');
    return;
  }

  const region = (document.getElementById('select-region') && document.getElementById('select-region').value) || 'Dire Dawa';
  const rawWereda = document.getElementById('input-wereda-kebele').value.trim();
  const wereda = rawWereda ? `${region} - ${rawWereda}` : region;
  const role = document.getElementById('select-role').value;
  const gender = document.getElementById('select-gender').value;
  const exp = parseFloat(document.getElementById('input-exp').value) || 0;
  const rawTestType = document.querySelector('input[name="test-type"]:checked').value;
  // Security: enforce post-test lock — examinees cannot bypass via DevTools
  const postTestRadio = document.getElementById('radio-test-post');
  const postTestAllowed = postTestRadio && !postTestRadio.disabled;
  const testType = (rawTestType === 'POST' && !postTestAllowed) ? 'PRE' : rawTestType;


  state.wizard.participant = {
    participant_code: code,
    region,
    wereda_kebele: wereda,
    role,
    gender,
    experience_years: exp
  };
  state.wizard.testType = testType;
  state.wizard.currentQIndex = 0;
  state.wizard.answers = {};

  showWizardScreen('questions');
  renderCurrentQuestion();
}

function renderCurrentQuestion() {
  const questions = state.wizard.flatQuestions;
  if (!questions.length) return;

  const q = questions[state.wizard.currentQIndex];
  const isAm = state.currentLang === 'am';

  // Progress
  const pct = Math.round(((state.wizard.currentQIndex + 1) / questions.length) * 100);
  elements.qProgressBar.style.width = `${pct}%`;

  // Headers
  elements.qSectionBadge.textContent = isAm ? q.sectionTitleAm : q.sectionTitleEn;
  elements.qNumberIndicator.textContent = `Question ${state.wizard.currentQIndex + 1} of ${questions.length}`;
  elements.qPointsIndicator.textContent = `${q.points} Point${q.points !== 1 ? 's' : ''}`;

  // Prompts
  elements.qPromptText.textContent = isAm ? q.prompt_am : (q.prompt_en || q.prompt_am);
  elements.qPromptAlt.style.display = 'none';
  elements.qPromptAlt.textContent = '';

  // Rubric info for open scenario questions
  if (q.type === 'TEXT_OPEN' && (q.rubric_am || q.rubric_en)) {
    elements.qRubricNotice.style.display = 'block';
    elements.qRubricText.textContent = isAm ? q.rubric_am : q.rubric_en;
  } else {
    elements.qRubricNotice.style.display = 'none';
  }

  // Answer Input Area
  elements.qInputArea.innerHTML = '';
  const currentAnswer = state.wizard.answers[q.id] || {};

  if (q.type === 'SINGLE_CHOICE' && q.options) {
    const optsContainer = document.createElement('div');
    optsContainer.className = 'q-options-container';

    q.options.forEach(opt => {
      const isSelected = currentAnswer.selected_option_key === opt.option_key;
      const card = document.createElement('div');
      card.className = `q-option-card ${isSelected ? 'selected' : ''}`;
      card.innerHTML = `
        <div class="opt-key-circle">${opt.option_key}</div>
        <div class="opt-text-wrap">
          <div class="opt-text-primary" style="font-size:15px;font-weight:500;color:#f1f5f9;line-height:1.5;">
            ${escapeHtml(isAm ? opt.label_am : (opt.label_en || opt.label_am))}
          </div>
        </div>
      `;
      card.addEventListener('click', () => {
        state.wizard.answers[q.id] = { selected_option_key: opt.option_key };
        optsContainer.querySelectorAll('.q-option-card').forEach(c => c.classList.remove('selected'));
        card.classList.add('selected');
        updateQuickStatus();
      });
      optsContainer.appendChild(card);
    });

    elements.qInputArea.appendChild(optsContainer);
  } else if (q.type === 'RATING_SCALE') {
    const ratingRow = document.createElement('div');
    ratingRow.className = 'rating-chips-row';

    [1, 2, 3, 4, 5].forEach(val => {
      const isSelected = currentAnswer.rating_value === val;
      const chip = document.createElement('button');
      chip.type = 'button';
      chip.className = `rating-chip ${isSelected ? 'selected' : ''}`;
      chip.textContent = val;
      chip.addEventListener('click', () => {
        state.wizard.answers[q.id] = { rating_value: val };
        ratingRow.querySelectorAll('.rating-chip').forEach(c => c.classList.remove('selected'));
        chip.classList.add('selected');
        updateQuickStatus();
      });
      ratingRow.appendChild(chip);
    });

    elements.qInputArea.appendChild(ratingRow);
  } else if (q.type === 'TEXT_OPEN') {
    const textarea = document.createElement('textarea');
    textarea.className = 'scenario-textarea';
    textarea.placeholder = isAm 
      ? 'የእርስዎን መልስ እዚህ ይጻፉ (2-4 ዓረፍተ ነገር)...'
      : 'Type your concise response here (2-4 sentences)...';
    textarea.value = currentAnswer.answer_text || '';
    textarea.addEventListener('input', (e) => {
      state.wizard.answers[q.id] = { answer_text: e.target.value };
      updateQuickStatus();
    });
    elements.qInputArea.appendChild(textarea);
  }

  // Navigation controls
  elements.btnQPrev.disabled = state.wizard.currentQIndex === 0;
  const isLast = state.wizard.currentQIndex === questions.length - 1;
  elements.btnQNext.textContent = isLast ? 'Review Answers →' : 'Next Question →';

  updateQuickStatus();
}

function updateQuickStatus() {
  const q = state.wizard.flatQuestions[state.wizard.currentQIndex];
  const ans = state.wizard.answers[q.id];
  const hasAnswer = ans && (ans.selected_option_key || ans.rating_value || (ans.answer_text && ans.answer_text.trim()));
  elements.qQuickStatus.textContent = hasAnswer ? '✓ Response saved' : '○ Pending response';
  elements.qQuickStatus.style.color = hasAnswer ? '#34d399' : 'var(--text-dim)';
}

function navigateQuestion(step) {
  const questions = state.wizard.flatQuestions;
  const currentQ = questions[state.wizard.currentQIndex];

  // Block forward navigation if no answer given for the current question
  if (step > 0 && currentQ) {
    const ans = state.wizard.answers[currentQ.id];
    const hasAnswer = ans && (ans.selected_option_key || ans.rating_value || (ans.answer_text && ans.answer_text.trim()));
    if (!hasAnswer) {
      if (elements.qInputArea) {
        elements.qInputArea.style.animation = 'none';
        void elements.qInputArea.offsetHeight;
        elements.qInputArea.style.animation = 'shakeWarn 0.5s ease';
      }
      const existingWarn = document.getElementById('no-answer-warning');
      if (existingWarn) existingWarn.remove();
      const warn = document.createElement('div');
      warn.id = 'no-answer-warning';
      warn.style.cssText = 'margin-top:14px;padding:12px 16px;background:rgba(239,68,68,0.15);border:1px solid rgba(239,68,68,0.5);border-radius:10px;color:#f87171;font-size:14px;font-weight:600;text-align:center;display:flex;align-items:center;justify-content:center;gap:8px;';
      const isAm = state.currentLang === 'am';
      warn.innerHTML = isAm 
        ? '⚠️ ወደ ቀጣዩ ጥያቄ ከመሄድዎ በፊት እባክዎ መልስ ይምረጡ/ይጻፉ።'
        : '⚠️ Please select or provide an answer before proceeding to the next question.';
      if (elements.qInputArea && elements.qInputArea.parentNode) {
        elements.qInputArea.parentNode.insertBefore(warn, elements.qInputArea.nextSibling);
      }
      setTimeout(() => { if (warn.parentNode) warn.remove(); }, 3500);
      return;
    }
  }

  const newIndex = state.wizard.currentQIndex + step;
  if (newIndex >= 0 && newIndex < questions.length) {
    state.wizard.currentQIndex = newIndex;
    renderCurrentQuestion();
  } else if (newIndex >= questions.length) {
    showReviewScreen();
  }
}

function speakCurrentQuestion() {
  const q = state.wizard.flatQuestions[state.wizard.currentQIndex];
  if (!q) return;

  const isAm = state.currentLang === 'am';
  const text = isAm ? q.prompt_am : q.prompt_en;

  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = isAm ? 'am-ET' : 'en-US';
    utterance.rate = 0.95;
    window.speechSynthesis.speak(utterance);
  } else {
    alert('Speech synthesis is not supported on this browser.');
  }
}

// Show Review Screen
function showReviewScreen() {
  showWizardScreen('review');
  const questions = state.wizard.flatQuestions;
  const answers = state.wizard.answers;
  const isAm = state.currentLang === 'am';

  elements.revParticipantCode.textContent = state.wizard.participant.participant_code;
  elements.revTestMode.textContent = `${state.wizard.testType}-TEST`;

  let answeredCount = 0;
  elements.reviewAnswersList.innerHTML = questions.map((q, idx) => {
    const ans = answers[q.id];
    const hasAns = ans && (ans.selected_option_key || ans.rating_value || ans.answer_text);
    if (hasAns) answeredCount++;

    const displayAns = ans?.selected_option_key || (ans?.rating_value ? `Rating: ${ans.rating_value}/5` : (ans?.answer_text ? `Text: "${ans.answer_text.slice(0, 30)}..."` : 'Not answered'));
    const prompt = isAm ? q.prompt_am : q.prompt_en;

    return `
      <div class="rev-item" onclick="jumpToQuestion(${idx})">
        <span class="rev-q-num">Q${q.question_number}</span>
        <span class="rev-q-text">${escapeHtml(prompt)}</span>
        <span class="rev-ans-badge" style="color: ${hasAns ? '#818cf8' : '#ef4444'}">${escapeHtml(displayAns)}</span>
      </div>
    `;
  }).join('');

  elements.revCompletionStat.textContent = `${answeredCount} / ${questions.length} Answered`;
}

window.jumpToQuestion = function(index) {
  state.wizard.currentQIndex = index;
  showWizardScreen('questions');
  renderCurrentQuestion();
};

// Final Submit
async function handleSubmitSurvey() {
  const questions = state.wizard.flatQuestions;

  // Block submission if any question remains unanswered
  const unansweredIdx = questions.findIndex(q => {
    const ans = state.wizard.answers[q.id];
    return !ans || (!ans.selected_option_key && !ans.rating_value && (!ans.answer_text || !ans.answer_text.trim()));
  });

  if (unansweredIdx !== -1) {
    const isAm = state.currentLang === 'am';
    const uQ = questions[unansweredIdx];
    alert(isAm 
      ? `እባክዎ ከማስገባትዎ በፊት ሁሉንም ጥያቄዎች ይመልሱ። ያልተመለሰ ጥያቄ ቁጥር ${uQ.question_number} አለ።` 
      : `Please answer all questions before submitting. Question Q${uQ.question_number} is unanswered.`);
    window.jumpToQuestion(unansweredIdx);
    return;
  }

  const answerEntries = [];

  for (const q of questions) {
    const ans = state.wizard.answers[q.id] || {};
    answerEntries.push({
      question_id: q.id,
      question_number: q.question_number,
      selected_option_key: ans.selected_option_key,
      answer_text: ans.answer_text,
      rating_value: ans.rating_value
    });
  }

  const payload = {
    survey_id: state.currentSurvey.id,
    participant: state.wizard.participant,
    test_type: state.wizard.testType,
    answers: answerEntries
  };

  try {
    elements.btnSubmitFinal.disabled = true;
    elements.btnSubmitFinal.textContent = 'Scoring & Submitting...';

    const res = await fetch(`${API_BASE}/submissions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const json = await res.json();
    if (json.success && json.data) {
      state.wizard.result = json.data;
      showResultScreen(json.data);
      await loadAnalytics();
      await loadSubmissions();
    } else {
      alert('Error submitting survey: ' + (json.error || 'Unknown error'));
    }
  } catch (err) {
    alert('Submission failed: ' + err.message);
  } finally {
    elements.btnSubmitFinal.disabled = false;
    elements.btnSubmitFinal.textContent = 'Submit & Score Now ✓';
  }
}

// Show Result Screen
function showResultScreen(result) {
  showWizardScreen('result');

  let user = null;
  try {
    const raw = localStorage.getItem('qcafr_user');
    if (raw) user = JSON.parse(raw);
  } catch (e) {}

  const canViewResult = user && (user.role === 'ADMIN' || user.role === 'USER_MANAGER');

  if (canViewResult) {
    if (elements.resBadgeCircle) elements.resBadgeCircle.style.display = 'flex';
    if (elements.resBandPill) elements.resBandPill.style.display = 'inline-block';
    if (elements.resPercentageText) elements.resPercentageText.style.display = 'block';
    const breakdown = document.querySelector('.result-breakdown-grid');
    if (breakdown) breakdown.style.display = 'grid';

    elements.resHeadline.textContent = 'Assessment Completed!';
    elements.resTotalScore.textContent = result.total_score;
    elements.resPercentageText.textContent = `Score: ${result.score_percentage}% (${result.total_score} of ${result.max_score} points)`;
    elements.resBandPill.textContent = result.performance_band;

    // Band badge color
    elements.resBandPill.className = `result-band ${
      result.performance_band === 'STRONG' ? 'band-strong' :
      result.performance_band === 'SATISFACTORY' ? 'band-satisfactory' :
      result.performance_band === 'NEEDS_REINFORCEMENT' ? 'band-reinforcement' : 'band-intensive'
    }`;

    // Calculate section scores
    if (result.answers) {
      const secA = result.answers.filter(a => a.question_number >= 1 && a.question_number <= 8);
      const scoreA = secA.reduce((acc, a) => acc + a.score_awarded, 0);

      const secB = result.answers.filter(a => a.question_number >= 9 && a.question_number <= 16);
      const scoreB = secB.reduce((acc, a) => acc + a.score_awarded, 0);

      const secC = result.answers.filter(a => a.question_number >= 17 && a.question_number <= 20);
      const scoreC = secC.reduce((acc, a) => acc + a.score_awarded, 0);

      elements.resScoreSecA.textContent = `${scoreA} / 8`;
      elements.resScoreSecB.textContent = `${scoreB} / 8`;
      elements.resScoreSecC.textContent = `${scoreC} / 8`;
    }
  } else {
    // Examinee view: scores and breakdown hidden
    if (elements.resBadgeCircle) elements.resBadgeCircle.style.display = 'none';
    if (elements.resBandPill) elements.resBandPill.style.display = 'none';
    if (elements.resPercentageText) elements.resPercentageText.style.display = 'none';
    const breakdown = document.querySelector('.result-breakdown-grid');
    if (breakdown) breakdown.style.display = 'none';

    elements.resHeadline.innerHTML = `
      <div style="font-size: 48px; margin-bottom: 12px;">✅</div>
      <div>ምዘናው በተሳካ ሁኔታ ተጠናቋል!</div>
      <div style="font-size: 15px; font-weight: normal; color: var(--text-muted); margin-top: 6px;">Assessment Submitted Successfully</div>
      <div style="margin-top: 18px; padding: 14px; background: rgba(99, 102, 241, 0.12); border: 1px solid rgba(99, 102, 241, 0.35); border-radius: 12px; font-size: 13.5px; color: #f59e0b; line-height: 1.5;">
        🔒 <strong>የውጤት ገደብ፦</strong> የፈተናው ውጤት የሚታየው ለአስተዳዳሪ (Admin) እና ሥራ አስኪያጅ (Manager) ብቻ ነው።<br>
        <span style="font-size: 12px; color: var(--text-muted);">Results are restricted and visible only to authorized administrators and managers.</span>
      </div>
    `;
  }
}

function resetWizard() {
  state.wizard.currentQIndex = 0;
  state.wizard.answers = {};
  state.wizard.result = null;
  showWizardScreen('demog');
}

// Render Question Bank
function renderQuestionBank() {
  if (!state.currentSurvey?.sections) return;
  const isAm = state.currentLang === 'am';

  // Only Admins and User Managers may see official correct answers and rubrics
  let _qbUser = null;
  try { _qbUser = JSON.parse(localStorage.getItem('qcafr_user') || 'null'); } catch(_) {}
  const canSeeAnswers = _qbUser && (_qbUser.role === 'ADMIN' || _qbUser.role === 'USER_MANAGER');

  elements.fullQuestionBankContainer.innerHTML = state.currentSurvey.sections.map(sec => `
    <div style="margin-bottom: 24px;">
      <h4 style="color: #a5b4fc; margin-bottom: 12px;">${escapeHtml(isAm ? sec.title_am : sec.title_en)}</h4>
      <div style="display: flex; flex-direction: column; gap: 12px;">
        ${(sec.questions || []).map(q => `
          <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); border-radius: 8px; padding: 14px 18px;">
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
              <strong>Q${q.question_number}. ${escapeHtml(isAm ? q.prompt_am : q.prompt_en)}</strong>
              <span class="badge badge-primary">${q.points} pt</span>
            </div>
            ${q.options ? `
              <div style="margin-left: 14px; display: flex; flex-direction: column; gap: 4px; font-size: 13px;">
                ${q.options.map(opt => `
                  <div style="color: ${(canSeeAnswers && opt.is_correct) ? '#34d399' : 'var(--text-muted)'}; font-weight: ${(canSeeAnswers && opt.is_correct) ? '600' : '400'};">
                    ${opt.option_key}. ${escapeHtml(isAm ? opt.label_am : opt.label_en)} ${(canSeeAnswers && opt.is_correct) ? '★ (Official Answer)' : ''}
                  </div>
                `).join('')}
              </div>
            ` : ''}
            ${(canSeeAnswers && q.rubric_am) ? `
              <div style="margin-top: 6px; font-size: 12px; color: #a7f3d0; background: rgba(16,185,129,0.08); padding: 8px; border-radius: 6px;">
                Rubric: ${escapeHtml(isAm ? q.rubric_am : q.rubric_en)}
              </div>
            ` : ''}
          </div>
        `).join('')}
      </div>
    </div>
  `).join('');
}

// =================== SETTINGS & QUESTION EDITOR ===================

function renderSettingsQuestions() {
  if (!elements.settingsQuestionsContainer || !state.currentSurvey?.sections) return;
  const isAm = state.currentLang === 'am';

  // Only Admins and Managers can see the correct answer markers
  let _sqUser = null;
  try { _sqUser = JSON.parse(localStorage.getItem('qcafr_user') || 'null'); } catch(_) {}
  const canSeeAnswers = _sqUser && (_sqUser.role === 'ADMIN' || _sqUser.role === 'USER_MANAGER');
  const filterSec = elements.settingsSectionFilter ? elements.settingsSectionFilter.value : 'ALL';

  let html = '';

  state.currentSurvey.sections.forEach(sec => {
    if (filterSec !== 'ALL' && sec.code !== filterSec) return;

    html += `
      <div style="margin-bottom: 28px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; border-bottom: 1px solid var(--border-color); padding-bottom: 8px;">
          <h4 style="color: #818cf8; font-size: 16px;">${escapeHtml(isAm ? sec.title_am : sec.title_en)}</h4>
          <button class="btn btn-secondary" style="padding: 4px 10px; font-size: 12px;" onclick="openAddQuestionModal(${sec.id})">
            + Add to this Section
          </button>
        </div>
        <div style="display: flex; flex-direction: column; gap: 14px;">
    `;

    (sec.questions || []).forEach(q => {
      html += `
        <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); border-radius: 12px; padding: 18px 20px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
            <div>
              <span class="badge badge-primary" style="margin-right: 8px;">Q${q.question_number}</span>
              <span class="badge" style="background: rgba(16,185,129,0.15); color: #34d399;">${q.points} Point${q.points !== 1 ? 's' : ''}</span>
              <span class="badge" style="margin-left: 6px;">${q.type}</span>
            </div>
            <div style="display: flex; gap: 8px;">
              <button class="btn btn-secondary" style="padding: 5px 12px; font-size: 12px;" onclick="openEditQuestionModal(${q.id})">
                ✏️ Edit Question
              </button>
              <button class="btn btn-secondary" style="padding: 5px 10px; font-size: 12px; color: #ef4444;" onclick="handleDeleteQuestion(${q.id})">
                🗑️
              </button>
            </div>
          </div>

          <div style="font-size: 15px; font-weight: 700; color: #fff; margin-bottom: 12px;">
            ${escapeHtml(isAm ? q.prompt_am : (q.prompt_en || q.prompt_am))}
          </div>

          ${q.options && q.options.length ? `
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 10px; background: rgba(0,0,0,0.2); padding: 12px; border-radius: 8px;">
              ${q.options.map(opt => `
                <div style="display: flex; align-items: center; gap: 8px; font-size: 13px; padding: 4px 8px; border-radius: 6px; ${(canSeeAnswers && opt.is_correct) ? 'background: rgba(16,185,129,0.12); border: 1px solid rgba(16,185,129,0.3); font-weight: 600;' : ''}">
                  <span style="font-weight: 800; color: ${(canSeeAnswers && opt.is_correct) ? '#34d399' : 'var(--text-dim)'};">${opt.option_key}.</span>
                  <span style="color: ${(canSeeAnswers && opt.is_correct) ? '#a7f3d0' : 'var(--text-main)'};">${escapeHtml(isAm ? opt.label_am : opt.label_en)}</span>
                  ${(canSeeAnswers && opt.is_correct) ? '<span style="color: #34d399; margin-left: auto;">✓ Correct</span>' : ''}
                </div>
              `).join('')}
            </div>
          ` : ''}

          ${q.rubric_am ? `
            <div style="margin-top: 10px; font-size: 12px; color: #a7f3d0; background: rgba(16,185,129,0.06); padding: 8px 12px; border-radius: 6px; border: 1px solid rgba(16,185,129,0.2);">
              <strong>Rubric:</strong> ${escapeHtml(isAm ? q.rubric_am : q.rubric_en)}
            </div>
          ` : ''}
        </div>
      `;
    });

    html += `
        </div>
      </div>
    `;
  });

  elements.settingsQuestionsContainer.innerHTML = html;
}

window.openEditQuestionModal = async function(qId) {
  try {
    const res = await fetch(`${API_BASE}/questions/${qId}`);
    const json = await res.json();
    if (!json.success || !json.data) throw new Error(json.error || 'Question not found');

    const q = json.data;
    elements.editModalTitle.textContent = `Edit Question Q${q.question_number} (ጥያቄ #${q.question_number} ማስተካከያ)`;
    elements.editQId.value = q.id;
    elements.editQSectionId.value = q.section_id;
    elements.editQPromptAm.value = q.prompt_am;
    elements.editQPromptEn.value = q.prompt_en || '';
    elements.editQType.value = q.type;
    elements.editQPoints.value = q.points;
    elements.editQRubricAm.value = q.rubric_am || '';
    elements.editQRubricEn.value = q.rubric_en || '';

    // Handle options
    elements.editOptionsList.innerHTML = '';
    if (q.options && q.options.length) {
      q.options.forEach(opt => {
        addOptionRowToEditor(opt.option_key, opt.label_am, opt.label_en, opt.is_correct);
      });
    } else if (q.type === 'SINGLE_CHOICE') {
      ['ሀ', 'ለ', 'ሐ', 'መ'].forEach((k, idx) => {
        addOptionRowToEditor(k, '', '', idx === 0);
      });
    }

    elements.editOptionsSection.style.display = q.type === 'SINGLE_CHOICE' ? 'block' : 'none';
    elements.editRubricSection.style.display = q.type === 'TEXT_OPEN' ? 'block' : 'none';

    elements.editQuestionModal.classList.add('active');
  } catch (err) {
    alert('Failed to load question details: ' + err.message);
  }
};

window.openAddQuestionModal = function(sectionId) {
  const targetSectionId = sectionId || (state.currentSurvey?.sections?.[0]?.id || 1);
  elements.editModalTitle.textContent = 'Add New Question / አዲስ ጥያቄ ማከል';
  elements.editQId.value = '';
  elements.editQSectionId.value = targetSectionId;
  elements.editQPromptAm.value = '';
  elements.editQPromptEn.value = '';
  elements.editQType.value = 'SINGLE_CHOICE';
  elements.editQPoints.value = 1;
  elements.editQRubricAm.value = '';
  elements.editQRubricEn.value = '';

  elements.editOptionsList.innerHTML = '';
  ['ሀ', 'ለ', 'ሐ', 'መ'].forEach((k, idx) => {
    addOptionRowToEditor(k, '', '', idx === 0);
  });

  elements.editOptionsSection.style.display = 'block';
  elements.editRubricSection.style.display = 'none';

  elements.editQuestionModal.classList.add('active');
};

function addOptionRowToEditor(key, labelAm, labelEn, isCorrect) {
  const row = document.createElement('div');
  row.className = 'edit-opt-row';
  row.style.display = 'flex';
  row.style.alignItems = 'center';
  row.style.gap = '8px';
  row.style.background = 'rgba(255,255,255,0.03)';
  row.style.padding = '8px 12px';
  row.style.borderRadius = '8px';
  row.style.border = '1px solid var(--border-color)';

  row.innerHTML = `
    <input type="radio" name="opt-is-correct-radio" title="Mark as correct answer" ${isCorrect ? 'checked' : ''} style="cursor: pointer; width: 18px; height: 18px;">
    <input type="text" class="opt-key-input" value="${escapeHtml(key)}" placeholder="Key" style="width: 48px; text-align: center; font-weight: bold; padding: 6px; background: rgba(0,0,0,0.3); border: 1px solid var(--border-color); border-radius: 4px; color: #fff;">
    <input type="text" class="opt-am-input" value="${escapeHtml(labelAm)}" placeholder="Option text in Amharic (አማርኛ) *" required style="flex: 1; padding: 6px 10px; background: rgba(0,0,0,0.3); border: 1px solid var(--border-color); border-radius: 4px; color: #fff;">
    <input type="text" class="opt-en-input" value="${escapeHtml(labelEn)}" placeholder="Option English translation" style="flex: 1; padding: 6px 10px; background: rgba(0,0,0,0.3); border: 1px solid var(--border-color); border-radius: 4px; color: #fff;">
    <button type="button" class="btn-remove-opt" style="background: transparent; border: none; color: #ef4444; font-size: 16px; cursor: pointer; padding: 0 4px;" title="Remove Option">&times;</button>
  `;

  row.querySelector('.btn-remove-opt').addEventListener('click', () => {
    row.remove();
  });

  elements.editOptionsList.appendChild(row);
}

function closeEditModal() {
  elements.editQuestionModal.classList.remove('active');
}

async function handleSaveQuestion(e) {
  e.preventDefault();
  const qId = elements.editQId.value;
  const sectionId = elements.editQSectionId.value;
  const promptAm = elements.editQPromptAm.value.trim();
  const promptEn = elements.editQPromptEn.value.trim();
  const type = elements.editQType.value;
  const points = parseInt(elements.editQPoints.value, 10) || 1;
  const rubricAm = elements.editQRubricAm.value.trim();
  const rubricEn = elements.editQRubricEn.value.trim();

  if (!promptAm) {
    alert('Amharic prompt is required.');
    return;
  }

  // Parse options if SINGLE_CHOICE
  let options = [];
  if (type === 'SINGLE_CHOICE') {
    const rows = elements.editOptionsList.querySelectorAll('.edit-opt-row');
    rows.forEach((r, idx) => {
      const isCorrect = r.querySelector('input[type="radio"]').checked;
      const key = r.querySelector('.opt-key-input').value.trim() || String.fromCharCode(65 + idx);
      const optAm = r.querySelector('.opt-am-input').value.trim();
      const optEn = r.querySelector('.opt-en-input').value.trim();
      options.push({
        option_key: key,
        label_am: optAm,
        label_en: optEn || optAm,
        is_correct: isCorrect,
        order_index: idx
      });
    });

    if (options.length && !options.some(o => o.is_correct)) {
      options[0].is_correct = true; // default first as correct if none selected
    }
  }

  const payload = {
    prompt_am: promptAm,
    prompt_en: promptEn,
    type,
    points,
    rubric_am: rubricAm || null,
    rubric_en: rubricEn || null,
    options
  };

  try {
    let url = qId ? `${API_BASE}/questions/${qId}` : `${API_BASE}/sections/${sectionId}/questions`;
    let method = qId ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const json = await res.json();
    if (json.success) {
      closeEditModal();
      await loadSurvey();
      await loadAnalytics();
      alert('✓ Question saved successfully! Changes are live across all assessment sessions.');
    } else {
      alert('Error saving question: ' + (json.error || 'Unknown error'));
    }
  } catch (err) {
    alert('Failed to save question: ' + err.message);
  }
}

window.handleDeleteQuestion = async function(qId) {
  if (!confirm(`Are you sure you want to delete question #${qId}? This action cannot be undone.`)) {
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/questions/${qId}`, { method: 'DELETE' });
    const json = await res.json();
    if (json.success) {
      await loadSurvey();
      await loadAnalytics();
      alert('Question deleted.');
    } else {
      alert('Error: ' + json.error);
    }
  } catch (err) {
    alert('Failed to delete question: ' + err.message);
  }
};

// Utilities
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

// ── Web Authentication System (Sign In, Sign Up, Session) ───────────────
function setupAuthSystem() {
  const btnOpenAuth = document.getElementById('btn-open-auth-modal');
  const authModal = document.getElementById('auth-modal');
  const btnCloseAuth = document.getElementById('btn-close-auth-modal');
  const tabSignIn = document.getElementById('tab-auth-signin');
  const tabSignUp = document.getElementById('tab-auth-signup');
  const formSignIn = document.getElementById('form-auth-signin');
  const formSignUp = document.getElementById('form-auth-signup');
  const authError = document.getElementById('auth-error-msg');
  const userLoggedBox = document.getElementById('user-logged-box');
  const navUserRole = document.getElementById('nav-user-role');
  const navUserName = document.getElementById('nav-user-name');
  const btnWebLogout = document.getElementById('btn-web-logout');

  function updateAuthUI() {
    const saved = localStorage.getItem('qcafr_user');
    if (saved) {
      try {
        const user = JSON.parse(saved);
        if (btnOpenAuth) btnOpenAuth.style.display = 'none';
        if (userLoggedBox) {
          userLoggedBox.style.display = 'inline-flex';
          navUserRole.textContent = user.role || 'EXAMINEE';
          navUserRole.style.background = user.role === 'ADMIN' ? '#6366f1' : '#10b981';
          navUserName.textContent = user.full_name || user.username;
        }
        // Auto-fill wizard participant if examinee
        const codeInput = document.getElementById('demog-code');
        const roleInput = document.getElementById('demog-role');
        const kebeleInput = document.getElementById('demog-wereda');
        if (codeInput && user.participant_code) codeInput.value = user.participant_code;
        if (roleInput && user.role) roleInput.value = user.role === 'ADMIN' ? 'Supervisor' : 'Registrar';
        if (kebeleInput && user.wereda_kebele) kebeleInput.value = user.wereda_kebele;
      } catch (_) {}
    } else {
      if (btnOpenAuth) btnOpenAuth.style.display = 'inline-block';
      if (userLoggedBox) userLoggedBox.style.display = 'none';
    }
  }

  if (btnOpenAuth) {
    btnOpenAuth.addEventListener('click', () => {
      authModal.classList.add('active');
      authError.style.display = 'none';
    });
  }

  if (btnCloseAuth) {
    btnCloseAuth.addEventListener('click', () => {
      authModal.classList.remove('active');
    });
  }

  if (tabSignIn && tabSignUp) {
    tabSignIn.addEventListener('click', () => {
      tabSignIn.style.borderColor = '#6366f1';
      tabSignIn.style.color = '#fff';
      tabSignUp.style.borderColor = 'transparent';
      tabSignUp.style.color = '#94a3b8';
      formSignIn.style.display = 'block';
      formSignUp.style.display = 'none';
      authError.style.display = 'none';
    });

    tabSignUp.addEventListener('click', () => {
      tabSignUp.style.borderColor = '#6366f1';
      tabSignUp.style.color = '#fff';
      tabSignIn.style.borderColor = 'transparent';
      tabSignIn.style.color = '#94a3b8';
      formSignUp.style.display = 'block';
      formSignIn.style.display = 'none';
      authError.style.display = 'none';
    });
  }

  // Handle Sign In Submit
  if (formSignIn) {
    formSignIn.addEventListener('submit', async (e) => {
      e.preventDefault();
      authError.style.display = 'none';
      const u = document.getElementById('signin-username').value.trim();
      const p = document.getElementById('signin-password').value;
      try {
        const res = await fetch(`${API_BASE}/auth/signin`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: u, password: p })
        });
        const data = await res.json();
        if (data.success && data.data) {
          localStorage.setItem('qcafr_user', JSON.stringify(data.data.user));
          localStorage.setItem('qcafr_token', data.data.token);
          authModal.classList.remove('active');
          updateAuthUI();
        } else {
          authError.textContent = data.error || 'Authentication failed';
          authError.style.display = 'block';
        }
      } catch (err) {
        authError.textContent = 'Server error: ' + err.message;
        authError.style.display = 'block';
      }
    });
  }

  // Handle Sign Up Submit
  if (formSignUp) {
    formSignUp.addEventListener('submit', async (e) => {
      e.preventDefault();
      authError.style.display = 'none';
      const fullName = document.getElementById('signup-fullname').value.trim();
      const role = document.getElementById('signup-role').value;
      const username = document.getElementById('signup-username').value.trim();
      const password = document.getElementById('signup-password').value;
      try {
        const res = await fetch(`${API_BASE}/auth/signup`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ fullName, role, username, password })
        });
        const data = await res.json();
        if (data.success && data.data) {
          localStorage.setItem('qcafr_user', JSON.stringify(data.data.user));
          localStorage.setItem('qcafr_token', data.data.token);
          authModal.classList.remove('active');
          updateAuthUI();
          alert('Account created! Welcome, ' + fullName);
        } else {
          authError.textContent = data.error || 'Sign up failed';
          authError.style.display = 'block';
        }
      } catch (err) {
        authError.textContent = 'Server error: ' + err.message;
        authError.style.display = 'block';
      }
    });
  }

  if (btnWebLogout) {
    btnWebLogout.addEventListener('click', () => {
      localStorage.removeItem('qcafr_user');
      localStorage.removeItem('qcafr_token');
      updateAuthUI();
    });
  }

  updateAuthUI();
}

function setupQrModal() {
  const btnOpen = document.getElementById('btn-open-qr-modal');
  const modal = document.getElementById('qr-modal');
  const btnClose = document.getElementById('btn-close-qr-modal');

  if (btnOpen && modal) {
    btnOpen.addEventListener('click', () => {
      modal.style.display = 'flex';
      modal.classList.add('active');
    });
  }

  if (btnClose && modal) {
    btnClose.addEventListener('click', () => {
      modal.style.display = 'none';
      modal.classList.remove('active');
    });
  }

  if (modal) {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        modal.style.display = 'none';
        modal.classList.remove('active');
      }
    });
  }
function setupProfileModal() {
  const btnOpen = document.getElementById('btn-open-profile-modal');
  const modal = document.getElementById('profile-modal');
  const btnClose = document.getElementById('btn-close-profile-modal');
  const body = document.getElementById('examinee-profile-body');

  async function loadProfileData() {
    const saved = localStorage.getItem('qcafr_user');
    if (!saved) {
      body.innerHTML = `
        <div style="text-align:center; padding: 24px; color: #94a3b8;">
          <p>Please sign in to view your profile and certificates.</p>
        </div>`;
      return;
    }
    const user = JSON.parse(saved);
    const code = user.participant_code || user.username;

    body.innerHTML = `
      <!-- User Info Header -->
      <div style="background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 14px; padding: 16px; margin-bottom: 20px;">
        <div style="display: flex; justify-content: space-between; align-items: flex-start;">
          <div>
            <div style="font-size: 18px; font-weight: 800; color: #fff;">\${escapeHtml(user.full_name || user.username)}</div>
            <div style="font-size: 12px; color: #94a3b8; margin-top: 3px;">Participant Code: <strong style="color: #38bdf8; font-family: monospace;">\${escapeHtml(code)}</strong></div>
            <div style="font-size: 12px; color: #94a3b8; margin-top: 2px;">Role: <strong>\${escapeHtml(user.role || 'Examinee')}</strong> · Facility: <strong>\${escapeHtml(user.wereda_kebele || 'Dire Dawa')}</strong></div>
          </div>
          <span style="background: rgba(99,102,241,0.2); color: #818cf8; border: 1px solid rgba(99,102,241,0.4); font-size: 11px; font-weight: bold; padding: 4px 10px; border-radius: 999px;">
            \${escapeHtml(user.role || 'EXAMINEE')}
          </span>
        </div>
      </div>

      <div style="font-size: 14px; font-weight: 700; color: #cbd5e1; margin-bottom: 12px; display: flex; align-items: center; gap: 8px;">
        <span>🏆</span>
        <span>Official Certificates &amp; Assessment Status</span>
      </div>

      <div id="examinee-cert-list">
        <div style="text-align:center; padding: 20px; color: #94a3b8;">
          Loading certificates...
        </div>
      </div>
    `;

    try {
      // 1. Fetch certificates for participant
      const certRes = await fetch(`\${API_BASE}/certificates/participant/\${encodeURIComponent(code)}`);
      const certData = await certRes.json();
      const certList = (certData.success && Array.isArray(certData.data)) ? certData.data : [];

      // 2. Fetch submissions for participant
      const subRes = await fetch(`\${API_BASE}/submissions?participant_code=\${encodeURIComponent(code)}`);
      const subData = await subRes.json();
      const subList = (subData.success && Array.isArray(subData.data)) ? subData.data : [];

      const certContainer = document.getElementById('examinee-cert-list');
      if (!certContainer) return;

      if (certList.length === 0 && subList.length === 0) {
        certContainer.innerHTML = `
          <div style="background: rgba(255,255,255,0.02); border: 1px dashed rgba(255,255,255,0.15); border-radius: 12px; padding: 24px; text-align: center; color: #94a3b8;">
            <div style="font-size: 32px; margin-bottom: 8px;">📝</div>
            <div style="font-weight: 600; color: #cbd5e1;">No Assessments Completed Yet</div>
            <div style="font-size: 12px; margin-top: 4px;">Click "Ask Questions (Live Flow)" in the menu to complete the assessment.</div>
          </div>
        `;
        return;
      }

      let html = '';

      // Render Approved Certificates First
      certList.forEach(c => {
        const issuedDate = new Date(c.issued_at || Date.now()).toLocaleDateString('de-DE');
        html += `
          <div style="background: linear-gradient(135deg, rgba(16,185,129,0.12) 0%, rgba(6,78,59,0.2) 100%); border: 1.5px solid #10b981; border-radius: 14px; padding: 18px; margin-bottom: 14px; box-shadow: 0 4px 12px rgba(0,0,0,0.2);">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px;">
              <span style="background: #10b981; color: #042f2e; font-size: 10.5px; font-weight: 800; padding: 3px 10px; border-radius: 999px; letter-spacing: 0.5px;">
                ✅ OFFICIALLY APPROVED &amp; ISSUED
              </span>
              <span style="font-family: monospace; font-size: 12px; color: #6ee7b7; font-weight: bold;">
                Doc: \${escapeHtml(c.certificate_number)}
              </span>
            </div>

            <div style="font-size: 16px; font-weight: 800; color: #fff; margin-bottom: 4px;">
              Certificate of Competency (የብቃት ምስክር ወረቀት)
            </div>
            <div style="font-size: 12px; color: #cbd5e1; margin-bottom: 12px;">
              Score: <strong style="color: #34d399;">\${c.total_score} / \${c.max_score} (\${c.score_percentage}%)</strong> · Band: <strong>\${escapeHtml(c.performance_band.replace('_', ' '))}</strong> · Issued: \${issuedDate} by <strong>\${escapeHtml(c.issued_by)}</strong>
            </div>

            <button class="btn btn-primary" onclick="exportCertificatePdf(\${c.submission_id})" style="width: 100%; padding: 10px; font-weight: bold; background: #00529c; border: none; display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 13px; border-radius: 8px; cursor: pointer;">
              📄 Download Official Certificate PDF (አውርድ)
            </button>
          </div>
        `;
      });

      // Render Submissions without Certificates (Pending or Under Review)
      subList.forEach(s => {
        const hasCert = certList.some(c => c.submission_id === s.id);
        if (hasCert) return;

        const isPassed = s.score_percentage >= 50;
        const statusBadge = isPassed
          ? `<span style="background: rgba(245,158,11,0.2); color: #fbbf24; border: 1px solid rgba(245,158,11,0.5); font-size: 11px; font-weight: bold; padding: 2px 8px; border-radius: 999px;">⏳ Pending Manager Approval</span>`
          : `<span style="background: rgba(239,68,68,0.2); color: #f87171; border: 1px solid rgba(239,68,68,0.5); font-size: 11px; font-weight: bold; padding: 2px 8px; border-radius: 999px;">⚠️ Retake Required (&lt; 50%)</span>`;

        html += `
          <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 16px; margin-bottom: 12px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
              <span style="font-weight: 700; color: #fff;">\${s.test_type === 'POST' ? 'Post-Test Assessment' : 'Pre-Test Assessment'}</span>
              \${statusBadge}
            </div>
            <div style="font-size: 12px; color: #94a3b8; line-height: 1.5;">
              \${isPassed
                ? 'Your assessment was submitted successfully with score ≥ 50%. A manager must approve it to issue your official Certificate PDF.'
                : 'Score did not meet the 50% passing threshold for competency certification.'}
            </div>
          </div>
        `;
      });

      certContainer.innerHTML = html;

    } catch (err) {
      const certContainer = document.getElementById('examinee-cert-list');
      if (certContainer) certContainer.innerHTML = `<div style="color: #ef4444; padding: 14px;">Error: \${err.message}</div>`;
    }
  }

  if (btnOpen && modal) {
    btnOpen.addEventListener('click', () => {
      modal.style.display = 'flex';
      loadProfileData();
    });
  }

  if (btnClose && modal) {
    btnClose.addEventListener('click', () => {
      modal.style.display = 'none';
    });
  }

  if (modal) {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) modal.style.display = 'none';
    });
  }
}

// Boot
window.addEventListener('DOMContentLoaded', () => {
  initApp();
  setupAuthSystem();
  setupQrModal();
  setupProfileModal();
});


