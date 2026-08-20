package com.clickdownloader.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DownloaderScreen(
    vm: MainViewModel,
    onPickCookie: () -> Unit,
    onPickFolder: () -> Unit
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val tabs = listOf("Download", "Queue", "History", "Settings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Click Downloader", fontWeight = FontWeight.Bold)
                        Text("Advanced downloader v0.2", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = s.selectedTab == i,
                        onClick = { vm.setTab(i) },
                        icon = {
                            Icon(
                                when (i) {
                                    0 -> Icons.Default.Download
                                    1 -> Icons.Default.List
                                    2 -> Icons.Default.History
                                    else -> Icons.Default.Settings
                                },
                                null
                            )
                        },
                        label = { Text(t) }
                    )
                }
            }
        }
    ) { pad ->
        when (s.selectedTab) {
            0 -> DownloadTab(s, vm, Modifier.padding(pad))
            1 -> QueueTab(s, vm, Modifier.padding(pad))
            2 -> HistoryTab(s, vm, Modifier.padding(pad))
            else -> SettingsTab(s, vm, onPickCookie, onPickFolder, Modifier.padding(pad))
        }
    }
}

@Composable
private fun DownloadTab(s: UiState, vm: MainViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Paste one or many links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = s.urlText,
                        onValueChange = vm::setUrlText,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("Media URLs") },
                        placeholder = { Text("One URL per line, or Share → Click Downloader") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = vm::analyze,
                            enabled = s.urlText.isNotBlank() && !s.analyzing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (s.analyzing) "Analyzing…" else "Analyze")
                        }
                        Button(
                            onClick = vm::enqueueBatch,
                            enabled = s.urlText.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Queue")
                        }
                    }
                }
            }
        }

        s.media?.let { m ->
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(m.title, fontWeight = FontWeight.SemiBold)
                        if (m.uploader.isNotBlank()) Text(m.uploader, style = MaterialTheme.typography.bodyMedium)
                        if (m.durationSeconds > 0) Text("Duration: ${duration(m.durationSeconds)}")
                    }
                }
            }
        }

        s.error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }

        item {
            Text("Quality preset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DownloadPreset.entries.forEach { p ->
                    FilterChip(
                        selected = s.selectedPreset == p,
                        onClick = { vm.setPreset(p) },
                        label = { Text(p.label) }
                    )
                }
            }
        }

        item {
            AssistChip(
                onClick = { vm.setTab(3) },
                label = {
                    Text(
                        buildString {
                            append(if (s.settings.wifiOnly) "Wi‑Fi only • " else "")
                            append(if (s.settings.useAria2) "aria2 • " else "")
                            append(if (s.cookieLoaded) "Cookies loaded" else "No cookies")
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.Tune, null) }
            )
        }
    }
}

@Composable
private fun QueueTab(s: UiState, vm: MainViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Download Queue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = vm::clearFinishedQueue) { Text("Clear finished") }
            }
        }
        if (s.queue.isEmpty()) {
            item { EmptyCard("Queue is empty") }
        } else {
            items(s.queue, key = { it.id }) { job -> JobCard(job, vm) }
        }
    }
}

@Composable
private fun HistoryTab(s: UiState, vm: MainViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = vm::clearHistory) { Text("Clear") }
            }
        }
        if (s.history.isEmpty()) item { EmptyCard("No download history yet") }
        else items(s.history, key = { it.id }) { job -> JobCard(job, vm, history = true) }
    }
}

@Composable
private fun SettingsTab(
    s: UiState,
    vm: MainViewModel,
    onPickCookie: () -> Unit,
    onPickFolder: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SwitchRow("Wi‑Fi only", "Queue waits for an unmetered network.", s.settings.wifiOnly) {
                        vm.updateSettings { x -> x.copy(wifiOnly = it) }
                    }
                    SwitchRow("aria2 acceleration", "Parallel segments for supported downloads.", s.settings.useAria2) {
                        vm.updateSettings { x -> x.copy(useAria2 = it) }
                    }
                    SwitchRow("Embed thumbnail", null, s.settings.embedThumbnail) {
                        vm.updateSettings { x -> x.copy(embedThumbnail = it) }
                    }
                    SwitchRow("Embed metadata", null, s.settings.embedMetadata) {
                        vm.updateSettings { x -> x.copy(embedMetadata = it) }
                    }
                    SwitchRow("Subtitles", "Write and embed available subtitles.", s.settings.subtitles) {
                        vm.updateSettings { x -> x.copy(subtitles = it) }
                    }
                    SwitchRow("SponsorBlock", "Remove selected SponsorBlock segments where supported.", s.settings.sponsorBlock) {
                        vm.updateSettings { x -> x.copy(sponsorBlock = it) }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Authentication cookies", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (s.cookieLoaded) "Private cookie file loaded." else "No cookie file loaded.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPickCookie) { Text("Import cookies.txt") }
                        if (s.cookieLoaded) OutlinedButton(onClick = vm::clearCookies) { Text("Remove") }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Output folder", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (s.settings.outputTreeUri.isBlank())
                            "Default: Downloads/ClickDownloader"
                        else
                            "Folder access saved. SAF export is prepared for the next engine step.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = onPickFolder) { Text("Choose folder") }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Filename template", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = s.settings.filenameTemplate,
                        onValueChange = { v -> vm.updateSettings { it.copy(filenameTemplate = v) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Example: %(title)s [%(id)s].%(ext)s", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Text(
                "No DRM or paywall bypass. Only download media you are allowed to save.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun JobCard(job: DownloadJob, vm: MainViewModel, history: Boolean = false) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                job.title.ifBlank { job.url },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(job.state.name, style = MaterialTheme.typography.labelMedium)
                Text("${job.preset.label} • ${job.progress}%")
            }
            if (job.state == JobState.RUNNING) {
                LinearProgressIndicator(
                    progress = { job.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (job.error.isNotBlank()) {
                Text(job.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!history && (job.state == JobState.QUEUED || job.state == JobState.RUNNING)) {
                    OutlinedButton(onClick = { vm.cancel(job) }) { Text("Cancel") }
                }
                if (job.state == JobState.FAILED || job.state == JobState.CANCELLED || history) {
                    TextButton(onClick = { vm.retry(job) }) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun EmptyCard(text: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(text)
        }
    }
}

private fun duration(total: Long): String {
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
