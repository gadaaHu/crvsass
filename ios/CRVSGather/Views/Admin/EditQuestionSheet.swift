import SwiftUI

struct EditQuestionSheet: View {
    @EnvironmentObject var adminVM: AdminViewModel
    @EnvironmentObject var assessmentVM: AssessmentViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme

    let question: Question

    @State private var promptAm: String
    @State private var promptEn: String
    @State private var pointsText: String
    @State private var options: [QuestionOption]
    @State private var isSaving = false

    init(question: Question) {
        self.question = question
        _promptAm = State(initialValue: question.promptAm)
        _promptEn = State(initialValue: question.promptEn ?? "")
        _pointsText = State(initialValue: "\(question.points)")
        _options = State(initialValue: question.options ?? [])
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.dynamicBg(for: colorScheme)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        // Amharic Prompt
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Amharic Prompt (በአማርኛ) *")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            TextEditor(text: $promptAm)
                                .frame(height: 90)
                                .padding(8)
                                .background(Color.dynamicCard(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                                )
                        }

                        // English Prompt
                        VStack(alignment: .leading, spacing: 6) {
                            Text("English Translation")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            TextEditor(text: $promptEn)
                                .frame(height: 70)
                                .padding(8)
                                .background(Color.dynamicCard(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                                )
                        }

                        // Points
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Points / ነጥብ")
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

                        // Options Editor (if MCQ)
                        if !options.isEmpty {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Multiple Choice Options (Tap circle to set correct):")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                                ForEach(options.indices, id: \.self) { index in
                                    let isCorrect = options[index].isCorrect
                                    HStack(spacing: 8) {
                                        Button(action: {
                                            for i in options.indices {
                                                options[i].isCorrect = (i == index)
                                            }
                                        }) {
                                            Image(systemName: isCorrect ? "checkmark.circle.fill" : "circle")
                                                .foregroundColor(isCorrect ? Color.accentEmerald : Color.dynamicTextMuted(for: colorScheme))
                                                .font(.title3)
                                        }

                                        Text("\(options[index].optionKey).")
                                            .font(.caption)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                                        TextField("Option text...", text: Binding(
                                            get: { options[index].labelAm },
                                            set: { newText in
                                                let old = options[index]
                                                options[index] = QuestionOption(
                                                    id: old.id,
                                                    questionId: old.questionId,
                                                    optionKey: old.optionKey,
                                                    labelAm: newText,
                                                    labelEn: old.labelEn,
                                                    isCorrect: old.isCorrect,
                                                    orderIndex: old.orderIndex
                                                )
                                            }
                                        ))
                                        .padding(10)
                                        .background(Color.dynamicCard(for: colorScheme))
                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 10)
                                                .stroke(isCorrect ? Color.accentEmerald : Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                                        )
                                    }
                                }
                            }
                        }

                        // Save Button
                        Button(action: saveChanges) {
                            HStack {
                                if isSaving {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Image(systemName: "checkmark")
                                    Text("Save Changes ✓")
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
            .navigationTitle("Edit Question Q\(question.questionNumber)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func saveChanges() {
        isSaving = true

        let optionsPayload: [[String: Any]]? = options.isEmpty ? nil : options.enumerated().map { idx, opt in
            [
                "option_key": opt.optionKey,
                "label_am": opt.labelAm,
                "label_en": opt.labelEn ?? opt.labelAm,
                "is_correct": opt.isCorrect,
                "order_index": idx
            ]
        }

        Task {
            let pts = Int(pointsText) ?? question.points
            let success = await adminVM.updateQuestion(
                questionId: question.id,
                promptAm: promptAm.trimmingCharacters(in: .whitespaces),
                promptEn: promptEn.trimmingCharacters(in: .whitespaces),
                points: pts,
                type: question.type.rawValue,
                options: optionsPayload,
                rubricAm: question.rubricAm
            )

            isSaving = false
            if success {
                await assessmentVM.loadSurvey()
                dismiss()
            }
        }
    }
}
