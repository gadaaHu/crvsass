import SwiftUI

struct AdminPortalView: View {
    @EnvironmentObject var adminVM: AdminViewModel
    @EnvironmentObject var assessmentVM: AssessmentViewModel
    @EnvironmentObject var authVM: AuthViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme

    @State private var selectedTab: Int = 0
    @State private var showAddQuestionSheet = false
    @State private var questionToEdit: Question?
    @State private var questionToDelete: Question?
    @State private var showDeleteQuestionAlert = false
    @State private var submissionToDelete: SubmissionRecordItem?
    @State private var showDeleteSubmissionAlert = false

    @State private var serverURLInput = ""

    var body: some View {
        ZStack {
            Color.dynamicBg(for: colorScheme)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // 4-Tab Custom Segmented Bar
                HStack(spacing: 6) {
                    tabButton(index: 0, title: "Results (\(adminVM.submissions.count))", icon: "chart.bar.xaxis")
                    tabButton(index: 1, title: "Users (\(adminVM.users.count))", icon: "person.2.fill")
                    tabButton(index: 2, title: "Questions (\(assessmentVM.flatQuestions.count))", icon: "doc.text.fill")
                    tabButton(index: 3, title: "Config", icon: "gearshape.fill")
                }
                .padding(6)
                .background(Color.dynamicCard(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .padding(.horizontal, 16)
                .padding(.vertical, 10)

                // Tab Content
                TabView(selection: $selectedTab) {
                    // Tab 0: Submissions & Exam Results
                    submissionsView
                        .tag(0)

                    // Tab 1: User Management
                    UserManagementView()
                        .tag(1)

                    // Tab 2: Question Bank & Authoring
                    questionBankView
                        .tag(2)

                    // Tab 3: System Config
                    systemConfigView
                        .tag(3)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
        }
        .navigationTitle(authVM.currentUser?.role == "USER_MANAGER" ? "User Management" : "Admin Portal")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    Task {
                        await adminVM.loadAllData()
                        await assessmentVM.loadSurvey()
                    }
                }) {
                    Image(systemName: "arrow.clockwise")
                        .font(.caption)
                }
            }
        }
        .sheet(isPresented: $showAddQuestionSheet) {
            AddQuestionSheet()
        }
        .sheet(item: $questionToEdit) { q in
            EditQuestionSheet(question: q)
        }
        .alert("Delete Question", isPresented: $showDeleteQuestionAlert) {
            Button("Cancel", role: .cancel) { questionToDelete = nil }
            Button("Delete", role: .destructive) {
                if let q = questionToDelete {
                    Task {
                        let success = await adminVM.deleteQuestion(questionId: q.id)
                        if success {
                            await assessmentVM.loadSurvey()
                            questionToDelete = nil
                        }
                    }
                }
            }
        } message: {
            if let q = questionToDelete {
                Text("Are you sure you want to delete question Q\(q.questionNumber)? It will be permanently removed from all assessments.")
            }
        }
        .alert("Delete Exam Submission", isPresented: $showDeleteSubmissionAlert) {
            Button("Cancel", role: .cancel) { submissionToDelete = nil }
            Button("Delete", role: .destructive) {
                if let sub = submissionToDelete {
                    Task {
                        _ = await adminVM.deleteSubmission(submissionId: sub.id)
                        submissionToDelete = nil
                    }
                }
            }
        } message: {
            if let sub = submissionToDelete {
                Text("Are you sure you want to delete the exam record for \(sub.participantCode) (\(sub.testType))? This will allow the examinee to retake the exam.")
            }
        }
        .task {
            await adminVM.loadAllData()
            serverURLInput = await APIService.shared.getBaseURL()
        }
    }

    // MARK: - Tab Button Component
    private func tabButton(index: Int, title: String, icon: String) -> some View {
        let isSelected = selectedTab == index
        return Button(action: { selectedTab = index }) {
            VStack(spacing: 3) {
                Image(systemName: icon)
                    .font(.caption)
                Text(title)
                    .font(.system(size: 10, weight: isSelected ? .bold : .medium))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(isSelected ? Color.primaryIndigo : Color.clear)
            .foregroundColor(isSelected ? .white : Color.dynamicTextMuted(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }

    // MARK: - Tab 0: Submissions View
    private var submissionsView: some View {
        ScrollView {
            VStack(spacing: 14) {
                // Filter Tabs (ALL / PRE / POST)
                HStack(spacing: 8) {
                    ForEach(["ALL", "PRE", "POST"], id: \.self) { type in
                        let isSelected = adminVM.filterTestType == type
                        Button(action: { adminVM.filterTestType = type }) {
                            Text(type == "ALL" ? "All Submissions" : "\(type)-Test")
                                .font(.caption)
                                .fontWeight(isSelected ? .bold : .medium)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(isSelected ? Color.primaryIndigo : Color.dynamicCard(for: colorScheme))
                                .foregroundColor(isSelected ? .white : Color.dynamicTextMuted(for: colorScheme))
                                .clipShape(Capsule())
                        }
                    }
                    Spacer()
                }

                if adminVM.filteredSubmissions.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: "doc.text.magnifyingglass")
                            .font(.largeTitle)
                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                        Text("No exam submissions recorded yet")
                            .font(.subheadline)
                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
                } else {
                    LazyVStack(spacing: 10) {
                        ForEach(adminVM.filteredSubmissions) { sub in
                            VStack(spacing: 8) {
                                HStack(spacing: 12) {
                                    VStack(alignment: .leading, spacing: 3) {
                                        HStack(spacing: 6) {
                                            Text(sub.participantCode)
                                                .font(.subheadline)
                                                .fontWeight(.bold)
                                                .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                                            Text(sub.testType)
                                                .font(.caption2)
                                                .fontWeight(.heavy)
                                                .foregroundColor(Color.primaryIndigo)
                                                .padding(.horizontal, 6)
                                                .padding(.vertical, 2)
                                                .background(Color.primaryIndigo.opacity(0.12))
                                                .clipShape(RoundedRectangle(cornerRadius: 4))
                                        }

                                        Text(sub.weredaKebele ?? "Dire Dawa CRVS")
                                            .font(.caption)
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                                        Text("🕒 \(sub.submittedAt)")
                                            .font(.caption2)
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme).opacity(0.7))
                                    }

                                    Spacer()

                                    VStack(alignment: .trailing, spacing: 4) {
                                        Text("\(Int(sub.scorePercentage))%")
                                            .font(.headline)
                                            .fontWeight(.black)
                                            .foregroundColor(sub.scorePercentage >= 60.0 ? Color.accentEmerald : Color.accentAmber)

                                        Text(sub.performanceBand)
                                            .font(.caption2)
                                            .fontWeight(.bold)
                                            .foregroundColor(sub.scorePercentage >= 60.0 ? Color.accentEmerald : Color.accentAmber)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background((sub.scorePercentage >= 60.0 ? Color.accentEmerald : Color.accentAmber).opacity(0.12))
                                            .clipShape(Capsule())
                                    }
                                }

                                Divider()
                                    .padding(.vertical, 2)

                                HStack {
                                    Text("Score: \(Int(sub.totalScore))/\(Int(sub.maxScore))")
                                        .font(.caption2)
                                        .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                                    Spacer()

                                    Button(role: .destructive, action: {
                                        submissionToDelete = sub
                                        showDeleteSubmissionAlert = true
                                    }) {
                                        HStack(spacing: 4) {
                                            Image(systemName: "trash")
                                            Text("Reset / Delete")
                                        }
                                        .font(.caption2)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.red)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.red.opacity(0.1))
                                        .clipShape(RoundedRectangle(cornerRadius: 6))
                                    }
                                }
                            }
                            .padding(14)
                            .cardStyle(cornerRadius: 14)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
        }
    }

    // MARK: - Tab 2: Question Bank View (with Add Question & Delete)
    private var questionBankView: some View {
        ScrollView {
            VStack(spacing: 14) {
                // Header with Add Question Action
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Question Bank (ጥያቄዎች)")
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                        Text("\(assessmentVM.flatQuestions.count) Questions active in exam")
                            .font(.caption)
                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                    }

                    Spacer()

                    // Add Question Button (Mobile authoring)
                    Button(action: { showAddQuestionSheet = true }) {
                        HStack(spacing: 4) {
                            Image(systemName: "plus.circle.fill")
                            Text("Add Question")
                                .fontWeight(.bold)
                        }
                        .font(.caption)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.primaryIndigo)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }

                // Questions List
                LazyVStack(spacing: 12) {
                    ForEach(assessmentVM.flatQuestions, id: \.question.id) { pair in
                        let q = pair.question
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("Q\(q.questionNumber)")
                                    .font(.caption)
                                    .fontWeight(.black)
                                    .foregroundColor(Color.primaryIndigo)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color.primaryIndigo.opacity(0.12))
                                    .clipShape(RoundedRectangle(cornerRadius: 6))

                                Text("\(q.points) Pt\(q.points > 1 ? "s" : "")")
                                    .font(.caption2)
                                    .fontWeight(.bold)
                                    .foregroundColor(Color.accentEmerald)

                                Text(q.type.displayName)
                                    .font(.caption2)
                                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                                Spacer()

                                // Edit Button
                                Button(action: { questionToEdit = q }) {
                                    Image(systemName: "pencil")
                                        .font(.caption)
                                        .foregroundColor(Color.primaryIndigo)
                                        .frame(width: 30, height: 30)
                                        .background(Color.primaryIndigo.opacity(0.12))
                                        .clipShape(Circle())
                                }

                                // Delete Button
                                Button(action: {
                                    questionToDelete = q
                                    showDeleteQuestionAlert = true
                                }) {
                                    Image(systemName: "trash")
                                        .font(.caption)
                                        .foregroundColor(Color.accentDanger)
                                        .frame(width: 30, height: 30)
                                        .background(Color.accentDanger.opacity(0.12))
                                        .clipShape(Circle())
                                }
                            }

                            Text(q.promptAm)
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                .lineLimit(3)

                            if let en = q.promptEn, !en.isEmpty, en != q.promptAm {
                                Text(en)
                                    .font(.caption)
                                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                    .italic()
                                    .lineLimit(2)
                            }

                            if let opts = q.options, !opts.isEmpty {
                                HStack(spacing: 6) {
                                    ForEach(opts) { o in
                                        Text("\(o.optionKey)\(o.isCorrect ? "✓" : "")")
                                            .font(.system(size: 10, weight: .bold))
                                            .foregroundColor(o.isCorrect ? Color.accentEmerald : Color.dynamicTextMuted(for: colorScheme))
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(o.isCorrect ? Color.accentEmerald.opacity(0.15) : Color.dynamicSecondary(for: colorScheme))
                                            .clipShape(RoundedRectangle(cornerRadius: 4))
                                    }
                                }
                            }
                        }
                        .padding(14)
                        .cardStyle(cornerRadius: 14)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
        }
    }

    // MARK: - Tab 3: System Config View
    private var systemConfigView: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Server Base URL Configuration
                VStack(alignment: .leading, spacing: 10) {
                    Text("Backend API Server URL")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                    TextField("http://IP:4000/api", text: $serverURLInput)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                        .padding(12)
                        .background(Color.dynamicSecondary(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                    Button(action: {
                        Task {
                            await APIService.shared.setBaseURL(serverURLInput)
                            await adminVM.loadAllData()
                            await assessmentVM.loadSurvey()
                        }
                    }) {
                        HStack {
                            Image(systemName: "network")
                            Text("Save & Reconnect")
                                .fontWeight(.bold)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(Color.primaryIndigo)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(16)
                .cardStyle(cornerRadius: 16)

                // Sign Out
                Button(action: { authVM.signOut() }) {
                    HStack {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                        Text("Sign Out")
                            .fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(Color.accentDanger.opacity(0.12))
                    .foregroundColor(Color.accentDanger)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
        }
    }
}
