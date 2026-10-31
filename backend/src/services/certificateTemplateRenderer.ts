/**
 * Official CRVS Certificate HTML/PDF Renderer
 * Uses the authentic Dire Dawa CRVS Portrait Certificate Template
 */
function escapeHtml(str: string): string {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function generateCertificateHtml(cert: {
  certificate_number?: string;
  participant_code?: string;
  full_name?: string;
  wereda_kebele?: string;
  role?: string;
  gender?: string;
  survey_title_en?: string;
  survey_title_am?: string;
  test_type?: string;
  score_percentage?: number;
  total_score?: number;
  max_score?: number;
  performance_band?: string;
  issued_by?: string;
  issued_at?: string;
  submission_id?: number;
}): string {
  const docNo = escapeHtml(cert.certificate_number || 'CRVS-2026-CERT');
  const rawDate = cert.issued_at ? new Date(cert.issued_at) : new Date();
  const dateStr = escapeHtml(rawDate.toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' }));
  const rawName = escapeHtml((cert.full_name || cert.participant_code || 'CRVS Candidate').trim());
  const participantCode = escapeHtml(cert.participant_code || 'REG-DD-001');
  const placeOfRegistration = escapeHtml(cert.wereda_kebele || 'Dire Dawa Administration');
  const role = escapeHtml(cert.role || 'Certified Vital Events Registrar');
  const rawGender = String(cert.gender || 'Female');
  const gender = escapeHtml((rawGender.toUpperCase().startsWith('M') || rawGender.includes('ወንድ')) ? 'Male (ወንድ)' : 'Female (ሴት)');
  const surveyTitleEn = escapeHtml(cert.survey_title_en || 'Dire Dawa CRVS Registrar Competency Assessment');
  const surveyTitleAm = escapeHtml(cert.survey_title_am || 'የድሬዳዋ ወሳኝ ሁነቶች ምዝገባ ብቃት ምዘና');
  const testType = escapeHtml((cert.test_type || 'POST-TEST').toUpperCase());
  const scorePct = Math.round(Number(cert.score_percentage) || 0);
  const totalScore = Number(cert.total_score) || 0;
  const maxScore = Number(cert.max_score) || 24;
  const performanceBand = escapeHtml(cert.performance_band || (scorePct >= 75 ? 'STRONG' : 'SATISFACTORY'));
  const issuingOfficer = escapeHtml(cert.issued_by || 'Dire Dawa CRVS Agency Director');
  const recordId = escapeHtml(`CRVS-REC-${cert.submission_id || '01'}`);

  return `<!DOCTYPE html>
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
        <svg class="emblem-svg" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="50" cy="50" r="46" stroke="#004c99" stroke-width="2.5" fill="#f0f6fc"/>
          <circle cx="50" cy="50" r="41" stroke="#d97706" stroke-width="1" stroke-dasharray="2 2"/>
          <line x1="28" y1="36" x2="72" y2="36" stroke="#003366" stroke-width="3" stroke-linecap="round"/>
          <circle cx="50" cy="36" r="4" fill="#d97706"/>
          <line x1="50" y1="36" x2="50" y2="74" stroke="#003366" stroke-width="3"/>
          <line x1="36" y1="74" x2="64" y2="74" stroke="#003366" stroke-width="3.5" stroke-linecap="round"/>
          <line x1="32" y1="37" x2="26" y2="52" stroke="#004c99" stroke-width="1.5"/>
          <line x1="32" y1="37" x2="38" y2="52" stroke="#004c99" stroke-width="1.5"/>
          <path d="M22 52 Q32 58 42 52 Z" fill="#003366"/>
          <line x1="68" y1="37" x2="62" y2="52" stroke="#004c99" stroke-width="1.5"/>
          <line x1="68" y1="37" x2="74" y2="52" stroke="#004c99" stroke-width="1.5"/>
          <path d="M58 52 Q68 58 78 52 Z" fill="#003366"/>
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
          <span class="meta-val">${dateStr}</span>
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
          <div class="recipient-name">${rawName}</div>
          <div class="recipient-name-underline"></div>
        </div>

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
        <div class="sig-column">
          <div class="sig-script">Mulugeta T.</div>
          <div class="sig-rule"></div>
          <div class="sig-officer-title">Training Coordinator</div>
          <div class="sig-officer-name">CRVS Capacity Building Directorate</div>
        </div>

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
</html>`;
}
