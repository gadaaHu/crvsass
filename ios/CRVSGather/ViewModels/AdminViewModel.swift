import Foundation
import SwiftUI

@MainActor
class AdminViewModel: ObservableObject {
    @Published var submissions: [SubmissionRecordItem] = []
    @Published var users: [User] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var successToast: String?

    @Published var filterTestType: String = "ALL" // ALL, PRE, POST
    @Published var userSearchQuery: String = ""
    @Published var userRoleFilter: String = "ALL" // ALL, ADMIN, USER_MANAGER, EXAMINEE

    var filteredSubmissions: [SubmissionRecordItem] {
        submissions.filter { sub in
            if filterTestType == "PRE" { return sub.testType.uppercased() == "PRE" }
            if filterTestType == "POST" { return sub.testType.uppercased() == "POST" }
            return true
        }
    }

    var filteredUsers: [User] {
        users.filter { user in
            let matchesRole: Bool = {
                switch userRoleFilter {
                case "ADMIN": return user.role == "ADMIN"
                case "USER_MANAGER": return user.role == "USER_MANAGER"
                case "EXAMINEE": return user.role == "EXAMINEE"
                default: return true
                }
            }()

            let q = userSearchQuery.trimmingCharacters(in: .whitespaces).lowercased()
            let matchesQuery = q.isEmpty ||
                user.username.lowercased().contains(q) ||
                user.fullName.lowercased().contains(q) ||
                (user.weredaKebele ?? "").lowercased().contains(q) ||
                (user.participantCode ?? "").lowercased().contains(q)

            return matchesRole && matchesQuery
        }
    }

    // MARK: - Data Fetching
    func loadAllData() async {
        isLoading = true
        errorMessage = nil
        do {
            async let subsTask = APIService.shared.getSubmissions()
            async let usersTask = APIService.shared.listUsers()

            let (subs, usrs) = try await (subsTask, usersTask)
            self.submissions = subs
            self.users = usrs
            isLoading = false
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
        }
    }

    func loadSubmissions() async {
        do {
            self.submissions = try await APIService.shared.getSubmissions()
        } catch {
            self.errorMessage = error.localizedDescription
        }
    }

    func deleteSubmission(submissionId: Int) async -> Bool {
        do {
            _ = try await APIService.shared.deleteSubmission(submissionId: submissionId)
            await loadSubmissions()
            self.successToast = "Examinee submission deleted."
            return true
        } catch {
            self.errorMessage = error.localizedDescription
            return false
        }
    }

    func loadUsers() async {
        do {
            self.users = try await APIService.shared.listUsers()
        } catch {
            self.errorMessage = error.localizedDescription
        }
    }

    // MARK: - User Role Management
    func updateUserRole(userId: Int, newRole: String) async -> Bool {
        do {
            _ = try await APIService.shared.updateUserRole(userId: userId, role: newRole)
            await loadUsers()
            self.successToast = "User role updated to \(newRole)!"
            return true
        } catch {
            self.errorMessage = error.localizedDescription
            return false
        }
    }

    func deleteUser(userId: Int) async -> Bool {
        do {
            _ = try await APIService.shared.deleteUser(userId: userId)
            await loadUsers()
            self.successToast = "User deleted successfully."
            return true
        } catch {
            self.errorMessage = error.localizedDescription
            return false
        }
    }

    // MARK: - Question Bank Operations (Admin Mobile Question Creator)
    func addQuestion(
        sectionId: Int,
        type: String,
        promptAm: String,
        promptEn: String?,
        points: Int,
        options: [[String: Any]]? = nil,
        rubricAm: String? = nil,
        rubricEn: String? = nil
    ) async -> Bool {
        var payload: [String: Any] = [
            "prompt_am": promptAm,
            "prompt_en": promptEn ?? promptAm,
            "type": type,
            "points": points
        ]

        if let options = options {
            payload["options"] = options
        }
        if let rubricAm = rubricAm, !rubricAm.isEmpty {
            payload["rubric_am"] = rubricAm
        }
        if let rubricEn = rubricEn, !rubricEn.isEmpty {
            payload["rubric_en"] = rubricEn
        }

        do {
            _ = try await APIService.shared.createQuestion(sectionId: sectionId, payload: payload)
            self.successToast = "Question added to exam successfully! ✓"
            return true
        } catch {
            self.errorMessage = error.localizedDescription
            return false
        }
    }

    func updateQuestion(
        questionId: Int,
        promptAm: String,
        promptEn: String?,
        points: Int,
        type: String,
        options: [[String: Any]]? = nil,
        rubricAm: String? = nil
    ) async -> Bool {
        var payload: [String: Any] = [
            "prompt_am": promptAm,
            "prompt_en": promptEn ?? promptAm,
            "points": points,
            "type": type
        ]

        if let options = options {
            payload["options"] = options
        }
        if let rubricAm = rubricAm {
            payload["rubric_am"] = rubricAm
        }

        do {
            _ = try await APIService.shared.updateQuestion(questionId: questionId, payload: payload)
            self.successToast = "Question updated live! ✓"
            return true
        } catch {
            self.errorMessage = error.localizedDescription
            return false
        }
    }

    func deleteQuestion(questionId: Int) async -> Bool {
        do {
            _ = try await APIService.shared.deleteQuestion(questionId: questionId)
            self.successToast = "Question deleted."
            return true
        } catch {
            self.errorMessage = error.localizedDescription
            return false
        }
    }
}
