package dev.peekakoog.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** Where Copy buttons put their text: the system clipboard (tests swap it for a fake). */
internal var copyToClipboard: (String) -> Unit = { text ->
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}

/** A panel's title with a Copy button that copies [text]: everything in the panel, not just what is on screen. */
@Composable
internal fun PanelTitle(title: String, text: () -> String) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = {
                copyToClipboard(text())
                copied = true
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text(if (copied) "✓ Copied" else "📋 Copy", fontSize = 12.sp)
        }
    }
}
