package uz.exemple.less64_tasks_kotlin

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import uz.exemple.less64_tasks_kotlin.adapter.ExternalAdapter
import uz.exemple.less64_tasks_kotlin.adapter.InternalAdapter
import uz.exemple.less64_tasks_kotlin.model.InternalPhotos
import uz.exemple.less64_tasks_kotlin.utils.Utils.fireToast
import java.io.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    var isInternal = true
    var isPersistent = true
    var readPermissionGranted = false
    var writePermissionGranted = false
    var locationPermissionGranted = false
    var cameraPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        requestPermissions()
    }

    private fun initViews() {

        val btn_camera = findViewById<ImageView>(R.id.btn_TakePhoto)
        var switch_private = findViewById<SwitchMaterial>(R.id.switchPrivate)
        val rvPrivatePhotos = findViewById<RecyclerView>(R.id.rvPrivatePhotos)
        val rvPublicPhotos = findViewById<RecyclerView>(R.id.rvPublicPhotos)
        val tvAllPhotos = findViewById<TextView>(R.id.tvAllPhotos)
        val tvPrivatePhotos = findViewById<TextView>(R.id.tvPrivatePhotos)


        rvPublicPhotos.setHasFixedSize(true)
        rvPublicPhotos.layoutManager = GridLayoutManager(this,2)
        val adapterEx = ExternalAdapter(this,loadPhotosFromExternalStorage())
        rvPublicPhotos.adapter = adapterEx

        rvPublicPhotos.visibility = View.VISIBLE
        tvAllPhotos.visibility = View.VISIBLE

        rvPrivatePhotos.visibility = View.GONE
        tvPrivatePhotos.visibility = View.GONE

        btn_camera.setOnClickListener {
            takePhoto.launch()
        }

        switch_private.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                rvPrivatePhotos.setHasFixedSize(true)
                rvPrivatePhotos.layoutManager = GridLayoutManager(this,2)
                val adapter = InternalAdapter(this,getInternalPhotos())
                rvPrivatePhotos.adapter = adapter

                rvPublicPhotos.visibility = View.GONE
                tvAllPhotos.visibility = View.GONE

                rvPrivatePhotos.visibility = View.VISIBLE
                tvPrivatePhotos.visibility = View.VISIBLE

                isInternal = true
            }else{
                rvPublicPhotos.setHasFixedSize(true)
                rvPublicPhotos.layoutManager = GridLayoutManager(this,2)
                val adapterEx = ExternalAdapter(this,loadPhotosFromExternalStorage())
                rvPublicPhotos.adapter = adapterEx

                rvPublicPhotos.visibility = View.VISIBLE
                tvAllPhotos.visibility = View.VISIBLE

                rvPrivatePhotos.visibility = View.GONE
                tvPrivatePhotos.visibility = View.GONE
                isInternal = false
            }
        }


    }

    private fun requestPermissions() {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || minSdk29
        locationPermissionGranted = hasLocationPermission
        cameraPermissionGranted = hasCameraPermission

        val permissionsToRequest = mutableListOf<String>()
        if (!readPermissionGranted)
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (!writePermissionGranted)
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!locationPermissionGranted)
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!cameraPermissionGranted)
            permissionsToRequest.add(Manifest.permission.CAMERA)


        if (permissionsToRequest.isNotEmpty())
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            readPermissionGranted =
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
            writePermissionGranted =
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted

            locationPermissionGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: locationPermissionGranted
            cameraPermissionGranted =
                permissions[Manifest.permission.CAMERA] ?: cameraPermissionGranted
        }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->

        val filename = UUID.randomUUID().toString()

        val isPhotoSaved = if (isInternal) {
            savePhotoToInternalStorage(filename, bitmap!!)
        } else {
            if (writePermissionGranted) {
                savePhotoToExternalStorage(filename, bitmap!!)
            } else {
                false
            }
        }
        if (isPhotoSaved) {
            fireToast(this, "Photo saved successfully")
        } else {
            fireToast(this, "Failed to save photo")
        }
    }

    private fun savePhotoToInternalStorage(filename: String, bmp: Bitmap): Boolean {
        return try {
            openFileOutput("$filename.jpg", MODE_PRIVATE).use { stream ->
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Couldn't save bitmap.")
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun savePhotoToExternalStorage(filename: String, bmp: Bitmap): Boolean {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }
        return try {
            contentResolver.insert(collection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }


    fun loadPhotosFromExternalStorage(): List<Uri> {

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )
        val photos = mutableListOf<Uri>()
        return contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                photos.add(contentUri)
            }
            photos.toList()
        } ?: listOf()
    }

    fun loadPhotosFromInternalStorage(): List<Bitmap> {
        val files = filesDir.listFiles()
        return files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") }?.map {
            val bytes = it.readBytes()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bmp
        } ?: listOf()
    }
    fun getInternalPhotos():ArrayList<InternalPhotos>{
        val photos = loadPhotosFromInternalStorage()
        val internalPhotos = ArrayList<InternalPhotos>()
        for (photo in photos){
            internalPhotos.add(InternalPhotos("",photo))
        }
        return internalPhotos
    }


    /*//Internal & External Paths
    fun checkStoragePaths() {
        val internal_m1 = getDir("custom", 0)
        val internal_m2 = filesDir

        val external_m1 = getExternalFilesDir(null)
        val external_m2 = externalCacheDir
        val external_m3 = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        Log.d("StorageActivity ", internal_m1.absolutePath)
        Log.d("StorageActivity ", internal_m2.absolutePath)
        Log.d("StorageActivity", external_m1!!.absolutePath)
        Log.d("StorageActivity ", external_m2!!.absolutePath)
        Log.d("StorageActivity ", external_m3!!.absolutePath)
    }

    //Internal Storages

    private fun createInternalFile() {
        val fileName = "pdp_internal.txt"
        val file: File
        file = if (isPersistent) {
            File(filesDir, fileName)
        } else {
            File(cacheDir, fileName)
        }
        if (!file.exists()) {
            try {
                file.createNewFile()
                fireToast(
                    this, String.format
                        ("File %s has been created", fileName)
                )
            } catch (e: IOException) {
                fireToast(
                    this, String.format
                        ("File %s creation failed", fileName)
                )
            }
        } else {
            Utils.fireToast(
                this, String.format
                    ("File %s already exists", fileName)
            )
        }
    }

    private fun saveInternalFile(data: String) {
        val fileName = "pdp_internal.txt"
        try {
            val fileOutputStream: FileOutputStream
            fileOutputStream = if (isPersistent) {
                openFileOutput(fileName, MODE_PRIVATE)
            } else {
                val file = File(cacheDir, fileName)
                FileOutputStream(file)
            }
            fileOutputStream.write(data.toByteArray(Charset.forName("UTF-8")))
            Utils.fireToast(
                this, String.format
                    ("Write to %s successful", fileName)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Utils.fireToast(
                this, String.format
                    ("Write to file %s failed", fileName)
            )
        }
    }

    private fun readInternalFile() {
        val fileName = "pdp_internal.txt"
        try {
            val fileInputStream: FileInputStream
            fileInputStream = if (isPersistent) {
                openFileInput(fileName)
            } else {
                val file = File(cacheDir, fileName)
                FileInputStream(file)
            }
            val inputStreamReader = InputStreamReader(
                fileInputStream,
                Charset.forName("UTF-8")
            )
            val lines: MutableList<String?> = ArrayList()
            val reader = BufferedReader(inputStreamReader)
            var line = reader.readLine()
            while (line != null) {
                lines.add(line)
                line = reader.readLine()
            }
            val readText = TextUtils.join("\n", lines)
            Utils.fireToast(
                this, String.format
                    ("Read from file %s successful", fileName)
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Utils.fireToast(
                this, String.format
                    ("Read from file %s failed", fileName)
            )
        }
    }

    private fun deleteInternalFile() {
        val fileName = "internal.txt"
        val file: File = if (isPersistent) {
            File(filesDir, fileName)
        } else {
            File(cacheDir, fileName)
        }
        if (file.exists()) {
            file.delete()
            fireToast(    this, String.format("File %s has been deleted", fileName))
        } else {
            fireToast(    this, String.format("File %s doesn't exist", fileName))
        }
    }

    //External Storages

    private fun createExternalFile() {
        val fileName = "pdp_external.txt"
        val file: File
        file = if (isPersistent) {
            File(getExternalFilesDir(null), fileName)
        } else {
            File(externalCacheDir, fileName)
        }
        Log.d("@@@", "absolutePath: " + file.absolutePath)
        if (!file.exists()) {
            try {
                file.createNewFile()
                Utils.fireToast(
                    this, String.format
                        ("File %s has been created", fileName)
                )
            } catch (e: IOException) {
                Utils.fireToast(
                    this, String.format
                        ("File %s creation failed", fileName)
                )
            }
        } else {
            Utils.fireToast(
                this, String.format
                    ("File %s already exists", fileName)
            )
        }
    }

    private fun saveExternalFile(data: String) {
        val fileName = "pdp_external.txt"

        val file: File
        file = if (isPersistent) {
            File(getExternalFilesDir(null), fileName)
        } else {
            File(externalCacheDir, fileName)
        }
        Log.d("@@@", "absolutePath: " + file.absolutePath)
        try {
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(data.toByteArray(Charset.forName("UTF-8")))
            Utils.fireToast(
                this, String.format
                    ("Write to %s successful", fileName)
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Utils.fireToast(
                this, String.format
                    ("Write to file %s failed", fileName)
            )
        }
    }

    private fun readExternalFile() {
        val fileName = "pdp_external.txt"
        val file: File
        file = if (isPersistent)
            File(getExternalFilesDir(null), fileName)
        else
            File(externalCacheDir, fileName)

        Log.d("@@@", "absolutePath: " + file.absolutePath)

        try {
            val fileInputStream = FileInputStream(file)
            val inputStreamReader = InputStreamReader(fileInputStream, Charset.forName("UTF-8"))
            val lines: MutableList<String?> = java.util.ArrayList()
            val reader = BufferedReader(inputStreamReader)
            var line = reader.readLine()
            while (line != null) {
                lines.add(line)
                line = reader.readLine()
            }
            val readText = TextUtils.join("\n", lines)
            Log.d("StorageActivity", readText)
            Utils.fireToast(this, String.format("Read from file %s successful", fileName))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Utils.fireToast(this, String.format("Read from file %s failed", fileName))
        }
    }

    private fun deleteExternalFile() {
        val fileName = "pdp_external.txt"
        val file: File
        file = if (isPersistent) {
            File(getExternalFilesDir(null), fileName)
        } else {
            File(externalCacheDir, fileName)
        }
        if (file.exists()) {
            file.delete()
            fireToast(this, String.format("File %s has been deleted", fileName))
        } else {
            fireToast(this, String.format("File %s doesn't exist", fileName))
        }
    }*/


}