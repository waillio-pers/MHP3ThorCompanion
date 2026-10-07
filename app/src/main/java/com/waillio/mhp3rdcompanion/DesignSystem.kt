package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object AppColors {
    val Night = Color(0xFF15120E)
    val NightRaised = Color(0xFF211B15)
    val Parchment = Color(0xFFF0DEB3)
    val ParchmentDeep = Color(0xFFD7BD86)
    val Ink = Color(0xFF2D2115)
    val Gold = Color(0xFFD6B15F)
    val Crimson = Color(0xFF7E2118)
    val Moss = Color(0xFF33461F)
    val Steel = Color(0xFF19394A)
    val Plum = Color(0xFF40283E)
}

internal object AppType {
    private val base = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineBreak = LineBreak.Simple,
        hyphens = Hyphens.None
    )
    val AppTitle = base.copy(fontSize = 20.sp, lineHeight = 21.sp, fontWeight = FontWeight.Black)
    val ScreenTitle = base.copy(fontSize = 26.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
    val SectionTitle = base.copy(fontSize = 18.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
    val CardTitle = base.copy(fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
    val Body = base.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val Metadata = base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    val ButtonLabel = base.copy(fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
}

internal object AppDimens {
    val ScreenPadding = 14.dp
    val CardGap = 12.dp
    val CardShape = CutCornerShape(topEnd = 14.dp, bottomStart = 10.dp)
    val ButtonShape = CutCornerShape(topEnd = 10.dp, bottomStart = 8.dp)
    val CardBorder = 1.dp
    val SectionHeaderHeight = 36.dp
    val EntitySize = 52.dp
}

@Composable
internal fun CompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppColors.Gold,
            onPrimary = AppColors.Ink,
            secondary = Color(0xFFB85B39),
            background = AppColors.Night,
            surface = AppColors.NightRaised,
            onSurface = AppColors.Parchment
        ),
        typography = Typography(
            displaySmall = AppType.ScreenTitle,
            headlineSmall = AppType.ScreenTitle,
            titleLarge = AppType.SectionTitle,
            titleMedium = AppType.CardTitle,
            bodyLarge = AppType.Body,
            bodyMedium = AppType.Body,
            bodySmall = AppType.Metadata,
            labelLarge = AppType.ButtonLabel
        ),
        content = content
    )
}

@Composable
internal fun EntityEmblem(label: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(AppDimens.EntitySize)
            .clip(AppDimens.ButtonShape)
            .background(AppColors.NightRaised)
            .border(AppDimens.CardBorder, accent, AppDimens.ButtonShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.trim().take(1).uppercase(),
            color = AppColors.Gold,
            style = AppType.CardTitle.copy(fontSize = 21.sp)
        )
    }
}

@Composable
internal fun ItemIcon(
    gameItemId: Int?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val icon = gameItemId?.let(ItemIconRegistry::resolve)
    Box(
        modifier.testTag(if (icon == null) "item-icon-fallback" else "item-icon-$gameItemId"),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = ImageBitmap.imageResource(icon.resourceId),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = AppColors.Crimson,
    headerHeight: androidx.compose.ui.unit.Dp = AppDimens.SectionHeaderHeight,
    contentPadding: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(AppDimens.CardShape)
            .background(AppColors.Parchment)
            .border(AppDimens.CardBorder, AppColors.ParchmentDeep, AppDimens.CardShape)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(accent)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(title, color = Color(0xFFFFE8B0), style = AppType.SectionTitle, maxLines = 1)
        }
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Shared native text-editing state for every app search field.
 * Keeping TextFieldValue (rather than only its String) preserves caret and
 * selection semantics while the surrounding search results recompose. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    tag: String,
    textStyle: TextStyle = AppType.Body,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = AppDimens.ButtonShape,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    var fieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(query))
    }
    LaunchedEffect(query) {
        if (query != fieldValue.text) {
            fieldValue = TextFieldValue(query, selection = TextRange(query.length))
        }
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = { value ->
            fieldValue = value
            onQueryChange(value.text)
        },
        modifier = modifier.testTag(tag),
        singleLine = true,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = colors
    )
}

/** Consistent hierarchy for short previews that lead to a complete list. */
@Composable
internal fun PreviewFooter(
    moreLabel: String,
    actionLabel: String,
    tag: String,
    onAction: () -> Unit,
    moreTag: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(top = 9.dp, bottom = 2.dp)) {
        Text(
            moreLabel,
            color = AppColors.Ink.copy(alpha = .9f),
            style = AppType.Metadata.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                .testTag(moreTag ?: "$tag-more")
        )
        Button(
            onClick = onAction,
            modifier = Modifier.align(Alignment.End).heightIn(min = 36.dp).testTag(tag),
            shape = AppDimens.ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Crimson,
                contentColor = Color(0xFFFFE8B0)
            ),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(actionLabel, style = AppType.ButtonLabel)
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
internal fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(46.dp),
        shape = AppDimens.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Crimson,
            contentColor = Color(0xFFFFE8B0),
            disabledContainerColor = AppColors.Moss,
            disabledContentColor = Color(0xFFFFE8B0)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(7.dp))
        }
        Text(text, style = AppType.ButtonLabel, maxLines = 1)
    }
}

@Composable
internal fun ParchmentSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = AppColors.Parchment,
        shape = AppDimens.CardShape,
        border = BorderStroke(AppDimens.CardBorder, AppColors.ParchmentDeep),
        content = content
    )
}
