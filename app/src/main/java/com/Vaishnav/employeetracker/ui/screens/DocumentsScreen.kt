package com.Vaishnav.employeetracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Vaishnav.employeetracker.data.firebase.FirebaseDocument
import com.Vaishnav.employeetracker.viewmodel.DocumentViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    employeeId: String = "",
    isAdmin: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: DocumentViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var allDocuments by remember { mutableStateOf<List<FirebaseDocument>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedDocument by remember { mutableStateOf<FirebaseDocument?>(null) }
    
    // Filter documents by search query
    val documents = remember(allDocuments, searchQuery) {
        if (searchQuery.isEmpty()) {
            allDocuments
        } else {
            allDocuments.filter { doc ->
                doc.documentName.contains(searchQuery, ignoreCase = true) ||
                doc.documentType.contains(searchQuery, ignoreCase = true) ||
                doc.notes?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(employeeId, selectedFilter, isAdmin) {
        scope.launch {
            try {
                allDocuments = when {
                    // Admin viewing all documents
                    isAdmin && employeeId.isEmpty() -> {
                        when (selectedFilter) {
                            "All" -> viewModel.getAllDocuments().first()
                            "Expiring" -> {
                                val currentTime = System.currentTimeMillis()
                                val warningTime = currentTime + (30L * 24 * 60 * 60 * 1000) // 30 days
                                viewModel.getExpiringDocuments(currentTime, warningTime).first()
                            }
                            "Expired" -> {
                                val currentTime = System.currentTimeMillis()
                                viewModel.getExpiredDocuments(currentTime).first()
                            }
                            else -> viewModel.getDocumentsByType("", selectedFilter).first()
                        }
                    }
                    // Viewing specific employee's documents (only if employeeId is not empty)
                    employeeId.isNotEmpty() -> {
                        when (selectedFilter) {
                            "All" -> viewModel.getEmployeeDocuments(employeeId).first()
                            "Expiring" -> {
                                val currentTime = System.currentTimeMillis()
                                val warningTime = currentTime + (30L * 24 * 60 * 60 * 1000) // 30 days
                                viewModel.getExpiringDocuments(currentTime, warningTime).first()
                                    .filter { it.employeeId == employeeId }
                            }
                            "Expired" -> {
                                val currentTime = System.currentTimeMillis()
                                viewModel.getExpiredDocuments(currentTime).first()
                                    .filter { it.employeeId == employeeId }
                            }
                            else -> viewModel.getDocumentsByType(employeeId, selectedFilter).first()
                        }
                    }
                    // No valid data - empty list
                    else -> emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("DocumentsScreen", "Error loading documents", e)
                allDocuments = emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Documents",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showUploadDialog = true }) {
                        Icon(Icons.Default.CloudUpload, "Upload Document")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF3F4F6),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                containerColor = Color(0xFF6366F1),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Upload, "Upload")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Professional Search Bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                "Search documents by title or type...",
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            
                
                // Enhanced Filter Chips
                val filters = listOf("All", "ID Proof", "Certificate", "Contract", "Expiring", "Expired")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters.size) { index ->
                    val filter = filters[index]
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        leadingIcon = {
                            if (selectedFilter == filter) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Statistics Cards
            val expiringCount = allDocuments.count { doc ->
                doc.expiryDate?.let { expiry ->
                    val daysUntil = (expiry - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                    daysUntil in 0..30
                } ?: false
            }
            val expiredCount = allDocuments.count { doc ->
                doc.expiryDate?.let { it < System.currentTimeMillis() } ?: false
            }
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DocumentStatCard(
                        icon = Icons.Default.Description,
                        title = "Total",
                        value = documents.size.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    DocumentStatCard(
                        icon = Icons.Default.Warning,
                        title = "Expiring Soon",
                        value = expiringCount.toString(),
                        color = Color(0xFFFF9800)
                    )
                }
                item {
                    DocumentStatCard(
                        icon = Icons.Default.Error,
                        title = "Expired",
                        value = expiredCount.toString(),
                        color = Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Documents List
            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No documents found",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload your first document",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(documents) { document ->
                        DocumentCard(
                            document = document,
                            onClick = { selectedDocument = document },
                            onDelete = {
                                if (isAdmin) {
                                    scope.launch {
                                        viewModel.deleteDocument(document.id)
                                        // Refresh will happen automatically via LaunchedEffect
                                    }
                                }
                            },
                            isAdmin = isAdmin
                        )
                    }
                }
            }
            }
        }

        if (showUploadDialog) {
            UploadDocumentDialog(
                employeeId = employeeId,
                onDismiss = { showUploadDialog = false },
                onUpload = { title, type, filePath, expiryDate ->
                    scope.launch {
                        val expiryMillis = expiryDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                        viewModel.uploadDocument(
                            employeeId = employeeId,
                            documentType = type,
                            documentName = title,
                            documentUri = filePath,
                            uploadedBy = employeeId,
                            expiryDate = expiryMillis
                        )
                        // Refresh will happen automatically via LaunchedEffect
                        showUploadDialog = false
                    }
                }
            )
        }

        selectedDocument?.let { document ->
            DocumentDetailsDialog(
                document = document,
                onDismiss = { selectedDocument = null }
            )
        }
    }
}

@Composable
fun DocumentStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DocumentCard(
    document: FirebaseDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isAdmin: Boolean
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val expiryStatus = document.expiryDate?.let { expiry ->
        val daysUntilExpiry = (expiry - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
        when {
            daysUntilExpiry < 0 -> "Expired"
            daysUntilExpiry <= 30 -> "Expires in $daysUntilExpiry days"
            else -> "Valid"
        }
    }
    
    val expiryColor = document.expiryDate?.let { expiry ->
        val daysUntilExpiry = (expiry - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
        when {
            daysUntilExpiry < 0 -> Color(0xFFF44336)
            daysUntilExpiry <= 30 -> Color(0xFFFF9800)
            else -> Color(0xFF4CAF50)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = getDocumentTypeColor(document.documentType).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getDocumentTypeIcon(document.documentType),
                            contentDescription = null,
                            tint = getDocumentTypeColor(document.documentType),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = document.documentName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = document.documentType,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (expiryStatus != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = expiryStatus,
                            fontSize = 12.sp,
                            color = expiryColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isAdmin) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFF44336)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete '${document.documentName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentDialog(
    employeeId: String,
    onDismiss: () -> Unit,
    onUpload: (String, String, String, LocalDate?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ID Proof") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var expiryDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
    }

    val documentTypes = listOf("ID Proof", "Certificate", "Contract", "Resume", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Document") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    placeholder = { Text("e.g., Aadhar Card") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                var expandedType by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        documentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (selectedFileUri != null) Icons.Default.CheckCircle else Icons.Default.Upload,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedFileUri != null) "File Selected" else "Choose File")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (expiryDate != null)
                            "Expires: ${expiryDate!!.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
                        else
                            "Set Expiry Date (Optional)"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedFileUri != null) {
                        onUpload(title, selectedType, selectedFileUri.toString(), expiryDate)
                    }
                },
                enabled = title.isNotBlank() && selectedFileUri != null
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsDialog(
    document: FirebaseDocument,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(document.documentName) },
        text = {
            Column {
                DetailRow("Type", document.documentType)
                document.uploadedAt?.let { uploadDate ->
                    DetailRow("Uploaded", java.time.Instant.ofEpochMilli(uploadDate.time)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                }
                document.expiryDate?.let {
                    DetailRow("Expires", java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { /* Open file */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Document")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun getDocumentTypeIcon(type: String): ImageVector {
    return when (type) {
        "ID Proof" -> Icons.Default.Badge
        "Certificate" -> Icons.Default.EmojiEvents
        "Contract" -> Icons.Default.Description
        "Resume" -> Icons.Default.Person
        else -> Icons.Default.Description
    }
}

fun getDocumentTypeColor(type: String): Color {
    return when (type) {
        "ID Proof" -> Color(0xFF2196F3)
        "Certificate" -> Color(0xFF4CAF50)
        "Contract" -> Color(0xFFFF9800)
        "Resume" -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
    }
}
