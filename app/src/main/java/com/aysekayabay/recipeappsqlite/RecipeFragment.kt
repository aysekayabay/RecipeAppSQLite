package com.aysekayabay.recipeappsqlite

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import java.io.ByteArrayOutputStream
//jetpack room fw is better
class RecipeFragment : Fragment() {
    lateinit var  saveButton : Button
    lateinit var mealImageView : ImageView
    lateinit var mealNameField : EditText
    lateinit var mealDetailField : EditText
    var selectedImage : Uri? = null
    var selectedBitmap : Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        saveButton = view.findViewById(R.id.saveDetailButton)
        mealImageView = view.findViewById(R.id.addMealImageView)
        mealNameField = view.findViewById(R.id.mealNameField)
        mealDetailField = view.findViewById(R.id.mealDetailField)
        saveButton.setOnClickListener {
            saveDetail(it)
        }
        mealImageView.setOnClickListener {
            selectImage(it)
        }
        arguments.let {
            var isForAdding = RecipeFragmentArgs.fromBundle(it).isForAdding
            if (isForAdding){
                mealNameField.setText("")
                mealDetailField.setText("")
                saveButton.visibility= View.VISIBLE
                val selectionImageBg = BitmapFactory.decodeResource(context?.resources, R.drawable.add_photo)
                mealImageView.setImageBitmap(selectionImageBg)
            }
            else{
                saveButton.visibility= View.INVISIBLE
                val selectedId = RecipeFragmentArgs.fromBundle(it).id
                context?.let {
                    try {
                        val db = it.openOrCreateDatabase("Meals", Context.MODE_PRIVATE, null)
                        val cursor  = db.rawQuery("SELECT * FROM meals WHERE id = ?", arrayOf(selectedId.toString()))
                        val mealNameIndex = cursor.getColumnIndex("mealName")
                        val mealDetailIndex =  cursor.getColumnIndex("mealDetail")
                        val mealImage = cursor.getColumnIndex("image")

                        while (cursor.moveToNext()){
                            mealNameField.setText(cursor.getString(mealNameIndex))
                            mealDetailField.setText(cursor.getString(mealDetailIndex))
                            val byteArray = cursor.getBlob(mealImage)
                            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                            mealImageView.setImageBitmap(bitmap)
                        }
                        cursor.close()

                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }

            }
        }
    }


    fun saveDetail(view: View){
        var mealName = mealNameField.text.toString()
        var mealDetail = mealDetailField.text.toString()

        if (selectedBitmap != null){
            val smallBitmap = createSmallBitmap(selectedBitmap!!, 300)
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                context?.let {
                    val database = it.openOrCreateDatabase("Meals", Context.MODE_PRIVATE, null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS meals (id INTEGER PRIMARY KEY, mealName VARCHAR, mealDetail VARCHAR, image BLOB)")
                    val sqlString = "INSERT INTO meals (mealName, mealDetail, image) VALUES (?, ?, ?)"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1, mealName)
                    statement.bindString(2, mealDetail)
                    statement.bindBlob(3, byteArray)
                    statement.execute()
                }
            }catch (e:Exception){
                e.printStackTrace()
            }

            val action = RecipeFragmentDirections.actionRecipeFragmentToListFragment()
            Navigation.findNavController(view).navigate(action)
        }
    }

    fun selectImage(view: View){
        activity?.let {
            if (ContextCompat.checkSelfPermission(it.applicationContext,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //not granted
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }
            else{
                //already granted
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent,2)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == 1){
            if (grantResults.size>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //granted now
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent,2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 2 && resultCode == Activity.RESULT_OK && data != null){
            selectedImage = data.data
            try {

                context?.let {
                    if (selectedImage != null ){
                        if (Build.VERSION.SDK_INT >= 28 ){
                           val source = ImageDecoder.createSource(it.contentResolver,selectedImage!!)
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
                            mealImageView.setImageBitmap(selectedBitmap)
                        }
                        else{
                            selectedBitmap = MediaStore.Images.Media.getBitmap(it.contentResolver, selectedImage)
                            mealImageView.setImageBitmap(selectedBitmap)
                        }
                    }
                }

            }catch (e : Exception){
                e.printStackTrace()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun createSmallBitmap(bitmap : Bitmap, maxSize: Int ) : Bitmap{
        var width = bitmap.width
        var height = bitmap.height
        var bitmapRate : Double = width.toDouble() / height.toDouble()
        if (bitmapRate>1){
            //horizontal
            width = maxSize
            val scaledHeight = width/bitmapRate
            height = scaledHeight.toInt()
        }
        else{
            //vertical
            height = maxSize
            val scaledWidth = height*bitmapRate
            width = scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(bitmap,width,height,true )
    }
}