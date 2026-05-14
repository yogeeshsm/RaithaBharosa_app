package com.raitha.bharosa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raitha.bharosa.data.Post
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.viewmodel.AppViewModel

val TOPICS = listOf("All", "Soil", "Fertilizer", "Irrigation", "Harvest", "General")
val CATEGORIES = listOf("Experience", "Question", "Update")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val lang by viewModel.lang.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val profile by viewModel.profile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("All") }
    var showAddPost by remember { mutableStateOf(false) }
    var expandedPostId by remember { mutableStateOf<String?>(null) }
    var newComment by remember { mutableStateOf("") }

    val filtered = posts.filter { post ->
        (selectedTopic == "All" || post.topic == selectedTopic) &&
        (searchQuery.isBlank() || post.message.contains(searchQuery, ignoreCase = true) || post.author.contains(searchQuery, ignoreCase = true))
    }

    // Add Post Dialog
    if (showAddPost) {
        var newCategory by remember { mutableStateOf("Experience") }
        var newTopic by remember { mutableStateOf("Soil") }
        var newMessage by remember { mutableStateOf("") }
        var topicExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddPost = false },
            title = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Create, contentDescription = null, tint = BrandDeep)
                    Text(viewModel.t("post"), fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Category
                    Text(viewModel.t("category"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CATEGORIES.forEach { cat ->
                            Surface(
                                modifier = Modifier.clickable { newCategory = cat },
                                color = if (newCategory == cat) BrandDeep else SurfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = if (newCategory == cat) Color.White else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }

                    // Topic
                    ExposedDropdownMenuBox(expanded = topicExpanded, onExpandedChange = { topicExpanded = it }) {
                        OutlinedTextField(
                            value = "#$newTopic", onValueChange = {}, readOnly = true,
                            label = { Text("Topic") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicExpanded) },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandDeep),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = topicExpanded, onDismissRequest = { topicExpanded = false }) {
                            TOPICS.drop(1).forEach { topic ->
                                DropdownMenuItem(text = { Text("#$topic") }, onClick = { newTopic = topic; topicExpanded = false })
                            }
                        }
                    }

                    // Message
                    OutlinedTextField(
                        value = newMessage, onValueChange = { newMessage = it },
                        label = { Text(viewModel.t("message")) },
                        placeholder = { Text("Share your farming experience...") },
                        maxLines = 4, minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandDeep),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (newMessage.isNotBlank()) {
                                viewModel.addPost(
                                    authorName = profile?.name ?: "Farmer",
                                    authorCrop = profile?.primaryCrop ?: com.raitha.bharosa.data.CropType.Rice,
                                    message = newMessage.trim(),
                                    category = newCategory, topic = newTopic
                                )
                                showAddPost = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(viewModel.t("post"), fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { showAddPost = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(viewModel.t("close"), color = Color.Gray)
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        // Header
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(viewModel.t("community"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text("${posts.size} ${viewModel.t("posts")}", fontSize = 10.sp, color = Color.Gray)
            }
            IconButton(onClick = { showAddPost = true }) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = BrandDeep)
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text(viewModel.t("search"), color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = if (searchQuery.isNotEmpty()) { { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray) } } } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandDeep, unfocusedBorderColor = Color(0xFFE0E0E0), focusedContainerColor = Surface, unfocusedContainerColor = Surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Topic Chips
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TOPICS) { topic ->
                Surface(
                    modifier = Modifier.clickable { selectedTopic = topic },
                    color = if (selectedTopic == topic) BrandDeep else Surface,
                    shape = RoundedCornerShape(20.dp),
                    border = if (selectedTopic != topic) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null
                ) {
                    Text(
                        if (topic == "All") topic else "#$topic",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (selectedTopic == topic) Color.White else Color.Gray,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Posts List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered) { post ->
                PostCard(
                    post = post, viewModel = viewModel, lang = lang,
                    isExpanded = expandedPostId == post.id,
                    onToggleExpand = { expandedPostId = if (expandedPostId == post.id) null else post.id },
                    newComment = newComment, onCommentChange = { newComment = it },
                    onAddComment = {
                        if (newComment.isNotBlank()) {
                            viewModel.addComment(post.id, profile?.name ?: "Farmer", newComment.trim())
                            newComment = ""
                        }
                    }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // FAB
    Box(Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { showAddPost = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = BrandDeep,
            shape = CircleShape
        ) { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) }
    }
}

@Composable
private fun PostCard(
    post: Post, viewModel: AppViewModel, lang: String,
    isExpanded: Boolean, onToggleExpand: () -> Unit,
    newComment: String, onCommentChange: (String) -> Unit, onAddComment: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Author Row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                // Avatar
                Box(Modifier.size(44.dp).clip(CircleShape).background(BrandDeep), contentAlignment = Alignment.Center) {
                    Text(post.author.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(post.author, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = BrandBg, shape = RoundedCornerShape(6.dp)) {
                            Text(post.crop, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(
                            color = when (post.category) { "Question" -> Color(0xFFEFF6FF); "Update" -> Color(0xFFF0FDF4); else -> Color(0xFFF5F3FF) },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(post.category, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                color = when (post.category) { "Question" -> Color(0xFF2563EB); "Update" -> Color(0xFF16A34A); else -> Color(0xFF7C3AED) },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp)) {
                    Text("#${post.topic}", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }

            Text(post.message, fontSize = 13.sp, color = OnBackground)

            // Actions Row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { viewModel.likePost(post.id) }, modifier = Modifier.size(32.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (post.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (post.likedByMe) BrandDanger else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text("${post.likes}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    Text("${post.comments.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp)) {
                    Text(post.time, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }

            // Comments
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider(color = Color(0xFFF3F4F6))
                    post.comments.forEach { comment ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(SurfaceVariant), contentAlignment = Alignment.Center) {
                                Text(comment.author.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandDeep)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(comment.author, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                Text(comment.message, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Add Comment
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newComment, onValueChange = onCommentChange,
                            placeholder = { Text("Write a comment...", fontSize = 12.sp) },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandDeep),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onAddComment,
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandDeep)
                        ) { Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
}
