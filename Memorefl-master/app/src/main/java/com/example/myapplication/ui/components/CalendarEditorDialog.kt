package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.CalendarEvent

@Composable
fun CalendarEditorDialog(
    title: String = "日程编辑器",
    initialEvents: List<CalendarEvent>,
    onSave: (List<CalendarEvent>) -> Unit,
    onDismiss: () -> Unit
) {
    var events by remember { mutableStateOf(initialEvents) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(events) { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var time by remember { mutableStateOf(event.time) }
                            var titleStr by remember { mutableStateOf(event.title) }
                            
                            OutlinedTextField(
                                value = time,
                                onValueChange = { 
                                    time = it
                                    events = events.map { e -> if (e.id == event.id) e.copy(time = it) else e }
                                },
                                modifier = Modifier.width(90.dp),
                                label = { Text("时间", fontSize = 10.sp) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = titleStr,
                                onValueChange = { 
                                    titleStr = it
                                    events = events.map { e -> if (e.id == event.id) e.copy(title = it) else e }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("事项", fontSize = 10.sp) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            IconButton(onClick = { events = events.filter { it.id != event.id } }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { events = events + CalendarEvent(time = "09:00", title = "") }) {
                        Icon(Icons.Default.Add, null)
                        Text("增加事项")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(events) }) { Text("保存") }
                }
            }
        }
    }
}
