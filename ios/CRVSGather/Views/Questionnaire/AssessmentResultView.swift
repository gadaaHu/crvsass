import SwiftUI

struct AssessmentResultView: View {
    @EnvironmentObject var assessmentVM: AssessmentViewModel
    @EnvironmentObject var authVM: AuthViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        ZStack {
            Color.dynamicBg(for: colorScheme)
                .ignoresSafeArea()

            if let result = assessmentVM.submissionResult {
                let canViewResult = authVM.currentUser?.role == "ADMIN" || authVM.currentUser?.role == "USER_MANAGER"

                ScrollView {
                    if canViewResult {
                        // ── ADMIN & MANAGER: Full Results & Answers Breakdown ──
                        VStack(spacing: 24) {
                            Spacer(minLength: 10)

                            // Hero Score Card
                            VStack(spacing: 16) {
                                let isPassed = result.scorePercentage >= 60.0

                                ZStack {
                                    Circle()
                                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 10)
                                        .frame(width: 140, height: 140)

                                    Circle()
                                        .trim(from: 0, to: CGFloat(result.scorePercentage / 100.0))
                                        .stroke(
                                            isPassed ? Color.accentEmerald : Color.accentAmber,
                                            style: StrokeStyle(lineWidth: 10, lineCap: .round)
                                        )
                                        .frame(width: 140, height: 140)
                                        .rotationEffect(.degrees(-90))

                                    VStack(spacing: 2) {
                                        Text("\(Int(result.scorePercentage))%")
                                            .font(.system(size: 34, weight: .black))
                                            .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                                        Text("\(String(format: "%.1f", result.totalScore)) / \(Int(result.maxScore)) Pts")
                                            .font(.caption)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                    }
                                }

                                VStack(spacing: 6) {
                                    Text(isPassed ? "Assessment Passed! 🎉" : "Assessment Completed")
                                        .font(.title3)
                                        .fontWeight(.bold)
                                        .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                                    Text("Performance Band: \(result.performanceBand)")
                                        .font(.subheadline)
                                        .fontWeight(.semibold)
                                        .foregroundColor(isPassed ? Color.accentEmerald : Color.accentAmber)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 5)
                                        .background((isPassed ? Color.accentEmerald : Color.accentAmber).opacity(0.12))
                                        .clipShape(Capsule())
                                }

                                Divider()
                                    .background(Color.dynamicBorder(for: colorScheme))

                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Participant")
                                            .font(.caption2)
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                        Text(result.participantCode)
                                            .font(.caption)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 2) {
                                        Text("Test Mode")
                                            .font(.caption2)
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                        Text(result.testType == "PRE" ? "Pre-Test" : "Post-Test")
                                            .font(.caption)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color.primaryIndigo)
                                    }
                                }
                            }
                            .padding(22)
                            .cardStyle(cornerRadius: 22)

                            // Answers Breakdown
                            if let answers = result.answers, !answers.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Answer Key & Point Breakdown")
                                        .font(.headline)
                                        .fontWeight(.bold)
                                        .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                                    ForEach(answers) { a in
                                        HStack(spacing: 12) {
                                            ZStack {
                                                Circle()
                                                    .fill(a.isCorrect ? Color.accentEmerald.opacity(0.15) : Color.accentDanger.opacity(0.15))
                                                    .frame(width: 32, height: 32)
                                                Image(systemName: a.isCorrect ? "checkmark" : "xmark")
                                                    .font(.caption)
                                                    .fontWeight(.bold)
                                                    .foregroundColor(a.isCorrect ? Color.accentEmerald : Color.accentDanger)
                                            }

                                            VStack(alignment: .leading, spacing: 2) {
                                                Text("Question Q\(a.questionNumber)")
                                                    .font(.subheadline)
                                                    .fontWeight(.bold)
                                                    .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                                                if let sel = a.selectedOptionKey {
                                                    Text("Selected: \(sel) · Correct: \(a.correctOptionKey ?? sel)")
                                                        .font(.caption2)
                                                        .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                                }
                                            }

                                            Spacer()

                                            Text("\(String(format: "%.1f", a.scoreAwarded)) / \(Int(a.maxPoints)) pt")
                                                .font(.caption)
                                                .fontWeight(.bold)
                                                .foregroundColor(a.isCorrect ? Color.accentEmerald : Color.dynamicTextMuted(for: colorScheme))
                                        }
                                        .padding(12)
                                        .cardStyle(cornerRadius: 12)
                                    }
                                }
                            }

                            // Done / Return Button
                            Button(action: {
                                assessmentVM.resetExam()
                                dismiss()
                            }) {
                                Text("Back to Dashboard (ተመለስ)")
                                    .fontWeight(.bold)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 50)
                                    .background(Color.primaryIndigo)
                                    .foregroundColor(.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                            }
                            .padding(.top, 10)

                            Spacer(minLength: 30)
                        }
                        .padding(.horizontal, 20)
                    } else {
                        // ── EXAMINEE: Secure Submission Confirmation ──
                        VStack(spacing: 24) {
                            Spacer(minLength: 10)

                            VStack(spacing: 18) {
                                ZStack {
                                    Circle()
                                        .fill(Color.accentEmerald.opacity(0.15))
                                        .frame(width: 100, height: 100)
                                        .overlay(
                                            Circle()
                                                .stroke(Color.accentEmerald, lineWidth: 3)
                                        )
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.system(size: 52))
                                        .foregroundColor(Color.accentEmerald)
                                }

                                VStack(spacing: 6) {
                                    Text("ምዘናው በተሳካ ሁኔታ ተጠናቋል!")
                                        .font(.title3)
                                        .fontWeight(.bold)
                                        .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                                    Text("Assessment Submitted Successfully")
                                        .font(.subheadline)
                                        .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                }

                                // Restricted Notice Card
                                HStack(alignment: .top, spacing: 10) {
                                    Image(systemName: "lock.fill")
                                        .foregroundColor(Color.accentAmber)
                                        .font(.title3)

                                    VStack(alignment: .leading, spacing: 3) {
                                        Text("የውጤት ገደብ (Restricted Result)")
                                            .font(.subheadline)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                        Text("የፈተናው ውጤት የሚታየው ለአስተዳዳሪ (Admin) እና ሥራ አስኪያጅ (Manager) ብቻ ነው።")
                                            .font(.caption)
                                            .foregroundColor(Color.accentAmber)
                                        Text("Exam results and performance scores are accessible only by authorized administrators and managers.")
                                            .font(.caption2)
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                    }
                                }
                                .padding(14)
                                .background(Color.primaryIndigo.opacity(0.1))
                                .clipShape(RoundedRectangle(cornerRadius: 14))

                                Divider()
                                    .background(Color.dynamicBorder(for: colorScheme))

                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Participant")
                                            .font(.caption2)
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                        Text(result.participantCode)
                                            .font(.caption)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 2) {
                                        Text("Test Mode")
                                            .font(.caption2)
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                        Text(result.testType == "PRE" ? "Pre-Test" : "Post-Test")
                                            .font(.caption)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color.primaryIndigo)
                                    }
                                }
                            }
                            .padding(22)
                            .cardStyle(cornerRadius: 22)

                            Text("የሰጡት ምላሾች በሙሉ በስርዓቱ ውስጥ በአስተማማኝ ሁኔታ ተመዝግበዋል። አስተዳዳሪ ወይም ሥራ አስኪያጅ ውጤቱን ገምግመው ይፋ ያደርጋሉ።")
                                .font(.caption)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 16)

                            // Done / Return Button
                            Button(action: {
                                assessmentVM.resetExam()
                                dismiss()
                            }) {
                                Text("Back to Dashboard (ወደ መነሻ ተመለስ)")
                                    .fontWeight(.bold)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 50)
                                    .background(Color.primaryIndigo)
                                    .foregroundColor(.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                            }
                            .padding(.top, 10)

                            Spacer(minLength: 30)
                        }
                        .padding(.horizontal, 20)
                    }
                }
            } else {
                ProgressView()
            }
        }
        .navigationBarBackButtonHidden(true)
    }
}
