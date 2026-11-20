package com.example.permisos.fetures

import android.content.Context
import android.content.IntentSender
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AudioHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var recordingJob: Job? = null

    val audioFile: File by lazy{
        File(context.cacheDir,"memo_audio_3s.m4a")
    }

    fun isAudioAvailable(): Boolean = audioFile.exists() && audioFile.length() > 0

    fun start4SecondsRecording(
        scope : CoroutineScope,
        onProgress: (Int)->Unit,
        onFinished: (Boolean, String) -> Unit
    ){
        stopRecording()
        stopPlayback()

        recordingJob = scope.launch(Dispatchers.IO){
            try {
                if(audioFile.exists()){
                    audioFile.delete()
                }
                val recorder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                    MediaRecorder(context)
                }else{
                    MediaRecorder()
                }

                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder

                for(secondsLeft in 4 downTo 1){
                    withContext(Dispatchers.Main){
                        onProgress(secondsLeft)
                    }
                    delay(1000)
                }

                recorder.stop()
                recorder.release()
                mediaRecorder = null

                withContext(Dispatchers.Main){
                    onFinished(true,"Audio grabado con éxito (4 segundos).")
                }

            }catch (e: Exception){
                mediaRecorder?.release()
                mediaRecorder = null
                withContext(Dispatchers.Main){
                    onFinished(false,"Error al grabar audio: ${e.localizedMessage}")
                }
            }
        }

    }

    fun playAudio(
        onPlaybackStarted: ()-> Unit,
        onPlaybackFinished: () -> Unit,
        onError: (String) -> Unit
    ){
        if(!isAudioAvailable()){
            onError("No hay ninguna nota de voz grabada aún")
            return
        }
        stopPlayback()
        try{
            val player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                    onPlaybackFinished()
                }
                start()
            }
            mediaPlayer = player
            onPlaybackStarted()
        }catch (e:Exception){
            stopPlayback()
            onError("Error al reproducir audio: ${e.localizedMessage}")
        }
    }

    fun stopRecording(){
        recordingJob?.cancel()
        recordingJob = null
        try {
            mediaRecorder?.stop()
        }catch (_: Exception){}
        mediaRecorder?.release()
        mediaRecorder = null
    }

    fun stopPlayback(){
        try{
           mediaPlayer?.stop()
        }catch (_: Exception){}
        mediaPlayer?.release()
        mediaPlayer = null
    }

}