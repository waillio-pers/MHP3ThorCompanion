package com.waillio.mhp3rdcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waillio.mhp3rdcompanion.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppState(
    val query: String = "",
    val filter: EntityType? = null,
    val favorites: List<FavoriteEntry> = emptyList(),
    val recent: List<RecentEntry> = emptyList(),
    val loaded: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CompanionRepository(application)
    val fixture = repository.data
    /** Schema-11 production weapon repository, indexed once outside Compose recomposition. */
    val weaponRepository: WeaponRepository by lazy {
        WeaponRepository.fromProduction(fixture.weapons, fixture.materials, fixture.huntingHornSongCatalog)
    }
    val weaponUsageIndex: WeaponRecipeUsageIndex by lazy { weaponRepository.recipeUsageIndex }
    /** Source-backed inverse Item Usage projection, indexed once outside Compose. */
    val itemUsageIndex: ItemUsageIndex by lazy { ItemUsageIndex.build(fixture) }
    /** Retains per-type weapon forest/detail state across Item Detail routes. */
    internal val weaponScreenState = WeaponScreenState()
    val fieldIndex = repository.fieldIndex
    private val mutableState = MutableStateFlow(AppState())
    val state = mutableState.asStateFlow()
    val results: StateFlow<List<SearchResult>> = state.map { CompanionLogic.search(fixture, it.query, it.filter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(repository.favorites, repository.recent) { favorites, recent -> favorites to recent }
                .collect { (favorites, recent) ->
                    mutableState.update { it.copy(favorites = favorites, recent = recent, loaded = true) }
                }
        }
    }

    fun setQuery(value: String) = mutableState.update { it.copy(query = value) }
    fun setFilter(value: EntityType?) = mutableState.update { it.copy(filter = value) }
    fun clearSearchContext() = mutableState.update { it.copy(query = "", filter = null) }
    fun toggleFavorite(type: ReferenceEntityType, id: String) {
        val updated = ReferenceHistoryLogic.toggleFavorite(state.value.favorites, type, id, System.currentTimeMillis())
        mutableState.update { it.copy(favorites = updated) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveFavorites(updated) }
    }

    fun recordOpened(type: ReferenceEntityType, id: String) {
        val updated = ReferenceHistoryLogic.recordRecent(state.value.recent, type, id, System.currentTimeMillis())
        mutableState.update { it.copy(recent = updated) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveRecent(updated) }
    }
}
