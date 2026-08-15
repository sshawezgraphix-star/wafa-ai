package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettingsManager
import com.example.data.SavedNote
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesBottomSheet(
    settingsManager: AppSettingsManager,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val notes by settingsManager.notesFlow.collectAsState()
    var newNoteText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF333A52))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Notes",
                    tint = NeonPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MAYA SAVED NOTES",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "${notes.size} notes stored on device",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick Add Note Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a quick note...", color = TextMuted, fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color(0xFF2B334E),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CyberSurfaceVariant,
                        unfocusedContainerColor = CyberSurfaceVariant
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newNoteText.isNotBlank()) {
                            settingsManager.addNote(newNoteText)
                            newNoteText = ""
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .background(NeonPurple, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = "Add Note",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes List
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes yet. Ask Maya \"Take a note...\" or type above!",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onDelete = { settingsManager.deleteNote(note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCardItem(
    note: SavedNote,
    onDelete: () -> Unit
) {
    val dateStr = remember(note.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        sdf.format(Date(note.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Note",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
