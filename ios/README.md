# CRVS Gather · iOS Mobile Application (Swift & SwiftUI)

Native iOS mobile application built with **Swift 5.9+**, **SwiftUI**, and modern **MVVM** architecture for the **Dire Dawa CRVS Vital Events & Family Law Knowledge Assessment System**.

---

## 📱 Features

1. **Clean Authentication**:
   - Native Sign In & Sign Up with role selector (`Examinee`, `User Manager`, `Admin`).
   - Completely free of hardcoded demo logins.
   - Session persistence in `UserDefaults` and secure token management.

2. **Dashboard & Exam Mode Selection**:
   - Real-time profile card showing user role badge (`ADMIN`, `USER MGR`, `EXAMINEE`), participant code, and office.
   - Survey overview card showing points, question count, and pass threshold.
   - Quick selection between **Pre-Test (ቅድመ)** and **Post-Test (ድህረ)** modes.
   - Section overviews (Proclamation 1370/2017, Digital CRVS, Case Scenarios).

3. **Interactive Assessment Experience**:
   - Stepper progress bar with points and question number.
   - **Text-to-Speech (AVSpeechSynthesizer)** for listening to Amharic & English questions aloud.
   - **Multiple Choice Questions (MCQ)** with quick-touch option buttons.
   - **Scenario / Short Answer Questions** with rich text editor and rubric preview.
   - **1–5 Rating Scale Questions** with star ratings.
   - Instant score calculation and performance band feedback.

4. **Admin & User Management Portal**:
   - **Exam Results**: Filter by All, Pre, or Post assessments, scores, bands, and timestamps.
   - **User Management**: KPI counters (Total, Admins, Managers, Examinees), search, role filter chips, role promotion/assignment dialog (`ADMIN`, `USER_MANAGER`, `EXAMINEE`), and account deletion.
   - **Question Bank & Live Authoring**:
     - **Add Question**: Author all kinds of questions directly from mobile (Section picker, MCQ with option builder & correct answer toggle, Scenario with rubrics, Rating scale).
     - **Edit Question**: Live editing of prompts, points, and correct answers.
     - **Delete Question**: Remove deprecated questions with confirmation.
   - **System Config**: Dynamic server IP/URL reconfiguration and reconnect.

---

## 📂 Project Structure

```
ios/CRVSGather/
├── Package.swift                     # Swift Package Manager manifest
├── App/
│   └── CRVSGatherApp.swift           # Main App entry point
├── Models/
│   └── SurveyModels.swift            # Swift Codable data models
├── Networking/
│   └── APIService.swift              # async/await URLSession API client
├── ViewModels/
│   ├── AuthViewModel.swift           # Authentication & session state
│   ├── AssessmentViewModel.swift     # Exam taking, progress, speech & scoring
│   └── AdminViewModel.swift          # User management, questions & submissions
└── Views/
    ├── Auth/
    │   ├── SignInView.swift          # Clean sign in
    │   └── SignUpView.swift          # Role-based registration
    ├── Home/
    │   └── HomeView.swift            # Main dashboard & portal launcher
    ├── Questionnaire/
    │   ├── QuestionnaireView.swift   # Interactive question flow
    │   └── AssessmentResultView.swift# Score & performance band review
    ├── Admin/
    │   ├── AdminPortalView.swift     # 4-tab admin control center
    │   ├── AddQuestionSheet.swift    # Mobile question creator (all types)
    │   ├── EditQuestionSheet.swift   # Live question editor
    │   └── UserManagementView.swift  # User list, role switcher & deletion
    └── Components/
        └── Theme.swift               # Palette & card styling modifiers
```

---

## 🚀 How to Run in Xcode

1. Open Xcode on macOS:
   ```bash
   open ios/CRVSGather/Package.swift
   ```
   Or create a new SwiftUI iOS App in Xcode and drag the `CRVSGather` folder into your project.
2. Target: **iOS 16.0+** (iPhone & iPad supported).
3. Ensure the backend server is running (`npm start` on port 4000).
4. Update the server URL in **Admin Portal -> Config** or enter your local Mac/network IP (e.g., `http://192.168.1.10:4000/api`).
