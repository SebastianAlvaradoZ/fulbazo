package com.vapsa.fulbazo.domain.model

/**
 * Datos del perfil que el usuario puede cambiar. El correo y la fecha de
 * nacimiento no se editan.
 */
data class ProfileUpdate(
    val firstName: String,
    val lastName: String,
    val nickname: String,
    val phone: String,
    val district: String,
    val photoUri: String?,
)
