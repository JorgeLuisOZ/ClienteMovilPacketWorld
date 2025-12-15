package com.example.packetworldmt.poko

data class Colaborador(

    var idColaborador: Int,
    var nombre: String,
    var apellidoPaterno: String,
    var apellidoMaterno: String?,
    var curp: String,
    var correo: String,
    var numeroPersonal: String,
    var numeroLicencia: String?,

    var idRol: Int,
    var rol: String,
    var idSucursal: Int,
    var sucursal: String,
    var idUnidad: Int,
    var unidad: String,

    var fotoBase64: String?

)
