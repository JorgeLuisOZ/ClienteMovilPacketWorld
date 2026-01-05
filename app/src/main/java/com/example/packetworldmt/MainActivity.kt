package com.example.packetworldmt

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.packetworld.databinding.ActivityMainBinding
import com.example.packetworldmt.dto.RSAutenticacionColaborador
import com.example.packetworldmt.poko.Colaborador
import com.example.packetworldmt.poko.Envio
import com.example.packetworldmt.util.Constantes
import com.google.gson.Gson
import com.koushikdutta.ion.Ion

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var colaborador: Colaborador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarColaboradorDesdeIntent()
        configurarRecycler()

        binding.fabPerfil.setOnClickListener {
            //irPerfilColaborador()
        }

        val idConductor = colaborador.idColaborador
        if (idConductor != null && idConductor > 0) {
            cargarEnvios(idConductor)
        } else {
            Toast.makeText(this, "No se pudo obtener el id del conductor", Toast.LENGTH_LONG).show()
        }
    }

    private fun cargarColaboradorDesdeIntent() {
        try {
            val jsonColaborador = intent.getStringExtra("colaborador")
            if (jsonColaborador.isNullOrEmpty()) {
                Toast.makeText(this, "No se recibió información del colaborador", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val gson = Gson()
            val respuesta = gson.fromJson(jsonColaborador, RSAutenticacionColaborador::class.java)

            if (respuesta.error || respuesta.colaborador == null) {
                Toast.makeText(this, respuesta.mensaje ?: "No fue posible validar la sesión", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            colaborador = respuesta.colaborador!!

            // Bienvenida grande
            val nombreCompleto = buildString {
                append(colaborador.nombre)
                if (!colaborador.apellidoPaterno.isNullOrEmpty()) append(" ").append(colaborador.apellidoPaterno)
                if (!colaborador.apellidoMaterno.isNullOrEmpty()) append(" ").append(colaborador.apellidoMaterno)
            }
            binding.tvBienvenida.text = "Bienvenido(a), $nombreCompleto"

        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar la sesión del colaborador", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun configurarRecycler() {
        binding.rvEnvios.layoutManager = LinearLayoutManager(this)

        // TODO: cuando tengas el modelo de Envio y su adapter, lo conectas aquí.
        // binding.rvEnvios.adapter = EnviosAdapter(listaEnvios) { envio ->
        //     abrirDetalleEnvio(envio)
        // }
    }

    private fun cargarEnvios(idConductor: Int) {

        Ion.getDefault(this).conscryptMiddleware.enable(false)

        Ion.with(this)
            .load(
                "GET",
                "${Constantes().URL_API}envio/consultar-por-conductor/$idConductor"
            )
            .asString()
            .setCallback { e, result ->

                if (e != null) {
                    Toast.makeText(
                        this,
                        "Error al conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setCallback
                }

                if (result.isNullOrEmpty()) {
                    Toast.makeText(
                        this,
                        "No se recibieron envíos",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setCallback
                }

                try {
                    val gson = Gson()

                    val listaEnvios: List<Envio> =
                        gson.fromJson(result, Array<Envio>::class.java).toList()

                    if (listaEnvios.isEmpty()) {
                        Toast.makeText(
                            this,
                            "No tienes envíos asignados",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        mostrarEnvios(listaEnvios)
                    }

                } catch (ex: Exception) {
                    Toast.makeText(
                        this,
                        "Error al procesar los envíos",
                        Toast.LENGTH_LONG
                    ).show()
                    ex.printStackTrace()
                }
            }
    }

    private fun mostrarEnvios(envios: List<Envio>) {
        binding.rvEnvios.layoutManager = LinearLayoutManager(this)
        /*binding.rvEnvios.adapter = EnvioAdapter(envios) { envio ->
            abrirDetalleEnvio(envio)
        }*/
    }


    /*private fun irPerfilColaborador() {
        val gson = Gson()
        val json = gson.toJson(colaborador)

        val intent = Intent(this, PerfilColaboradorActivity::class.java)
        intent.putExtra("colaborador", json)
        startActivity(intent)
    }*/

    // private fun abrirDetalleEnvio(envio: Envio) {
    //     val intent = Intent(this, DetalleEnvioActivity::class.java)
    //     intent.putExtra("envio", Gson().toJson(envio))
    //     startActivity(intent)
    // }
}
