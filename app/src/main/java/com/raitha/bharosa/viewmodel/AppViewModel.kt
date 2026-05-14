package com.raitha.bharosa.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.raitha.bharosa.data.*
import com.raitha.bharosa.data.Translations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.raitha.bharosa.data.db.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val userDao = AppDatabase.getInstance(application).userDao()
    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

    private val _lang = MutableStateFlow(repository.getLanguage())
    val lang: StateFlow<String> = _lang.asStateFlow()

    val profile: StateFlow<FarmerProfile?> = userDao.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val soilHistory: StateFlow<List<SoilData>> = userDao.getSoilHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<Task>> = userDao.getTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (userDao.getTasks().first().isEmpty()) {
                userDao.insertAllTasks(loadInitialTasks())
            }
        }
    }

    private val _posts = MutableStateFlow(loadInitialPosts())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private fun loadInitialTasks(): List<Task> {
        val now = sdf.format(Date())
        return listOf(
            Task("1", "Test Soil NPK", now, false, "Soil Testing"),
            Task("2", "Primary Irrigation", now, true, "Irrigation"),
            Task("3", "Apply Fertilizer", now, false, "Fertilizer"),
            Task("4", "Weed Management", now, false, "Crop Care")
        )
    }

    private fun loadInitialPosts(): List<Post> {
        val saved = repository.getPosts()
        if (saved.isNotEmpty()) return saved
        return listOf(
            Post(
                id = "1", authorName = "Ramesh Gowda", authorCrop = CropType.Rice,
                message = "Has anyone started sowing rice this season? The monsoon seems a bit late in my area.",
                timestamp = sdf.format(Date(System.currentTimeMillis() - 3600000)),
                likes = 12, isLiked = false,
                comments = listOf(Comment("c1", "Suresh K", "Yes, I started last week. Soil moisture is just right.", sdf.format(Date(System.currentTimeMillis() - 1800000)))),
                category = "Question", topic = "General"
            ),
            Post(
                id = "2", authorName = "Manjula Devi", authorCrop = CropType.Maize,
                message = "Found that using organic vermicompost significantly improved my maize yield by 15% this year!",
                timestamp = sdf.format(Date(System.currentTimeMillis() - 7200000)),
                likes = 45, isLiked = false, comments = emptyList(),
                category = "Experience", topic = "Fertilizer"
            ),
            Post(
                id = "3", authorName = "Lakshmi Narayana", authorCrop = CropType.Wheat,
                message = "What is the best time to apply DAP for wheat in December? Looking for ICAR guidance.",
                timestamp = sdf.format(Date(System.currentTimeMillis() - 14400000)),
                likes = 8, isLiked = false, comments = emptyList(),
                category = "Question", topic = "Fertilizer"
            )
        )
    }

    fun t(key: String): String = Translations.get(_lang.value, key)

    fun setLang(l: String) {
        _lang.value = l
        repository.saveLanguage(l)
    }

    fun setProfile(p: FarmerProfile) {
        viewModelScope.launch {
            userDao.saveProfile(p)
        }
    }

    fun addSoilReading(data: SoilData) {
        viewModelScope.launch {
            userDao.insertSoilData(data)
        }
    }

    fun toggleTask(id: String) {
        viewModelScope.launch {
            val task = tasks.value.find { it.id == id }
            if (task != null) {
                userDao.updateTaskStatus(id, !task.isCompleted)
            }
        }
    }

    fun addPost(authorName: String, authorCrop: CropType, message: String, category: String, topic: String) {
        val newPost = Post(
            id = UUID.randomUUID().toString().take(9),
            authorName = authorName, authorCrop = authorCrop,
            message = message, timestamp = sdf.format(Date()),
            likes = 0, isLiked = false, comments = emptyList(),
            category = category, topic = topic
        )
        val newList = listOf(newPost) + _posts.value
        _posts.value = newList
        repository.savePosts(newList)
    }

    fun likePost(id: String) {
        val newList = _posts.value.map { post ->
            if (post.id == id) post.copy(
                likes = if (post.isLiked) post.likes - 1 else post.likes + 1,
                isLiked = !post.isLiked
            ) else post
        }
        _posts.value = newList
        repository.savePosts(newList)
    }

    fun addComment(postId: String, authorName: String, message: String) {
        val comment = Comment(
            id = UUID.randomUUID().toString().take(9),
            authorName = authorName,
            message = message, timestamp = sdf.format(Date())
        )
        val newList = _posts.value.map { post ->
            if (post.id == postId) post.copy(comments = post.comments + comment) else post
        }
        _posts.value = newList
        repository.savePosts(newList)
    }

    fun addToCart(product: com.raitha.bharosa.data.Product) {
        val existing = _cart.value.find { it.id == product.id }
        _cart.value = if (existing != null) {
            _cart.value.map { if (it.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            _cart.value + CartItem(product.id, product.name, product.price, 1, "", product.desc)
        }
    }

    fun updateQuantity(id: String, newQty: Int) {
        if (newQty <= 0) {
            _cart.value = _cart.value.filter { it.id != id }
        } else {
            _cart.value = _cart.value.map { item ->
                if (item.id == id) item.copy(quantity = newQty) else item
            }
        }
    }

    fun removeFromCart(id: String) {
        _cart.value = _cart.value.filter { it.id != id }
    }

    fun clearCart() { _cart.value = emptyList() }

    fun logout() {
        viewModelScope.launch {
            userDao.clearAllUserData()
            _cart.value = emptyList()
            repository.clear()
            
            // Re-insert initial tasks on logout to be ready for next user, or just let init handle it later
            userDao.insertAllTasks(loadInitialTasks())
        }
    }
}
