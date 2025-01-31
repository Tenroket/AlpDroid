package com.alpdroid.huGen10


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap


class CanframeBuffer {

    private var mapFrame : ConcurrentHashMap<Int, CanFrame> = ConcurrentHashMap<Int, CanFrame>(100)
    private var queueoutFrame : LinkedHashMap<Int, CanFrame> = LinkedHashMap(50)
    private var sendingSwitch : Boolean = false
    private var mutex_add : Mutex = Mutex()
    private var mutex_push : Mutex = Mutex()



    @Synchronized
    fun addFrame(frame: CanFrame) {

        CoroutineScope(Dispatchers.IO).launch {
            mutex_add.withLock {
                if (this@CanframeBuffer.mapFrame.replace(frame.id, frame) == null)
                    this@CanframeBuffer.mapFrame[frame.id] = frame
            }
        }
    }


    fun setSending()
    {
        sendingSwitch=true
    }

    fun unsetSending()
    {
        sendingSwitch=false
    }

    fun isFrametoSend() : Boolean
    {
        return sendingSwitch
    }

    fun getFrame(candID:Int): CanFrame? {
        try {
            return this.mapFrame[candID]
        }
        catch (e:Exception) {
            return null
        }
    }

    @Synchronized
    fun pushFifoFrame(candID: Int)
    {
        CoroutineScope(Dispatchers.IO).launch {
            mutex_push.withLock {  // Push frame to send into FiFO queue
            getFrame(candID).also {
            if (it!=null) this@CanframeBuffer.queueoutFrame[candID] = it
            }
          }
        }
    }

    fun getKeys(): Set<Int> {
        return queueoutFrame.keys
    }

    fun getMapKeys() : MutableSet<Int> {
        return mapFrame.keys
    }

    fun isNotEmpty(): Boolean {
        return queueoutFrame.isNotEmpty()

    }

    fun get(next: Int): CanFrame? {
        try {
            return queueoutFrame.get(next)
        }
        catch (e:Exception) {
            return null
        }
    }

    fun remove(id: Int) {
        queueoutFrame.remove(id)
    }


    fun flush()
    {
        CoroutineScope(Dispatchers.IO).launch {
            mutex_push.withLock {
                queueoutFrame.clear()
            }
       }
    }


}


