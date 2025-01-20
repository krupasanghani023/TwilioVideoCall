package com.example.twiliovideocalling

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.twiliovideocalling.databinding.ActivityMainBinding
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions


class MainActivity : AppCompatActivity() {

    companion object{
        const val IS_HOST = "IS_HOST"
        const val ENTER_ROOM_NAME = "ENTER_ROOM_NAME"
        const val ENTER_TOKEN = "ENTER_TOKEN"
    }

    private lateinit var binding: ActivityMainBinding
    var isAbleToClick = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()

        binding.createButton.setOnClickListener {
            if(isAbleToClick){
                if(binding.roomNameEditTextText.text.isNullOrEmpty() && binding.tokenEditTextText.text.isNullOrEmpty()){
                    Toast.makeText(this, "Please enter room name and token", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, VideoCallingActivity::class.java)
                    intent.putExtra(IS_HOST, true)
                    intent.putExtra(ENTER_ROOM_NAME, binding.roomNameEditTextText.text.toString())
                    intent.putExtra(ENTER_TOKEN, binding.tokenEditTextText.text.toString())
                    startActivity(intent)
                }
            } else {
                checkPermissions()
            }
        }

        binding.joinButton.setOnClickListener {
            if(isAbleToClick){
                if(binding.roomNameEditTextText.text.isNullOrEmpty() && binding.tokenEditTextText.text.isNullOrEmpty()){
                    Toast.makeText(this, "Please enter room name and token", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, VideoCallingActivity::class.java)
                    intent.putExtra(IS_HOST, false)
                    intent.putExtra(ENTER_ROOM_NAME, binding.roomNameEditTextText.text.toString())
                    intent.putExtra(ENTER_TOKEN, binding.tokenEditTextText.text.toString())
                    startActivity(intent)
                }
            } else {
                checkPermissions()
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            XXPermissions.with(this)
                .permission(Permission.CAMERA)
                .permission(Permission.RECORD_AUDIO)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        if (all) {
                            isAbleToClick = true
                        } else {
                            Toast.makeText(
                                this@MainActivity, "Permissions not granted", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onDenied(permissions: List<String>, never: Boolean) {
                        Toast.makeText(
                            this@MainActivity, "Permissions denied", Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } else {
            isAbleToClick = true
        }
    }



}
