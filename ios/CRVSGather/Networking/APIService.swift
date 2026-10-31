import Foundation

enum NetworkError: LocalizedError {
    case invalidURL
    case serverError(String)
    case decodingError(Error)
    case unauthorized
    case unknown

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "Invalid server endpoint URL"
        case .serverError(let msg): return msg
        case .decodingError(let err): return "Data parsing error: \(err.localizedDescription)"
        case .unauthorized: return "Session expired or invalid credentials"
        case .unknown: return "An unknown network error occurred"
        }
    }
}

actor APIService {
    static let shared = APIService()

    // Configurable server base URL (supports local IP, emulator, or production domain)
    private var baseURLString: String {
        get {
            UserDefaults.standard.string(forKey: "server_base_url") ?? "http://192.168.1.10:4000/api"
        }
    }

    func setBaseURL(_ url: String) {
        var cleanURL = url.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleanURL.hasSuffix("/") {
            cleanURL.removeLast()
        }
        if !cleanURL.hasSuffix("/api") {
            cleanURL += "/api"
        }
        UserDefaults.standard.set(cleanURL, forKey: "server_base_url")
    }

    func getBaseURL() -> String {
        baseURLString
    }

    private var authToken: String? {
        UserDefaults.standard.string(forKey: "auth_token")
    }

    func setAuthToken(_ token: String?) {
        UserDefaults.standard.set(token, forKey: "auth_token")
    }

    // MARK: - Core Request Helper
    private func request<T: Codable>(
        path: String,
        method: String = "GET",
        body: Data? = nil
    ) async throws -> T {
        guard let url = URL(string: "\(baseURLString)/\(path)") else {
            throw NetworkError.invalidURL
        }

        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("application/json", forHTTPHeaderField: "Accept")

        if let token = authToken {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body = body {
            req.httpBody = body
        }

        let (data, response) = try await URLSession.shared.data(for: req)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.unknown
        }

        if httpResponse.statusCode == 401 {
            throw NetworkError.unauthorized
        }

        let decoder = JSONDecoder()

        if !(200...299).contains(httpResponse.statusCode) {
            // Attempt to parse server error payload
            if let errResp = try? decoder.decode(ApiResponse<String>.self, from: data), let msg = errResp.error {
                throw NetworkError.serverError(msg)
            }
            if let rawMsg = String(data: data, encoding: .utf8) {
                throw NetworkError.serverError("Server returned code \(httpResponse.statusCode): \(rawMsg)")
            }
            throw NetworkError.serverError("Server returned error code \(httpResponse.statusCode)")
        }

        do {
            let apiResponse = try decoder.decode(ApiResponse<T>.self, from: data)
            if let payload = apiResponse.data {
                return payload
            } else if let errorMsg = apiResponse.error {
                throw NetworkError.serverError(errorMsg)
            } else {
                // If T is an empty or raw type
                if let direct = try? decoder.decode(T.self, from: data) {
                    return direct
                }
                throw NetworkError.serverError("Empty payload received")
            }
        } catch {
            // Try direct decoding if response was not wrapped
            if let direct = try? decoder.decode(T.self, from: data) {
                return direct
            }
            throw NetworkError.decodingError(error)
        }
    }

    // MARK: - Auth API
    func signIn(request req: SignInRequest) async throws -> AuthData {
        let encoder = JSONEncoder()
        let body = try encoder.encode(req)
        let authData: AuthData = try await request(path: "auth/signin", method: "POST", body: body)
        setAuthToken(authData.token)
        return authData
    }

    func signUp(request req: SignUpRequest) async throws -> AuthData {
        let encoder = JSONEncoder()
        let body = try encoder.encode(req)
        let authData: AuthData = try await request(path: "auth/signup", method: "POST", body: body)
        setAuthToken(authData.token)
        return authData
    }

    func listUsers() async throws -> [User] {
        try await request(path: "auth/users", method: "GET")
    }

    func updateUserRole(userId: Int, role: String) async throws -> User {
        let body = try JSONSerialization.data(withJSONObject: ["role": role])
        return try await request(path: "auth/users/\(userId)/role", method: "PATCH", body: body)
    }

    func deleteUser(userId: Int) async throws -> Bool {
        guard let url = URL(string: "\(baseURLString)/auth/users/\(userId)") else {
            throw NetworkError.invalidURL
        }
        var req = URLRequest(url: url)
        req.httpMethod = "DELETE"
        let (_, response) = try await URLSession.shared.data(for: req)
        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            throw NetworkError.serverError("Failed to delete user")
        }
        return true
    }

    // MARK: - Surveys & Assessment API
    func getSurvey(id: Int = 1) async throws -> Survey {
        try await request(path: "surveys/\(id)", method: "GET")
    }

    func submitAssessment(input: SubmissionInput) async throws -> SubmissionResponse {
        let encoder = JSONEncoder()
        let body = try encoder.encode(input)
        return try await request(path: "submissions", method: "POST", body: body)
    }

    func getSubmissions() async throws -> [SubmissionRecordItem] {
        try await request(path: "submissions", method: "GET")
    }

    // MARK: - Question Bank Live Operations (Admin Role)
    func createQuestion(sectionId: Int, payload: [String: Any]) async throws -> Question {
        let body = try JSONSerialization.data(withJSONObject: payload)
        return try await request(path: "sections/\(sectionId)/questions", method: "POST", body: body)
    }

    func updateQuestion(questionId: Int, payload: [String: Any]) async throws -> Question {
        let body = try JSONSerialization.data(withJSONObject: payload)
        return try await request(path: "questions/\(questionId)", method: "PUT", body: body)
    }

    func deleteQuestion(questionId: Int) async throws -> Bool {
        guard let url = URL(string: "\(baseURLString)/questions/\(questionId)") else {
            throw NetworkError.invalidURL
        }
        var req = URLRequest(url: url)
        req.httpMethod = "DELETE"
        let (_, response) = try await URLSession.shared.data(for: req)
        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            throw NetworkError.serverError("Failed to delete question")
        }
        return true
    }

    func deleteSubmission(submissionId: Int) async throws -> Bool {
        guard let url = URL(string: "\(baseURLString)/submissions/\(submissionId)") else {
            throw NetworkError.invalidURL
        }
        var req = URLRequest(url: url)
        req.httpMethod = "DELETE"
        let (_, response) = try await URLSession.shared.data(for: req)
        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            throw NetworkError.serverError("Failed to delete submission")
        }
        return true
    }
}
