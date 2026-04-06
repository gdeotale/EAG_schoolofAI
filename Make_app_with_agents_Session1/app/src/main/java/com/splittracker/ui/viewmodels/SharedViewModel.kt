package com.splittracker.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.splittracker.models.Expense
import com.splittracker.models.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SharedViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _userMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val userMap: StateFlow<Map<String, String>> = _userMap.asStateFlow()

    private var groupsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private val expenseListeners = mutableMapOf<String, ListenerRegistration>()

    init {
        setupListeners()
    }

    fun clearData() {
        groupsListener?.remove()
        groupsListener = null
        usersListener?.remove()
        usersListener = null
        expenseListeners.values.forEach { it.remove() }
        expenseListeners.clear()
        expenseCache.clear()
        _groups.value = emptyList()
        _expenses.value = emptyList()
        _userMap.value = emptyMap()
    }

    fun setupListeners() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("SplitTracker", "setupListeners: currentUser is null")
            return
        }

        val email = currentUser.email?.trim()?.lowercase() ?: return
        val displayName = currentUser.displayName ?: "Unknown"
        Log.d("SplitTracker", "setupListeners: Starting sync for email='$email'")

        // 1. Create default group if new user, then update user document
        val userRef = db.collection("users").document(email)
        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                Log.d("SplitTracker", "setupListeners: First time user detected! Creating default group.")
                createGroup("Self Expense Tracker")
            }
            
            // Always save/update the user info (in case displayName changed)
            userRef.set(mapOf("email" to email, "displayName" to displayName))
                .addOnSuccessListener {
                    Log.d("SplitTracker", "users doc updated successfully for $email")
                }.addOnFailureListener { e ->
                    Log.e("SplitTracker", "users doc update failed", e)
                }
        }.addOnFailureListener { e ->
            Log.e("SplitTracker", "Failed to check if user exists", e)
        }

        // 2. Listen to users
        usersListener?.remove()
        usersListener = db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val map = mutableMapOf<String, String>()
            for (doc in snapshot.documents) {
                map[doc.getString("email") ?: ""] = doc.getString("displayName") ?: ""
            }
            _userMap.value = map
        }

        // 3. Listen to groups current user is part of
        groupsListener?.remove()
        Log.d("SplitTracker", "setupListeners: Creating groupsListener whereArrayContains('members', '$email')")
        groupsListener = db.collection("groups")
            .whereArrayContains("members", email)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SplitTracker", "groupsListener failed", e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                
                Log.d("SplitTracker", "groupsListener triggered! Found ${snapshot.documents.size} groups")
                val newGroups = snapshot.documents.mapNotNull { doc ->
                    val grp = doc.toObject(Group::class.java)
                    Log.d("SplitTracker", "Deserialized group: ${grp?.name} with members: ${grp?.members}")
                    grp
                }
                _groups.value = newGroups
                updateExpenseListeners(newGroups)
            }
    }

    private fun updateExpenseListeners(newGroups: List<Group>) {
        val currentGroupIds = newGroups.map { it.id }.toSet()
        
        // Remove old listeners
        val iterator = expenseListeners.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!currentGroupIds.contains(entry.key)) {
                entry.value.remove()
                iterator.remove()
                expenseCache.remove(entry.key)
            }
        }
        recalculateAllExpenses()

        // Add new listeners
        for (groupId in currentGroupIds) {
            bindExpenseListener(groupId)
        }
    }

    // Because Firestore doesn't provide a synchronous way to read cached query results across multiple queries, 
    // a simple way to combine expenses is just to query them or maintain a cache map.
    private val expenseCache = mutableMapOf<String, List<Expense>>()

    // Let's rewrite updateExpenseListeners to populate expenseCache
    private fun bindExpenseListener(groupId: String) {
        if (expenseListeners.containsKey(groupId)) return
        val listener = db.collection("expenses")
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    val exps = snapshot.documents.mapNotNull { it.toObject(Expense::class.java) }
                    expenseCache[groupId] = exps
                    _expenses.value = expenseCache.values.flatten()
                }
            }
        expenseListeners[groupId] = listener
    }

    private fun recalculateAllExpenses() {
         _expenses.value = expenseCache.values.flatten()
    }

    override fun onCleared() {
        super.onCleared()
        groupsListener?.remove()
        usersListener?.remove()
        expenseListeners.values.forEach { it.remove() }
    }
    
    // Actions
    fun createGroup(name: String) {
        val email = auth.currentUser?.email?.trim()?.lowercase() ?: return
        val group = Group(name = name, members = mutableListOf(email))
        Log.d("SplitTracker", "createGroup: Creating group '$name' with member: '$email'")
        db.collection("groups").document(group.id).set(group)
            .addOnSuccessListener {
                Log.d("SplitTracker", "createGroup: Successfully created to Firestore")
            }.addOnFailureListener { e ->
                Log.e("SplitTracker", "createGroup: Failure", e)
            }
    }

    fun addExpense(expense: Expense) {
        Log.d("SplitTracker", "addExpense: Saving expense ${expense.id} to Firestore")
        db.collection("expenses").document(expense.id).set(expense)
    }

    fun updateExpense(expenseId: String, newDescription: String, newAmount: Double) {
        db.collection("expenses").document(expenseId).update(
            mapOf("description" to newDescription, "amount" to newAmount)
        ).addOnSuccessListener {
            Log.d("SplitTracker", "updateExpense: Successfully updated $expenseId")
        }.addOnFailureListener { e ->
            Log.e("SplitTracker", "updateExpense: Failed to update", e)
        }
    }

    fun deleteExpense(expenseId: String) {
        db.collection("expenses").document(expenseId).delete()
            .addOnSuccessListener {
                Log.d("SplitTracker", "deleteExpense: Successfully deleted $expenseId")
            }.addOnFailureListener { e ->
                Log.e("SplitTracker", "deleteExpense: Failed", e)
            }
    }

    fun joinGroup(groupId: String) {
        val email = auth.currentUser?.email?.lowercase() ?: return
        db.collection("groups").document(groupId).get().addOnSuccessListener { doc ->
            val group = doc.toObject(Group::class.java)
            if (group != null) {
                if (!group.members.contains(email)) {
                    val newMembers = group.members.toMutableList()
                    newMembers.add(email)
                    db.collection("groups").document(groupId).update("members", newMembers)
                }
            }
        }
    }

    fun removeMember(groupId: String, memberEmail: String) {
        db.collection("groups").document(groupId).get().addOnSuccessListener { doc ->
            val group = doc.toObject(Group::class.java)
            if (group != null) {
                val newMembers = group.members.toMutableList()
                newMembers.remove(memberEmail)
                db.collection("groups").document(groupId).update("members", newMembers)
            }
        }
    }
    
    fun inviteByEmail(groupId: String, emailToAdd: String) {
        val targetEmail = emailToAdd.trim().lowercase()
        Log.d("SplitTracker", "inviteByEmail: Attempting to add '$targetEmail' to group $groupId")
        db.collection("groups").document(groupId).get().addOnSuccessListener { doc ->
            val group = doc.toObject(Group::class.java)
            if (group != null) {
                // Add them as a member inherently
                val newMembers = group.members.toMutableList()
                Log.d("SplitTracker", "inviteByEmail: Current members before adding: $newMembers")
                if (!newMembers.contains(targetEmail)) {
                    newMembers.add(targetEmail)
                    Log.d("SplitTracker", "inviteByEmail: Executing update to Firestore with members: $newMembers")
                    db.collection("groups").document(groupId).update("members", newMembers)
                        .addOnSuccessListener {
                            Log.d("SplitTracker", "inviteByEmail: Succesfully updated the group array in Firestore!")
                        }.addOnFailureListener { e ->
                            Log.e("SplitTracker", "inviteByEmail: UPDATE FAILED!", e)
                        }
                } else {
                    Log.d("SplitTracker", "inviteByEmail: targetEmail '$targetEmail' is ALREADY in the list.")
                }
            } else {
                Log.d("SplitTracker", "inviteByEmail: Error! Fetched group was null somehow.")
            }
        }.addOnFailureListener { e ->
            Log.e("SplitTracker", "inviteByEmail: Failed to fetch group to update!", e)
        }
    }

    fun deleteGroup(groupId: String) {
        db.collection("groups").document(groupId).delete()
        val exps = expenseCache[groupId] ?: emptyList()
        exps.forEach { exp ->
            db.collection("expenses").document(exp.id).delete()
        }
    }
}
