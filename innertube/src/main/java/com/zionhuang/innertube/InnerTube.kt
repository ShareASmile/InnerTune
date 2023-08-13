package com.zionhuang.innertube

import com.zionhuang.innertube.models.Context
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.YouTubeLocale
import com.zionhuang.innertube.models.body.*
import com.zionhuang.innertube.models.response.AccountMenuResponse
import com.zionhuang.innertube.models.response.BrowseResponse
import com.zionhuang.innertube.models.response.GetQueueResponse
import com.zionhuang.innertube.models.response.GetSearchSuggestionsResponse
import com.zionhuang.innertube.models.response.GetTranscriptResponse
import com.zionhuang.innertube.models.response.NextResponse
import com.zionhuang.innertube.models.response.PipedResponse
import com.zionhuang.innertube.models.response.PlayerResponse
import com.zionhuang.innertube.models.response.SearchResponse
import com.zionhuang.innertube.utils.parseCookieString
import com.zionhuang.innertube.utils.sha1
import `in`.shabinder.soundbound.providers.Dependencies
import `in`.shabinder.soundbound.utils.DevicePreferences
import `in`.shabinder.soundbound.utils.GlobalJson
import `in`.shabinder.soundbound.zipline.HttpClient
import `in`.shabinder.soundbound.zipline.LocaleProvider
import `in`.shabinder.soundbound.zipline.build
import `in`.shabinder.soundbound.zipline.get
import `in`.shabinder.soundbound.zipline.post
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

/**
 * Provide access to InnerTube endpoints.
 * For making HTTP requests, not parsing response.
 */
class InnerTube(
    dependencies: Dependencies
) : Dependencies by dependencies {
    private var httpClient = createClient()

    var locale = YouTubeLocale(
        gl = localeProvider.getDefaultLocaleCountry(),
        hl = localeProvider.getDefaultLocaleLanguageTag()
    )

    var visitorData: String = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value == null) emptyMap() else parseCookieString(value)
        }
    private var cookieMap = emptyMap<String, String>()

    private fun createClient() = httpClientBuilder.build {
        setDefaultURL("https://music.youtube.com/youtubei/v1/")
    }

    private suspend inline fun <reified T> ytClientCall(
        url: String = "",
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        body: HttpClient.BodyType = HttpClient.BodyType.NONE,
        client: YouTubeClient,
        setLogin: Boolean = false
    ): T = httpClient.post(
        url,
        body = body,
        params = mutableMapOf(
            "key" to client.api_key,
            "prettyPrint" to "false"
        ).apply {
            putAll(params)
        },
        headers = mutableMapOf(
            "X-Goog-Api-Format-Version" to "1",
            "X-YouTube-Client-Name" to client.clientName,
            "X-YouTube-Client-Version" to client.clientVersion,
            "x-origin" to "https://music.youtube.com",
            "Content-Type" to "application/json",
            "User-Agent" to client.userAgent,
        ).apply {
            if (client.referer != null) {
                put("Referer", client.referer)
            }

            if (setLogin) {
                cookie?.let { cookie ->
                    put("cookie", cookie)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = devicePreferences.getSystemTimeMillis() / 1000
                    val sapisidHash =
                        sha1("$currentTime ${cookieMap["SAPISID"]} https://music.youtube.com")
                    put("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }

            putAll(headers)
        }
    )

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
        isIsrcSearch: Boolean = false, //isrc searches come up in de `hl` locale
    ): SearchResponse = ytClientCall(
        url = "search",
        params = mutableMapOf(
            "continuation" to continuation.toString(),
            "ctoken" to continuation.toString(),
        ).apply {
            if (continuation != null) {
                put("type", "next")
            }

        },
        body = HttpClient.BodyType.JSON(
            Json.encodeToString(
                SearchBody(
                    context = client.toContext(locale.run {
                        if (isIsrcSearch) copy(hl = "de") else this
                    }, visitorData),
                    query = query,
                    params = params
                )
            )
        ),
        client = client
    )

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
    ): PlayerResponse = ytClientCall(
        url = "player",
        client = client,
        setLogin = true,
        body = HttpClient.BodyType.JSON(
            Json.encodeToString(
                PlayerBody(
                    context = client.toContext(locale, visitorData).let {
                        if (client == YouTubeClient.TVHTML5) {
                            it.copy(
                                thirdParty = Context.ThirdParty(
                                    embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                                )
                            )
                        } else it
                    },
                    videoId = videoId,
                    playlistId = playlistId
                )
            )
        )
    )

    suspend fun pipedStreams(videoId: String): PipedResponse =
        httpClient.get(
            "https://pipedapi.kavin.rocks/streams/${videoId}",
            headers = mapOf("Content-Type" to "application/json")
        )

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ): BrowseResponse = ytClientCall(
        url = "browse",
        client = client,
        setLogin = setLogin,
        body = HttpClient.BodyType.JSON(
            Json.encodeToString(
                BrowseBody(
                    context = client.toContext(locale, visitorData),
                    browseId = browseId,
                    params = params
                )
            )
        ),
        params = mutableMapOf(
            "continuation" to continuation.toString(),
            "ctoken" to continuation.toString(),
        ).apply {
            if (continuation != null) {
                put("type", "next")
            }
        }
    )

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ): NextResponse = ytClientCall(
        url = "next",
        client = client,
        setLogin = true,
        body = HttpClient.BodyType.JSON(
            Json.encodeToString(
                NextBody(
                    context = client.toContext(locale, visitorData),
                    videoId = videoId,
                    playlistId = playlistId,
                    playlistSetVideoId = playlistSetVideoId,
                    index = index,
                    params = params,
                    continuation = continuation
                )
            )
        )
    )

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ): GetSearchSuggestionsResponse = ytClientCall(
        url = "music/get_search_suggestions",
        client = client,
        body = HttpClient.BodyType.JSON(
            GlobalJson.encodeToString(
                GetSearchSuggestionsBody(
                    context = client.toContext(locale, visitorData),
                    input = input
                )
            )
        )
    )

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ): GetQueueResponse = ytClientCall(
        url = "music/get_queue",
        client = client,
        body = HttpClient.BodyType.JSON(
            GlobalJson.encodeToString(
                GetQueueBody(
                    context = client.toContext(locale, visitorData),
                    videoIds = videoIds,
                    playlistId = playlistId
                )
            )
        )
    )

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ): GetTranscriptResponse = httpClient.post(
        url = "https://music.youtube.com/youtubei/v1/get_transcript",
        params = mapOf(
            "key" to "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3"
        ),
        headers = mapOf(
            "Content-Type" to "application/json"
        ),
        body = HttpClient.BodyType.JSON(
            GlobalJson.encodeToString(
                GetTranscriptBody(
                    context = client.toContext(locale, null),
                    params = crypto.encodeBase64("\n${11.toChar()}$videoId")
                )
            )
        )
    )

    suspend fun getSwJsData() = httpClient.getAsString("https://music.youtube.com/sw.js_data")

    suspend fun accountMenu(client: YouTubeClient): AccountMenuResponse =
        ytClientCall(
            url = "account/account_menu",
            setLogin = true,
            client = client,
            body = HttpClient.BodyType.JSON(
                GlobalJson.encodeToString(
                    AccountMenuBody(client.toContext(locale, visitorData))
                )
            )
        )
}
