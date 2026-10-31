import SwiftUI

struct QuestionnaireView: View {
    @EnvironmentObject var assessmentVM: AssessmentViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme

    @State private var navigateToResult = false
    @State private var showConfirmSubmit = false

    var body: some View {
        ZStack {
            Color.dynamicBg(for: colorScheme)
                .ignoresSafeArea()

            if let pair = assessmentVM.currentQuestionPair {
                VStack(spacing: 0) {
                    // Top Progress & Question Tracker Bar
                    VStack(spacing: 8) {
                        HStack {
                            Text("Q\(pair.question.questionNumber) of \(assessmentVM.flatQuestions.count)")
                                .font(.headline)
                                .fontWeight(.black)
                                .foregroundColor(Color.primaryIndigo)

                            Spacer()

                            // Points Badge
                            Text("\(pair.question.points) Pt\(pair.question.points > 1 ? "s" : "")")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(Color.accentEmerald)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Color.accentEmerald.opacity(0.15))
                                .clipShape(Capsule())

                            // Text-To-Speech Button
                            Button(action: {
                                if assessmentVM.isSpeaking {
                                    assessmentVM.stopSpeaking()
                                } else {
                                    assessmentVM.speakCurrentQuestion()
                                }
                            }) {
                                Image(systemName: assessmentVM.isSpeaking ? "speaker.wave.3.fill" : "speaker.wave.2")
                                    .font(.system(size: 16))
                                    .foregroundColor(assessmentVM.isSpeaking ? Color.accentAmber : Color.primaryIndigo)
                                    .frame(width: 34, height: 34)
                                    .background(Color.primaryIndigo.opacity(0.12))
                                    .clipShape(Circle())
                            }
                        }

                        // Progress Bar
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(Color.dynamicBorder(for: colorScheme))
                                    .frame(height: 6)

                                RoundedRectangle(cornerRadius: 4)
                                    .fill(
                                        LinearGradient(
                                            colors: [Color.primaryIndigo, Color.accentEmerald],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                    .frame(width: max(0, geo.size.width * CGFloat(assessmentVM.progress)), height: 6)
                                    .animation(.easeInOut(duration: 0.25), value: assessmentVM.progress)
                            }
                        }
                        .frame(height: 6)
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(Color.dynamicCard(for: colorScheme))

                    // Scrollable Question Content
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            // Section Badge
                            HStack {
                                Text(pair.sectionTitle)
                                    .font(.caption2)
                                    .fontWeight(.heavy)
                                    .foregroundColor(Color.primaryIndigo)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 4)
                                    .background(Color.primaryIndigo.opacity(0.12))
                                    .clipShape(Capsule())

                                Spacer()

                                Text(pair.question.type.displayName)
                                    .font(.caption2)
                                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                            }
                            .padding(.top, 14)

                            // Prompts Card
                            VStack(alignment: .leading, spacing: 10) {
                                Text(pair.question.promptAm)
                                    .font(.system(size: 17, weight: .bold))
                                    .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                    .lineSpacing(4)

                                if let en = pair.question.promptEn, !en.isEmpty, en != pair.question.promptAm {
                                    Text(en)
                                        .font(.subheadline)
                                        .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                        .italic()
                                }
                            }
                            .padding(18)
                            .cardStyle(cornerRadius: 18)

                            // Question Input Area Based on QuestionType
                            VStack(alignment: .leading, spacing: 12) {
                                switch pair.question.type {
                                case .singleChoice:
                                    mcqOptionsView(question: pair.question)
                                case .textOpen:
                                    scenarioTextInputView(question: pair.question)
                                case .ratingScale:
                                    ratingScaleView(question: pair.question)
                                }
                            }

                            Spacer(minLength: 40)
                        }
                        .padding(.horizontal, 20)
                    }

                    // Bottom Navigation Bar
                    HStack(spacing: 12) {
                        // Previous Button
                        Button(action: { assessmentVM.previousQuestion() }) {
                            HStack(spacing: 6) {
                                Image(systemName: "chevron.left")
                                Text("Previous")
                                    .fontWeight(.bold)
                            }
                            .frame(height: 48)
                            .frame(maxWidth: .infinity)
                            .background(Color.dynamicSecondary(for: colorScheme))
                            .foregroundColor(assessmentVM.currentIndex == 0 ? Color.dynamicTextMuted(for: colorScheme).opacity(0.5) : Color.dynamicTextPrimary(for: colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(assessmentVM.currentIndex == 0)

                        // Next or Submit Button
                        if assessmentVM.currentIndex < assessmentVM.flatQuestions.count - 1 {
                            Button(action: { assessmentVM.nextQuestion() }) {
                                HStack(spacing: 6) {
                                    Text("Next")
                                        .fontWeight(.bold)
                                    Image(systemName: "chevron.right")
                                }
                                .frame(height: 48)
                                .frame(maxWidth: .infinity)
                                .background(Color.primaryIndigo)
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        } else {
                            Button(action: { showConfirmSubmit = true }) {
                                HStack(spacing: 6) {
                                    if assessmentVM.isSubmitting {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    } else {
                                        Image(systemName: "checkmark.circle.fill")
                                        Text("Submit Exam")
                                            .fontWeight(.bold)
                                    }
                                }
                                .frame(height: 48)
                                .frame(maxWidth: .infinity)
                                .background(Color.accentEmerald)
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .disabled(assessmentVM.isSubmitting)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 14)
                    .background(Color.dynamicCard(for: colorScheme))
                }
            } else {
                ProgressView("Loading CRVS Exam...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    HStack(spacing: 4) {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .bold))
                        Text("Exit")
                    }
                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                }
            }
        }
        .alert("Submit Assessment", isPresented: $showConfirmSubmit) {
            Button("Review Answers", role: .cancel) { }
            Button("Submit Now", role: .none) {
                Task {
                    let success = await assessmentVM.submitAssessment()
                    if success {
                        navigateToResult = true
                    }
                }
            }
        } message: {
            Text("You have answered \(assessmentVM.answeredCount) of \(assessmentVM.flatQuestions.count) questions. Are you ready to submit your exam?")
        }
        .navigationDestination(isPresented: $navigateToResult) {
            AssessmentResultView()
        }
    }

    // MARK: - MCQ Input
    private func mcqOptionsView(question: Question) -> some View {
        VStack(spacing: 10) {
            ForEach(question.options ?? []) { opt in
                let isSelected = assessmentVM.answers[question.id]?.selectedOptionKey == opt.optionKey
                Button(action: {
                    assessmentVM.selectMCQOption(questionId: question.id, optionKey: opt.optionKey)
                }) {
                    HStack(alignment: .top, spacing: 12) {
                        Text(opt.optionKey)
                            .font(.system(size: 14, weight: .black))
                            .foregroundColor(isSelected ? .white : Color.primaryIndigo)
                            .frame(width: 32, height: 32)
                            .background(isSelected ? Color.primaryIndigo : Color.primaryIndigo.opacity(0.12))
                            .clipShape(Circle())

                        VStack(alignment: .leading, spacing: 3) {
                            Text(opt.labelAm)
                                .font(.subheadline)
                                .fontWeight(isSelected ? .bold : .medium)
                                .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                .multilineTextAlignment(.leading)

                            if let en = opt.labelEn, !en.isEmpty, en != opt.labelAm {
                                Text(en)
                                    .font(.caption2)
                                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                    .multilineTextAlignment(.leading)
                            }
                        }

                        Spacer()

                        Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(isSelected ? Color.primaryIndigo : Color.dynamicTextMuted(for: colorScheme))
                            .font(.title3)
                    }
                    .padding(14)
                    .background(isSelected ? Color.primaryIndigo.opacity(0.10) : Color.dynamicCard(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(isSelected ? Color.primaryIndigo : Color.dynamicBorder(for: colorScheme), lineWidth: isSelected ? 2 : 1)
                    )
                }
            }
        }
    }

    // MARK: - Scenario Input
    private func scenarioTextInputView(question: Question) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Write your detailed scenario decision / መልስዎን እዚህ ይጻፉ:")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

            TextEditor(text: Binding(
                get: { assessmentVM.answers[question.id]?.answerText ?? "" },
                set: { assessmentVM.setTextAnswer(questionId: question.id, text: $0) }
            ))
            .frame(minHeight: 120)
            .padding(10)
            .background(Color.dynamicCard(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.primaryIndigo.opacity(0.4), lineWidth: 1)
            )

            if let rubric = question.rubricAm {
                Text("Rubric: \(rubric)")
                    .font(.caption2)
                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                    .padding(8)
                    .background(Color.dynamicSecondary(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
    }

    // MARK: - Rating Scale Input
    private func ratingScaleView(question: Question) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Rate your agreement or confidence level (ደረጃ ይምረጡ):")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

            let currentVal = assessmentVM.answers[question.id]?.ratingValue ?? 0

            HStack(spacing: 12) {
                ForEach(1...5, id: \.self) { r in
                    let isSelected = currentVal == r
                    Button(action: {
                        assessmentVM.setRatingAnswer(questionId: question.id, rating: r)
                    }) {
                        VStack(spacing: 4) {
                            Text("\(r)")
                                .font(.headline)
                                .fontWeight(.black)
                                .foregroundColor(isSelected ? .white : Color.primaryIndigo)

                            Image(systemName: "star.fill")
                                .font(.caption2)
                                .foregroundColor(isSelected ? .white : Color.accentAmber)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 60)
                        .background(isSelected ? Color.primaryIndigo : Color.dynamicCard(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(isSelected ? Color.primaryIndigo : Color.dynamicBorder(for: colorScheme), lineWidth: isSelected ? 2 : 1)
                        )
                    }
                }
            }

            HStack {
                Text("1: ዝቅተኛ (Low)")
                    .font(.caption2)
                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                Spacer()
                Text("5: ከፍተኛ (High)")
                    .font(.caption2)
                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
            }
        }
    }
}
