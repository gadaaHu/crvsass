import SwiftUI

@main
struct CRVSGatherApp: App {
    @StateObject private var authVM = AuthViewModel()
    @StateObject private var assessmentVM = AssessmentViewModel()
    @StateObject private var adminVM = AdminViewModel()

    var body: some Scene {
        WindowGroup {
            Group {
                if authVM.isAuthenticated {
                    HomeView()
                } else {
                    SignInView()
                }
            }
            .environmentObject(authVM)
            .environmentObject(assessmentVM)
            .environmentObject(adminVM)
        }
    }
}
