package com.example.packetworldmt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.packetworldmt.databinding.ActivityMainBinding
import com.example.packetworldmt.dto.RSAutenticacionColaborador
import com.example.packetworldmt.poko.Colaborador
import com.example.packetworldmt.poko.Envio
import com.example.packetworldmt.util.Constantes
import com.google.gson.Gson
import com.koushikdutta.ion.Ion

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var colaborador: Colaborador
    private lateinit var adapter: EnvioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarColaboradorDesdeIntent()

        adapter = EnvioAdapter(this, emptyList()) { envio ->
            val intent = Intent(this, DetalleEnvioActivity::class.java)
            intent.putExtra("envio", Gson().toJson(envio))
            startActivity(intent)
        }

        binding.rvEnvios.layoutManager = LinearLayoutManager(this)
        binding.rvEnvios.adapter = adapter

        binding.fabPerfil.setOnClickListener {
            val intent = Intent(this, EdicionColaboradorActivity::class.java)
            intent.putExtra("colaborador", Gson().toJson(
                RSAutenticacionColaborador(
                    error = false,
                    mensaje = "OK",
                    colaborador = colaborador
                )
            ))
            startActivity(intent)
        }

        val idConductor = colaborador.idColaborador ?: 0
        if (idConductor > 0) cargarEnvios(idConductor) else Toast.makeText(this, "No se pudo obtener id conductor", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        val idConductor = colaborador.idColaborador ?: 0
        if (idConductor > 0) cargarEnvios(idConductor)
    }

    private fun cargarColaboradorDesdeIntent() {
        try {
            val jsonColaborador = intent.getStringExtra("colaborador")
            if (jsonColaborador.isNullOrEmpty()) {
                Toast.makeText(this, "No se recibió información del colaborador", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val respuesta = Gson().fromJson(jsonColaborador, RSAutenticacionColaborador::class.java)
            if (respuesta.error || respuesta.colaborador == null) {
                Toast.makeText(this, respuesta.mensaje ?: "No fue posible validar la sesión", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            colaborador = respuesta.colaborador!!

            val nombreCompleto = buildString {
                append(colaborador.nombre)
                if (!colaborador.apellidoPaterno.isNullOrEmpty()) append(" ").append(colaborador.apellidoPaterno)
                if (!colaborador.apellidoMaterno.isNullOrEmpty()) append(" ").append(colaborador.apellidoMaterno)
            }
            binding.tvBienvenida.text = "Bienvenido(a), $nombreCompleto"

        } catch (_: Exception) {
            Toast.makeText(this, "Error al cargar sesión", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun cargarEnvios(idConductor: Int) {
        Ion.with(this)
            .load("GET", "${Constantes().URL_API}envio/obtener-por-conductor/$idConductor")
            .asString()
            .setCallback { e, result ->

                if (e != null) {
                    Toast.makeText(this, "Error al conectar con el servidor", Toast.LENGTH_LONG).show()
                    mostrarSinEnvios()
                    return@setCallback
                }

                if (result.isNullOrEmpty()) {
                    mostrarSinEnvios()
                    return@setCallback
                }

                try {
                    val lista = Gson().fromJson(result, Array<Envio>::class.java).toList()
                    if (lista.isEmpty()) {
                        mostrarSinEnvios()
                    } else {
                        binding.rvEnvios.visibility = View.VISIBLE
                        binding.tvSubtitulo.text = "Toca un envío para ver detalle y actualizar estatus."
                        adapter.actualizarLista(lista)
                    }
                } catch (_: Exception) {
                    Toast.makeText(this, "Error al procesar envíos", Toast.LENGTH_LONG).show()
                    mostrarSinEnvios()
                }
            }
    }

    private fun mostrarSinEnvios() {
        binding.rvEnvios.visibility = View.GONE
        binding.tvSubtitulo.text = "No tienes envíos asignados."
    }
}
