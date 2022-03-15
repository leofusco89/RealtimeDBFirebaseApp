package com.example.realtimedbfirebaseapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {
    var firebaseAuth: FirebaseAuth? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private lateinit var database: DatabaseReference
    private lateinit var btnLogInOut: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnCargar: Button
    private lateinit var btnEliminar: Button
    private lateinit var etNombre: EditText
    private lateinit var etEdad: EditText
    private lateinit var etCarga: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Gestionar Firebase Authenticator para email+password
        gestionarAutorizacion()

        //Modificar acciones disponibles o no dependiendo autorización
        setUI()

        //Instanciamos BD
        database = Firebase.database.reference

        //Agregamos listener de cambios solo para árbol "people" dentro de la BD (maximizamos
        //performance al solo gatillar el listener para cambios de este árbol en particular)
        database.child("people").addValueEventListener(personListener)
    }

    private fun gestionarAutorizacion() {
        //Instancia para autorización
        firebaseAuth = FirebaseAuth.getInstance()

//        For AuthStateListener, its listener will be called when there is a change in the
//        authentication state, will be call when:
//          - Right after the listener has been registered
//          - When a user is signed in
//          - When the current user is signed out
//          - When the current user changes
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            //Check if there is a user has already logged in
            val user = firebaseAuth.currentUser
            if (user != null) {
                Toast.makeText(this, "Usuario conectado", Toast.LENGTH_SHORT).show()
                btnLogInOut.text = "Logout"
            } else {
                Toast.makeText(this, "Usuario no conectado", Toast.LENGTH_SHORT).show()
                btnLogInOut.text = "Login"
            }
        }
        firebaseAuth?.addAuthStateListener(authStateListener!!)

    }

    private fun setUI() {
        btnLogInOut = findViewById(R.id.btn_loginout)
        btnGuardar = findViewById(R.id.btn_cargar)
        btnCargar = findViewById(R.id.btn_guardar)
        btnEliminar = findViewById(R.id.btn_eliminar)
        etNombre = findViewById(R.id.et_nombre)
        etEdad = findViewById(R.id.et_edad)
        etCarga = findViewById(R.id.et_carga)

        //Habilitamos botón de Login o Logout
        if (firebaseAuth?.currentUser != null) {
            //Botón Login
            btnLogInOut.text = "Login"
        } else {
            //Botón Login
            btnLogInOut.text = "Logout"
        }
    }

    private val personListener = object : ValueEventListener {
        //Se utiliza para escuchar cuando se modifique la BD, pero no solo desde el dispositivo actual
        // sino que desde cualquier dispositivo que use esta app. Sirve para actualizar en tiempo
        // real valores que se estén visualizando. En este caso, es para el  árbol "people"
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // Si hay ABM de "people", devuelve el listado completo de Personas

            // IMPORTANTE: No es posible obtener SOLO el objeto modificado/incluido/borrado,
            // getValue<Person>() siempre trae el listado completo de Personas. Lo que
            // habría que hacer es comparar los datos que vienen en getValue<Person> contra
            // los datos visualizados y actualizar los valores distintos o eliminar
            // registros que no se encuentren más.

            // IMPORTANTE: Para que getValue<Person>() no tire error, en la clase Person,
            // hay que declarar safe null en parámetros para que se cree automáticamente el
            // constructor por defecto: Person(val name: String? = null, val edad: Double? = null)

            val people = dataSnapshot.getValue<Person>()
            //Recorrer listado
            for (person in dataSnapshot.children) {
                //Usar, comparar, etc, cada registro de persona obtenido
            }
            Toast.makeText(applicationContext, "personListener: onDataChange", Toast.LENGTH_SHORT).show()

        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Error al obtener Personas
            Toast.makeText(applicationContext, "personListener: onCancelled", Toast.LENGTH_SHORT).show()
        }
    }

    fun loginout(v: View?) {
        when (btnLogInOut.text) {
            "Login" -> login()
            "Logout" -> logout()
        }

    }

    fun login() {
        //Loguear usuario
        val userEmail = "test@test.com"
        val userPass = "testtest"

        //Log user by using Firebase registered user list
        firebaseAuth!!.signInWithEmailAndPassword(userEmail, userPass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Usuario logueado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al loguear usuario", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun logout() {
        firebaseAuth?.signOut()
    }

    fun guardar(v: View?) {
        //Validamos que el usuario haya completado los datos
        if (etNombre.text.isNotEmpty() || etEdad.text.isNotEmpty()) {
            val person = Person(etNombre.text.toString(), etEdad.text.toString().toDouble())

            //Guardamos nuevo registro o actualizamos registro con mismo nombre
            database.child("people").child(person.name!!).setValue(person)
                .addOnSuccessListener {
                    //Callback para guardado exitoso
                    Toast.makeText(
                        this,
                        person.name + " guardado/actualizado correctamente",
                        Toast.LENGTH_LONG
                    ).show()

                }
                .addOnFailureListener {
                    //Callback para error en guardado
                    Toast.makeText(
                        this,
                        "Error al guardar (addOnFailureListener)",
                        Toast.LENGTH_LONG
                    ).show()
                }

        } else {
            Toast.makeText(this, "Completar Nombre y Edad para guardar", Toast.LENGTH_LONG).show()
        }
    }

    fun cargar(v: View?) {
        //Generalmente, con el callback ValueEventListener podemos obtener los datos en tiempo real
        //cuando son modificados desde este u otro dispositivo y así actualizar lo que se visualiza,
        //pero para una única lectura, se puede utilizar el get() que primero intenta traer los
        //datos de BD, pero si el dispositivo se encuentra offline, busca en caché los datos
        database.child("people").get().addOnSuccessListener {
            etCarga.setText("")
            for (snapshot in it.children) {
                val person = snapshot.getValue<Person>()
                etCarga.setText(etCarga.text.toString() + person?.name + " " + person?.edad.toString() + "\n")
            }
            //Ejemplo búsqueda de un hijo en particular
            //database.child("people").child("Leo").get().addOnSuccessListener  {}
        }.addOnFailureListener {
            Toast.makeText(this, "Error al obtener datos (addOnFailureListener)", Toast.LENGTH_LONG)
                .show()
        }
    }

    fun eliminar(v: View?) {
        if (etNombre.text.isNotEmpty()) {
            //updateChildren() to delete multiple children in a single API call.
            database.child("people").child(etNombre.text.toString()).removeValue()
                .addOnSuccessListener {
                    //Callback para guardado exitoso
                    Toast.makeText(this, etNombre.text.toString() + " eliminado correctamente", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    //Callback para error en guardado
                    Toast.makeText(this, "Error al eliminar (addOnFailureListener)", Toast.LENGTH_LONG).show()
                }

        } else {
            Toast.makeText(this, "Completar Nombre para eliminar", Toast.LENGTH_LONG).show()
        }
    }
}