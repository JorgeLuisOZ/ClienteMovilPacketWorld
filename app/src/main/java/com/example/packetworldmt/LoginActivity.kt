package com.example.packetworldmt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.packetworld.databinding.ActivityLoginBinding
import com.example.packetworldmt.dto.RSAutenticacionColaborador
import com.example.packetworldmt.util.Constantes
import com.google.gson.Gson
import com.koushikdutta.ion.Ion

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIngresar.setOnClickListener {
            verificarCredenciales()
        }
    }

    private fun verificarCredenciales() {
        if (sonCamposValidos()) {
            consumirAPI(
                binding.etNumeroPersonal.text.toString().trim(),
                binding.etPassword.text.toString().trim()
            )
        }
    }

    private fun sonCamposValidos(): Boolean {
        var valido = true

        if (binding.etNumeroPersonal.text.isNullOrEmpty()) {
            binding.tilNumeroPersonal.error = "Número de personal obligatorio"
            valido = false
        } else {
            binding.tilNumeroPersonal.error = null
        }

        if (binding.etPassword.text.isNullOrEmpty()) {
            binding.tilPassword.error = "Contraseña obligatoria"
            valido = false
        } else {
            binding.tilPassword.error = null
        }

        return valido
    }

    private fun consumirAPI(numeroPersonal: String, contrasena: String) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)

        Ion.with(this)
            .load("POST", "${Constantes().URL_API}autentificar/conductor")
            .setHeader(
                Constantes().HEADERNAMEFORM,
                Constantes().HEADERVALUEFORM
            )

            .setBodyParameter("numero_personal", numeroPersonal)
            .setBodyParameter("contrasena", contrasena)
            .asString()
            .setCallback { e, result ->
                if (e == null && result != null) {
                    serializarRespuesta(result)
                } else {
                    Toast.makeText(
                        this,
                        "Error de conexión con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("LOGIN_WS", e?.message ?: "Error desconocido")
                }
            }
    }

    private fun serializarRespuesta(json: String) {
        Log.d("LOGIN_WS", json)

        try {
            val gson = Gson()
            val respuesta =
                gson.fromJson(json, RSAutenticacionColaborador::class.java)

            if (!respuesta.error) {
                Toast.makeText(
                    this,
                    "Bienvenido(a) ${respuesta.colaborador?.nombre}",
                    Toast.LENGTH_LONG
                ).show()
                irPantallaPrincipal(json)
            } else {
                Toast.makeText(
                    this,
                    respuesta.mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al procesar la respuesta del servidor",
                Toast.LENGTH_LONG
            ).show()
            Log.e("LOGIN_WS", e.message ?: "Error parseando JSON")
        }
    }

    private fun irPantallaPrincipal(json: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("colaborador", json)
        startActivity(intent)
        finish()
    }
}
