package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
class ItemShopUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun initialStoresAggregateOnlyEquivalentUnderlyingRelations() {
        val material = CompanionRepository(ApplicationProvider.getApplicationContext()).data.materials.single { it.gameItemId == 151 }
        rule.setContent { CompanionTheme { MaterialScreen(material, emptyList(), false, {}, {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("item-shop-source-item_shop_purchase_001-item_shop_purchase_087").fetchSemanticsNode()
        rule.onNodeWithTag("item-shop-source-item_shop_purchase_173").fetchSemanticsNode()
    }
}
