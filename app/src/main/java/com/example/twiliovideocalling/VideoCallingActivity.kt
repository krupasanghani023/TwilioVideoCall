package com.example.twiliovideocalling

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.twiliovideocalling.MainActivity.Companion.ENTER_ROOM_NAME
import com.example.twiliovideocalling.MainActivity.Companion.ENTER_TOKEN
import com.example.twiliovideocalling.MainActivity.Companion.IS_HOST
import com.example.twiliovideocalling.databinding.ActivityVideoCallingBinding
import com.twilio.video.Camera2Capturer
import com.twilio.video.ConnectOptions
import com.twilio.video.LocalAudioTrack
import com.twilio.video.LocalVideoTrack
import com.twilio.video.RemoteAudioTrack
import com.twilio.video.RemoteAudioTrackPublication
import com.twilio.video.RemoteDataTrack
import com.twilio.video.RemoteDataTrackPublication
import com.twilio.video.RemoteParticipant
import com.twilio.video.RemoteVideoTrack
import com.twilio.video.RemoteVideoTrackPublication
import com.twilio.video.Room
import com.twilio.video.TwilioException
import com.twilio.video.Video


class VideoCallingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoCallingBinding
    private var camera2Capturer: Camera2Capturer? = null
    private var isUsingFrontCamera = true
    private var localVideoTrack: LocalVideoTrack? = null
    private var room: Room? = null
    private var localAudioTrack: LocalAudioTrack? = null
    private var isHost: Boolean = false
    private var isMute: Boolean = false
    private var isVideoOff: Boolean = false
    private var extraRoomName: String = ""
    private var extraToken: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoCallingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isHost = intent.getBooleanExtra(IS_HOST, false)
        extraRoomName = intent.getStringExtra(ENTER_ROOM_NAME).toString()
        extraToken = intent.getStringExtra(ENTER_TOKEN).toString()
        Log.d("development", "extraRoomName $extraRoomName")
        Log.d("development", "extraToken $extraToken")

        if(isHost){
            startVideo(extraRoomName,extraToken)
        } else {
            joinRoom(extraRoomName, extraToken)
        }

        binding.mute.setOnClickListener {
            if(isMute){
                localAudioTrack?.enable(true)
                binding.ivMute.setImageResource(R.drawable.ic_unmute)
            } else {
                localAudioTrack?.enable(false)
                binding.ivMute.setImageResource(R.drawable.ic_mute)
            }
            isMute = !isMute
        }

        binding.videoOff.setOnClickListener {
            if(isVideoOff){
                localVideoTrack?.enable(true)
                binding.ivVideoOff.setImageResource(R.drawable.video_unmute)
            } else {
                localVideoTrack?.enable(false)
                binding.ivVideoOff.setImageResource(R.drawable.video_mute)
            }
            isVideoOff = !isVideoOff
        }


        binding.switchCamera.setOnClickListener {
            if (camera2Capturer != null) {
                val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
                try {
                    val cameraIdList = cameraManager.cameraIdList
                    var newCameraId: String? = null

                    // Get the current camera ID and choose the opposite one
                    if (isUsingFrontCamera) {
                        // Switch to the rear camera
                        for (cameraId in cameraIdList) {
                            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                                newCameraId = cameraId
                                break
                            }
                        }
                    } else {
                        // Switch to the front camera
                        for (cameraId in cameraIdList) {
                            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                                newCameraId = cameraId
                                break
                            }
                        }
                    }

                    if (newCameraId != null) {
                        // Switch camera if a valid cameraId is found
                        camera2Capturer?.switchCamera(newCameraId)
                        isUsingFrontCamera = !isUsingFrontCamera
                    } else {
                        Log.e("development", "Failed to find a valid camera ID.")
                    }
                } catch (e: Exception) {
                    Log.e("development", "Failed to switch camera: $e")
                }
            } else {
                Log.e("development", "camera2Capturer is not initialized.")
            }
        }


        binding.primaryVideoView.setOnClickListener {
            room?.localParticipant?.videoTracks?.forEach {
                Log.d("development", "localParticipant videoTrack $it")
            }

            room?.remoteParticipants?.forEach {
                Log.w("development", "remoteParticipant $it")
            }

        }
    }


    private fun getFrontCameraId(): String? {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun joinRoom(roomName: String, accessToken: String) {
        val cameraId = getFrontCameraId()
        if (cameraId != null) {
            camera2Capturer = Camera2Capturer(
                this@VideoCallingActivity,
                cameraId,
                object : Camera2Capturer.Listener {
                    override fun onFirstFrameAvailable() {
                        Log.d("development", "First frame available from Camera2Capturer.")
                    }

                    override fun onCameraSwitched(newCameraId: String) {
                        Log.d("development", "Camera switched to: $newCameraId")
                    }

                    override fun onError(camera2CapturerException: Camera2Capturer.Exception) {
                        Log.e("development", "Camera2Capturer error: $camera2CapturerException")
                    }
                }
            )

            localVideoTrack = LocalVideoTrack.create(this@VideoCallingActivity, true, camera2Capturer!!)
            localAudioTrack = LocalAudioTrack.create(this@VideoCallingActivity, true)

            binding.primaryVideoView.mirror = true
            localVideoTrack?.addSink(binding.primaryVideoView)

            val connectOptions = ConnectOptions.Builder(accessToken)
                .roomName(roomName)
                .audioTracks(listOf(localAudioTrack))
                .videoTracks(listOf(localVideoTrack))
                .build()

            room = Video.connect(this@VideoCallingActivity, connectOptions, roomListener)
        } else {
            Toast.makeText(this@VideoCallingActivity, "No front camera found", Toast.LENGTH_SHORT).show()
        }
    }



    private fun startVideo(extraRoomName: String, extraToken: String) {
        val cameraId = getFrontCameraId() // Use Camera2 to get the front camera ID

        if (cameraId != null) {
             camera2Capturer = Camera2Capturer(
                 this@VideoCallingActivity,
                cameraId,
                object : Camera2Capturer.Listener {
                    override fun onFirstFrameAvailable() {
                        Log.d("development", "First frame available from Camera2Capturer.")
                    }

                    override fun onCameraSwitched(newCameraId: String) {
                        Log.d("development", "Camera switched to: $newCameraId")
                    }

                    override fun onError(camera2CapturerException: Camera2Capturer.Exception) {
                        Log.e("development", "Camera2Capturer error: $camera2CapturerException")
                    }

                }
            )

            localVideoTrack = LocalVideoTrack.create(this@VideoCallingActivity, true, camera2Capturer!!)
            localAudioTrack = LocalAudioTrack.create(this@VideoCallingActivity, true)

            // Attach the video track to the primary video view
            binding.primaryVideoView.mirror = true
            localVideoTrack?.addSink(binding.primaryVideoView)

            // Connect to the room
            connectToRoom(extraRoomName,extraToken)
        } else {
            Toast.makeText(this@VideoCallingActivity, "No front camera found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToRoom(extraRoomName: String, extraToken: String) {
        val connectOptions = ConnectOptions.Builder(extraToken)
            .roomName(extraRoomName)
            .audioTracks(listOf(localAudioTrack))
            .videoTracks(listOf(localVideoTrack))
            .build()

        room = Video.connect(this@VideoCallingActivity, connectOptions, roomListener)
    }

    private val roomListener = object : Room.Listener {
        override fun onConnected(room: Room) {
            this@VideoCallingActivity.room = room
            Log.d("development", "Connected to name :-${room.name}")
            Log.d("development", "Connected to sid :- ${room.sid}")

            room.remoteParticipants.forEach { participant ->
                addParticipant(participant)
            }
        }

        override fun onConnectFailure(room: Room, e: TwilioException) {
            Log.e("development", "Connection failed: ${e.message}")
            Toast.makeText(this@VideoCallingActivity, "Failed to connect to the room.", Toast.LENGTH_SHORT).show()

        }

        override fun onDisconnected(room: Room, e: TwilioException?) {
            Log.d("development", "Disconnected from room ${room.name}")
            localVideoTrack?.release()
            localAudioTrack?.release()
        }

        override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
            Log.d("development", "Participant connected: ${participant.identity}")
            addParticipant(participant)
        }

        override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
            Log.d("development", "Participant disconnected: ${participant.identity}")
        }

        override fun onReconnecting(room: Room, e: TwilioException) {
            Log.d("development", "onReconnecting e: $e")

        }
        override fun onReconnected(room: Room) {
            Log.d("development", "onReconnected")
        }
        override fun onRecordingStarted(room: Room) {
            Log.d("development", "onRecordingStarted")
        }
        override fun onRecordingStopped(room: Room) {
            Log.d("development", "onRecordingStopped")
        }
    }

    private fun addParticipant(participant: RemoteParticipant) {
        Log.w("development", "addParticipant is calling")
        participant.videoTracks.forEach { videoTrackPublication ->
            Log.w("development", "Already subscribed video track for participant: ${videoTrackPublication.videoTrack}")
        }
        participant.setListener(object : RemoteParticipant.Listener {
            override fun onVideoTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                Log.d("development", "addParticipant onVideoTrackSubscribed")
                binding.mainRemoteView.visibility = View.VISIBLE
                binding.remoteVideoOff.setImageResource(R.drawable.video_unmute)
                binding.ivRemoteMute.setImageResource(R.drawable.ic_unmute)
                remoteVideoTrack.addSink(binding.secondryVideoView)
            }

            override fun onVideoTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                twilioException: TwilioException
            ) {
                Log.d("development", "addParticipant onVideoTrackSubscriptionFailed")
            }

            override fun onVideoTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                Log.d("development", "addParticipant onVideoTrackUnsubscribed")
                binding.mainRemoteView.visibility = View.GONE
                remoteVideoTrack.removeSink(binding.secondryVideoView)
            }

            override fun onAudioTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                remoteAudioTrack: RemoteAudioTrack
            ) {
                remoteAudioTrack.enablePlayback(true)
                Log.d("development", "addParticipant onAudioTrackSubscribed")
            }

            override fun onAudioTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                twilioException: TwilioException
            ) {
                Log.d("development", "addParticipant onAudioTrackSubscriptionFailed")
            }

            override fun onAudioTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                remoteAudioTrack: RemoteAudioTrack
            ) {
                remoteAudioTrack.enablePlayback(false)
                Log.d("development", "addParticipant onAudioTrackUnsubscribed")
            }

            override fun onAudioTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                Log.d("development", "addParticipant onAudioTrackPublished")
            }

            override fun onAudioTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                Log.d("development", "addParticipant onAudioTrackUnpublished")
            }

            override fun onVideoTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                Log.d("development", "addParticipant onVideoTrackPublished")
            }

            override fun onVideoTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                Log.d("development", "addParticipant onVideoTrackUnpublished")
            }

            override fun onDataTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication
            ) {
                Log.d("development", "addParticipant onDataTrackPublished")
            }

            override fun onDataTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication
            ) {
                Log.d("development", "addParticipant onDataTrackUnpublished")
            }

            override fun onDataTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                remoteDataTrack: RemoteDataTrack
            ) {
                Log.d("development", "addParticipant onDataTrackSubscribed")
            }

            override fun onDataTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                twilioException: TwilioException
            ) {
                Log.d("development", "addParticipant onDataTrackSubscriptionFailed")
            }

            override fun onDataTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                remoteDataTrack: RemoteDataTrack
            ) {
                Log.d("development", "addParticipant onDataTrackUnsubscribed")
            }

            override fun onAudioTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                Log.d("development", "addParticipant onAudioTrackEnabled")
                binding.ivRemoteMute.setImageResource(R.drawable.ic_unmute)
            }

            override fun onAudioTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                Log.d("development", "addParticipant onAudioTrackDisabled")
                binding.ivRemoteMute.setImageResource(R.drawable.ic_mute)
            }

            override fun onVideoTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                Log.d("development", "addParticipant onVideoTrackEnabled")
                binding.remoteVideoOff.setImageResource(R.drawable.video_unmute)
                binding.remoteVideoPauseTv.visibility = View.GONE
            }

            override fun onVideoTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                Log.d("development", "addParticipant onVideoTrackDisabled")
                binding.remoteVideoPauseTv.visibility = View.VISIBLE
                binding.remoteVideoOff.setImageResource(R.drawable.video_mute)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        room?.disconnect()
        localVideoTrack?.release()
        localAudioTrack?.release()
        camera2Capturer?.stopCapture()
    }

}
