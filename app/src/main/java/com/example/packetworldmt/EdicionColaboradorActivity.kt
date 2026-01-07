package com.example.packetworldmt

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.packetworldmt.databinding.ActivityEdicionConductorBinding
import com.example.packetworldmt.dto.RSAutenticacionColaborador
import com.example.packetworldmt.dto.Respuesta
import com.example.packetworldmt.poko.Colaborador
import com.example.packetworldmt.poko.Sucursal
import com.example.packetworldmt.util.Constantes
import com.google.gson.Gson
import com.koushikdutta.ion.Ion

class EdicionColaboradorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEdicionConductorBinding
    private lateinit var colaborador: Colaborador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEdicionConductorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarColaborador()

        binding.btnActualizar.setOnClickListener {
            validarYEditar()
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

        val nombre = binding.etNombre.text.toString().trim()
        val apPaterno = binding.etApellidoPaterno.text.toString().trim()
        val apMaterno = binding.etApellidoMaterno.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val curp = binding.etCurp.text.toString().trim().uppercase()
        val licencia = binding.etNumeroLicencia.text.toString().trim()

        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_LONG).show()
            binding.etNombre.requestFocus()
            return
        }

        if (apPaterno.isEmpty()) {
            Toast.makeText(this, "El apellido paterno es obligatorio", Toast.LENGTH_LONG).show()
            binding.etApellidoPaterno.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Correo electrónico inválido", Toast.LENGTH_LONG).show()
            binding.etCorreo.requestFocus()
            return
        }

        if (!esCurpValida(curp)) {
            Toast.makeText( this,"La CURP debe tener 18 caracteres y un formato válido", Toast.LENGTH_LONG).show()
            binding.etCurp.requestFocus()
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
        colaborador.curp = curp
        colaborador.numeroLicencia = licencia

        editarColaboradorApi()
    }

    private fun esCurpValida(curp: String): Boolean {
        val regex = Regex("^[A-Z]{4}\\d{6}[A-Z]{6}[A-Z0-9]{2}$")
        return curp.length == 18 && regex.matches(curp)
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
                    val nombre = sucursal.nombreCorto ?: sucursal.nombreCorto ?: "N/D"
                    binding.etSucursal.setText(nombre)
                } catch (_: Exception) {
                    binding.etSucursal.setText("No disponible")
                }
            }
    }

}
