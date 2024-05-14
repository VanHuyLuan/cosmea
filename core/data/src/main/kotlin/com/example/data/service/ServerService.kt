package com.example.data.service

import android.util.Log
import com.example.data.repo.ServerRepository
import com.example.model.ServerData
import com.example.model.UserData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ServerService(private val firestore: FirebaseFirestore): ServerRepository {
    override suspend fun addServerData(serverData: ServerData) {
        firestore.collection("servers").
        document(serverData.id).set(serverData)
            .addOnSuccessListener {
                Log.d("FIRESTORE", "Created server successfully: $serverData")
            }
            .addOnFailureListener {exception ->
                Log.e("FIRESTORE ERROR", "Error adding server data to Firestore: $exception")
            }.await()
    }

    override suspend fun getAdminId(serverId: String): String? {
        var adminId: String? = null
        firestore.collection("servers").document(serverId).get()
            .addOnSuccessListener {documentSnapshot ->
                adminId = documentSnapshot.data?.get("adminId").toString()
                Log.d("FIRESTORE", "Get admin ID successfully: $adminId")
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE ERROR", "Failed to get admin ID")
            }.await()
        return adminId
    }

    override suspend fun getServerDataById(serverId: String): ServerData? {
        var server: ServerData? = null
        firestore.collection("servers").document(serverId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val id: String = document.data?.get("id").toString()
                    val adminId: String = document.data?.get("adminId").toString()
                    val avatar: String = document.data?.get("avatar").toString()
                    val categories: MutableList<String> = document.data?.get("categories") as MutableList<String>
                    val members: MutableList<UserData> = document.data?.get("members") as MutableList<UserData>
                    val name: String = document.data?.get("name").toString()
                    Log.d("FIRESTORE", "Get server with ID: $serverId successfully")
                    server =  ServerData(adminId, name, avatar, members, categories, id = id)
                } else {
                    Log.d("FIRESTORE ERROR", "Server not found with ID: $serverId")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE ERROR", "Error getting server: ", exception)
            }.await()
        return server
    }

    override suspend fun updateServerData(serverId: String, serverData: ServerData) {
        val server = getServerDataById(serverId)
        if (server != null) {
            firestore.collection("servers").document(serverId).set(serverData)
                .addOnSuccessListener {
                    Log.d("FIRESTORE", "Updated server successfully: $serverData")
                }
                .addOnFailureListener { exception ->
                    Log.e("FIRESTORE ERROR", "Error updating server data to Firestore: $exception")
                }.await()
        }
        else {
            Log.d("FIRESTORE ERROR", "Server not found with ID: $serverId")
        }
    }

    override suspend fun deleteServerDataById(serverId: String) {
        val server = getServerDataById(serverId)
        if (server != null) {
            firestore.collection("servers").document(serverId).delete()
                .addOnSuccessListener {
                    Log.d("FIRESTORE", "Deleted server with ID: $serverId successfully")
                }
                .addOnFailureListener { exception ->
                    Log.e("FIRESTORE ERROR", "Error deleting server data: $exception")
                }.await()
        }
        else {
            Log.d("FIRESTORE ERROR", "Server not found with ID: $serverId")
        }
    }

    override suspend fun addMember(serverId: String, userData: UserData) {
        val server = getServerDataById(serverId)
        if (server != null) {
            firestore.collection("servers").document(serverId).get()
                .addOnSuccessListener {documentSnapshot ->
                    var users: MutableList<UserData> = documentSnapshot.get("members") as MutableList<UserData>
                    users.add(userData)
                    firestore.collection("servers").document(serverId).update("members", users)
                    Log.d("FIRESTORE", "Added user successfully: ${users}")
                }
                .addOnFailureListener { exception ->
                    Log.e("FIRESTORE ERROR", "Error adding user: $exception")
                }.await()
        }
    }

    override suspend fun getAllMembers(serverId: String): String? {
        var members: String? = null
        firestore.collection("servers").document(serverId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    members = documentSnapshot.get("members").toString()
                    Log.d("FIRESTORE", "Get all members successfully: $members")
                } else {
                    Log.e("FIRESTORE ERROR", "Not found server with ID: $serverId")
                }
            }
            .addOnFailureListener {exception ->
                Log.e("FIRESTORE ERROR", "Error getting members: $exception")
            }.await()
        return null
    }

    override suspend fun getAllServerData(): List<ServerData> {
        val servers = mutableListOf<ServerData>()
        firestore.collection("servers").get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val id: String = document.data["id"].toString()
                    val adminId: String = document.data["adminId"].toString()
                    val avatar: String = document.data["avatar"].toString()
                    val categories: MutableList<String> = document.data["categories"] as MutableList<String>
                    val members: MutableList<UserData> = document.data["members"] as MutableList<UserData>
                    val name: String = document.data["name"].toString()
                    servers.add(ServerData(adminId, name, avatar, members, categories, id = id))
                }
                Log.d("FIRESTORE", "Get all servers successfully:")
                servers.forEach { server ->
                    println(server.toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE ERROR", "Error getting all servers: $exception")
            }.await()

        return servers
    }
}