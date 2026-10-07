package com.waillio.mhp3rdcompanion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal const val ABOUT_REPOSITORY_URL = "https://github.com/waillio-pers/MHP3ThorCompanion"

internal data class AboutSourceCredit(
    val name: String,
    val description: String,
    val url: String,
    val linkLabel: String
)

/** Public-facing references and visual credits for players. */
internal val ABOUT_SOURCE_CREDITS = listOf(
    AboutSourceCredit(
        "Team Maverick ONE · TMO PSP v6.1 (r754)",
        "English names and translation references used throughout the app.",
        "https://teammaverickone.blogspot.com/",
        "Visit website"
    ),
    AboutSourceCredit(
        "MHP3@Wiki · AtWiki",
        "Monster data, maps, gathering spots, farm data, quests, items, and other game information.",
        "https://w.atwiki.jp/mhp3/",
        "Visit website"
    ),
    AboutSourceCredit(
        "Kouryakutsushin · MHP3 Wiki",
        "Detailed Yukumo Farm data, quest supply tables, and other Japanese game references.",
        "https://kouryakutsushin.com/mhp3rdwiki/%E3%83%A6%E3%82%AF%E3%83%A2%E8%BE%B2%E5%A0%B4.html",
        "Open source"
    ),
    AboutSourceCredit(
        "Game-Cap · MHP3",
        "Invasion Reward tables and additional monster and item data.",
        "https://game-cap.com/mhp3rd/data/monstlist.html",
        "Visit website"
    ),
    AboutSourceCredit(
        "VioletKIRA · Monster Data Guide",
        "Monster stats, status data, hitzones, and reward tables.",
        "https://gamefaqs.gamespot.com/psp/991479-monster-hunter-portable-3rd/faqs/61490",
        "Open source"
    ),
    AboutSourceCredit(
        "MHP3rd攻略広場 · mhp3rd.net",
        "Monster behavior, item effects, and other monster information.",
        "https://www.mhp3rd.net/book/index.html",
        "Visit website"
    ),
    AboutSourceCredit(
        "Monhammer",
        "Supply Box data for some Guild ★7 and Event quests.",
        "https://monhammer.com/",
        "Visit website"
    ),
    AboutSourceCredit(
        "gaugustini · Monster Hunter Armor Data",
        "Used to match armor data between Japanese and English names.",
        "https://github.com/gaugustini/monster-hunter-armor-data",
        "Open on GitHub"
    ),
    AboutSourceCredit(
        "MHP3rd Database · mhp3db.github.io",
        "Source for item icon pixel art used in the app.",
        "https://github.com/mhp3db/mhp3db.github.io/tree/7ad3cf1c5ba07ce2e63afdaa1fa37c4671edc3bf",
        "Open on GitHub"
    ),
    AboutSourceCredit(
        "MHP3DB · mikejsavage",
        "Additional reference data used to match item identities.",
        "https://github.com/mikejsavage/MHP3DB/tree/bfbab18460d95d921d4a3492644b46bfc5ca1f3c",
        "Open on GitHub"
    ),
    AboutSourceCredit(
        "Monster Hunter Wiki · MHP3 Monsters",
        "Used to check the MHP3 monster roster, English names, and monster classes.",
        "https://monsterhunter.fandom.com/wiki/MHP3:_Monsters",
        "Open source"
    ),
    AboutSourceCredit(
        "Monster Hunter Wiki · MHP3 Item List",
        "Used to check the MHP3 item list and item names.",
        "https://monsterhunter.fandom.com/wiki/MHP3:_Item_List",
        "Open source"
    ),
    AboutSourceCredit(
        "MHAG · mhag-info",
        "Used to check decoration and jewel data.",
        "https://github.com/bronwen-cassidy/mhag-info/tree/556e16b4fc2a5f4bf29f65641da95ac7e1d30bb2",
        "Open on GitHub"
    ),
    AboutSourceCredit(
        "Nenaiko · Monster Hunter item references",
        "Reference for item icon types and colors.",
        "https://wikiwiki.jp/nenaiko/%E3%82%B2%E3%83%BC%E3%83%A0%E7%94%A8%E8%AA%9E/%E3%82%A2%E3%82%A4%E3%82%B3%E3%83%B3/%E3%82%A2%E3%82%A4%E3%83%86%E3%83%A0%E3%82%A2%E3%82%A4%E3%82%B3%E3%83%B3%E3%81%AE%E4%B8%80%E8%A6%A7",
        "Open source"
    ),
    AboutSourceCredit(
        "RavenRepublic community reference",
        "Additional reference for identifying item icon shapes and colors.",
        "https://ravenrepublic.net/forums/showthread.php?pid=7854495",
        "Open source"
    ),
    AboutSourceCredit(
        "MHP3 GorillaWiki",
        "Used to check Training quests and related data.",
        "https://mhp3rd.gorillawiki.jp/entry/1121",
        "Open source"
    ),
    AboutSourceCredit(
        "Gron MHP3 reference",
        "Another reference used to verify Training data.",
        "https://www.gron.jp/mhp3/",
        "Visit website"
    ),
    AboutSourceCredit(
        "CAPCOM · Monster Hunter Portable 3rd",
        "Creator and rights holder of Monster Hunter Portable 3rd and its original game assets.",
        "https://www.capcom.com/",
        "Visit website"
    )
)

internal fun aboutViewIntent(url: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

internal fun openAboutExternalLink(context: Context, url: String) {
    try {
        context.startActivity(aboutViewIntent(url))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No browser is available to open this link.", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "This link could not be opened.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val compactPortrait = LocalAppWindowLayout.current.profile == AppLayoutProfile.COMPACT_PORTRAIT
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (compactPortrait) .99f else .94f)
                .fillMaxHeight(if (compactPortrait) .97f else .9f)
                .widthIn(max = if (compactPortrait) 780.dp else 680.dp)
                .testTag("about-dialog"),
            shape = CutCornerShape(topEnd = 20.dp),
            color = AppColors.Parchment,
            contentColor = AppColors.Ink,
            border = BorderStroke(2.dp, AppColors.Gold.copy(alpha = .8f)),
            shadowElevation = 12.dp
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 5.dp, top = 8.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("About", modifier = Modifier.weight(1f), style = AppType.SectionTitle)
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("about-close")) {
                        Icon(Icons.Default.Close, "Close About", tint = AppColors.Ink)
                    }
                }
                HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .7f))
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp).testTag("about-scroll-content"),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("MHP3rd Companion", style = AppType.ScreenTitle, color = AppColors.Ink)
                    Text("Version ${BuildConfig.VERSION_NAME}", style = AppType.Body, color = AppColors.Ink,
                        modifier = Modifier.testTag("about-version"))
                    Text("Made by Waillio with help from OpenAI Codex", style = AppType.Body, color = AppColors.Ink)
                    AboutLink(context, "Project repository", ABOUT_REPOSITORY_URL,
                        "Project repository", "about-repository-link")

                    Spacer(Modifier.height(7.dp))
                    Text("Sources & big thanks", style = AppType.SectionTitle, color = AppColors.Crimson,
                        modifier = Modifier.testTag("about-sources-heading"))
                    ABOUT_SOURCE_CREDITS.forEachIndexed { index, source ->
                        Column(
                            Modifier.fillMaxWidth().padding(top = 3.dp)
                                .testTag("about-source-$index"),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(source.name, style = AppType.CardTitle, color = AppColors.Ink)
                            Text(source.description, style = AppType.Metadata, color = AppColors.Ink.copy(alpha = .78f))
                            AboutLink(context, source.linkLabel, source.url, source.url, "about-source-link-$index")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This is an unofficial fan-made companion and is not affiliated with or endorsed by CAPCOM. " +
                            "Monster Hunter and related game artwork, characters, trademarks and assets are property of their respective owners.",
                        style = AppType.Metadata,
                        color = AppColors.Ink.copy(alpha = .78f),
                        modifier = Modifier.testTag("about-fan-disclaimer")
                    )
                }
                HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .7f))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp, vertical = 9.dp)
                        .testTag("about-done"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Crimson,
                        contentColor = AppColors.Parchment
                    ),
                    shape = AppDimens.ButtonShape
                ) {
                    Text("Done", style = AppType.ButtonLabel)
                }
            }
        }
    }
}

@Composable
private fun AboutLink(
    context: Context,
    label: String,
    url: String,
    contentDescription: String,
    tag: String
) {
    Text(
        label,
        color = AppColors.Crimson,
        style = AppType.Metadata.copy(textDecoration = TextDecoration.Underline),
        modifier = Modifier.clickable { openAboutExternalLink(context, url) }
            .semantics { this.contentDescription = contentDescription }
            .padding(vertical = 2.dp).testTag(tag),
        maxLines = 2
    )
}
