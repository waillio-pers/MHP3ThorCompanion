package com.waillio.mhp3rdcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waillio.mhp3rdcompanion.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal data class AppState(
    val query: String = "",
    val filter: EntityType? = null,
    val favorites: List<FavoriteEntry> = emptyList(),
    val recent: List<RecentEntry> = emptyList(),
    val currentCharmTable: Int? = null,
    val charmIdentifyWorkingState: CharmIdentifyWorkingState = CharmIdentifyWorkingState(),
    val loaded: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CompanionRepository(application)
    val fixture = repository.data
    internal val questProgression = repository.questProgression
    private val charmRepository = CharmTableRepository(application, fixture.skillTrees)
    private val mutableCharmIndex = MutableStateFlow<CharmTableIndex?>(null)
    internal val charmIndex = mutableCharmIndex.asStateFlow()
    private val mutableCharmLoadFailed = MutableStateFlow(false)
    internal val charmLoadFailed = mutableCharmLoadFailed.asStateFlow()
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
    internal val state = mutableState.asStateFlow()
    val results: StateFlow<List<SearchResult>> = state.map { CompanionLogic.search(fixture, it.query, it.filter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(repository.favorites, repository.recent, repository.currentCharmTable) { favorites, recent, table ->
                Triple(favorites, recent, table)
            }.collect { (favorites, recent, table) ->
                    mutableState.update { it.copy(favorites = favorites, recent = recent, currentCharmTable = table, loaded = true) }
                }
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { charmRepository.index }
                .onSuccess { mutableCharmIndex.value = it }
                .onFailure { mutableCharmLoadFailed.value = true }
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

    fun setCurrentCharmTable(table: Int?) {
        require(table == null || table in 1..12)
        mutableState.update { it.copy(currentCharmTable = table) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveCurrentCharmTable(table) }
    }

    /** Keeps incomplete Identify work across Activity configuration changes only. */
    internal fun updateCharmIdentifyWorkingState(transform: (CharmIdentifyWorkingState) -> CharmIdentifyWorkingState) {
        mutableState.update { state -> state.copy(charmIdentifyWorkingState = transform(state.charmIdentifyWorkingState)) }
    }
}
