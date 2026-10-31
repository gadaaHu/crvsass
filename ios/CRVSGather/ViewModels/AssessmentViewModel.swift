import Foundation
import SwiftUI
import AVFoundation

@MainActor
class AssessmentViewModel: ObservableObject {
    @Published var survey: Survey?
    @Published var flatQuestions: [(sectionTitle: String, question: Question)] = []
    @Published var currentIndex: Int = 0
    @Published var answers: [Int: AnswerInput] = [:] // questionId -> AnswerInput

    @Published var testType: String = "POST" // "PRE" or "POST"
    @Published var participant: Participant?

    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var errorMessage: String?
    @Published var submissionResult: SubmissionResponse?

    // Text to Speech
    private let synthesizer = AVSpeechSynthesizer()
    @Published var isSpeaking: Bool = false

    var currentQuestionPair: (sectionTitle: String, question: Question)? {
        guard flatQuestions.indices.contains(currentIndex) else { return nil }
        return flatQuestions[currentIndex]
    }

    var progress: Double {
        guard !flatQuestions.isEmpty else { return 0 }
        return Double(currentIndex + 1) / Double(flatQuestions.count)
    }

    var answeredCount: Int {
        answers.count
    }

    init() {
        Task {
            await loadSurvey()
        }
    }

    func loadSurvey(id: Int = 1) async {
        isLoading = true
        errorMessage = nil
        do {
            let fetchedSurvey = try await APIService.shared.getSurvey(id: id)
            self.survey = fetchedSurvey
            self.flatQuestions = fetchedSurvey.allQuestions
            isLoading = false
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
        }
    }

    func startExam(with user: User?, type: String = "POST") {
        self.testType = type
        self.currentIndex = 0
        self.answers = [:]
        self.submissionResult = nil

        let code = user?.participantCode ?? "DD-\(Int.random(in: 1000...9999))"
        let kebele = user?.weredaKebele ?? "Dire Dawa CRVS"
        let r = user?.role == "ADMIN" ? "Administrator" : "CRVS Registrar"

        self.participant = Participant(
            participantCode: code,
            weredaKebele: kebele,
            role: r,
            gender: user?.gender ?? "Unspecified",
            experienceYears: user?.experienceYears ?? 1.0
        )
    }

    // MARK: - Answer Recording
    func selectMCQOption(questionId: Int, optionKey: String) {
        var input = answers[questionId] ?? AnswerInput(questionId: questionId)
        input.selectedOptionKey = optionKey
        answers[questionId] = input
    }

    func setTextAnswer(questionId: Int, text: String) {
        var input = answers[questionId] ?? AnswerInput(questionId: questionId)
        input.answerText = text
        answers[questionId] = input
    }

    func setRatingAnswer(questionId: Int, rating: Int) {
        var input = answers[questionId] ?? AnswerInput(questionId: questionId)
        input.ratingValue = rating
        answers[questionId] = input
    }

    // MARK: - Navigation
    func nextQuestion() {
        stopSpeaking()
        if currentIndex < flatQuestions.count - 1 {
            currentIndex += 1
        }
    }

    func previousQuestion() {
        stopSpeaking()
        if currentIndex > 0 {
            currentIndex -= 1
        }
    }

    // MARK: - Text-to-Speech
    func speakCurrentQuestion() {
        guard let pair = currentQuestionPair else { return }
        stopSpeaking()

        let text = pair.question.promptAm
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "am-ET") ?? AVSpeechSynthesisVoice(language: "en-US")
        utterance.rate = 0.45

        isSpeaking = true
        synthesizer.speak(utterance)
    }

    func stopSpeaking() {
        if synthesizer.isSpeaking {
            synthesizer.stopImmediate(at: .immediate)
        }
        isSpeaking = false
    }

    // MARK: - Assessment Submission
    func submitAssessment() async -> Bool {
        guard let survey = survey, let participant = participant else {
            errorMessage = "Missing survey or participant information."
            return false
        }

        isSubmitting = true
        errorMessage = nil

        let submissionInput = SubmissionInput(
            surveyId: survey.id,
            participant: participant,
            testType: testType,
            confidenceDigital: 4,
            confidenceLaw: 4,
            answers: Array(answers.values),
            feedbackText: "Completed via iOS CRVS Gather App"
        )

        do {
            let res = try await APIService.shared.submitAssessment(input: submissionInput)
            self.submissionResult = res
            isSubmitting = false
            return true
        } catch {
            isSubmitting = false
            errorMessage = error.localizedDescription
            return false
        }
    }

    func resetExam() {
        currentIndex = 0
        answers = [:]
        submissionResult = nil
        errorMessage = nil
        stopSpeaking()
    }
}
