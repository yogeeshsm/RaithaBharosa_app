package com.raitha.bharosa.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("raitha_bharosa", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLanguage(lang: String) = prefs.edit().putString("lang", lang).apply()
    fun getLanguage(): String = prefs.getString("lang", "en") ?: "en"

    fun saveProfile(profile: FarmerProfile?) {
        prefs.edit().putString("profile", profile?.let { gson.toJson(it) }).apply()
    }
    fun getProfile(): FarmerProfile? {
        return try {
            val raw = prefs.getString("profile", null) ?: return null
            // Must use cropType as string to enum conversion
            val map = gson.fromJson(raw, Map::class.java)
            val cropStr = map["primaryCrop"]?.toString() ?: "Rice"
            val crop = CropType.entries.find { it.name == cropStr } ?: CropType.Rice
            FarmerProfile(
                name = map["name"]?.toString() ?: "",
                village = map["village"]?.toString() ?: "",
                district = map["district"]?.toString() ?: "",
                landArea = (map["landArea"] as? Double) ?: 2.5,
                primaryCrop = crop
            )
        } catch (e: Exception) { null }
    }

    fun saveSoilHistory(list: List<SoilData>) =
        prefs.edit().putString("soil", gson.toJson(list)).apply()
    fun getSoilHistory(): List<SoilData> {
        return try {
            val json = prefs.getString("soil", null) ?: return emptyList()
            gson.fromJson(json, object : TypeToken<List<SoilData>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    fun saveTasks(list: List<Task>) =
        prefs.edit().putString("tasks", gson.toJson(list)).apply()
    fun getTasks(): List<Task> {
        return try {
            val json = prefs.getString("tasks", null) ?: return emptyList()
            gson.fromJson(json, object : TypeToken<List<Task>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    fun savePosts(list: List<Post>) =
        prefs.edit().putString("posts", gson.toJson(list)).apply()
    fun getPosts(): List<Post> {
        return try {
            val json = prefs.getString("posts", null) ?: return emptyList()
            val rawList = gson.fromJson(json, object : TypeToken<List<Map<String, Any>>>() {}.type) as? List<Map<String, Any>> ?: return emptyList()
            rawList.map { map ->
                val cropStr = map["authorCrop"]?.toString() ?: "Rice"
                val crop = CropType.entries.find { it.name == cropStr } ?: CropType.Rice
                val rawComments = map["comments"] as? List<Map<String, Any>> ?: emptyList()
                val comments = rawComments.map { c ->
                    Comment(
                        id = c["id"]?.toString() ?: "",
                        authorName = c["authorName"]?.toString() ?: "",
                        message = c["message"]?.toString() ?: "",
                        timestamp = c["timestamp"]?.toString() ?: ""
                    )
                }
                Post(
                    id = map["id"]?.toString() ?: "",
                    authorName = map["authorName"]?.toString() ?: "",
                    authorCrop = crop,
                    message = map["message"]?.toString() ?: "",
                    timestamp = map["timestamp"]?.toString() ?: "",
                    likes = (map["likes"] as? Double)?.toInt() ?: 0,
                    isLiked = map["isLiked"] as? Boolean ?: false,
                    comments = comments,
                    category = map["category"]?.toString() ?: "Experience",
                    topic = map["topic"]?.toString() ?: "General"
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun clear() {
        prefs.edit()
            .remove("profile").remove("soil")
            .remove("tasks").remove("posts")
            .apply()
    }
}
