package com.example.permisos.fetures

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object ContactsHelper {

    fun getFirstContact(context: Context):String{
        if(ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED){
            return "Permiso READ_CONTACTS no otorgado"
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        return try{
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {
                    val nameIndex =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    val name = if (nameIndex >= 0) cursor.getString(nameIndex)
                        ?: "Sin nombre" else "Sin nombre"
                    val number = if (numberIndex >= 0) cursor.getString(numberIndex)
                        ?: "Sin número" else "Sin número"

                    "Contacto: $name\n Tel: $number"
                }else{
                    "No se encontraron contactos guardados en la agenda"
                }
            }?: "No se pudo acceder a la base de datos de contactos"
        }catch (e: SecurityException){
            "Error de seguridad: ${e.localizedMessage}"
        }catch (e: Exception){
            "Error al leer contactos: ${e.localizedMessage}"
        }

    }
}