package com.example.packetworldmt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.packetworldmt.databinding.ActivityEdicionAlumnoBinding
import com.example.packetworldmt.util.Constantes
import com.google.gson.Gson
import com.koushikdutta.ion.Ion

class EdicionAlumnoActivity : AppCompatActivity() {

    private lateinit var binding : ActivityEdicionAlumnoBinding
    private lateinit var alumno : Alumno

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEdicionAlumnoBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        cargarDatosAlumno()
        binding.btnActualizar.setOnClickListener {
            //ACTUALIZAR LA INFORMACIÓN DEL ALUMNO
            editarDatosAlumno()
        }
    }

    fun cargarDatosAlumno(){
        val jsonAlumno = intent.getStringExtra("alumno")
        val gson = Gson()
        alumno = gson.fromJson(jsonAlumno, Alumno::class.java)
        binding.tvMatricula.text = alumno.matricula
        binding.etNombre.setText(alumno.nombre)
        binding.etApellidoPaterno.setText(alumno.apellidoPaterno)
        binding.etApellidoMaterno.setText(alumno.apellidoMaterno)
        binding.etCorreo.setText(alumno.correo)
    }

    fun editarDatosAlumno(){
        alumno.nombre = binding.etNombre.text.toString()
        alumno.apellidoPaterno = binding.etApellidoPaterno.text.toString()
        alumno.apellidoMaterno = binding.etApellidoMaterno.text.toString()
        alumno.correo = binding.etCorreo.text.toString()

        val gson = Gson()
        val alumnoJson  = gson.toJson(alumno)

        Ion.with(this@EdicionAlumnoActivity).load("PUT", "${Constantes().URL_API}alumno/editar")
            .setHeader("Content-Type", "application/json").setStringBody(alumnoJson).asString(Charsets.UTF_8).setCallback { e, result ->
                if (e == null){
                    // TODO VALIDACIÓN SI NO HUBO ERROR AL EDITAR CON GSON
                } else {
                    // TODO TOAST DE ERROR DE PETICIÓN
                }
            }
    }
}