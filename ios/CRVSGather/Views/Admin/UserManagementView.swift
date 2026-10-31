import SwiftUI

struct UserManagementView: View {
    @EnvironmentObject var adminVM: AdminViewModel
    @EnvironmentObject var authVM: AuthViewModel
    @Environment(\.colorScheme) var colorScheme

    @State private var userToEditRole: User?
    @State private var userToDelete: User?
    @State private var showDeleteAlert = false
    @State private var selectedNewRole: String = "EXAMINEE"

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // 4 KPI Stat Chips
                HStack(spacing: 8) {
                    kpiChip(title: "Total", count: "\(adminVM.users.count)", color: Color.primaryIndigo)
                    kpiChip(title: "Admins", count: "\(adminVM.users.filter { $0.role == "ADMIN" }.count)", color: Color(red: 0.0, green: 0.6, blue: 0.5))
                    kpiChip(title: "Managers", count: "\(adminVM.users.filter { $0.role == "USER_MANAGER" }.count)", color: Color.accentAmber)
                    kpiChip(title: "Examinees", count: "\(adminVM.users.filter { $0.role == "EXAMINEE" }.count)", color: Color.accentPurple)
                }

                // Search Bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                    TextField("Search users by name, username, code...", text: $adminVM.userSearchQuery)

                    if !adminVM.userSearchQuery.isEmpty {
                        Button(action: { adminVM.userSearchQuery = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                        }
                    }
                }
                .padding(12)
                .background(Color.dynamicCard(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                )

                // Role Filter Chips
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        filterChip(title: "All (\(adminVM.users.count))", value: "ALL")
                        filterChip(title: "Admins", value: "ADMIN")
                        filterChip(title: "Managers", value: "USER_MANAGER")
                        filterChip(title: "Examinees", value: "EXAMINEE")
                    }
                }

                // User List
                if adminVM.filteredUsers.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: "person.slash")
                            .font(.largeTitle)
                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                        Text("No matching users found")
                            .font(.subheadline)
                            .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
                } else {
                    LazyVStack(spacing: 10) {
                        ForEach(adminVM.filteredUsers) { user in
                            userCard(user: user)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
        }
        .sheet(item: $userToEditRole) { user in
            roleSwitcherSheet(user: user)
        }
        .alert("Delete User", isPresented: $showDeleteAlert) {
            Button("Cancel", role: .cancel) { userToDelete = nil }
            Button("Delete", role: .destructive) {
                if let u = userToDelete {
                    Task {
                        _ = await adminVM.deleteUser(userId: u.id)
                        userToDelete = nil
                    }
                }
            }
        } message: {
            if let u = userToDelete {
                if u.id == authVM.currentUser?.id {
                    Text("You cannot delete your own account while signed in.")
                } else {
                    Text("Are you sure you want to delete user @\(u.username)? This action cannot be undone.")
                }
            }
        }
    }

    // MARK: - Subviews
    private func kpiChip(title: String, count: String, color: Color) -> some View {
        VStack(spacing: 2) {
            Text(count)
                .font(.title3)
                .fontWeight(.black)
                .foregroundColor(color)
            Text(title)
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .cardStyle(cornerRadius: 12)
    }

    private func filterChip(title: String, value: String) -> some View {
        let isSelected = adminVM.userRoleFilter == value
        return Button(action: { adminVM.userRoleFilter = value }) {
            Text(title)
                .font(.caption)
                .fontWeight(isSelected ? .bold : .medium)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? Color.primaryIndigo : Color.dynamicCard(for: colorScheme))
                .foregroundColor(isSelected ? .white : Color.dynamicTextMuted(for: colorScheme))
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(isSelected ? Color.primaryIndigo : Color.dynamicBorder(for: colorScheme), lineWidth: 1)
                )
        }
    }

    private func userCard(user: User) -> some View {
        HStack(spacing: 12) {
            // Avatar
            ZStack {
                Circle()
                    .fill(roleColor(user.role).opacity(0.15))
                    .frame(width: 44, height: 44)
                Image(systemName: roleIcon(user.role))
                    .foregroundColor(roleColor(user.role))
                    .font(.body)
            }

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(user.weredaKebele ?? user.fullName)
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                    if user.id == authVM.currentUser?.id {
                        Text("YOU")
                            .font(.system(size: 9, weight: .heavy))
                            .foregroundColor(Color.primaryIndigo)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 2)
                            .background(Color.primaryIndigo.opacity(0.15))
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                }

                Text("@\(user.username) · Code: \(user.participantCode ?? "N/A")")
                    .font(.caption)
                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                if !user.fullName.isEmpty {
                    Text("Name: \(user.fullName)")
                        .font(.caption2)
                        .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                }
            }

            Spacer()

            // Actions & Role
            VStack(alignment: .trailing, spacing: 6) {
                // Role Badge button (tap to edit)
                Button(action: {
                    userToEditRole = user
                    selectedNewRole = user.role
                }) {
                    HStack(spacing: 4) {
                        Text(roleBadge(user.role))
                            .font(.system(size: 10, weight: .heavy))
                        Image(systemName: "pencil")
                            .font(.system(size: 9))
                    }
                    .foregroundColor(roleColor(user.role))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(roleColor(user.role).opacity(0.12))
                    .clipShape(Capsule())
                }

                // Delete button
                if user.id != authVM.currentUser?.id {
                    Button(action: {
                        userToDelete = user
                        showDeleteAlert = true
                    }) {
                        Image(systemName: "trash")
                            .font(.caption)
                            .foregroundColor(Color.accentDanger)
                            .padding(6)
                            .background(Color.accentDanger.opacity(0.10))
                            .clipShape(Circle())
                    }
                }
            }
        }
        .padding(14)
        .cardStyle(cornerRadius: 14)
    }

    // Role Switcher Modal
    private func roleSwitcherSheet(user: User) -> some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 18) {
                Text("Assign Role for @\(user.username)")
                    .font(.headline)
                    .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))

                Text("Select the access level for \(user.fullName.isEmpty ? user.username : user.fullName):")
                    .font(.caption)
                    .foregroundColor(Color.dynamicTextMuted(for: colorScheme))

                VStack(spacing: 10) {
                    roleOptionCard(role: "ADMIN", title: "Admin (ሥርዓት አስተዳዳሪ)", desc: "Full permissions: Questions, results, users & config")
                    roleOptionCard(role: "USER_MANAGER", title: "User Manager (የተጠቃሚዎች አስተዳዳሪ)", desc: "Can view exam results, manage accounts & assign roles")
                    roleOptionCard(role: "EXAMINEE", title: "Examinee (ተፈታኝ)", desc: "Take Pre & Post assessments; results viewable by admin & manager")
                }

                Spacer()

                Button(action: {
                    Task {
                        _ = await adminVM.updateUserRole(userId: user.id, newRole: selectedNewRole)
                        userToEditRole = nil
                    }
                }) {
                    Text("Save Role Changes ✓")
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        .background(Color.primaryIndigo)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(20)
            .navigationTitle("Change User Role")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { userToEditRole = nil }
                }
            }
        }
    }

    private func roleOptionCard(role: String, title: String, desc: String) -> some View {
        let isSelected = selectedNewRole == role
        return Button(action: { selectedNewRole = role }) {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? Color.primaryIndigo : Color.dynamicTextMuted(for: colorScheme))
                    .font(.title3)

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(Color.dynamicTextPrimary(for: colorScheme))
                    Text(desc)
                        .font(.caption2)
                        .foregroundColor(Color.dynamicTextMuted(for: colorScheme))
                }
                Spacer()
            }
            .padding(12)
            .background(isSelected ? Color.primaryIndigo.opacity(0.12) : Color.dynamicCard(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Color.primaryIndigo : Color.dynamicBorder(for: colorScheme), lineWidth: isSelected ? 2 : 1)
            )
        }
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
