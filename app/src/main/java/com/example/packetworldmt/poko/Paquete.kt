package com.example.packetworldmt.poko

data class Paquete(
    var idPaquete: Int?,
    var descripcion: String?,

    var peso: Double?,
    var alto: Double?,
    var ancho: Double?,
    var profundidad: Double?,

    var idEnvio: Int?
)
