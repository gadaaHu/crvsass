import SwiftUI

struct AddQuestionSheet: View {
    @EnvironmentObject var adminVM: AdminViewModel
    @EnvironmentObject var assessmentVM: AssessmentViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme

    @State private var selectedSectionId: Int = 1
    @State private var questionType: String = "SINGLE_CHOICE" // SINGLE_CHOICE, TEXT_OPEN, RATING_SCALE
    @State private var promptAm: String = ""
    @State private var promptEn: String = ""
    @State private var pointsText: String = "1"
    @State private var rubricAm: String = ""
    @State private var rubricEn: String = ""

    // Dynamic MCQ Options
    @State private var options: [(key: String, label: String, isCorrect: Bool)] = [
        ("A", "", true),
        ("B", "", false),
        ("C", "", false),
        ("D", "", false)
    ]

    @State private var isSaving = false
    @State private var validationError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                Color.dynamicBg(for: colorScheme)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        // 1. Target Section Picker
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Target Section (የጥያቄው ክፍል) *")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            let sections = assessmentVM.survey?.sections ?? [
                                Section(id: 1, surveyId: 1, code: "SECTION_A", titleAm: "Part A - Proclamation 1370/2017", titleEn: nil, descriptionAm: nil, descriptionEn: nil, orderIndex: 0, questions: nil),
                                Section(id: 2, surveyId: 1, code: "SECTION_B", titleAm: "Part B - Digital CRVS System", titleEn: nil, descriptionAm: nil, descriptionEn: nil, orderIndex: 1, questions: nil),
                                Section(id: 3, surveyId: 1, code: "SECTION_C", titleAm: "Part C - Practical Scenarios", titleEn: nil, descriptionAm: nil, descriptionEn: nil, orderIndex: 2, questions: nil)
                            ]

                            VStack(spacing: 6) {
                                ForEach(sections) { sec in
                                    let isSelected = selectedSectionId == sec.id
                                    Button(action: { selectedSectionId = sec.id }) {
                                        HStack {
                                            Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                                                .foregroundColor(isSelected ? Color.primaryIndigo : Color.dynamicTextMuted(for: colorScheme))
                                            Text(sec.titleAm)
                                                .font(.subheadline)
                                                .fontWeight(isSelected ? .bold : .medium)
                                                .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                            Spacer()
                                        }
                                        .padding(10)
                                        .background(isSelected ? Color.primaryIndigo.opacity(0.12) : Color.dynamicCard(for: colorScheme))
                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                    }
                                }
                            }
                        }

                        // 2. Question Type Selector (Single Choice MCQ, Open Scenario, Rating Scale)
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Question Type (የጥያቄ ዓይነት) *")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            HStack(spacing: 8) {
                                typeCard(type: "SINGLE_CHOICE", label: "MCQ\n(ምርጫ)", icon: "list.bullet.circle.fill", color: Color.primaryIndigo)
                                typeCard(type: "TEXT_OPEN", label: "Scenario\n(ክፍት)", icon: "text.justify.leading", color: Color.accentEmerald)
                                typeCard(type: "RATING_SCALE", label: "Rating\n(ደረጃ)", icon: "star.circle.fill", color: Color.accentAmber)
                            }
                        }

                        // 3. Amharic Prompt Input
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Amharic Prompt (በአማርኛ) *")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            TextEditor(text: $promptAm)
                                .frame(height: 80)
                                .padding(8)
                                .background(Color.dynamicCard(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                                )
                        }

                        // 4. English Prompt Input
                        VStack(alignment: .leading, spacing: 6) {
                            Text("English Prompt (Optional)")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            TextEditor(text: $promptEn)
                                .frame(height: 60)
                                .padding(8)
                                .background(Color.dynamicCard(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                                )
                        }

                        // 5. Points
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Points / ነጥብ (Default: 1)")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            TextField("Points", text: $pointsText)
                                .keyboardType(.numberPad)
                                .padding(12)
                                .background(Color.dynamicCard(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                                )
                        }

                        // 6. Dynamic Section Based on Question Type
                        if questionType == "SINGLE_CHOICE" {
                            mcqOptionsSection
                        } else if questionType == "TEXT_OPEN" {
                            scenarioRubricSection
                        } else if questionType == "RATING_SCALE" {
                            ratingScaleSection
                        }

                        if let err = validationError {
                            Text(err)
                                .font(.caption)
                                .foregroundColor(Color.accentDanger)
                        }

                        // Submit Button
                        Button(action: saveQuestion) {
                            HStack {
                                if isSaving {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Image(systemName: "plus.circle.fill")
                                    Text("Add Question to Exam ✓")
                                        .fontWeight(.bold)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(Color.primaryIndigo)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .disabled(isSaving)

                        Spacer(minLength: 30)
                    }
                    .padding(20)
                }
            }
            .navigationTitle("Add New Question")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    // MARK: - Subcomponents
    private func typeCard(type: String, label: String, icon: String, color: Color) -> some View {
        let isSelected = questionType == type
        return Button(action: { questionType = type }) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.title3)
                Text(label)
                    .font(.system(size: 11, weight: isSelected ? .bold : .medium))
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 64)
            .background(isSelected ? color.opacity(0.18) : Color.dynamicCard(for: colorScheme))
            .foregroundColor(isSelected ? color : Color.dynamicTextMuted(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? color : Color.dynamicBorder(for: colorScheme), lineWidth: isSelected ? 2 : 1)
            )
        }
    }

    private var mcqOptionsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Multiple Choice Options (የምርጫ አማራጮች):")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                Spacer()

                Button(action: {
                    let nextKey = String(UnicodeScalar(65 + options.count)!)
                    options.append((nextKey, "", false))
                }) {
                    Text("+ Add Option")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(Color.primaryIndigo)
                }
            }

            Text("Tap the checkmark circle to mark the official correct answer.")
                .font(.caption2)
                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

            ForEach(options.indices, id: \.self) { index in
                HStack(spacing: 8) {
                    Button(action: {
                        for i in options.indices {
                            options[i].isCorrect = (i == index)
                        }
                    }) {
                        Image(systemName: options[index].isCorrect ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(options[index].isCorrect ? Color.accentEmerald : Color.dynamicTextMuted(for: colorScheme))
                            .font(.title3)
                    }

                    Text("\(options[index].key).")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                    TextField("Option text...", text: $options[index].label)
                        .padding(10)
                        .background(Color.dynamicCard(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                        )

                    if options.count > 2 {
                        Button(action: {
                            options.remove(at: index)
                            for i in options.indices {
                                options[i].key = String(UnicodeScalar(65 + i)!)
                            }
                            if !options.contains(where: { $0.isCorrect }) && !options.isEmpty {
                                options[0].isCorrect = true
                            }
                        }) {
                            Image(systemName: "trash")
                                .foregroundColor(Color.accentDanger)
                                .font(.caption)
                        }
                    }
                }
            }
        }
    }

    private var scenarioRubricSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Scoring Rubric (የነጥብ አሰጣጥ መመሪያ):")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

            TextEditor(text: $rubricAm)
                .frame(height: 70)
                .padding(8)
                .background(Color.dynamicCard(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                )
        }
    }

    private var ratingScaleSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Rating Scale (ደረጃ)")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(Color.accentAmber)

            Text("Examinees will rate on a scale from 1 (ዝቅተኛ) to 5 (ከፍተኛ). Points will be awarded proportionally.")
                .font(.caption2)
                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                .padding(12)
                .cardStyle(cornerRadius: 10)
        }
    }

    // MARK: - Save Action
    private func saveQuestion() {
        if promptAm.trimmingCharacters(in: .whitespaces).isEmpty {
            validationError = "Please enter the question prompt in Amharic."
            return
        }

        if questionType == "SINGLE_CHOICE" {
            if options.contains(where: { $0.label.trimmingCharacters(in: .whitespaces).isEmpty }) {
                validationError = "Please fill in all choice option labels."
                return
            }
            if !options.contains(where: { $0.isCorrect }) {
                validationError = "Please mark which option is the correct answer."
                return
            }
        }

        isSaving = true
        validationError = nil

        let optionsPayload: [[String: Any]]? = questionType == "SINGLE_CHOICE" ? options.enumerated().map { idx, opt in
            [
                "option_key": opt.key,
                "label_am": opt.label.trimmingCharacters(in: .whitespaces),
                "label_en": opt.label.trimmingCharacters(in: .whitespaces),
                "is_correct": opt.isCorrect,
                "order_index": idx
            ]
        } : nil

        Task {
            let pts = Int(pointsText) ?? 1
            let success = await adminVM.addQuestion(
                sectionId: selectedSectionId,
                type: questionType,
                promptAm: promptAm.trimmingCharacters(in: .whitespaces),
                promptEn: promptEn.trimmingCharacters(in: .whitespaces),
                points: pts,
                options: optionsPayload,
                rubricAm: rubricAm.isEmpty ? nil : rubricAm,
                rubricEn: rubricEn.isEmpty ? nil : rubricEn
            )

            isSaving = false
            if success {
                await assessmentVM.loadSurvey()
                dismiss()
            }
        }
    }
}
