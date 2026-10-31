import SwiftUI

struct HomeView: View {
    @EnvironmentObject var authVM: AuthViewModel
    @EnvironmentObject var assessmentVM: AssessmentViewModel
    @EnvironmentObject var adminVM: AdminViewModel
    @Environment(\.colorScheme) var colorScheme

    @State private var selectedTestType: String = "POST" // "PRE" or "POST"
    @State private var navigateToExam = false
    @State private var navigateToAdmin = false
    @State private var showLogoutAlert = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.dynamicBg(for: colorScheme)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Top Bar
                        HStack {
                            HStack(spacing: 12) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(LinearGradient(colors: [Color.primaryIndigo, Color.primaryIndigoDark], startPoint: .topLeading, endPoint: .bottomTrailing))
                                        .frame(width: 44, height: 44)
                                    Image(systemName: "checkmark.seal.fill")
                                        .foregroundColor(.white)
                                        .font(.title3)
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text("CRVS Gather")
                                        .font(.headline)
                                        .fontWeight(.black)
                                        .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                    Text("Dire Dawa Administration")
                                        .font(.caption2)
                                        .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                }
                            }

                            Spacer()

                            // Admin Portal Button (for Admin & User Manager)
                            if authVM.currentUser?.canManage == true {
                                Button(action: { navigateToAdmin = true }) {
                                    Image(systemName: "gearshape.fill")
                                        .font(.system(size: 18))
                                        .foregroundColor(Color.primaryIndigo)
                                        .frame(width: 38, height: 38)
                                        .background(Color.primaryIndigo.opacity(0.12))
                                        .clipShape(Circle())
                                }
                            }

                            // Sign Out Button
                            Button(action: { showLogoutAlert = true }) {
                                Image(systemName: "rectangle.portrait.and.arrow.right")
                                    .font(.system(size: 16))
                                    .foregroundColor(Color.accentDanger)
                                    .frame(width: 38, height: 38)
                                    .background(Color.accentDanger.opacity(0.12))
                                    .clipShape(Circle())
                            }
                        }
                        .padding(.top, 10)

                        // User Profile Card
                        if let user = authVM.currentUser {
                            HStack(spacing: 14) {
                                ZStack {
                                    Circle()
                                        .fill(roleColor(user.role).opacity(0.15))
                                        .frame(width: 50, height: 50)
                                    Image(systemName: roleIcon(user.role))
                                        .foregroundColor(roleColor(user.role))
                                        .font(.title3)
                                }

                                VStack(alignment: .leading, spacing: 3) {
                                    Text(user.weredaKebele ?? user.fullName)
                                        .font(.subheadline)
                                        .fontWeight(.bold)
                                        .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                                    Text("Code: \(user.participantCode ?? "DD-REG") · @\(user.username)")
                                        .font(.caption)
                                        .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                }

                                Spacer()

                                // Role Pill
                                Text(roleBadge(user.role))
                                    .font(.caption2)
                                    .fontWeight(.heavy)
                                    .foregroundColor(roleColor(user.role))
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 5)
                                    .background(roleColor(user.role).opacity(0.12))
                                    .clipShape(Capsule())
                            }
                            .padding(16)
                            .cardStyle(cornerRadius: 18)
                        }

                        // Hero Survey Banner Card
                        VStack(alignment: .leading, spacing: 14) {
                            HStack {
                                Text("Dire Dawa CRVS")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(Color.accentEmerald)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 4)
                                    .background(Color.accentEmerald.opacity(0.15))
                                    .clipShape(Capsule())

                                Spacer()

                                if assessmentVM.isLoading {
                                    ProgressView()
                                        .scaleEffect(0.8)
                                }
                            }

                            Text(assessmentVM.survey?.titleAm ?? "Pre & Post Training Knowledge Assessment")
                                .font(.title3)
                                .fontWeight(.black)
                                .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                .fixedSize(horizontal: false, vertical: true)

                            Text("የሲቪል ምዝገባ እና የቤተሰብ ሕግ አዋጅ 1370/2017 እንዲሁም የዲጂታል ሲአርቪኤስ አሰራር")
                                .font(.caption)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            Divider()
                                .background(Color.dynamicBorder(for: colorScheme))

                            // Stats Row
                            HStack {
                                statItem(title: "TOTAL POINTS", value: "\(assessmentVM.survey?.totalPoints ?? 20) pts")
                                Spacer()
                                statItem(title: "QUESTIONS", value: "\(assessmentVM.flatQuestions.count) Qs")
                                Spacer()
                                statItem(title: "PASS SCORE", value: "\(assessmentVM.survey?.passingScore ?? 12) pts")
                            }
                        }
                        .padding(20)
                        .cardStyle(cornerRadius: 20)

                        // Role-Specific Action Card (Admin / User Manager Portal)
                        if authVM.currentUser?.canManage == true {
                            let isAdmin = authVM.currentUser?.role == "ADMIN"
                            VStack(alignment: .leading, spacing: 12) {
                                HStack(spacing: 12) {
                                    Image(systemName: isAdmin ? "shield.lefthalf.filled" : "person.3.fill")
                                        .foregroundColor(Color.primaryIndigo)
                                        .font(.title2)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(isAdmin ? "Admin Control Center" : "Manager Control Center")
                                            .font(.headline)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                        Text(isAdmin ? "Review scores, manage users & add questions live" : "Review examinee scores, manage accounts & monitor users")
                                            .font(.caption)
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                    }
                                }

                                Button(action: { navigateToAdmin = true }) {
                                    HStack {
                                        Image(systemName: isAdmin ? "chart.bar.doc.horizontal.fill" : "chart.bar.xaxis")
                                        Text(isAdmin ? "Open Admin Portal" : "Open Manager Portal")
                                            .fontWeight(.bold)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(Color.primaryIndigo)
                                    .foregroundColor(.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                }
                            }
                            .padding(18)
                            .cardStyle(cornerRadius: 18)
                        }

                        // Take Assessment Card
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Take Assessment (ፈተና ውሰድ)")
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                            // Pre-Test vs Post-Test Tabs
                            HStack(spacing: 12) {
                                modeTab(type: "PRE", title: "Pre-Test (ቅድመ)")
                                modeTab(type: "POST", title: "Post-Test (ድህረ)")
                            }

                            Button(action: {
                                assessmentVM.startExam(with: authVM.currentUser, type: selectedTestType)
                                navigateToExam = true
                            }) {
                                HStack {
                                    Image(systemName: "play.circle.fill")
                                        .font(.title3)
                                    Text("Begin \(selectedTestType == "PRE" ? "Pre-Test" : "Post-Test") Assessment")
                                        .fontWeight(.bold)
                                }
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .background(
                                    LinearGradient(
                                        colors: [Color.accentEmerald, Color(red: 0.05, green: 0.60, blue: 0.42)],
                                        startPoint: .leading,
                                        endPoint: .trailing
                                    )
                                )
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .shadow(color: Color.accentEmerald.opacity(0.3), radius: 6, y: 3)
                            }
                        }
                        .padding(18)
                        .cardStyle(cornerRadius: 18)

                        // Section Overviews
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Assessment Sections (ክፍሎች)")
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            sectionRow(number: "Part A", title: "Civil Registration Proclamation 1370/2017", detail: "8 Questions · Legal framework & vital events")
                            sectionRow(number: "Part B", title: "Digital CRVS Registration System", detail: "8 Questions · System usage, security & procedures")
                            sectionRow(number: "Part C", title: "Practical Case Scenarios", detail: "4 Questions · Decision-making & scenario analysis")
                        }

                        Spacer(minLength: 30)
                    }
                    .padding(.horizontal, 20)
                }
            }
            .navigationDestination(isPresented: $navigateToExam) {
                QuestionnaireView()
            }
            .navigationDestination(isPresented: $navigateToAdmin) {
                AdminPortalView()
            }
            .alert("Sign Out", isPresented: $showLogoutAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Sign Out", role: .destructive) {
                    authVM.signOut()
                }
            } message: {
                Text("Are you sure you want to sign out from CRVS Gather?")
            }
        }
    }

    // MARK: - Subviews & Helpers
    private func modeTab(type: String, title: String) -> some View {
        let isSelected = selectedTestType == type
        return Button(action: { selectedTestType = type }) {
            Text(title)
                .font(.subheadline)
                .fontWeight(isSelected ? .bold : .medium)
                .frame(maxWidth: .infinity)
                .frame(height: 42)
                .background(isSelected ? Color.primaryIndigo.opacity(0.18) : Color.dynamicSecondary(for: colorScheme))
                .foregroundColor(isSelected ? Color.primaryIndigo : Color.dynamicTextMuted(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(isSelected ? Color.primaryIndigo : Color.dynamicBorder(for: colorScheme), lineWidth: isSelected ? 2 : 1)
                )
        }
    }

    private func statItem(title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
            Text(value)
                .font(.subheadline)
                .fontWeight(.black)
                .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
        }
    }

    private func sectionRow(number: String, title: String, detail: String) -> some View {
        HStack(spacing: 12) {
            Text(number)
                .font(.caption2)
                .fontWeight(.heavy)
                .foregroundColor(Color.primaryIndigo)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.primaryIndigo.opacity(0.15))
                .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.footnote)
                    .fontWeight(.bold)
                    .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                Text(detail)
                    .font(.caption2)
                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
            }
            Spacer()
        }
        .padding(12)
        .cardStyle(cornerRadius: 12)
    }

    private func roleColor(_ role: String) -> Color {
        switch role {
        case "ADMIN": return Color(red: 0.0, green: 0.6, blue: 0.5)
        case "USER_MANAGER": return Color.accentAmber
        default: return Color.primaryIndigo
        }
    }

    private func roleIcon(_ role: String) -> String {
        switch role {
        case "ADMIN": return "shield.fill"
        case "USER_MANAGER": return "person.2.fill"
        default: return "person.fill"
        }
    }

    private func roleBadge(_ role: String) -> String {
        switch role {
        case "ADMIN": return "ADMIN"
        case "USER_MANAGER": return "USER MGR"
        default: return "EXAMINEE"
        }
    }
}
