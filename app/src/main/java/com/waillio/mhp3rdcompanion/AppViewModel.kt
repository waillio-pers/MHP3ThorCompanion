package com.waillio.mhp3rdcompanion

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waillio.mhp3rdcompanion.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

internal data class AppState(
    val query: String = "",
    val queryResetGeneration: Int = 0,
    val fieldGuideRootPage: FieldGuideRootPage = FieldGuideRootPage.ALL,
    val favorites: List<FavoriteEntry> = emptyList(),
    val recent: List<RecentEntry> = emptyList(),
    val currentCharmTable: Int? = null,
    val charmIdentifyWorkingState: CharmIdentifyWorkingState = CharmIdentifyWorkingState()
) {
    val filter: EntityType? get() = fieldGuideRootPage.entityType
}

private sealed interface PersistedUserMutation {
    data class ToggleFavorite(val type: ReferenceEntityType, val id: String, val atMillis: Long) : PersistedUserMutation
    data class RecordOpened(val type: ReferenceEntityType, val id: String, val atMillis: Long) : PersistedUserMutation
    data class SetCurrentCharmTable(val table: Int?) : PersistedUserMutation
    data class SaveCharmSearch(val name: String, val criteria: CharmSearchCriteria) : PersistedUserMutation
    data class DeleteCharmSearch(val id: String) : PersistedUserMutation
}

private data class PersistedUserSnapshot(
    val favorites: PreferenceDecodeResult<List<FavoriteEntry>>,
    val recent: PreferenceDecodeResult<List<RecentEntry>>,
    val currentCharmTable: PreferenceDecodeResult<Int>,
    val charmSavedSearches: PreferenceDecodeResult<List<CharmSavedSearch>>
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CompanionRepository(application)
    private val mutableFixture = MutableStateFlow<FixtureData?>(null)
    internal val fixtureState = mutableFixture.asStateFlow()
    internal val fixture: FixtureData get() = checkNotNull(mutableFixture.value) { "Production data is not ready yet" }
    private val mutableQuestProgression = MutableStateFlow<QuestProgressionData?>(null)
    internal val questProgression: QuestProgressionData get() = checkNotNull(mutableQuestProgression.value) { "Quest progression is not ready yet" }
    private val mutableInitialLoadFailed = MutableStateFlow(false)
    internal val initialLoadFailed = mutableInitialLoadFailed.asStateFlow()
    private val mutablePreferencesLoaded = MutableStateFlow(false)
    internal val preferencesLoaded = mutablePreferencesLoaded.asStateFlow()
    private val mutableSearchCorpus = MutableStateFlow<SearchCorpus?>(null)
    private val mutableCharmSavedSearches = MutableStateFlow(emptyList<CharmSavedSearch>())
    internal val charmSavedSearches = mutableCharmSavedSearches.asStateFlow()
    private val mutableCharmIndex = MutableStateFlow<CharmTableIndex?>(null)
    internal val charmIndex = mutableCharmIndex.asStateFlow()
    private val mutableFieldIndex = MutableStateFlow<FieldSourceIndex?>(null)
    internal val fieldIndexState = mutableFieldIndex.asStateFlow()
    private val mutableFieldIndexFailed = MutableStateFlow(false)
    internal val fieldIndexFailed = mutableFieldIndexFailed.asStateFlow()
    private var fieldIndexLoadStarted = false
    private val mutableWeaponRepository = MutableStateFlow<WeaponRepository?>(null)
    internal val weaponRepositoryState = mutableWeaponRepository.asStateFlow()
    private val mutableWeaponRepositoryFailed = MutableStateFlow(false)
    internal val weaponRepositoryFailed = mutableWeaponRepositoryFailed.asStateFlow()
    private var weaponRepositoryLoadStarted = false
    private val mutableItemUsageIndex = MutableStateFlow<ItemUsageIndex?>(null)
    internal val itemUsageIndexState = mutableItemUsageIndex.asStateFlow()
    private val mutableItemUsageIndexFailed = MutableStateFlow(false)
    internal val itemUsageIndexFailed = mutableItemUsageIndexFailed.asStateFlow()
    private var itemUsageIndexLoadStarted = false
    private val firstSearchQueryMeasured = AtomicBoolean(false)
    private val reportedMalformedPreferences = mutableSetOf<String>()
    private val mutableCharmLoadFailed = MutableStateFlow(false)
    internal val charmLoadFailed = mutableCharmLoadFailed.asStateFlow()
    private var charmIndexLoadStarted = false
    /** Schema-11 production weapon repository, indexed once outside Compose recomposition. */
    /** Source-backed inverse Item Usage projection, prepared once when a material is opened. */
    /** Retains per-type weapon forest/detail state across Item Detail routes. */
    internal val weaponScreenState = WeaponScreenState()
    private val mutableState = MutableStateFlow(AppState())
    internal val state = mutableState.asStateFlow()
    private val persistedMutations = OrderedMutationQueue<PersistedUserMutation>(
        scope = viewModelScope,
        apply = { mutation ->
            when (mutation) {
                is PersistedUserMutation.ToggleFavorite -> repository.toggleFavorite(mutation.type, mutation.id, mutation.atMillis)
                is PersistedUserMutation.RecordOpened -> repository.recordOpened(mutation.type, mutation.id, mutation.atMillis)
                is PersistedUserMutation.SetCurrentCharmTable -> repository.saveCurrentCharmTable(mutation.table)
                is PersistedUserMutation.SaveCharmSearch -> repository.saveCharmSearch(mutation.name, mutation.criteria)
                is PersistedUserMutation.DeleteCharmSearch -> repository.deleteCharmSearch(mutation.id)
            }
        },
        onFailure = { failure -> Log.e("CompanionPreferences", "Persisted user action failed", failure) }
    )
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<SearchResult>> = combine(
        state.map { it.query to it.filter }.distinctUntilChanged(),
        mutableSearchCorpus
    ) { searchContext, corpus -> searchContext to corpus }
        .mapLatest { (searchContext, corpus) ->
            if (corpus == null) emptyList()
            else withContext(Dispatchers.Default) {
                if (BuildConfig.DEBUG && searchContext.first.isNotBlank() && firstSearchQueryMeasured.compareAndSet(false, true)) {
                    val start = SystemClock.elapsedRealtimeNanos()
                    try {
                        corpus.searchCancellable(searchContext.first, searchContext.second)
                    } finally {
                        val elapsedMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                        Log.d("MHP3rdFirstUse", "global-search-first-query ${"%.2f".format(java.util.Locale.US, elapsedMs)}ms")
                    }
                } else corpus.searchCancellable(searchContext.first, searchContext.second)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            runCatching {
                val fixture = repository.loadDataOffMain()
                val progression = repository.loadQuestProgressionOffMain()
                val corpus = withContext(Dispatchers.Default) { measureDebug("global-search-corpus-build") { SearchCorpus(fixture) } }
                mutableQuestProgression.value = progression
                mutableSearchCorpus.value = corpus
                mutableFixture.value = fixture
            }.onFailure {
                Log.e("MHP3rdStartup", "Unable to prepare packaged reference data", it)
                mutableInitialLoadFailed.value = true
            }
        }
        combine(
            repository.favoritesStoredState,
            repository.recentStoredState,
            repository.currentCharmTableStoredState,
            repository.charmSavedSearchesStoredState
        ) { favorites, recent, table, savedSearches ->
            PersistedUserSnapshot(favorites, recent, table, savedSearches)
        }
            .onEach { snapshot ->
                logMalformedPreference("favorites", snapshot.favorites)
                logMalformedPreference("recent", snapshot.recent)
                logMalformedPreference("current_charm_table", snapshot.currentCharmTable)
                logMalformedPreference("charm_saved_searches", snapshot.charmSavedSearches)
                mutableState.update {
                    it.copy(
                        favorites = snapshot.favorites.listOrEmpty(),
                        recent = snapshot.recent.listOrEmpty(),
                        currentCharmTable = (snapshot.currentCharmTable as? PreferenceDecodeResult.Valid)?.value
                    )
                }
                mutableCharmSavedSearches.value = snapshot.charmSavedSearches.listOrEmpty()
                mutablePreferencesLoaded.value = true
            }
            .catch { failure ->
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                Log.e("MHP3rdStartup", "Unable to load user preferences", failure)
                mutableInitialLoadFailed.value = true
            }
            .launchIn(viewModelScope)
    }

    fun setQuery(value: String) {
        val snapshot = mutableState.updateAndGet { it.copy(query = value) }
        if (value.isBlank() && snapshot.filter == EntityType.WEAPON) prepareWeaponRepository()
    }
    fun resetQuery() = mutableState.update { it.copy(query = "", queryResetGeneration = it.queryResetGeneration + 1) }
    fun setFilter(value: EntityType?) {
        selectFieldGuideRootPage(fieldGuidePageForCategory(value))
    }
    internal fun selectFieldGuideRootPage(page: FieldGuideRootPage) {
        mutableState.update { current -> current.copy(fieldGuideRootPage = page) }
        if (page == FieldGuideRootPage.WEAPONS) prepareWeaponRepository()
        if (page == FieldGuideRootPage.CHARM_TABLES) prepareCharmIndex()
    }
    fun clearSearchContext() = mutableState.update {
        it.copy(query = "", queryResetGeneration = it.queryResetGeneration + 1, fieldGuideRootPage = FieldGuideRootPage.ALL)
    }
    fun toggleFavorite(type: ReferenceEntityType, id: String) {
        persistedMutations.submit(PersistedUserMutation.ToggleFavorite(type, id, System.currentTimeMillis()))
    }

    fun recordOpened(type: ReferenceEntityType, id: String) {
        persistedMutations.submit(PersistedUserMutation.RecordOpened(type, id, System.currentTimeMillis()))
    }

    fun setCurrentCharmTable(table: Int?) {
        require(table == null || table in 1..12)
        persistedMutations.submit(PersistedUserMutation.SetCurrentCharmTable(table))
    }

    internal fun saveCharmSearch(name: String, criteria: CharmSearchCriteria) {
        persistedMutations.submit(PersistedUserMutation.SaveCharmSearch(name, criteria))
    }

    internal fun deleteCharmSearch(id: String) {
        persistedMutations.submit(PersistedUserMutation.DeleteCharmSearch(id))
    }

    /** Map source projection is built once on Default, only when a map is first opened. */
    @Synchronized
    internal fun prepareFieldIndex() {
        if (mutableFieldIndex.value != null || fieldIndexLoadStarted) return
        fieldIndexLoadStarted = true
        viewModelScope.launch {
            runCatching { repository.loadFieldIndexOffMain() }
                .onSuccess { mutableFieldIndex.value = it }
                .onFailure {
                    Log.e("MHP3rdFirstUse", "field-source-index-build failed", it)
                    mutableFieldIndexFailed.value = true
                }
        }
    }

    /** Weapon validation/indexing stays intact but is prepared off Main on first use. */
    @Synchronized
    internal fun prepareWeaponRepository() {
        if (mutableWeaponRepository.value != null || weaponRepositoryLoadStarted) return
        weaponRepositoryLoadStarted = true
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    measureDebug("weapon-repository-validation+index") {
                        WeaponRepository.fromProduction(fixture.weapons, fixture.materials, fixture.huntingHornSongCatalog)
                    }
                }
            }.onSuccess { mutableWeaponRepository.value = it }
                .onFailure {
                    Log.e("MHP3rdFirstUse", "weapon-repository-validation+index failed", it)
                    mutableWeaponRepositoryFailed.value = true
                    weaponRepositoryLoadStarted = false
                }
        }
    }

    @Synchronized
    internal fun prepareItemUsageIndex() {
        if (mutableItemUsageIndex.value != null || itemUsageIndexLoadStarted) return
        itemUsageIndexLoadStarted = true
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) { measureDebug("item-usage-index-build") { ItemUsageIndex.build(fixture) } }
            }.onSuccess { mutableItemUsageIndex.value = it }
                .onFailure {
                    Log.e("MHP3rdFirstUse", "item-usage-index-build failed", it)
                    mutableItemUsageIndexFailed.value = true
                    itemUsageIndexLoadStarted = false
                }
        }
    }

    @Synchronized
    internal fun prepareCharmIndex() {
        if (mutableCharmIndex.value != null || charmIndexLoadStarted) return
        val fixture = mutableFixture.value ?: return
        charmIndexLoadStarted = true
        val repository = CharmTableRepository(getApplication(), fixture.skillTrees)
        viewModelScope.launch {
            runCatching { repository.loadIndexOffMain() }
                .onSuccess { mutableCharmIndex.value = it }
                .onFailure {
                    Log.e("MHP3rdFirstUse", "charm-json-decode+index-validation failed", it)
                    mutableCharmLoadFailed.value = true
                    charmIndexLoadStarted = false
                }
        }
    }

    /** Keeps incomplete Identify work across Activity configuration changes only. */
    internal fun updateCharmIdentifyWorkingState(transform: (CharmIdentifyWorkingState) -> CharmIdentifyWorkingState) {
        mutableState.update { state -> state.copy(charmIdentifyWorkingState = transform(state.charmIdentifyWorkingState)) }
    }

    private inline fun <T> measureDebug(name: String, block: () -> T): T {
        val start = SystemClock.elapsedRealtimeNanos()
        return try {
            block()
        } finally {
            if (BuildConfig.DEBUG) {
                val elapsedMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                Log.d("MHP3rdFirstUse", "$name ${"%.2f".format(java.util.Locale.US, elapsedMs)}ms")
            }
        }
    }

    private fun <T> logMalformedPreference(name: String, value: PreferenceDecodeResult<T>) {
        if (value is PreferenceDecodeResult.Malformed && reportedMalformedPreferences.add(name)) {
            Log.w("CompanionPreferences", "Ignoring malformed $name preference: ${value.reason}")
        }
    }
}
