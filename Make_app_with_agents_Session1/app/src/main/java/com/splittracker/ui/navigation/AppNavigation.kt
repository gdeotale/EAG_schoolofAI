package com.splittracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.splittracker.ui.screens.*
import com.splittracker.ui.viewmodels.SharedViewModel
import com.splittracker.models.Expense
import com.splittracker.models.Group
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.splittracker.R
import java.util.UUID

@Composable
fun AppNavigation(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val navController = rememberNavController()
    val sharedViewModel: SharedViewModel = viewModel()
    val groups by sharedViewModel.groups.collectAsState()
    val expenses by sharedViewModel.expenses.collectAsState()
    val userMap by sharedViewModel.userMap.collectAsState()

    fun getCurrentEmail() = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase() ?: ""
    
    // Check if we need to setup listeners right away if already logged in
    remember {
        if (FirebaseAuth.getInstance().currentUser != null) {
            sharedViewModel.setupListeners()
        }
        true
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            val context = LocalContext.current
            SplashScreen(
                onAnimationFinish = {
                    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    val hasSeen = prefs.getBoolean("has_seen_onboarding", false)
                    val currentUser = FirebaseAuth.getInstance().currentUser

                    if (currentUser != null) {
                        sharedViewModel.setupListeners()
                        navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
                    } else if (!hasSeen) {
                        navController.navigate("onboarding") { popUpTo("splash") { inclusive = true } }
                    } else {
                        navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                    }
                }
            )
        }
        composable("onboarding") {
            val context = LocalContext.current
            OnboardingScreen(
                onFinish = {
                    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                    navController.navigate("login") { popUpTo("onboarding") { inclusive = true } }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    sharedViewModel.setupListeners()
                    navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        composable("dashboard") {
            val context = LocalContext.current
            DashboardScreen(
                groups = groups,
                expenses = expenses,
                userMap = userMap,
                currentUserEmail = getCurrentEmail(),
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onSignOut = {
                    sharedViewModel.clearData()
                    FirebaseAuth.getInstance().signOut()
                    // Also sign out from Google to allow account selection next time
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken("163551579266-5r8d380jrfflv2paql7ibqio6hftnq53.apps.googleusercontent.com")
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    googleSignInClient.signOut().addOnCompleteListener {
                        navController.navigate("login") { popUpTo("dashboard") { inclusive = true } }
                    }
                },
                onNavigateToGroup = { groupId ->
                    navController.navigate("groupDetails/$groupId")
                },
                onNavigateToCreateGroup = {
                    navController.navigate("createGroup")
                },
                onDeleteGroup = { groupId ->
                    sharedViewModel.deleteGroup(groupId)
                }
            )
        }
        composable("createGroup") {
            CreateGroupScreen(
                onBack = { navController.popBackStack() },
                onCreate = { groupName ->
                    sharedViewModel.createGroup(groupName)
                    navController.popBackStack()
                }
            )
        }
        composable("groupDetails/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val group = groups.find { it.id == groupId }
            val groupExpenses = expenses.filter { it.groupId == groupId }
            
            if (group != null) {
                GroupDetailsScreen(
                    group = group,
                    expenses = groupExpenses,
                    userMap = userMap,
                    currentUserEmail = getCurrentEmail(),
                    onBack = { navController.popBackStack() },
                    onAddExpense = { id -> 
                        navController.navigate("addExpense/$id")
                    },
                    onInviteFriend = { id ->
                        navController.navigate("inviteFriend/$id")
                    },
                    onSettleUp = { amount, paidBy, paidTo ->
                        sharedViewModel.addExpense(
                            Expense(
                                id = UUID.randomUUID().toString(),
                                groupId = groupId,
                                description = "Settlement",
                                amount = amount,
                                paidBy = paidBy,
                                splitAmong = listOf(paidTo)
                            )
                        )
                    },
                    onRemoveMember = { member ->
                        sharedViewModel.removeMember(groupId, member)
                        if (member == getCurrentEmail()) {
                            navController.popBackStack()
                        }
                    },
                    onDeleteExpense = { expId -> sharedViewModel.deleteExpense(expId) },
                    onEditExpense = { expId, desc, amt -> sharedViewModel.updateExpense(expId, desc, amt) }
                )
            }
        }
        composable("addExpense/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val group = groups.find { it.id == groupId }
            if (group != null) {
                AddExpenseScreen(
                    groupId = groupId,
                    members = group.members,
                    userMap = userMap,
                    currentUserEmail = getCurrentEmail(),
                    onBack = { navController.popBackStack() },
                    onSave = { desc, amount, paidBy, splitAmong ->
                        sharedViewModel.addExpense(
                            Expense(
                                id = UUID.randomUUID().toString(),
                                groupId = groupId, 
                                description = desc, 
                                amount = amount,
                                paidBy = paidBy,
                                splitAmong = splitAmong
                            )
                        )
                        navController.popBackStack()
                    }
                )
            }
        }
        composable("inviteFriend/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val groupName = groups.find { it.id == groupId }?.name ?: "a group"
            val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "A friend"
            
            InviteFriendScreen(
                groupId = groupId,
                groupName = groupName,
                senderName = senderName,
                onBack = { navController.popBackStack() },
                onInvite = { emailToInvite ->
                    sharedViewModel.inviteByEmail(groupId, emailToInvite)
                    navController.popBackStack()
                }
            )
        }
        // Deep linking integration
        composable(
            route = "joinGroup/{groupId}",
            deepLinks = listOf(navDeepLink { uriPattern = "splittracker://join?groupId={groupId}" })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            
            // Navigate based on auth state
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                sharedViewModel.setupListeners()
                sharedViewModel.joinGroup(groupId)
                navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
            } else {
                navController.navigate("login") { popUpTo("splash") { inclusive = true } }
            }
        }
    }
}
