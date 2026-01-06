package com.example.packetworldmt

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.packetworldmt.databinding.ItemEnvioBinding
import com.example.packetworldmt.poko.Destinatario
import com.example.packetworldmt.poko.Envio
import com.example.packetworldmt.util.Constantes
import com.google.gson.Gson
import com.koushikdutta.ion.Ion
import androidx.recyclerview.widget.RecyclerView

class EnvioAdapter(
    private val context: Context,
    private var envios: List<Envio>,
    private val onVerDetalle: (Envio) -> Unit
) : RecyclerView.Adapter<EnvioAdapter.EnvioViewHolder>() {

    private val cacheDirecciones = mutableMapOf<Int, String>()

    class EnvioViewHolder(val binding: ItemEnvioBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnvioViewHolder {
        val binding = ItemEnvioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EnvioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EnvioViewHolder, position: Int) {
        val envio = envios[position]
        val b = holder.binding

        b.tvGuia.text = "Guía: ${envio.numeroGuia ?: "N/D"}"
        b.tvEstatus.text = "Estatus: ${envio.estatusActual ?: "N/D"}"

        // Destino (por destinatario)
        val idDest = envio.idDestinatario ?: 0
        b.tvDestino.tag = idDest

        if (idDest <= 0) {
            b.tvDestino.text = "Destino: No disponible"
        } else {
            val enCache = cacheDirecciones[idDest]
            if (enCache != null) {
                b.tvDestino.text = "Destino: $enCache"
            } else {
                b.tvDestino.text = "Destino: Cargando..."
                cargarDireccionDestinatario(idDest, holder)
            }
        }

        b.btnVerDetalle.setOnClickListener { onVerDetalle(envio) }
        b.root.setOnClickListener { onVerDetalle(envio) }
    }

    override fun getItemCount(): Int = envios.size

    fun actualizarLista(nueva: List<Envio>) {
        envios = nueva
        notifyDataSetChanged()
    }

    private fun cargarDireccionDestinatario(idDestinatario: Int, holder: EnvioViewHolder) {
        Ion.with(context)
            .load("GET", "${Constantes().URL_API}destinatario/obtener-por-id/$idDestinatario")
            .asString()
            .setCallback { e, result ->
                if (e != null || result.isNullOrEmpty()) {
                    if (holder.binding.tvDestino.tag == idDestinatario) {
                        holder.binding.tvDestino.text = "Destino: No disponible"
                    }
                    return@setCallback
                }

                try {
                    val d = Gson().fromJson(result, Destinatario::class.java)
                    val direccion = formatearDireccion(d)
                    cacheDirecciones[idDestinatario] = direccion

                    // evita bug por reciclado
                    if (holder.binding.tvDestino.tag == idDestinatario) {
                        holder.binding.tvDestino.text = "Destino: $direccion"
                    }
                } catch (_: Exception) {
                    if (holder.binding.tvDestino.tag == idDestinatario) {
                        holder.binding.tvDestino.text = "Destino: No disponible"
                    }
                }
            }
    }

    private fun formatearDireccion(d: Destinatario): String {
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
}
