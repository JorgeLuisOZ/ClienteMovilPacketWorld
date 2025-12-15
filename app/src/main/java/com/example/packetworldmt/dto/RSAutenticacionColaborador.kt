package com.example.packetworldmt.dto

import com.example.packetworldmt.poko.Colaborador

data class RSAutenticacionColaborador(

    val error : Boolean,
    val mensaje : String,
    var colaborador : Colaborador?

)
