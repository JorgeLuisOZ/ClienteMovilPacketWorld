package com.example.packetworldmt.poko

data class Envio(

    var idEnvio: Int?,
    var numeroGuia: String?,

    var idDestinatario: Int?,
    var nombreDestinatario: String?,
    var apellidoPaternoDestinatario: String?,

    var idCliente: Int?,
    var nombreCliente: String?,
    var apellidoPaternoCliente: String?,

    var idSucursal: Int?,
    var codigoSucursal: String?,

    var idConductor: Int?,
    var numeroPersonalConductor: String?,

    var idEstatusActual: Int?,
    var costoTotal: Double,

    var idCreadoPor: Int?,
    var numeroPersonalColaborador: String?,

    var estatusActual: String?

)
