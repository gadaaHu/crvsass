import Foundation
import SwiftUI

@MainActor
class AuthViewModel: ObservableObject {
    @Published var currentUser: User?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let userDefaultsKey = "saved_current_user"

    init() {
        loadSavedSession()
    }

    var isAuthenticated: Bool {
        currentUser != nil
    }

    private func loadSavedSession() {
        if let data = UserDefaults.standard.data(forKey: userDefaultsKey),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            self.currentUser = user
        }
    }

    private func saveSession(user: User) {
        if let data = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(data, forKey: userDefaultsKey)
        }
        self.currentUser = user
    }

    func signIn(username: String, password: String) async -> Bool {
        guard !username.trimmingCharacters(in: .whitespaces).isEmpty,
              !password.isEmpty else {
            self.errorMessage = "Please enter both username and password."
            return false
        }

        isLoading = true
        errorMessage = nil

        do {
            let req = SignInRequest(username: username.trimmingCharacters(in: .whitespaces), password: password)
            let authData = try await APIService.shared.signIn(request: req)
            saveSession(user: authData.user)
            isLoading = false
            return true
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            return false
        }
    }

    func signUp(
        username: String,
        password: String,
        fullName: String,
        role: String,
        weredaKebele: String?,
        gender: String?,
        experienceYears: Double?
    ) async -> Bool {
        guard !username.trimmingCharacters(in: .whitespaces).isEmpty,
              !password.isEmpty,
              !fullName.trimmingCharacters(in: .whitespaces).isEmpty else {
            self.errorMessage = "Please fill in all required fields."
            return false
        }

        isLoading = true
        errorMessage = nil

        let participantCode = "DD-\(String(format: "%04d", Int.random(in: 1000...9999)))"

        let req = SignUpRequest(
            username: username.trimmingCharacters(in: .whitespaces),
            password: password,
            fullName: fullName.trimmingCharacters(in: .whitespaces),
            role: role,
            participantCode: participantCode,
            weredaKebele: weredaKebele?.trimmingCharacters(in: .whitespaces),
            gender: gender,
            experienceYears: experienceYears
        )

        do {
            let authData = try await APIService.shared.signUp(request: req)
            saveSession(user: authData.user)
            isLoading = false
            return true
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            return false
        }
    }

    func signOut() {
        UserDefaults.standard.removeObject(forKey: userDefaultsKey)
        UserDefaults.standard.removeObject(forKey: "auth_token")
        currentUser = nil
    }
}
