package com.example.packetworldmt.poko

data class Sucursal(
    var idSucursal: Int,

    var codigo: String?,
    var nombreCorto: String?,
    var calle: String?,
    var numero: String?,

    var idPais: Int,
    var pais: String?,

    var idEstado: Int,
    var estado: String?,

    var idMunicipio: Int,
    var municipio: String?,

    var idColonia: Int,
    var colonia: String?,

    var codigoPostal: String?,

    var idEstatusSucursal: Int,
    var estatusSucursal: String?
)
