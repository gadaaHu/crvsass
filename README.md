# CRVS — Interactive Assessment & Civil Registration System
### CRVS Field Assessment & Digital Survey Suite

A complete, enterprise-grade, offline-first Question-and-Answer Gathering & Assessment System featuring:
1. **Native Android Application in Kotlin**: Built with Jetpack Compose, Material 3, MVVM Architecture, StateFlow, Room Database, and Retrofit.
2. **High-Performance Backend**: Production TypeScript / Node.js architecture with Clean Architecture, SQLite transactional database (`node:sqlite`), automated scoring, pre/post evaluation analytics, and an interactive Web Management Dashboard.
3. **Pre-Loaded Curriculum**: The official **Dire Dawa Administration CRVS Registrar Pre & Post Training Knowledge Assessment** (Proclamation No. 1370/2017 & New CRVS Digital System) with full Amharic and English localization, answer keys, and grading rubrics.
4. **🌗 Light & Dark Mode**: Persistent user-selectable UI theme (DataStore) — toggle via 🌙/☀ button in Home or Settings screen.
5. **📄 PDF Export**: Instantly generate and download a branded A4 PDF result report from the Result screen (no external libraries — uses Android's built-in `PdfDocument` API).
6. **✏ Editable Questions**: Administrators can edit question prompts, translations, points, and correct answers live from the Settings screen.
7. **🔐 User Sign Up, Admin & Sign In to Take Exam**:
   - Role-based access control with **Admin** (`admin` / `admin123`) and **Examinee** (`examinee1` / `user123`) roles.
   - Examinees sign in and can immediately take the exam (Pre-Test or Post-Test) with automatic participant profile linking.
   - Admins unlock the Question Editor, analytics control, and question bank management.
8. **📋 Training & Technical Assessment Governance**: Complete operational framework, trainee response management SOP, analysis plan, and standardized technical evaluation checklist documented in [TRAINING_AND_ASSESSMENT_FRAMEWORK.md](file:///c:/inetpub/wwwroot/CRVSAFR/TRAINING_AND_ASSESSMENT_FRAMEWORK.md).

---

## 🏛 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         CRVS System                         │
└─────────────────────────────────────────────────────────────┘
                               │
       ┌───────────────────────┴───────────────────────┐
       ▼                                               ▼
┌─────────────────────────────┐         ┌─────────────────────────────┐
│     Android App (Kotlin)    │         │       Backend Server        │
│  com.qcafr.gatherapp        │         │   (TypeScript / Node.js)    │
├─────────────────────────────┤         ├─────────────────────────────┤
│ • Jetpack Compose UI (M3)   │         │ • Clean Architecture        │
│ • StateFlow & ViewModel     │ ◄─────► │ • Native SQLite (node:sqlite)│
│ • Room DB (Offline Queue)   │  REST   │ • Automated Scoring Engine  │
│ • Retrofit & Moshi          │  JSON   │ • Pre vs Post Analytics     │
│ • TTS Audio Question Prompt │         │ • Web Dashboard & CSV Export│
│ • Bilingual: Amharic + Eng  │         │ • Live on port 4000         │
└─────────────────────────────┘         └─────────────────────────────┘
```

---

## 📱 1. Android Application (Kotlin)

Located in `android/`:
- **Package**: `com.qcafr.gatherapp`
- **Language**: Kotlin 2.0+
- **UI Framework**: Modern Jetpack Compose with Material 3 design system.
- **Data Persistence**: AndroidX Room Database for 100% offline-first questionnaire answering.
- **Networking**: Retrofit 2 + Moshi + OkHttp with connection timeouts and auto-retry.
- **Speech**: Integrated Text-to-Speech (TTS) engine that reads question prompts aloud.

### Key Screens & Flow:
1. **[HomeScreen](file:///c:/Users/Administrator/Desktop/QCAFR/android/app/src/main/java/com/qcafr/gatherapp/ui/screens/home/HomeScreen.kt)**: Assessment curriculum summary, passing score indicator (15/24 points), and pending submission synchronization counter.
2. **[ParticipantScreen](file:///c:/Users/Administrator/Desktop/QCAFR/android/app/src/main/java/com/qcafr/gatherapp/ui/screens/participant/ParticipantScreen.kt)**: Demographic data capture (Code, Kebele, Role, Gender, Experience) and Pre-Test vs Post-Test mode selector.
3. **[QuestionnaireScreen](file:///c:/Users/Administrator/Desktop/QCAFR/android/app/src/main/java/com/qcafr/gatherapp/ui/screens/questionnaire/QuestionnaireScreen.kt)**: Wizard asking questions step-by-step with progress bar, multiple choice cards (`ሀ`, `ለ`, `ሐ`, `መ`), rating chips, and scenario text responses.
4. **[ReviewScreen](file:///c:/Users/Administrator/Desktop/QCAFR/android/app/src/main/java/com/qcafr/gatherapp/ui/screens/review/ReviewScreen.kt)**: Overview of all answered questions before final submission; tap any question to jump back and revise.
5. **[ResultScreen](file:///c:/Users/Administrator/Desktop/QCAFR/android/app/src/main/java/com/qcafr/gatherapp/ui/screens/result/ResultScreen.kt)**: Instant score presentation, classification badge (`STRONG`, `SATISFACTORY`, `NEEDS_REINFORCEMENT`), and section-by-section score breakdown.
6. **[SettingsScreen](file:///c:/Users/Administrator/Desktop/QCAFR/android/app/src/main/java/com/qcafr/gatherapp/ui/screens/settings/SettingsScreen.kt)**: Dedicated settings management allowing administrators to live-edit question prompts (Amharic & English), adjust points, change correct answers, and configure the backend server endpoint.

### How to Open & Build the Android App:
1. Open **Android Studio** (Koala / Ladybug or newer).
2. Choose **Open Project** and select `c:\Users\Administrator\Desktop\QCAFR\android`.
3. Gradle will sync dependencies automatically via `libs.versions.toml`.
4. Click **Run 'app'** on an Android Emulator (API 24+) or connected physical device.
   - *Note*: On the emulator, `NetworkClient.kt` uses `http://10.0.2.2:4000/api/` by default to reach the host backend.

---

## ⚡ 2. Backend Server & Web Dashboard

Located in `backend/`:
- **Language**: TypeScript / Node.js (Node 24)
- **Database**: Native SQLite engine (`node:sqlite`) with zero external binary build prerequisites.
- **REST APIs**: Fast, typed endpoints with CORS and JSON serialization.
- **Web UI**: Glassmorphic, modern dark-themed web management console at `http://localhost:4000`.

### Running the Backend:
```powershell
cd c:\Users\Administrator\Desktop\QCAFR\backend
npm start
```
The server will run at:
- **Web Dashboard**: [http://localhost:4000](http://localhost:4000)
- **API Health**: [http://localhost:4000/api/health](http://localhost:4000/api/health)
- **Surveys API**: [http://localhost:4000/api/surveys](http://localhost:4000/api/surveys)
- **Analytics API**: [http://localhost:4000/api/analytics/1](http://localhost:4000/api/analytics/1)

### REST Endpoints:
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Service health status |
| `GET` | `/api/surveys` | List all assessments with section & submission counts |
| `GET` | `/api/surveys/:id` | Full questionnaire with sections, questions, options, and rubrics |
| `POST` | `/api/surveys` | Dynamically create new survey questionnaires |
| `GET` | `/api/questions/:id` | Retrieve single question details and choice options |
| `PUT` | `/api/questions/:id` | Edit question prompt (Amharic & English), points, type, choices, and correct answer |
| `POST` | `/api/sections/:sectionId/questions` | Add a new question to a section |
| `DELETE` | `/api/questions/:id` | Delete question and cascade delete options |
| `POST` | `/api/submissions` | Submit participant answers with automated scoring |
| `GET` | `/api/submissions` | Filter submissions by survey, test mode (`PRE`/`POST`), or code |
| `GET` | `/api/submissions/:id` | Detailed answer breakdown with correct option comparison |
| `PATCH` | `/api/submissions/:id/grade` | Grade open scenario questions using evaluation rubric |
| `GET` | `/api/analytics/:surveyId` | Pre vs Post comparison, pass rates, question accuracy % |
| `GET` | `/api/export/:surveyId/csv` | Download complete dataset as CSV |
| `GET` | `/api/export/:surveyId/json` | Download complete dataset as JSON |

---

## 📋 3. Pre-Loaded Dire Dawa CRVS Assessment Content

Based on Ethiopian Civil and Family Registration Proclamation No. 1370/2017:
- **Total Points**: 24 Points (Passing threshold: 15 Points / 62.5%)
- **Sections**:
  - **Part A (Q1–Q8)**: Proclamation 1370/2017 principles, deadlines (90 days for birth, 50 days for death, 30 days for other events), family registration definitions.
  - **Part B (Q9–Q16)**: Digital CRVS operations, duplicate search prior to entry, mandatory death fields, amendment protocols, credential confidentiality, offline data guidelines.
  - **Part C (Q17–Q20)**: Real-world field scenarios (late unregistered births, delayed death declarations, duplicate records deduplication, privacy protection).
  - **Section D (Q21–Q26)**: Post-training 5-dimension Likert ratings and feedback comments.
