import SwiftUI

struct SignUpView: View {
    @EnvironmentObject var authVM: AuthViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme

    @State private var username = ""
    @State private var password = ""
    @State private var fullName = ""
    @State private var weredaKebele = "Dire Dawa CRVS Office"
    @State private var selectedRole = "EXAMINEE" // EXAMINEE, USER_MANAGER, ADMIN
    @State private var gender = "Female"
    @State private var experienceYearsText = "2.0"

    private let roles: [(key: String, titleAm: String, titleEn: String, icon: String, color: Color)] = [
        ("EXAMINEE", "ተፈታኝ", "Examinee / CRVS Registrar", "pencil.circle.fill", Color.primaryIndigo),
        ("USER_MANAGER", "የተጠቃሚዎች አስተዳዳሪ", "User Manager", "person.2.circle.fill", Color.accentAmber),
        ("ADMIN", "ሥርዓት አስተዳዳሪ", "System Administrator", "shield.checkered", Color.accentEmerald)
    ]

    var body: some View {
        ZStack {
            Color.dynamicBg(for: colorScheme)
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Create Account")
                            .font(.system(size: 26, weight: .black))
                            .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                        Text("አዲስ መለያ መፍጠሪያ (CRVS Registration)")
                            .font(.subheadline)
                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 10)

                    // Role Selector Cards
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Select System Role (የተጠቃሚ ሚና) *")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                        ForEach(roles, id: \.key) { r in
                            let isSelected = selectedRole == r.key
                            Button(action: { selectedRole = r.key }) {
                                HStack(spacing: 12) {
                                    Image(systemName: r.icon)
                                        .font(.title2)
                                        .foregroundColor(r.color)
                                        .frame(width: 32)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("\(r.titleEn) (\(r.titleAm))")
                                            .font(.subheadline)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                                    }

                                    Spacer()

                                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                                        .font(.title3)
                                        .foregroundColor(isSelected ? r.color : Color.dynamicTextMuted(for: colorScheme))
                                }
                                .padding(14)
                                .background(isSelected ? r.color.opacity(0.12) : Color.dynamicCard(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14)
                                        .stroke(isSelected ? r.color : Color.dynamicBorder(for: colorScheme), lineWidth: isSelected ? 2 : 1)
                                )
                            }
                        }
                    }

                    // Form Fields Card
                    VStack(spacing: 16) {
                        customTextField(label: "Full Name (ሙሉ ስም) *", placeholder: "e.g., Almaz Bekele", text: $fullName, icon: "person.crop.rectangle")
                        customTextField(label: "Username (የተጠቃሚ ስም) *", placeholder: "e.g., almaz.crvs", text: $username, icon: "person.fill")
                        customSecureField(label: "Password (የይለፍ ቃል) *", placeholder: "At least 6 characters", text: $password, icon: "lock.fill")
                        customTextField(label: "Wereda / Kebele / Office (ወረዳ/ቀበሌ)", placeholder: "e.g., Kebele 02 Registry", text: $weredaKebele, icon: "building.2.fill")

                        HStack(spacing: 14) {
                            // Gender Picker
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Gender (ጾታ)")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                                Picker("Gender", selection: $gender) {
                                    Text("Female (ሴት)").tag("Female")
                                    Text("Male (ወንድ)").tag("Male")
                                }
                                .pickerStyle(.menu)
                                .frame(maxWidth: .infinity)
                                .padding(10)
                                .background(Color.dynamicSecondary(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            }

                            // Experience Years
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Experience (ዓመታት)")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                                TextField("Years", text: $experienceYearsText)
                                    .keyboardType(.decimalPad)
                                    .padding(12)
                                    .background(Color.dynamicSecondary(for: colorScheme))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }

                        if let error = authVM.errorMessage {
                            Text(error)
                                .font(.caption)
                                .foregroundColor(Color.accentDanger)
                        }

                        // Submit Button
                        Button(action: {
                            Task {
                                let exp = Double(experienceYearsText) ?? 1.0
                                let success = await authVM.signUp(
                                    username: username,
                                    password: password,
                                    fullName: fullName,
                                    role: selectedRole,
                                    weredaKebele: weredaKebele,
                                    gender: gender,
                                    experienceYears: exp
                                )
                                if success {
                                    dismiss()
                                }
                            }
                        }) {
                            HStack {
                                if authVM.isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text("Register & Sign In ✓")
                                        .fontWeight(.bold)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(
                                LinearGradient(
                                    colors: [Color.primaryIndigo, Color.primaryIndigoDark],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                            .shadow(color: Color.primaryIndigo.opacity(0.3), radius: 6, y: 3)
                        }
                        .disabled(authVM.isLoading)
                    }
                    .padding(20)
                    .cardStyle(cornerRadius: 20)

                    Spacer(minLength: 20)
                }
                .padding(.horizontal, 20)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
    }

    private func customTextField(label: String, placeholder: String, text: Binding<String>, icon: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

            HStack {
                Image(systemName: icon)
                    .foregroundColor(Color.primaryIndigo)
                    .frame(width: 20)
                TextField(placeholder, text: text)
                    .autocapitalization(.none)
            }
            .padding(12)
            .background(Color.dynamicSecondary(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
            )
        }
    }

    private func customSecureField(label: String, placeholder: String, text: Binding<String>, icon: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

            HStack {
                Image(systemName: icon)
                    .foregroundColor(Color.primaryIndigo)
                    .frame(width: 20)
                SecureField(placeholder, text: text)
                    .autocapitalization(.none)
            }
            .padding(12)
            .background(Color.dynamicSecondary(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
            )
        }
    }
}
