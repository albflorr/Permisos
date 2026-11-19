package com.example.permisos.permissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


enum class PermissionStatus(
    val title:String,
    val color: Color,
    val containerColor: Color,
    val icon: ImageVector
) {
    GRANTED(
        title = "CONCEDIDO",
        color = Color(0xFF2E7D32),
        containerColor = Color(0xFFF5F5F5),
        icon = Icons.Default.CheckCircle
    ),
    NOT_REQUESTED(
        title = "SIN SOLICITAR / DENEGADO",
        color = Color(0xFF616161),
        containerColor = Color(0xFFF5F5F5),
        icon = Icons.Default.HelpOutline
    ),
    RATIONALE_REQUIRED(
        title = "EXPLICACIÓN REQUERIDA",
        color = Color(0xFFE65100),
        containerColor = Color(0xFFFFF3E0),
        icon = Icons.Default.Warning
    ),
    PERMANENTLY_DENIED(
        title = "BLOQUEADO PERMANENTEMENTE",
        color = Color(0xFFC62828),
        containerColor = Color(0xFFFFEBEE),
        icon = Icons.Default.Block
    );

    companion object{
        fun evaluate(
          context: Context,
          permission: String,
          hasBeenRequestedBefore: Boolean
        ): PermissionStatus{
            val isGranted = ContextCompat
                .checkSelfPermission(context,permission) == PackageManager.PERMISSION_GRANTED

            if(isGranted) return PermissionStatus.GRANTED

            val activity = context as? Activity
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it,permission)
            }?:false

            return when{
                shouldShowRationale -> PermissionStatus.RATIONALE_REQUIRED
                hasBeenRequestedBefore -> PermissionStatus.PERMANENTLY_DENIED
                else -> PermissionStatus.NOT_REQUESTED
            }
        }
    }

}