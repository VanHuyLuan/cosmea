package com.example.data.repo

import com.example.model.ServerData
import com.example.model.UserData

interface ServerRepository {
    suspend fun addServerData(serverData: ServerData)
    suspend fun getServerDataById(serverId: String): ServerData?
    suspend fun updateServerData(serverId: String, serverData: ServerData)
    suspend fun deleteServerDataById(serverId: String)
    suspend fun getAllMembers(serverId: String): String?
    suspend fun addMember(serverId: String, userData: UserData)
    suspend fun getAdminId(serverId: String): String?
    suspend fun getAllServerData(): List<ServerData>
}