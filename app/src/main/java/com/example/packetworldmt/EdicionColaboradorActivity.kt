package com.example.packetworldmt

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.packetworldmt.databinding.ActivityEdicionConductorBinding
import com.example.packetworldmt.dto.RSAutenticacionColaborador
import com.example.packetworldmt.dto.Respuesta
import com.example.packetworldmt.poko.Colaborador
import com.example.packetworldmt.poko.Sucursal
import com.example.packetworldmt.util.Constantes
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.util.Base64

class EdicionColaboradorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEdicionConductorBinding
    private lateinit var colaborador: Colaborador
    private var fotoPerfilBytes: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEdicionConductorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarColaborador()

        binding.btnActualizar.setOnClickListener {
            validarYEditar()
        }

        binding.imgFotoPerfil.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            seleccionarFotoPerfil.launch(intent)
        }

    }

    private fun cargarColaborador() {
        val json = intent.getStringExtra("colaborador")
        if (json.isNullOrEmpty()) {
            Toast.makeText(this, "No se recibió el colaborador", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            val rs = Gson().fromJson(json, RSAutenticacionColaborador::class.java)
            if (rs.error || rs.colaborador == null) {
                Toast.makeText(this,rs.mensaje ?: "Sesión inválida", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            colaborador = rs.colaborador!!
            descargarFotoColaborador()

            binding.etNumeroPersonal.setText(colaborador.numeroPersonal ?: "")
            binding.etRol.setText(colaborador.rol ?: "")

            binding.etSucursal.setText("Cargando...")
            val idSucursal = colaborador.idSucursal ?: 0
            if (idSucursal > 0) {
                cargarNombreSucursal(idSucursal)
            } else {
                binding.etSucursal.setText("N/D")
            }

            binding.etNombre.setText(colaborador.nombre ?: "")
            binding.etApellidoPaterno.setText(colaborador.apellidoPaterno ?: "")
            binding.etApellidoMaterno.setText(colaborador.apellidoMaterno ?: "")
            binding.etCorreo.setText(colaborador.correo ?: "")
            binding.etCurp.setText(colaborador.curp ?: "")
            binding.etNumeroLicencia.setText(colaborador.numeroLicencia ?: "")

        } catch (e: Exception) {
            Toast.makeText(this,"Error al leer el colaborador", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun validarYEditar() {

        val nombre = binding.etNombre.text.toString().trim().replace(Regex("\\s+"), " ")
        val apPaterno = binding.etApellidoPaterno.text.toString().trim().replace(Regex("\\s+"), " ")
        val apMaterno = binding.etApellidoMaterno.text.toString().trim().replace(Regex("\\s+"), " ")
        val correo = binding.etCorreo.text.toString().trim()
        val licencia = binding.etNumeroLicencia.text.toString().trim()

        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_LONG).show()
            binding.etNombre.requestFocus()
            return
        }

        if (!esNombreValido(nombre)) {
            Toast.makeText(this, "Nombre inválido: no debe llevar números ni símbolos", Toast.LENGTH_LONG).show()
            binding.etNombre.requestFocus()
            return
        }

        if (apPaterno.isEmpty()) {
            Toast.makeText(this, "El apellido paterno es obligatorio", Toast.LENGTH_LONG).show()
            binding.etApellidoPaterno.requestFocus()
            return
        }

        if (!esNombreValido(apPaterno)) {
            Toast.makeText(this, "Apellido paterno inválido: no debe llevar números ni símbolos", Toast.LENGTH_LONG).show()
            binding.etApellidoPaterno.requestFocus()
            return
        }

        if (apMaterno.isNotEmpty() && !esNombreValido(apMaterno)) {
            Toast.makeText(this, "Apellido materno inválido: no debe llevar números ni símbolos", Toast.LENGTH_LONG).show()
            binding.etApellidoMaterno.requestFocus()
            return
        }

        if (correo.isEmpty()) {
            Toast.makeText(this, "El correo no puede estar vacío", Toast.LENGTH_LONG).show()
            binding.etCorreo.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Correo electrónico inválido", Toast.LENGTH_LONG).show()
            binding.etCorreo.requestFocus()
            return
        }

        if (licencia.isEmpty()) {
            Toast.makeText(this, "Debe ingresar un número de licencia", Toast.LENGTH_LONG).show()
            binding.etNumeroLicencia.requestFocus()
            return
        }

        colaborador.nombre = nombre
        colaborador.apellidoPaterno = apPaterno
        colaborador.apellidoMaterno = apMaterno
        colaborador.correo = correo
        colaborador.numeroLicencia = licencia

        validarLicenciaAPI(licencia)
    }

    private fun esNombreValido(texto: String): Boolean {
        val regex = Regex("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]+$")
        if (texto.length !in 2..50) return false
        if (!regex.matches(texto)) return false
        if (texto.contains(Regex("\\s{2,}"))) return false
        return true
    }

    private fun validarLicenciaAPI(licencia: String) {
        val idColab = colaborador.idColaborador ?: 0

        Ion.with(this)
            .load("GET", "${Constantes().URL_API}colaborador/licencia-disponible")
            .addQuery("licencia", licencia)
            .addQuery("idColaborador", idColab.toString())
            .asString()
            .setCallback { e, result ->
                if (e != null || result.isNullOrEmpty()) {
                    Toast.makeText(this, "No se pudo validar la licencia", Toast.LENGTH_LONG).show()
                    return@setCallback
                }

                try {
                    val resp = Gson().fromJson(result, Respuesta::class.java)
                    if (resp.error) {
                        Toast.makeText(this, resp.mensaje ?: "Licencia no disponible", Toast.LENGTH_LONG).show()
                        binding.etNumeroLicencia.requestFocus()
                    } else {
                        editarColaboradorApi()
                    }
                } catch (ex: Exception) {
                    Toast.makeText(this, "Respuesta inválida del servidor", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun editarColaboradorApi() {
        val jsonBody = Gson().toJson(colaborador)

        Ion.with(this)
            .load("PUT", "${Constantes().URL_API}colaborador/editar")
            .setHeader("Content-Type", "application/json")
            .setStringBody(jsonBody)
            .asString(Charsets.UTF_8)
            .setCallback { e, result ->
                if (e != null) {
                    Toast.makeText(this, "Error al conectar con el servidor", Toast.LENGTH_LONG).show()
                    return@setCallback
                }

                if (result.isNullOrEmpty()) {
                    Toast.makeText(this, "Respuesta vacía del servidor", Toast.LENGTH_LONG).show()
                    return@setCallback
                }

                try {
                    val resp = Gson().fromJson(result, Respuesta::class.java)
                    Toast.makeText(this, resp.mensaje, Toast.LENGTH_LONG).show()
                    if (!resp.error) finish()
                } catch (_: Exception) {
                    Toast.makeText(this, "No se pudo leer la respuesta", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun cargarNombreSucursal(idSucursal: Int) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)

        Ion.with(this)
            .load("GET", "${Constantes().URL_API}sucursal/obtener-por-id/$idSucursal")
            .asString()
            .setCallback { e, result ->
                if (e != null || result.isNullOrEmpty()) {
                    binding.etSucursal.setText("No disponible")
                    return@setCallback
                }

                try {
                    val sucursal = Gson().fromJson(result, Sucursal::class.java)
                    val nombre = sucursal.nombreCorto ?: "N/D"
                    binding.etSucursal.setText(nombre)
                } catch (_: Exception) {
                    binding.etSucursal.setText("No disponible")
                }
            }
    }

    private fun descargarFotoColaborador() {
        val idColaborador = colaborador.idColaborador ?: 0
        if (idColaborador <= 0) return

        Ion.with(this)
            .load("GET", "${Constantes().URL_API}colaborador/obtener-foto/$idColaborador")
            .asString()
            .setCallback { e, result ->
                if (e == null && !result.isNullOrEmpty()) {
                    cargarFotoPerfilAPI(result)
                } else {
                    Toast.makeText(this, "No se pudo descargar la foto", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun cargarFotoPerfilAPI(json: String) {
        try {
            val gson = Gson()
            val colab = gson.fromJson(json, Colaborador::class.java)

            if (colab.fotoBase64 != null) {
                val imgBytes = Base64.decode(colab.fotoBase64, Base64.DEFAULT)
                val imgBitMap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                binding.imgFotoPerfil.setImageBitmap(imgBitMap)
            } else {
                binding.imgFotoPerfil.setImageResource(R.drawable.ic_person)
            }
        } catch (e: Exception) {

            Toast.makeText(this, "Error al cargar foto", Toast.LENGTH_LONG).show()
        }
    }

    private val seleccionarFotoPerfil =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val imgURI = data?.data
                if (imgURI != null) {
                    fotoPerfilBytes = uriToByteArray(imgURI)
                    if (fotoPerfilBytes != null) {
                        subirFotoColaborador()
                    } else {
                        Toast.makeText(this, "No se pudo leer la imagen", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Selección de foto cancelada", Toast.LENGTH_LONG).show()
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

    private fun subirFotoColaborador() {
        val idColaborador = colaborador.idColaborador ?: 0
        if (idColaborador <= 0) {
            Toast.makeText(this, "ID de colaborador inválido", Toast.LENGTH_LONG).show()
            return
        }

        if (fotoPerfilBytes == null) {
            Toast.makeText(this, "No hay foto para enviar", Toast.LENGTH_LONG).show()
            return
        }

        Ion.with(this)
            .load("PUT", "${Constantes().URL_API}colaborador/guardar-foto/$idColaborador")
            .setByteArrayBody(fotoPerfilBytes!!)
            .asString()
            .setCallback { e, result ->
                if (e != null) {
                    Toast.makeText(this, "Error al enviar foto: ${e.message}", Toast.LENGTH_LONG).show()
                    return@setCallback
                }

                if (result.isNullOrEmpty()) {
                    Toast.makeText(this, "Respuesta vacía del servidor", Toast.LENGTH_LONG).show()
                    return@setCallback
                }

                verificarEnvioFoto(result)
            }

    }

    private fun verificarEnvioFoto(result: String) {
        val body = result.trim()

        if (!body.startsWith("{")) {
            Toast.makeText(this, body, Toast.LENGTH_LONG).show()
            descargarFotoColaborador()
            return
        }

        try {
            val respuesta = Gson().fromJson(body, Respuesta::class.java)
            Toast.makeText(this, respuesta.mensaje ?: "Foto subida", Toast.LENGTH_LONG).show()

            if (!respuesta.error) {
                descargarFotoColaborador()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Respuesta inválida: ${body.take(120)}", Toast.LENGTH_LONG).show()
        }
    }


}
