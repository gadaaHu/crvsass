import SwiftUI

struct SignInView: View {
    @EnvironmentObject var authVM: AuthViewModel
    @Environment(\.colorScheme) var colorScheme

    @State private var username = ""
    @State private var password = ""
    @State private var showPassword = false
    @State private var showSignUp = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.dynamicBg(for: colorScheme)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        Spacer(minLength: 20)

                        // App Branding Header
                        VStack(spacing: 12) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 22)
                                    .fill(
                                        LinearGradient(
                                            colors: [Color.primaryIndigo, Color.primaryIndigoDark],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(width: 80, height: 80)
                                    .shadow(color: Color.primaryIndigo.opacity(0.35), radius: 12, y: 6)

                                Image(systemName: "checkmark.seal.fill")
                                    .font(.system(size: 38))
                                    .foregroundColor(.white)
                            }

                            Text("CRVS Gather")
                                .font(.system(size: 28, weight: .black))
                                .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                            Text("የድሬዳዋ አስተዳደር የሲአርቪኤስ (CRVS) ስልጠና እና መመዘኛ ስርዓት")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 16)
                        }

                        // Login Form Card
                        VStack(alignment: .leading, spacing: 18) {
                            Text("Sign In to Your Account")
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                            // Username Input
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Username / የተጠቃሚ ስም")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                                HStack {
                                    Image(systemName: "person.fill")
                                        .foregroundColor(Color.primaryIndigo)
                                        .frame(width: 20)

                                    TextField("Enter your username", text: $username)
                                        .autocapitalization(.none)
                                        .disableAutocorrection(true)
                                }
                                .padding(12)
                                .background(Color.dynamicSecondary(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                                )
                            }

                            // Password Input
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Password / የይለፍ ቃል")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                                HStack {
                                    Image(systemName: "lock.fill")
                                        .foregroundColor(Color.primaryIndigo)
                                        .frame(width: 20)

                                    if showPassword {
                                        TextField("Enter your password", text: $password)
                                            .autocapitalization(.none)
                                    } else {
                                        SecureField("Enter your password", text: $password)
                                    }

                                    Button(action: { showPassword.toggle() }) {
                                        Image(systemName: showPassword ? "eye.slash.fill" : "eye.fill")
                                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                                            .font(.caption)
                                    }
                                }
                                .padding(12)
                                .background(Color.dynamicSecondary(for: colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                                )
                            }

                            if let error = authVM.errorMessage {
                                Text(error)
                                    .font(.caption)
                                    .foregroundColor(Color.accentDanger)
                                    .padding(.vertical, 2)
                            }

                            // Sign In Button
                            Button(action: {
                                Task {
                                    _ = await authVM.signIn(username: username, password: password)
                                }
                            }) {
                                HStack {
                                    if authVM.isLoading {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    } else {
                                        Text("Sign In (ግባ)")
                                            .fontWeight(.bold)
                                        Image(systemName: "arrow.right")
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

                        // Sign Up Navigation Link
                        HStack(spacing: 6) {
                            Text("Don't have an account?")
                                .font(.footnote)
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                            Button(action: { showSignUp = true }) {
                                Text("Sign Up (ተመዝገብ)")
                                    .font(.footnote)
                                    .fontWeight(.bold)
                                    .foregroundColor(Color.primaryIndigo)
                            }
                        }
                        .padding(.top, 4)

                        Spacer(minLength: 30)
                    }
                    .padding(.horizontal, 20)
                }
            }
            .navigationDestination(isPresented: $showSignUp) {
                SignUpView()
            }
        }
    }
}
