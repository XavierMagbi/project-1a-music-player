package com.epfl.esl.musicplayer.presentation


import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class WearPlayViewModel(app: Application) : AndroidViewModel(app) {


    val context = getApplication<Application>().applicationContext

    val messageClient : MessageClient = Wearable.getMessageClient(context)
    //val nodeClient = Wearable.getNodeClient(application)

    private val _nodeIds = MutableLiveData<List<String>>(emptyList<String>())
    val nodeIds: LiveData<List<String>> = _nodeIds

    /**
     * Safely get all connected Message API node IDs using coroutines
     */
    fun getNodes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch nodes off the main thread
                val nodes = Tasks.await(
                    Wearable.getNodeClient(getApplication()).connectedNodes
                )
                val ids = nodes.map { it.id }

                // Update LiveData on the main thread
                withContext(Dispatchers.Main) {
                    _nodeIds.value = ids
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _nodeIds.value = emptyList() // safe fallback if error occurs
                }
            }
        }
    }

    // Send a MessageItem to mobile
    fun sendMessageToMobile(messageClient: MessageClient,message: String, MESSAGE_PATH:String) {
        getNodes()

        for(nodeId in _nodeIds.value){
            Wearable.getMessageClient(context.applicationContext).sendMessage(
                nodeId,
                MESSAGE_PATH,
                message.toByteArray()
            )
        }

    }

    // Pause/Play button
    fun onPlayPauseClick(){
        sendMessageToMobile(messageClient,"true", "PlayPause")
    }
    // Left arrow button
    fun onLeftArrowClick(){
        sendMessageToMobile(messageClient,"true", "LeftArrow")
    }
    // Right arrow button
    fun onRightArrowClick() {
        sendMessageToMobile(messageClient,"true", "RightArrow")

    }



}