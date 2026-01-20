package com.epfl.esl.musicplayer.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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