package com.artsistem.assistme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ITEM_HEIGHT = 44.dp
private const val VISIBLE_ITEMS = 5

/**
 * Yukarı-aşağı kaydırılan, ortadaki değere oturan çark seçici.
 *
 * Değer dışarıdan değişirse (ör. kayıt DB'den geç yüklendiğinde) çark o
 * değere kendini hizalar; kullanıcı kaydırıp bırakınca [onValueChange] çağrılır.
 */
@Composable
fun WheelPicker(
    values: List<Int>,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    label: (Int) -> String = { it.toString().padStart(2, '0') }
) {
    fun indexOf(v: Int) = values.indexOf(v).coerceAtLeast(0)

    val state = rememberLazyListState(initialFirstVisibleItemIndex = indexOf(value))
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val scope = rememberCoroutineScope()
    val currentValue by rememberUpdatedState(value)
    val currentValues by rememberUpdatedState(values)
    val currentOnChange by rememberUpdatedState(onValueChange)

    // Görünür alanın ortasına en yakın öğe = seçili öğe.
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index
                ?: state.firstVisibleItemIndex
        }
    }

    // Kaydırma bitince seçimi bildir.
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val v = currentValues.getOrNull(centered) ?: return@collect
                if (v != currentValue) currentOnChange(v)
            }
    }

    // Dışarıdan gelen değere hizalan (kullanıcı kaydırmıyorken).
    LaunchedEffect(value, values) {
        if (!state.isScrollInProgress && values.getOrNull(centered) != value) {
            state.scrollToItem(indexOf(value))
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = ITEM_HEIGHT * (VISIBLE_ITEMS / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(width).height(ITEM_HEIGHT * VISIBLE_ITEMS)
    ) {
        items(values.size) { i ->
            val distance = abs(i - centered)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(ITEM_HEIGHT)
                    .fillMaxWidth()
                    .clickable { scope.launch { state.animateScrollToItem(i) } }
                    .graphicsLayer { alpha = when (distance) { 0 -> 1f; 1 -> 0.55f; else -> 0.25f } }
            ) {
                Text(
                    label(values[i]),
                    fontSize = if (distance == 0) 28.sp else 22.sp,
                    fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Saat + dakika çarkı; ortadaki seçim bandıyla birlikte. Dakikalar 5'er adım. */
@Composable
fun TimeWheel(
    hour: Int,
    minute: Int,
    onChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    /** 5'in katı olmayan mevcut dakika (ör. eski kayıt) listede kalsın diye. */
    extraMinute: Int? = null
) {
    val hours = remember { (0..23).toList() }
    val minutes = remember(extraMinute) {
        ((0..55 step 5).toList() + listOfNotNull(extraMinute)).distinct().sorted()
    }
    val currentHour by rememberUpdatedState(hour)
    val currentMinute by rememberUpdatedState(minute)

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Seçim bandı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(ITEM_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            WheelPicker(values = hours, value = hour, onValueChange = { onChange(it, currentMinute) })
            Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
            WheelPicker(values = minutes, value = minute, onValueChange = { onChange(currentHour, it) })
        }
    }
}
