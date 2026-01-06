package com.example.packetworldmt.poko

data class Cliente(
    var idCliente: Int?,

    var nombre: String?,
    var apellidoPaterno: String?,
    var apellidoMaterno: String?,

    var calle: String?,
    var numero: String?,
    var telefono: String?,
    var correo: String?,

    var idPais: Int?,
    var pais: String?,

    var idEstado: Int?,
    var estado: String?,

    var idMunicipio: Int?,
    var municipio: String?,

    var idColonia: Int?,
    var colonia: String?,

    var codigoPostal: String?
)
