package com.freellm.superpro.ui.chat.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freellm.superpro.domain.model.SearchSource
import com.freellm.superpro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitationBottomSheet(
    source: SearchSource?,
    onDismiss: () -> Unit
) {
    if (source == null) return
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DividerDark) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "[${source.index}]",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNeonCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = source.domain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = source.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark)
                    .padding(14.dp)
            ) {
                Text(
                    text = source.snippet,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = "Open Link",
                    tint = BackgroundDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Visit Web Source",
                    color = BackgroundDark,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
