package com.freellm.superpro.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freellm.superpro.domain.model.SearchSource
import com.freellm.superpro.ui.theme.*

@Composable
fun WebSourcesCarousel(
    sources: List<SearchSource>,
    onSourceClicked: (SearchSource) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sources.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "Sources",
                tint = PrimaryNeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sources (${sources.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryNeonCyan
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(sources) { source ->
                SourceChip(source = source, onClick = { onSourceClicked(source) })
            }
        }
    }
}

@Composable
private fun SourceChip(
    source: SearchSource,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark)
            .border(0.5.dp, DividerDark, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "[${source.index}]",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNeonCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = source.domain,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = source.title,
                fontSize = 11.sp,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
