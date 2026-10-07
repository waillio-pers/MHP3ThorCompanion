package com.waillio.mhp3rdcompanion

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/** Deterministic local MHP3 atlas mapping; no network or runtime tinting. */
data class WeaponIconRef(
    val sourcePath: String,
    val sourceSha256: String,
    val sourceRectPx: String,
    val gameColorVariant: String,
    val extractedSha256: String,
    val extractedDimensionsPx: String,
    @param:DrawableRes val resourceId: Int,
    val contentDescription: String
)

object WeaponIconRegistry {
    private const val GREAT_SWORD_RECT = "x=324,y=318,width=54,height=62"
    private const val LONG_SWORD_RECT = "x=388,y=318,width=54,height=62"
    private const val SWORD_AND_SHIELD_RECT = "x=452,y=318,width=54,height=62"
    private const val DUAL_BLADES_RECT = "x=516,y=318,width=54,height=62"
    private const val HAMMER_RECT = "x=580,y=318,width=54,height=62"
    private const val LANCE_RECT = "x=68,y=382,width=54,height=62"
    private const val HUNTING_HORN_RECT = "x=4,y=382,width=54,height=62"
    private const val GUNLANCE_RECT = "x=132,y=382,width=54,height=62"
    private const val SWITCH_AXE_RECT = "x=388,y=382,width=54,height=62"
    private const val BOW_RECT = "x=324,y=382,width=54,height=62"
    private const val LIGHT_BOWGUN_RECT = "x=196,y=382,width=54,height=62"
    private const val HEAVY_BOWGUN_RECT = "x=260,y=382,width=54,height=62"

    private val raritySourceFiles = listOf(
        "inv_white.png", "inv_purple.png", "inv_yellowpale.png", "inv_pink.png",
        "inv_green.png", "inv_blue.png", "inv_red.png"
    )
    private val raritySourceShas = listOf(
        "30DBD71C18CAEEC2626DB10AD3B87D49DEEC5482DAE9D10289FE56F6DDE36F03",
        "935EDAE71566A451AF47DBF395B7CC15AF9BF3E320FF427454A681F0940FE462",
        "D4FC43559DDC5A4B11AD9335A45E675534A847AC317C4A90E54C724505F161D8",
        "FFA50BA34DA4D064F75AA25E2EFC498D14DF0DC0234AD752726C2343677212C9",
        "C244563F91AB82C2FB5A56B2AA355B657DFEB18B408E45BD70CB02C9E4957FCC",
        "E442C73971A13D0876AC2F10B34F03CF50924E23A9D21DC0147C5F0899A2A512",
        "C7CB8306EA31602C5BB28808AB3104DB5C5D7E482688A2E2B4B171EA857C67F3"
    )
    private val rarityVariantNames = listOf("WHITE", "LIGHT_PURPLE", "PALE_YELLOW", "PINK", "GREEN", "BLUE", "RED")

    private fun variant(
        typeLabel: String,
        rarity: Int,
        sourceFile: String,
        sourceSha256: String,
        sourceRectPx: String,
        extractedSha256: String,
        gameColorVariant: String,
        resourceId: Int
    ) = WeaponIconRef(
        sourcePath = "Textures/ULJM05800/ui/icons/$sourceFile",
        sourceSha256 = sourceSha256,
        sourceRectPx = sourceRectPx,
        gameColorVariant = gameColorVariant,
        extractedSha256 = extractedSha256,
        extractedDimensionsPx = "54x62",
        resourceId = resourceId,
        contentDescription = "$typeLabel rarity $rarity icon"
    )

    private val greatSwordByRarity = mapOf(
        1 to variant("Great Sword", 1, "inv_white.png", "30DBD71C18CAEEC2626DB10AD3B87D49DEEC5482DAE9D10289FE56F6DDE36F03", GREAT_SWORD_RECT, "4729B8FA5093D3C394692BE82D095C40449A53635A4DE7CE2307B22276A016B4", "WHITE", R.drawable.weapon_great_sword_r1_icon),
        2 to variant("Great Sword", 2, "inv_purple.png", "935EDAE71566A451AF47DBF395B7CC15AF9BF3E320FF427454A681F0940FE462", GREAT_SWORD_RECT, "7671387C1B3802F23647F8C62368CFC914F59CB9D1F249D0ACC9ECB32E50E8FB", "PURPLE", R.drawable.weapon_great_sword_r2_icon),
        3 to variant("Great Sword", 3, "inv_yellowpale.png", "D4FC43559DDC5A4B11AD9335A45E675534A847AC317C4A90E54C724505F161D8", GREAT_SWORD_RECT, "980C015519588ACA427A761D547EF1D6C7B607FEA01969559700D85193BEEC4E", "YELLOW_PALE", R.drawable.weapon_great_sword_r3_icon),
        4 to variant("Great Sword", 4, "inv_pink.png", "FFA50BA34DA4D064F75AA25E2EFC498D14DF0DC0234AD752726C2343677212C9", GREAT_SWORD_RECT, "6769944557C6255FB115AC42D86C7E7E72B094B1148037C0F02810EC770A897D", "PINK", R.drawable.weapon_great_sword_r4_icon),
        5 to variant("Great Sword", 5, "inv_green.png", "C244563F91AB82C2FB5A56B2AA355B657DFEB18B408E45BD70CB02C9E4957FCC", GREAT_SWORD_RECT, "9CA2F45A03EFA96D9981EF94E488CA5D2CE40D6B91A124D5173D1F053C305B56", "GREEN", R.drawable.weapon_great_sword_r5_icon),
        6 to variant("Great Sword", 6, "inv_blue.png", "E442C73971A13D0876AC2F10B34F03CF50924E23A9D21DC0147C5F0899A2A512", GREAT_SWORD_RECT, "4E0B558191B4301C136CC62376AC6552FFC036FDE5F39BEDB303ABC250755C61", "BLUE", R.drawable.weapon_great_sword_r6_icon),
        7 to variant("Great Sword", 7, "inv_red.png", "C7CB8306EA31602C5BB28808AB3104DB5C5D7E482688A2E2B4B171EA857C67F3", GREAT_SWORD_RECT, "9D5B1F1D618F0B7B11FBFFD99EFD54AD6681EADDE8FD1EEF0F7AD19A64A76948", "RED", R.drawable.weapon_great_sword_r7_icon)
    )

    private val longSwordByRarity = mapOf(
        1 to variant("Long Sword", 1, "inv_white.png", "30DBD71C18CAEEC2626DB10AD3B87D49DEEC5482DAE9D10289FE56F6DDE36F03", LONG_SWORD_RECT, "EC0F78576789E4A69BC00430695C165F39138F1B22D6DB922B11542CE722EC4A", "WHITE", R.drawable.weapon_long_sword_r1_icon),
        2 to variant("Long Sword", 2, "inv_purple.png", "935EDAE71566A451AF47DBF395B7CC15AF9BF3E320FF427454A681F0940FE462", LONG_SWORD_RECT, "770BC6B76793364EEC959EE95B36427D262E505E7D278A4DF4A3393FFA1FAE7D", "LIGHT_PURPLE", R.drawable.weapon_long_sword_r2_icon),
        3 to variant("Long Sword", 3, "inv_yellowpale.png", "D4FC43559DDC5A4B11AD9335A45E675534A847AC317C4A90E54C724505F161D8", LONG_SWORD_RECT, "7A58E33D42C564558499CBF932FC6E8683E04A7147F82866AC5DD679B78662BC", "PALE_YELLOW", R.drawable.weapon_long_sword_r3_icon),
        4 to variant("Long Sword", 4, "inv_pink.png", "FFA50BA34DA4D064F75AA25E2EFC498D14DF0DC0234AD752726C2343677212C9", LONG_SWORD_RECT, "09B9FC589BE557AE912FC71265B33442ABCD41D98C7548C7F8CF42E70588745B", "PINK", R.drawable.weapon_long_sword_r4_icon),
        5 to variant("Long Sword", 5, "inv_green.png", "C244563F91AB82C2FB5A56B2AA355B657DFEB18B408E45BD70CB02C9E4957FCC", LONG_SWORD_RECT, "3FE9E671006A2618B734AA7576DC03ED230CFB11CEF81A61BC9330F5F15E42FE", "GREEN", R.drawable.weapon_long_sword_r5_icon),
        6 to variant("Long Sword", 6, "inv_blue.png", "E442C73971A13D0876AC2F10B34F03CF50924E23A9D21DC0147C5F0899A2A512", LONG_SWORD_RECT, "EA351393CE557037CBEB9315436853988A0285020848A63A6612EFBD9ECEBA7E", "BLUE", R.drawable.weapon_long_sword_r6_icon),
        7 to variant("Long Sword", 7, "inv_red.png", "C7CB8306EA31602C5BB28808AB3104DB5C5D7E482688A2E2B4B171EA857C67F3", LONG_SWORD_RECT, "C25A0B2DAF169F1628346BB353DFAF4F426826CBEBD9100B1FC501D0E618B980", "RED", R.drawable.weapon_long_sword_r7_icon)
    )

    private fun conventionalRarityMap(
        typeLabel: String,
        sourceRect: String,
        extractedShas: List<String>,
        resources: List<Int>
    ): Map<Int, WeaponIconRef> = (1..7).associateWith { rarity ->
        variant(
            typeLabel = typeLabel,
            rarity = rarity,
            sourceFile = raritySourceFiles[rarity - 1],
            sourceSha256 = raritySourceShas[rarity - 1],
            sourceRectPx = sourceRect,
            extractedSha256 = extractedShas[rarity - 1],
            gameColorVariant = rarityVariantNames[rarity - 1],
            resourceId = resources[rarity - 1]
        )
    }

    private val swordAndShieldByRarity = conventionalRarityMap(
        "Sword & Shield", SWORD_AND_SHIELD_RECT,
        listOf(
            "17B1692322BEFE1B825EB3016415F330203810A8FE17E88F233A65D484EBD8AA",
            "E95A57B7A382F99B591E2CD41E049CE2954664841B7844B6C2BBA9F965F63B61",
            "4208977810A20B79FF4780A7E28890E008D3F29DC68A986A6EF160E1B516131B",
            "542D931CD0E50BFCE53064479CD1B2B7C4854CF17BD9CFCF5AB81D51D0B5F73E",
            "3B0817A87970C367B672E2FA9F4F8F5D6AD500B7EA30B2A2CF6A04BAF275C161",
            "F326FB7E97B5E8C27F8774EABF0C42C7CA56F570C48455FF9B45E38B4D80C867",
            "A7E35922DA59EE80E029EC41664B4B4528B94FC185DEE89C09504DA25E3F0637"
        ),
        listOf(R.drawable.weapon_sword_and_shield_r1_icon, R.drawable.weapon_sword_and_shield_r2_icon, R.drawable.weapon_sword_and_shield_r3_icon, R.drawable.weapon_sword_and_shield_r4_icon, R.drawable.weapon_sword_and_shield_r5_icon, R.drawable.weapon_sword_and_shield_r6_icon, R.drawable.weapon_sword_and_shield_r7_icon)
    )
    private val dualBladesByRarity = conventionalRarityMap(
        "Dual Blades", DUAL_BLADES_RECT,
        listOf(
            "08C9E64C073B5C71E95ED5971D20CE924EDC8824ACD1BD59E8364BC7814B1F54",
            "0549FFE44981A6FF92F2A0FEFA78ECE6A76237352B43717B527C78CEC0E44369",
            "CE72E1B76CADE8588BA6C29702E80D896875B2BD2C4764F17B9CC6EB6F453F44",
            "9E0A7D57190A919AAA80BC33C81AF748B4AF6D138CC5C2F9805C47631C8C0D4B",
            "54448DC28312BAC707632FD623DF8E97DD0798DDA5F729A40973C780A9237436",
            "09C83BB41B6C31696F040E1494731A9921B14CE8ABEE7A4A7F0785B6CB397D23",
            "639456DF4E9671A8BCE95277C7F2ACA53295D7ABF06BA307363BC90EBE4D357C"
        ),
        listOf(R.drawable.weapon_dual_blades_r1_icon, R.drawable.weapon_dual_blades_r2_icon, R.drawable.weapon_dual_blades_r3_icon, R.drawable.weapon_dual_blades_r4_icon, R.drawable.weapon_dual_blades_r5_icon, R.drawable.weapon_dual_blades_r6_icon, R.drawable.weapon_dual_blades_r7_icon)
    )
    private val hammerByRarity = conventionalRarityMap(
        "Hammer", HAMMER_RECT,
        listOf(
            "86EB009EA748E0CAA5CA539E7C6B7D201D757722AF8E0EDB844416001D68AE47",
            "E9E086F17B6287582A10A3D0C12A39FF8D12AE822F063F7EA595B72EDAA884C3",
            "7507F024C1B019758E785F06DD6CAACB1E19BA0BB279125834D48D83C12DDBF5",
            "06987D81DA01202A37F1977CE1B4512C8DCF4EBE0F06292DACB176057D0D20C6",
            "8804D7B79A3AF4F8339198E6D84D1EFCBAA716A580146219753C7AD4C5C3D772",
            "3EC0C9979964134B69C80D580146A541686BA2115729012A1F5C8261AB9C65F8",
            "20C2D87735F4D432D800C3C7547488E703F9221CC5E152650CDD838758CD7BEE"
        ),
        listOf(R.drawable.weapon_hammer_r1_icon, R.drawable.weapon_hammer_r2_icon, R.drawable.weapon_hammer_r3_icon, R.drawable.weapon_hammer_r4_icon, R.drawable.weapon_hammer_r5_icon, R.drawable.weapon_hammer_r6_icon, R.drawable.weapon_hammer_r7_icon)
    )
    private val lanceByRarity = conventionalRarityMap(
        "Lance", LANCE_RECT,
        listOf(
            "F561A1DA2E890F6523DB333EB5097E1CE5FD3AA7F0175DA2827B745A706B0022",
            "8724F744E5D42DFBAA03EF02964262CBDE4FF8D226BF60EDF3801E503B3D5A50",
            "F987654E339C967E7973FCD86D059850A4F00B706BE3F36F4F5BF02C9D6C0711",
            "931572B086B74114D22D6F1C4B382F9E5F242D2E0A02BEC8D405E72AF4FB9F90",
            "DDDD0F6A39CBECF47E02BF1B4EFB26EE3A8D9CC7453D1B2CED42756D08FB3620",
            "F216A3BD02EB317F33C4C12E6678E0B9666EB925F93CF058CC3650A2DAE659E2",
            "EE65876E5C9928C4D05503FD2A7805F7500DC96B3C83DB611F1B78900241BA40"
        ),
        listOf(R.drawable.weapon_lance_r1_icon, R.drawable.weapon_lance_r2_icon, R.drawable.weapon_lance_r3_icon, R.drawable.weapon_lance_r4_icon, R.drawable.weapon_lance_r5_icon, R.drawable.weapon_lance_r6_icon, R.drawable.weapon_lance_r7_icon)
    )

    private val huntingHornByRarity = conventionalRarityMap(
        "Hunting Horn", HUNTING_HORN_RECT,
        listOf("C62F545EAF3DF7331C8F883E026C43C903142860E545C31897A88D247E5AEE7C", "FC489BC361478C59200097B50E268D20C8DE7FA64A4ADF8B8508B6FE1514F6D2", "14D5CE4E67447220D7C92D7C4C1A2DA1CDB5757873C0C0F742DB41200C81CE05", "ADEF6025EAD314FC13830D740E4411B7182E85F60DFC6A0D5188288D1E835D3A", "F97013A15D6FA46F63E58943BB3B9308B1442B9587B681CF030AE31FFDAD89E5", "980C832FF09EA24455CACD81CB10C57F8C494E4DD28F2245C87BC6252D5ECE8C", "07F77F3529D2E7E2363D2688BE668E0C4E20751B35B4C290284ECF0C61A85842"),
        listOf(R.drawable.weapon_hunting_horn_r1_icon, R.drawable.weapon_hunting_horn_r2_icon, R.drawable.weapon_hunting_horn_r3_icon, R.drawable.weapon_hunting_horn_r4_icon, R.drawable.weapon_hunting_horn_r5_icon, R.drawable.weapon_hunting_horn_r6_icon, R.drawable.weapon_hunting_horn_r7_icon)
    )
    private val gunlanceByRarity = conventionalRarityMap(
        "Gunlance", GUNLANCE_RECT,
        listOf("7BAF7EB516A894508FD2211ED005625B416C643512E9B620862F981197EF0D70", "B41ADB29C4977A81721E6D376097BD8F8A85E9FED78E397A53F731C15F424C10", "E379B648DCD66F31603A45DA88320F194C907C207863279ACB375D01FB33B13A", "16E79DFDA9E4DF2316F976C975F15B77E016CB9142F90E7BF8BB0A7223022C58", "2DFC089744449B29D150CDFD23AFD460CA1FF12E633B86E441B7D7CE72ECF965", "E92E3B68929EDDE1B742D27A2015E24C42D0B5E068352A60B280F51F57051DD4", "61EE043DC685A1D8332562861B10F34707F7ACD258762B4670A46B802CBAEE00"),
        listOf(R.drawable.weapon_gunlance_r1_icon, R.drawable.weapon_gunlance_r2_icon, R.drawable.weapon_gunlance_r3_icon, R.drawable.weapon_gunlance_r4_icon, R.drawable.weapon_gunlance_r5_icon, R.drawable.weapon_gunlance_r6_icon, R.drawable.weapon_gunlance_r7_icon)
    )
    private val switchAxeByRarity = conventionalRarityMap(
        "Switch Axe", SWITCH_AXE_RECT,
        listOf(
        "A608CA0B6C2FAE7D10CB9F6E24F3F3270F84BC208D18FA35625709069AABC661", "841E310F77294F27E37E695C66B32BF23F31594311EE6CE1F59E028566FABAC4", "6EB6C99F861ED952433F672F479BA242B65DD1E04DEAB3632EE0C30C61E2839F", "ECB9DAA4B66259AD6DFEC30CDCDB454B7762551FA495DA7436C887FE8F00BFA5", "D3E3B1A65D7E656A7640FC97544EBA73C727F146BFA73A356E561FCF881009D6", "F0FFBBB3D757B9CD56D97F781F7335C75D49B064E94BE86746CAD9E1C6BD4E36", "72C91440BB447317368983AAC6CC2ACDAE08CEDBDC7E24356B2B13B8F45F39B3"
        ),
        listOf(R.drawable.weapon_switch_axe_r1_icon, R.drawable.weapon_switch_axe_r2_icon, R.drawable.weapon_switch_axe_r3_icon, R.drawable.weapon_switch_axe_r4_icon, R.drawable.weapon_switch_axe_r5_icon, R.drawable.weapon_switch_axe_r6_icon, R.drawable.weapon_switch_axe_r7_icon)
    )
    private val bowByRarity = conventionalRarityMap(
        "Bow", BOW_RECT,
        listOf(
            "ECCBC7D619F033D60DD76F6B229BDE69ACF08E0FEABB4F6FC243126E04195B2D",
            "44056386F8A2764109C69A1579A47A0ACD0F860A990914E41DF60D6B6E595B0E",
            "2C6F292F7784A8147894F98BF27EF1DDC359BB44143EC361D52C8B7F9ACAD9F7",
            "59C763B1CE0B088BCDB94D99BA6B636398496251F2C9A3726369BF4E75FCBAE6",
            "30871F1327462B337B9742CD2500A6E87840B0E82C7370B986AE8644BFF42515",
            "E7432212BBD6613CF8819DB106B8DB31ED20EB51E121451AA089A4FAE123E831",
            "428D7D7AE988C6FBB2CDAB2B68D60B4C446479B4F9413181396CAC68122AD0A6"
        ),
        listOf(R.drawable.weapon_bow_r1_icon, R.drawable.weapon_bow_r2_icon, R.drawable.weapon_bow_r3_icon, R.drawable.weapon_bow_r4_icon, R.drawable.weapon_bow_r5_icon, R.drawable.weapon_bow_r6_icon, R.drawable.weapon_bow_r7_icon)
    )
    private val lightBowgunByRarity = conventionalRarityMap(
        "Light Bowgun", LIGHT_BOWGUN_RECT,
        listOf(
            "F00A6FE41F12DBFDEE81B711E6868E99167A0896E78EB9E18DAFF33B47749EC7",
            "A4FEF27A0D2257F441EC3B705542EE58B7B89692AD1DFC27149D71B39A3371D4",
            "5155A521657C8A69B8A67747ADFA6363A5A1DFE86358E3B21A98D41FA91A3529",
            "D7A274C53643F3DF95BDCC246571156ECDAF8F9465C0E0AAB1B9819D6B8D85C6",
            "2C560FCC4834AAE64FA1FF9446AD300AF33878726814C2104081364247E0199B",
            "219AF6D9DF86A5D7983006E4364A05C82FD681525F5B0D51C1B2C4205D47C95F",
            "EC2AF258ADA980BF7C578917D2722D94E4EBA0EA2A983618D65E29ACC634AA42"
        ),
        listOf(R.drawable.weapon_light_bowgun_r1_icon, R.drawable.weapon_light_bowgun_r2_icon, R.drawable.weapon_light_bowgun_r3_icon, R.drawable.weapon_light_bowgun_r4_icon, R.drawable.weapon_light_bowgun_r5_icon, R.drawable.weapon_light_bowgun_r6_icon, R.drawable.weapon_light_bowgun_r7_icon)
    )
    private val heavyBowgunByRarity = conventionalRarityMap(
        "Heavy Bowgun", HEAVY_BOWGUN_RECT,
        listOf(
            "A1D64BFCEEE995869588B7E5C539235D271882CBC79E154C15E86819DB72E02C",
            "AA51615D0CE1D395D0893EBED52FF7619E07B829FF30962CCE81D971A9EE5A07",
            "A7D868DE07AECF213C9912087B8D1AD2E7C1D7AB38424B942BCFD9337A86855E",
            "091AA4ED327A93D1307BF31B9C9D2013867B14C7F6310FF7052BACB85FC1D711",
            "7378E6604BAD8DDF31D721E57D20EF8CD9ED811425C9EC643DC1B4D0AD096EC3",
            "6372C4167EF5B288DAFEAFC5D6138A6826565692A03B5C66BDF5E678FBDF4D19",
            "0E36DD9A5219DA49D9FF4CE945ED5B533A23FA8BB2E2CEDD4C4988437356F20B"
        ),
        listOf(R.drawable.weapon_heavy_bowgun_r1_icon, R.drawable.weapon_heavy_bowgun_r2_icon, R.drawable.weapon_heavy_bowgun_r3_icon, R.drawable.weapon_heavy_bowgun_r4_icon, R.drawable.weapon_heavy_bowgun_r5_icon, R.drawable.weapon_heavy_bowgun_r6_icon, R.drawable.weapon_heavy_bowgun_r7_icon)
    )

    val greatSword: WeaponIconRef = greatSwordByRarity.getValue(1)

    fun resolve(weaponType: String, rarity: Int = 1): WeaponIconRef? = when (weaponType) {
        "GREAT_SWORD" -> greatSwordByRarity[rarity.coerceIn(1, 7)]
        "LONG_SWORD" -> longSwordByRarity[rarity.coerceIn(1, 7)]
        "SWORD_AND_SHIELD" -> swordAndShieldByRarity[rarity.coerceIn(1, 7)]
        "DUAL_BLADES" -> dualBladesByRarity[rarity.coerceIn(1, 7)]
        "HAMMER" -> hammerByRarity[rarity.coerceIn(1, 7)]
        "LANCE" -> lanceByRarity[rarity.coerceIn(1, 7)]
        "HUNTING_HORN" -> huntingHornByRarity[rarity.coerceIn(1, 7)]
        "GUNLANCE" -> gunlanceByRarity[rarity.coerceIn(1, 7)]
        "SWITCH_AXE" -> switchAxeByRarity[rarity.coerceIn(1, 7)]
        "BOW" -> bowByRarity[rarity.coerceIn(1, 7)]
        "LIGHT_BOWGUN" -> lightBowgunByRarity[rarity.coerceIn(1, 7)]
        "HEAVY_BOWGUN" -> heavyBowgunByRarity[rarity.coerceIn(1, 7)]
        else -> null
    }
}

data class WeaponRarityVisual(val rarity: Int, val icon: WeaponIconRef, val nameColor: Color)

object WeaponRarityVisuals {
    private val nameColors = mapOf(
        1 to Color(0xFF2B2118), 2 to Color(0xFF6F5D77), 3 to Color(0xFF7E7937),
        4 to Color(0xFF8D4D59), 5 to Color(0xFF407142), 6 to Color(0xFF41507A),
        7 to Color(0xFF802D34)
    )

    fun resolve(rarity: Int, weaponType: String = "GREAT_SWORD"): WeaponRarityVisual {
        val safe = rarity.coerceIn(1, 7)
        return WeaponRarityVisual(safe, requireNotNull(WeaponIconRegistry.resolve(weaponType, safe)), nameColors.getValue(safe))
    }
}

internal fun rarityVisual(rarity: Int, weaponType: String = "GREAT_SWORD"): WeaponRarityVisual = WeaponRarityVisuals.resolve(rarity, weaponType)
