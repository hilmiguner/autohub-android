package com.autohub.android.tv

data class TvPlaylistSnapshot(
    val source: TvPlaylistSource,
    val name: String? = null,
    val epgUrls: List<String> = emptyList(),
    val channels: List<IdentifiedTvChannel> = emptyList(),
    val parseIssues: List<M3uParseIssue> = emptyList(),
)

interface TvPlaylistRepository {
    fun listSources(): List<TvPlaylistSource>

    fun getSource(sourceId: TvPlaylistSourceId): TvPlaylistSource?

    fun upsertSource(source: TvPlaylistSource)

    fun removeSource(sourceId: TvPlaylistSourceId): Boolean

    fun replaceParsedPlaylist(
        sourceId: TvPlaylistSourceId,
        result: M3uParseResult,
    ): TvPlaylistSnapshot

    fun getSnapshot(sourceId: TvPlaylistSourceId): TvPlaylistSnapshot?

    fun findChannel(channelId: TvChannelId): IdentifiedTvChannel?
}

class InMemoryTvPlaylistRepository : TvPlaylistRepository {
    private val sources = linkedMapOf<TvPlaylistSourceId, TvPlaylistSource>()
    private val snapshots = linkedMapOf<TvPlaylistSourceId, TvPlaylistSnapshot>()

    @Synchronized
    override fun listSources(): List<TvPlaylistSource> =
        sources.values.toList()

    @Synchronized
    override fun getSource(sourceId: TvPlaylistSourceId): TvPlaylistSource? =
        sources[sourceId]

    @Synchronized
    override fun upsertSource(source: TvPlaylistSource) {
        sources[source.id] = source
        snapshots[source.id]?.let { current ->
            snapshots[source.id] = current.copy(source = source)
        }
    }

    @Synchronized
    override fun removeSource(sourceId: TvPlaylistSourceId): Boolean {
        snapshots.remove(sourceId)
        return sources.remove(sourceId) != null
    }

    @Synchronized
    override fun replaceParsedPlaylist(
        sourceId: TvPlaylistSourceId,
        result: M3uParseResult,
    ): TvPlaylistSnapshot {
        val source = requireNotNull(sources[sourceId]) {
            "Playlist source must be registered before storing parsed content."
        }
        val identifiedChannels = TvChannelIdentityPolicy
            .identify(sourceId, result.playlist.channels)
            .distinctBy { it.id }

        return TvPlaylistSnapshot(
            source = source,
            name = result.playlist.name,
            epgUrls = result.playlist.epgUrls.distinct(),
            channels = identifiedChannels,
            parseIssues = result.issues,
        ).also { snapshot ->
            snapshots[sourceId] = snapshot
        }
    }

    @Synchronized
    override fun getSnapshot(sourceId: TvPlaylistSourceId): TvPlaylistSnapshot? =
        snapshots[sourceId]

    @Synchronized
    override fun findChannel(channelId: TvChannelId): IdentifiedTvChannel? =
        snapshots.values
            .asSequence()
            .flatMap { it.channels.asSequence() }
            .firstOrNull { it.id == channelId }
}
