package com.raitha.bharosa.data.db

import androidx.room.*
import com.raitha.bharosa.data.FarmerProfile
import com.raitha.bharosa.data.SoilData
import com.raitha.bharosa.data.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    // --- Profile ---
    @Query("SELECT * FROM profile WHERE dbId = 1")
    fun getProfile(): Flow<FarmerProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: FarmerProfile)

    @Query("DELETE FROM profile")
    suspend fun clearProfile()

    // --- Soil Data ---
    @Query("SELECT * FROM soil_data ORDER BY timestamp DESC")
    fun getSoilHistory(): Flow<List<SoilData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoilData(data: SoilData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSoilData(list: List<SoilData>)

    @Query("DELETE FROM soil_data")
    suspend fun clearSoilHistory()

    // --- Tasks ---
    @Query("SELECT * FROM tasks ORDER BY dueDate DESC")
    fun getTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTasks(tasks: List<Task>)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, isCompleted: Boolean)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    // Clear everything
    @Transaction
    suspend fun clearAllUserData() {
        clearProfile()
        clearSoilHistory()
        clearTasks()
    }
}
