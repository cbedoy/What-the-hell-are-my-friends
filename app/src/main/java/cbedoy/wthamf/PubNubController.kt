package cbedoy.wthamf

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.PNTimeResult
import com.pubnub.api.models.consumer.history.PNFetchMessagesResult
import com.pubnub.api.models.consumer.history.PNHistoryResult
import com.pubnub.api.models.consumer.presence.PNHereNowResult
import com.pubnub.api.models.consumer.presence.PNSetStateResult
import com.pubnub.api.models.consumer.presence.PNWhereNowResult
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@SuppressLint("StaticFieldLeak")
/**
 * DagM8
 *
 * Created by bedoy on 6/29/18.
 */
object PubNubController{
    private const val TAG = "PNController"
    private var pubNub: PubNub?= null
    private var context : Context? = null
    private var sessionUser : String? = null

    /**
     * Initialization module
     */
    fun init(_context: Context){
        val configuration = PNConfiguration()
        configuration.subscribeKey = BuildConfig.PUB_NUB_SUBSCRIPTION_KEY
        configuration.publishKey = BuildConfig.PUB_NUB_PUBLISH_KEY
        configuration.secretKey = BuildConfig.PUB_NUB_SECRET_KEY
        configuration.isSecure = false
        configuration.presenceTimeout = 120

        if (BuildConfig.DEBUG)
            configuration.logVerbosity = PNLogVerbosity.BODY

        pubNub = PubNub(configuration)
        context = _context

        if (BuildConfig.DEBUG)
            Timber.tag(TAG).d("init Version -> ${pubNub?.version}")
    }


    /**
     *
     * Should register an user to send message like @sha1value or @cbedoy
     */
    fun registerUser(nickname: String){
        pubNub?.configuration?.uuid = nickname

        sessionUser = nickname

        if (BuildConfig.DEBUG)
            Timber.tag(TAG).d("registerUser -> $nickname")
    }

    /**
     * Should specify the channels you want to have subscription
     */
    fun subscribeToChannels(channels: ArrayList<String>){
        pubNub?.subscribe()?.withPresence()?.channels(channels)?.execute()

        if (BuildConfig.DEBUG)
            Timber.tag(TAG).d("subscribeToChannel -> $channels")
    }

    /**
     * Should specify the channel you want to have subscription
     */
    fun subscribeToChannel(channel: String?){

        if (channel != null) {
            val channels = Arrays.asList(channel)

            pubNub?.subscribe()?.withPresence()?.channels(channels)?.execute()

            if (BuildConfig.DEBUG)
                Timber.tag(TAG).d("subscribeToChannel -> $channel")
        }
    }

    /**
     * Should specify the channels you want to have unsubscription
     */
    fun unsubscribeFromChannels(channels: ArrayList<String>){
        pubNub?.unsubscribe()?.channels(channels)?.execute()

        if (BuildConfig.DEBUG)
            Timber.tag(TAG).d("subscribeToChannel -> $channels")
    }

    /**
     * Should specify the channel you want to have unsubscription
     */
    fun unsubscribeFromChannel(channel: String?){
        if (channel != null) {
            val channels = Arrays.asList(channel)

            pubNub?.unsubscribe()?.channels(channels)?.execute()

            if (BuildConfig.DEBUG)
                Timber.tag(TAG).d("unsubscribeFromChannel -> $channel")
        }
    }


    fun publishMessageToChannel(message: HashMap<String, Any>, to: String){
        message["timestamp"] = System.currentTimeMillis()/1000
        message["from"] = sessionUser!!
        message["uuid"] = UUID.randomUUID().toString()
        pubNub?.publish()?.channel(to)?.message(message)?.shouldStore(true)?.usePOST(true)?.async(object : PNCallback<PNPublishResult>() {
            override fun onResponse(result: PNPublishResult?, status: PNStatus?) {
                if (status != null) {
                    if (!status.isError) {

                        if (BuildConfig.DEBUG)
                            Timber.tag(TAG).d("publishMessageToChannel -> $to")
                    }
                }
            }
        })
    }


    /**
     * Request history from channel, you should only specify the channels where you want request history.
     */
    fun requestHistoryFromChannel(channel: String?, callback : PNControllerHistoryCallback){
        if (channel != null) {
            pubNub?.history()?.reverse(true)?.channel(channel)?.async(object  : PNCallback<PNHistoryResult>(){
                override fun onResponse(result: PNHistoryResult?, status: PNStatus?) {
                    if (status != null && !status.isError){
                        val messages : ArrayList<JsonElement> = ArrayList()

                        val rawMessages = result?.messages
                        rawMessages?.forEach {
                            if (it.entry != null)
                                messages.add(it.entry)
                        }

                        callback.onLoadedMessages(messages)
                    }else{
                        callback.onLoadedMessages(ArrayList())
                    }
                }
            })
        }else{
            callback.onLoadedMessages(ArrayList())
        }
    }

    /**
     * Request history from channel, you should only the channel where you want request history.
     */
    fun requestHistoryFromChannel(channel: String?, limit: Int, offset: Long, callback : PNControllerHistoryCallback) {
        if(channel != null){
            pubNub?.history()?.reverse(true)?.channel(channel)?.start(offset)?.count(limit)?.async(object  : PNCallback<PNHistoryResult>(){
                override fun onResponse(result: PNHistoryResult?, status: PNStatus?) {
                    if (status != null && !status.isError){
                        val messages : ArrayList<JsonElement> = ArrayList()

                        val rawMessages = result?.messages
                        rawMessages?.forEach {
                            if (it.entry != null)
                                messages.add(it.entry)
                        }

                        callback.onLoadedMessages(messages)
                    }else{
                        callback.onLoadedMessages(ArrayList())
                    }
                }
            })
        }
    }

    fun fetchMessagesFromChannels(channels:  List<String>, callback: PNControllerFetchMessagesCallback){
        pubNub?.fetchMessages()?.channels(channels)?.maximumPerChannel(1)?.async(object  : PNCallback<PNFetchMessagesResult>(){
            override fun onResponse(result: PNFetchMessagesResult?, status: PNStatus?) {
                if (status != null && !status.isError){
                    val results = HashMap<String, JsonElement>()
                    val mutableMap = result?.channels
                    mutableMap?.forEach { channel, mutableList ->
                        run {

                            if (mutableList.size > 0) {
                                results[channel] = mutableList[0].message
                            }
                            callback.onLoadedMessages(results)
                        }
                    }
                }else{
                    callback.onLoadedMessages(HashMap())
                }
            }
        })
    }

    fun postPresenceStateToChannels(channels: MutableList<String>, state: JsonObject, callback: PNControllerPresenceStateCallback?){
        pubNub?.setPresenceState()?.state(state)?.channels(channels)?.uuid(sessionUser)?.async(object : PNCallback<PNSetStateResult>(){
            override fun onResponse(result: PNSetStateResult?, status: PNStatus?) {
                val jsonElement = result?.state
                Timber.d(jsonElement.toString())

                callback?.onResult(result)
            }
        })
    }

    fun postTypingPresenceToChannel(typing: Boolean, channel: String){
        val state = JsonObject()
        state.addProperty("typing", typing)

        pubNub?.setPresenceState()?.channels(Arrays.asList(channel))?.state(state)?.async(object : PNCallback<PNSetStateResult>(){
            override fun onResponse(result: PNSetStateResult?, status: PNStatus?) {
                val jsonElement = result?.state
                Timber.d(jsonElement.toString())
            }
        })
    }

    fun whereNow(userId: String, callback: PNControllerWhereNowCallback){
        pubNub?.whereNow()?.uuid(sessionUser)?.async(object : PNCallback<PNWhereNowResult>(){
            override fun onResponse(result: PNWhereNowResult?, status: PNStatus?) {
                val channels = ArrayList<String>()
                result?.channels?.forEach {
                    if (BuildConfig.DEBUG) {
                        Timber.d(it)
                    }
                    channels.add(it)
                }
                callback.onLoadedMetadata(channels)
            }
        })
    }

    fun hereNowToChannels(channels: ArrayList<String>, callback: PNControllerHereNowCallback){
        pubNub?.hereNow()?.channels(channels)?.includeUUIDs(true)?.async(object : PNCallback<PNHereNowResult>(){
            override fun onResponse(result: PNHereNowResult?, status: PNStatus?) {
                val metadata = HashMap<String, Any>()
                result?.channels?.values?.forEach {
                    if (BuildConfig.DEBUG) {
                        Timber.d("--------------------------------")
                        Timber.d("Channel ${it.channelName}")
                        Timber.d("Occupancy ${it.occupancy}")
                        Timber.d("Occupants:")
                    }

                    metadata["channelName"] = it.channelName
                    metadata["occupancy"] = it.occupancy
                    metadata["occupants"] = ArrayList<HashMap<String, Any>>()

                    it.occupants.forEach {
                        if (BuildConfig.DEBUG)
                            Timber.d("uuid: ${it.uuid} state: ${it.state}")

                        val list = metadata["occupants"] as ArrayList<HashMap<String, JsonElement>>
                        val element = HashMap<String, JsonElement>()
                        element[it.uuid] = it.state

                        list.add(element)
                    }

                    callback.onLoadedMetadata(metadata)
                }
            }
        })
    }


    fun verifyClientConnectivity(callback: PNControllerTimeCallback?){
        pubNub?.time()?.async(object : PNCallback<PNTimeResult>() {
            override fun onResponse(result: PNTimeResult?, status: PNStatus?) {
                val timetoken = result?.timetoken

                Timber.d("Time token $timetoken")

                callback?.onResult(result)
            }
        })
    }

    /**
     * Add Listeners
     */
    fun addSubscribeCallback(subscribeCallback: SubscribeCallback) {
        pubNub?.addListener(subscribeCallback)
    }

    /**
     * Remove Listeners
     */
    fun removeSubscribeCallback(subscribeCallback: SubscribeCallback){
        pubNub?.removeListener(subscribeCallback)
    }

    /**
     * Call when you want to kill pubNub
     */
    fun destroy() {
        pubNub?.destroy()

        pubNub = null
    }

    interface PNControllerPresenceStateCallback{
        fun onResult(result: PNSetStateResult?)
    }

    interface PNControllerTimeCallback{
        fun onResult(result: PNTimeResult?)
    }

    interface PNControllerPostMessageCallback{
        fun onPostMessage(message: JsonElement)
    }

    interface PNControllerHistoryCallback{
        fun onLoadedMessages(messages: ArrayList<JsonElement>)
    }

    interface PNControllerFetchMessagesCallback{
        fun onLoadedMessages(dataModel: HashMap<String, JsonElement>)
    }

    interface PNControllerHereNowCallback{
        fun onLoadedMetadata(metadata: HashMap<String, Any>)
    }

    interface PNControllerWhereNowCallback{
        fun onLoadedMetadata(channels: ArrayList<String>)
    }
}