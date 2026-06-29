package com.tuckercr.catsdogs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.model.GeoLocationViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsRoute(
    locations: List<SavedLocation>,
    activeIndex: Int,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onSetActive: (index: Int) -> Unit,
    onNavigateBack: () -> Unit,
    geoViewModel: GeoLocationViewModel,
    cityListViewModel: com.tuckercr.catsdogs.model.CityListViewModel,
) {
    var showAddSheet by remember { mutableStateOf(false) }

    val cityInput by geoViewModel.cityInput.collectAsStateWithLifecycle()
    val suggestions by geoViewModel.citySuggestions.collectAsStateWithLifecycle()
    val suggestLoading by geoViewModel.citySuggestLoading.collectAsStateWithLifecycle()
    val selectedSuggestion by geoViewModel.selectedSuggestion.collectAsStateWithLifecycle()

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = {
                geoViewModel.reset()
                showAddSheet = false
            },
            sheetState = sheetState,
        ) {
            AddCitySheetContent(
                savedLocations = locations,
                cityInput = cityInput,
                suggestions = suggestions,
                suggestLoading = suggestLoading,
                selectedSuggestion = selectedSuggestion,
                onCityInputChange = geoViewModel::onCityInputChange,
                onSuggestionChosen = geoViewModel::onCitySuggestionChosen,
                onAddCity = {
                    val suggestion = selectedSuggestion
                    if (suggestion != null) {
                        cityListViewModel.addLocation(
                            SavedLocation(
                                label = suggestion.label,
                                latitude = suggestion.weatherLat,
                                longitude = suggestion.weatherLon,
                            ),
                        )
                    } else if (cityInput.isNotBlank()) {
                        cityListViewModel.addLocation(
                            SavedLocation(
                                label = cityInput.trim(),
                                latitude = null,
                                longitude = null,
                            ),
                        )
                    }
                    geoViewModel.reset()
                    showAddSheet = false
                },
                onRemoveSaved = cityListViewModel::removeLocation,
                onDismiss = {
                    geoViewModel.reset()
                    showAddSheet = false
                },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Locations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add location",
                        )
                    }
                },
            )
        },
    ) { padding ->
        DraggableLocationList(
            locations = locations,
            activeIndex = activeIndex,
            onReorder = onReorder,
            onRemove = onRemove,
            onSetActive = onSetActive,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
private fun DraggableLocationList(
    locations: List<SavedLocation>,
    activeIndex: Int,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onSetActive: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableIntStateOf(1) }

    val targetIndex: Int? = draggingIndex?.let { from ->
        val steps = (dragOffsetY / itemHeightPx).roundToInt()
        (from + steps).coerceIn(0, locations.size - 1)
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        locations.forEachIndexed { index, location ->
            val isDragging = draggingIndex == index

            val visualOffsetY = when {
                isDragging -> dragOffsetY
                draggingIndex != null && targetIndex != null -> {
                    val from = draggingIndex!!
                    val to = targetIndex
                    when {
                        from < to && index in (from + 1)..to -> -itemHeightPx.toFloat()
                        from > to && index in to until from -> itemHeightPx.toFloat()
                        else -> 0f
                    }
                }
                else -> 0f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        if (itemHeightPx == 1 || coords.size.height > 0) {
                            itemHeightPx = coords.size.height
                        }
                    }.zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = visualOffsetY }
                    .background(
                        when {
                            isDragging -> MaterialTheme.colorScheme.primaryContainer
                            index == activeIndex -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        },
                    ),
            ) {
                ListItem(
                    headlineContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (index == activeIndex) {
                                    "${location.label} (Active)"
                                } else {
                                    location.label
                                },
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { onSetActive(index) },
                                enabled = index != activeIndex,
                            ) {
                                // Invisible but tappable area — tapping row body also works
                            }
                            IconButton(
                                onClick = { onRemove(index) },
                                enabled = locations.size > 1,
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                )
                            }
                        }
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            modifier = Modifier.pointerInput(index) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        val from = draggingIndex
                                        if (from != null) {
                                            val steps = (dragOffsetY / itemHeightPx).roundToInt()
                                            val to = (from + steps).coerceIn(0, locations.size - 1)
                                            if (from != to) onReorder(from, to)
                                        }
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    },
                                )
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    colors = androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                )
            }
        }
    }
}
