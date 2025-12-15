package com.example.packetworldmt

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.packetworldmt.databinding.ActivityMainBinding
import com.example.packetworldmt.dto.RSAutenticacionAlumno
import com.example.packetworldmt.dto.Respuesta
import com.example.packetworldmt.util.Constantes
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import java.io.ByteArrayOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var alumno : Alumno
    private var fotoPerfilBytes : ByteArray ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        mostrarInformacionAlumno()
    }

    override fun onStart(){
        super.onStart()
        descargarFotoAlumno(idAlumno = alumno.idAlumno)
        binding.ivSeleccionFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            seleccionarFotoPerfil.launch(intent)
        }

        binding.ivEditarAlumno.setOnClickListener {
            val gson = Gson()
            val jsonAlumno : String = gson.toJson(alumno)
            val intent = Intent(this@MainActivity, EdicionAlumnoActivity::class.java)
            intent.putExtra("alumno", jsonAlumno)
            startActivity(intent)
        }
    }

    fun mostrarInformacionAlumno(){
        try{
            val jsonAlumno : String? = intent.getStringExtra("alumno")
            if(jsonAlumno != null){
                val gson = Gson()
                val respuestaLogin : RSAutenticacionAlumno = gson.fromJson(jsonAlumno,
                    RSAutenticacionAlumno::class.java)
                alumno = respuestaLogin.alumno!!
                binding.tvMatricula.text = alumno.matricula
                binding.tvNombre.text = "${alumno.nombre} ${alumno.apellidoPaterno} ${alumno.apellidoMaterno}"
                binding.tvCorreo.text = alumno.correo
                binding.tvFechaNacimiento.text = alumno.fechaNacimiento
                binding.tvCarrera.text = alumno.carrera
                binding.tvFacultad.text = alumno.facultad
            }
        } catch (e : Exception){
            Toast.makeText(this@MainActivity, "Error al cargar la información del alumno(a)", Toast.LENGTH_LONG).show()
        }
    }

    fun descargarFotoAlumno(idAlumno : Int){
        Ion.with(this@MainActivity).load("GET", "${Constantes().URL_API}alumno/obtener-foto/${idAlumno}").asString()
            .setCallback { e, result ->
            if(e == null){
                cargarFotoPerfilAPI(result)
            } else {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }

        }
    }

    fun cargarFotoPerfilAPI(json : String){
        try{
            if (json.isNotEmpty()){
                val gson = Gson()
                val alumno : Alumno = gson.fromJson(json, Alumno::class.java)
                if(alumno.fotoBase64 != null){
                    val imgBytes = Base64.decode(alumno.fotoBase64, Base64.DEFAULT)
                    val imgBitMap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                    binding.ivFotoPerfil.setImageBitmap(imgBitMap)
                } else {
                    Toast.makeText(this@MainActivity, "No cuentas con foto de perfil", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception){
            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
        }
    }

    fun subirFotoPerfil(){
        Ion.with(this@MainActivity).load("PUT", "${Constantes().URL_API}alumno/subir-foto/${alumno.idAlumno}")
            .setByteArrayBody(fotoPerfilBytes).asString().setCallback { e, result ->
                if (e == null){
                    verificarEnvioFoto(result)
                } else{
                    Toast.makeText(this@MainActivity, "Error al enviar foto", Toast.LENGTH_LONG).show()
                }
            }
    }

    fun verificarEnvioFoto(result : String){
        try{
            val gson = Gson()
            val respuesta = gson.fromJson(result, Respuesta::class.java)
            if(!respuesta.error){
                Toast.makeText(this@MainActivity, respuesta.mensaje, Toast.LENGTH_LONG).show()
                descargarFotoAlumno(alumno.idAlumno)
            }
        } catch (e : Exception){
            e.printStackTrace()
            android.util.Log.d("UPLOAD_ERROR", "Error: ${e.message}")
            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // IMPLEMENTACIÓN DE SELECCIÓN DE FOTO
    private val seleccionarFotoPerfil = this.registerForActivityResult ( ActivityResultContracts.StartActivityForResult() ){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            val data = result.data
            val imgURI = data?.data
            if(imgURI != null){
                fotoPerfilBytes = uriToByteArray(uri = imgURI)
                if(fotoPerfilBytes != null){
                    //ENVIAR LA FOTO AL SERVICIO
                    subirFotoPerfil()
                }
            }
        } else {
            Toast.makeText(this@MainActivity, "Selección de foto cancelada ", Toast.LENGTH_LONG).show()
        }
    }

    private fun uriToByteArray(uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}