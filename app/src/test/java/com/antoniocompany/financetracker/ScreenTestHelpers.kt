package com.antoniocompany.financetracker

import android.app.Activity
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog

/*
 * Ayudas de los tests de pantallas (Robolectric): escribir, pulsar y leer lo
 * que se ve, sin repetir findViewById en cada prueba.
 */

fun Activity.type(id: Int, text: String) {
    findViewById<EditText>(id).setText(text)
}

fun Activity.click(id: Int) {
    findViewById<View>(id).performClick()
    idle()
}

fun Activity.textOf(id: Int): String = findViewById<TextView>(id).text.toString()

fun Activity.isShown(id: Int): Boolean = findViewById<View>(id).visibility == View.VISIBLE

/** La Activity que ha pedido abrir esta pantalla, o null. */
fun Activity.startedActivity(): String? = shadowOf(this).nextStartedActivity?.component?.className

/** Ejecuta lo pendiente en el hilo principal (corrutinas, dialogos). */
fun idle() = shadowOf(Looper.getMainLooper()).idle()

/** El ultimo dialogo abierto, si sigue a la vista. */
fun openDialog(): AlertDialog? = (ShadowDialog.getLatestDialog() as? AlertDialog)?.takeIf { it.isShowing }

fun AlertDialog.type(id: Int, text: String) {
    findViewById<EditText>(id)!!.setText(text)
}

fun AlertDialog.click(which: Int) {
    getButton(which).performClick()
    idle()
}
