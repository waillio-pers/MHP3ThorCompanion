package com.waillio.mhp3rdcompanion.data

internal enum class SkillCategory(val label: String) {
    BLADEMASTER("Blademaster"),
    GUNNER("Gunner"),
    DEFENSE("Defense"),
    UTILITY("Utility")
}

internal val AllSkillCategoryIds: Set<String> = SkillCategory.values().mapTo(linkedSetOf()) { it.name }

internal fun filterSkillTreesByCategories(
    skills: List<SkillTree>,
    selectedCategories: Set<String>
): List<SkillTree> {
    if (selectedCategories.containsAll(AllSkillCategoryIds)) return skills
    return skills.filter { skill -> skill.categories.any(selectedCategories::contains) }
}
