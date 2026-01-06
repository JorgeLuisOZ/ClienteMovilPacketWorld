package com.example.packetworldmt

import android.app.AlertDialog
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.packetworldmt.databinding.ActivityDetalleEnvioBinding
import com.example.packetworldmt.poko.Cliente
import com.example.packetworldmt.poko.Destinatario
import com.example.packetworldmt.poko.Envio
import com.example.packetworldmt.poko.Paquete
import com.example.packetworldmt.poko.Sucursal
import com.example.packetworldmt.util.Constantes
import com.google.gson.Gson
import com.koushikdutta.ion.Ion

class DetalleEnvioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleEnvioBinding
    private lateinit var envio: Envio

    private val ESTATUS_TRANSITO = 3
    private val ESTATUS_DETENIDO = 4
    private val ESTATUS_ENTREGADO = 5
    private val ESTATUS_CANCELADO = 6


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleEnvioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jsonEnvio = intent.getStringExtra("envio")
        if (jsonEnvio.isNullOrEmpty()) {
            Toast.makeText(this, "No se recibió el envío", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        envio = Gson().fromJson(jsonEnvio, Envio::class.java)
        pintarBaseEnvio(envio)

        envio.idSucursal?.takeIf { it > 0 }?.let { cargarSucursal(it) }
        envio.idCliente?.takeIf { it > 0 }?.let { cargarCliente(it) }
        envio.idDestinatario?.takeIf { it > 0 }?.let { cargarDestinatario(it) }
        envio.idEnvio?.takeIf { it > 0 }?.let { cargarPaquetesPorEnvio(it) }
    }

    private fun pintarBaseEnvio(envio: Envio) {
        binding.tvGuiaDetalle.text = "Guía: ${envio.numeroGuia ?: "N/D"}"
        binding.tvEstatusDetalle.text = "${envio.estatusActual ?: "N/D"}"

        binding.tvSucursalDetalle.text = "${envio.codigoSucursal ?: "Cargando..."}"

        val nombreDest = listOfNotNull(envio.nombreDestinatario, envio.apellidoPaternoDestinatario)
            .joinToString(" ").trim().ifBlank { "N/D" }
        binding.tvDestinatarioDetalle.text = "Destinatario: $nombreDest"

        val nombreCli = listOfNotNull(envio.nombreCliente, envio.apellidoPaternoCliente)
            .joinToString(" ").trim().ifBlank { "N/D" }
        binding.tvClienteDetalle.text = "Cliente: $nombreCli"

        binding.tvDireccionDetalle.text = "Dirección destino: Cargando..."
        binding.tvContactoDetalle.text = "Contacto: Cargando..."

        binding.llPaquetes.removeAllViews()
        binding.llPaquetes.addView(TextView(this).apply {
            text = "Cargando paquetes..."
            textSize = 14f
        })

        binding.btnActualizarEstatus.setOnClickListener {
            mostrarDialogoActualizarEstatus()
        }

        val idActual = envio.idEstatusActual ?: 0
        if (idActual == ESTATUS_ENTREGADO || idActual == ESTATUS_CANCELADO) {
            binding.btnActualizarEstatus.isEnabled = false
            binding.btnActualizarEstatus.text = "Estatus final"
        } else {
            binding.btnActualizarEstatus.isEnabled = true
            binding.btnActualizarEstatus.text = "Actualizar estatus"
        }
    }

    private fun cargarSucursal(idSucursal: Int) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)

        Ion.with(this)
            .load("GET", "${Constantes().URL_API}sucursal/obtener-por-id/$idSucursal")
            .asString()
            .setCallback { e, result ->
                if (e != null || result.isNullOrEmpty()) return@setCallback
                try {
                    val sucursal = Gson().fromJson(result, Sucursal::class.java)
                    binding.tvSucursalDetalle.text = "Sucursal origen:\n${formatearSucursal(sucursal)}"
                } catch (_: Exception) { }
            }
    }

    private fun cargarCliente(idCliente: Int) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)

        Ion.with(this)
            .load("GET", "${Constantes().URL_API}cliente/obtener-por-id/$idCliente")
            .asString()
            .setCallback { e, result ->
                if (e != null || result.isNullOrEmpty()) {
                    binding.tvContactoDetalle.text = "Contacto: No disponible"
                    return@setCallback
                }
                try {
                    val cliente = Gson().fromJson(result, Cliente::class.java)
                    binding.tvClienteDetalle.text = "Cliente: ${formatearNombreCliente(cliente)}"
                    binding.tvContactoDetalle.text = "Contacto: ${formatearContactoCliente(cliente)}"
                } catch (_: Exception) {
                    binding.tvContactoDetalle.text = "Contacto: No disponible"
                }
            }
    }

    private fun cargarDestinatario(idDestinatario: Int) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)

        Ion.with(this)
            .load("GET", "${Constantes().URL_API}destinatario/obtener-por-id/$idDestinatario")
            .asString()
            .setCallback { e, result ->
                if (e != null || result.isNullOrEmpty()) {
                    binding.tvDireccionDetalle.text = "Dirección destino: No disponible"
                    return@setCallback
                }
                try {
                    val d = Gson().fromJson(result, Destinatario::class.java)
                    // Aquí no uso nombre porque tu Envio ya trae nombre del destinatario
                    binding.tvDireccionDetalle.text = "Dirección destino:\n${formatearDireccionDestinatario(d)}"
                } catch (_: Exception) {
                    binding.tvDireccionDetalle.text = "Dirección destino: No disponible"
                }
            }
    }

    private fun cargarPaquetesPorEnvio(idEnvio: Int) {
        Ion.getDefault(this).conscryptMiddleware.enable(false)

        Ion.with(this)
            .load("GET", "${Constantes().URL_API}paquete/consultar-por-envio/$idEnvio")
            .asString()
            .setCallback { e, result ->
                if (e != null || result.isNullOrEmpty()) {
                    pintarPaquetes(emptyList())
                    return@setCallback
                }
                try {
                    val paquetes = Gson().fromJson(result, Array<Paquete>::class.java).toList()
                    pintarPaquetes(paquetes)
                } catch (_: Exception) {
                    pintarPaquetes(emptyList())
                }
            }
    }

    private fun pintarPaquetes(paquetes: List<Paquete>) {
        binding.llPaquetes.removeAllViews()

        if (paquetes.isEmpty()) {
            binding.llPaquetes.addView(TextView(this).apply {
                text = "Sin paquetes"
                textSize = 14f
            })
            return
        }

        paquetes.forEachIndexed { index, p ->
            val tv = TextView(this).apply {
                text = construirTextoPaquete(index + 1, p)
                textSize = 14f
                setPadding(0, 8, 0, 8)
            }
            binding.llPaquetes.addView(tv)
        }
    }

    private fun construirTextoPaquete(n: Int, p: Paquete): String {
        val desc = p.descripcion?.takeIf { it.isNotBlank() } ?: "Sin descripción"
        val peso = p.peso?.let { "${it} kg" } ?: "N/D"
        val dims = if (p.alto != null && p.ancho != null && p.profundidad != null) {
            "${p.alto} x ${p.ancho} x ${p.profundidad} cm"
        } else {
            "Dimensiones: N/D"
        }
        return "Paquete $n: $desc\nPeso: $peso\n$dims"
    }

    private fun formatearNombreCliente(c: Cliente): String {
        val nombre = listOfNotNull(c.nombre, c.apellidoPaterno, c.apellidoMaterno)
            .joinToString(" ").trim()
        return if (nombre.isBlank()) "N/D" else nombre
    }

    private fun formatearContactoCliente(c: Cliente): String {
        val partes = listOfNotNull(
            c.telefono?.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
            c.correo?.takeIf { it.isNotBlank() }?.let { "Correo: $it" }
        )
        return if (partes.isEmpty()) "N/D" else partes.joinToString(" | ")
    }

    private fun formatearSucursal(s: Sucursal): String {
        val nombre = s.nombreCorto?.takeIf { it.isNotBlank() } ?: "N/D"
        val codigo = s.codigo?.takeIf { it.isNotBlank() } ?: ""

        val direccion = listOfNotNull(
            s.calle?.takeIf { it.isNotBlank() }?.let { "C. $it" },
            s.numero?.takeIf { it.isNotBlank() }?.let { "#$it" },
            s.colonia?.takeIf { it.isNotBlank() },
            s.municipio?.takeIf { it.isNotBlank() },
            s.estado?.takeIf { it.isNotBlank() },
            s.codigoPostal?.takeIf { it.isNotBlank() }?.let { "CP $it" }
        ).joinToString(", ")

        val encabezado = listOf(nombre, codigo).filter { it.isNotBlank() }.joinToString(" - ")
        return if (direccion.isBlank()) encabezado else "$encabezado\n$direccion"
    }

    private fun formatearDireccionDestinatario(d: Destinatario): String {
        val partes = listOfNotNull(
            d.calle?.takeIf { it.isNotBlank() }?.let { "C. $it" },
            d.numero?.takeIf { it.isNotBlank() }?.let { "#$it" },
            d.colonia?.takeIf { it.isNotBlank() },
            d.municipio?.takeIf { it.isNotBlank() },
            d.estado?.takeIf { it.isNotBlank() },
            d.codigoPostal?.takeIf { it.isNotBlank() }?.let { "CP $it" }
        )
        return if (partes.isEmpty()) "N/D" else partes.joinToString(", ")
    }

    private fun mostrarDialogoActualizarEstatus() {
        val idActual = envio.idEstatusActual ?: 0

        if (idActual == ESTATUS_ENTREGADO || idActual == ESTATUS_CANCELADO) {
            Toast.makeText(this, "Este envío ya tiene estatus final y no puede modificarse.", Toast.LENGTH_LONG).show()
            return
        }

        val opciones = obtenerOpcionesSegunEstatusActual(idActual)

        if (opciones.isEmpty()) {
            Toast.makeText(this, "No hay cambios de estatus disponibles.", Toast.LENGTH_LONG).show()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val spEstatus = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@DetalleEnvioActivity,
                android.R.layout.simple_spinner_dropdown_item,
                opciones.map { it.first }
            )
        }

        val etComentario = EditText(this).apply {
            hint = "Comentario (obligatorio si es Detenido/Cancelado)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 2
        }

        layout.addView(spEstatus)
        layout.addView(etComentario)

        AlertDialog.Builder(this)
            .setTitle("Actualizar estatus")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val (nombre, idNuevo) = opciones[spEstatus.selectedItemPosition]
                val comentario = etComentario.text.toString().trim()

                val requiereComentario = (idNuevo == ESTATUS_DETENIDO || idNuevo == ESTATUS_CANCELADO)
                if (requiereComentario && comentario.isBlank()) {
                    Toast.makeText(this, "Debes escribir un comentario para $nombre.", Toast.LENGTH_LONG).show()
                    mostrarDialogoActualizarEstatus()
                    return@setPositiveButton
                }

                if (idNuevo == idActual) {
                    Toast.makeText(this, "El envío ya tiene ese estatus.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                actualizarEstatusEnvioApi(idNuevo, comentario)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarEstatusEnvioApi(idEstatusNuevo: Int, comentario: String) {
        val guia = envio.numeroGuia?.trim()
        if (guia.isNullOrEmpty()) {
            Toast.makeText(this, "No hay número de guía", Toast.LENGTH_LONG).show()
            return
        }

        val idColaborador = envio.idConductor
        if (idColaborador == null || idColaborador <= 0) {
            Toast.makeText(this, "No se pudo obtener el id del conductor", Toast.LENGTH_LONG).show()
            return
        }

        val envioReq = mutableMapOf<String, Any>(
            "numeroGuia" to guia,
            "idEstatusActual" to idEstatusNuevo,
            "idCreadoPor" to idColaborador
        )

        val jsonBody = Gson().toJson(envioReq)
        Log.d("PUT_ESTATUS_REQ", jsonBody)

        Ion.with(this)
            .load("PUT", "${Constantes().URL_API}envio/estatus")
            .setHeader("Content-Type", "application/json")
            .setStringBody(jsonBody)
            .asString()
            .setCallback { e, result ->
                if (e != null) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al conectar con el servidor", Toast.LENGTH_LONG).show()
                    return@setCallback
                }

                if (result.isNullOrEmpty()) {
                    Toast.makeText(this, "Respuesta vacía del servidor", Toast.LENGTH_LONG).show()
                    return@setCallback
                }

                Log.d("PUT_ESTATUS_RES", result)

                try {
                    val resp = Gson().fromJson(result, com.example.packetworldmt.dto.Respuesta::class.java)
                    if (!resp.error) {
                        Toast.makeText(this, resp.mensaje, Toast.LENGTH_SHORT).show()

                        recargarEnvio()

                    } else {
                        Toast.makeText(this, resp.mensaje, Toast.LENGTH_LONG).show()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Toast.makeText(this, "Error al procesar respuesta", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun obtenerOpcionesSegunEstatusActual(idActual: Int): List<Pair<String, Int>> {
        return when (idActual) {
            1, 2 -> listOf(
                "En tránsito" to ESTATUS_TRANSITO
            )

            // En tránsito
            ESTATUS_TRANSITO -> listOf(
                "Detenido" to ESTATUS_DETENIDO,
                "Entregado" to ESTATUS_ENTREGADO,
                "Cancelado" to ESTATUS_CANCELADO
            )

            // Detenido
            ESTATUS_DETENIDO -> listOf(
                "En tránsito" to ESTATUS_TRANSITO,
                "Cancelado" to ESTATUS_CANCELADO
            )

            // Entregado / Cancelado -> nada
            ESTATUS_ENTREGADO, ESTATUS_CANCELADO -> emptyList()

            else -> listOf(
                "En tránsito" to ESTATUS_TRANSITO,
                "Detenido" to ESTATUS_DETENIDO,
                "Entregado" to ESTATUS_ENTREGADO,
                "Cancelado" to ESTATUS_CANCELADO
            )
        }
    }

    private fun recargarEnvio() {
        val idConductor = envio.idConductor
        if (idConductor == null || idConductor <= 0) return

        Ion.with(this)
            .load("GET", "${Constantes().URL_API}envio/obtener-por-conductor/$idConductor")
            .asString()
            .setCallback { e, result ->
                if (e != null || result.isNullOrEmpty()) return@setCallback
                try {
                    envio = Gson().fromJson(result, Envio::class.java)
                    binding.tvEstatusDetalle.text = envio.estatusActual ?: "N/D"
                } catch (_: Exception) { }
            }

        val idActual = envio.idEstatusActual ?: 0
        if (idActual == ESTATUS_ENTREGADO || idActual == ESTATUS_CANCELADO) {
            binding.btnActualizarEstatus.isEnabled = false
            binding.btnActualizarEstatus.text = "Estatus final"
        } else {
            binding.btnActualizarEstatus.isEnabled = true
            binding.btnActualizarEstatus.text = "Actualizar estatus"
        }

    }

}
 