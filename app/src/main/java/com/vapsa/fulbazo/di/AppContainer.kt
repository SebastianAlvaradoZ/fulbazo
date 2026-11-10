package com.vapsa.fulbazo.di

import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.domain.repository.AuthRepository

/**
 * Contenedor simple de dependencias compartidas por toda la app.
 *
 * Cumple el papel que luego tendrán los módulos de Hilt: los ViewModels reciben
 * las dependencias por constructor, así que migrar solo cambia este archivo y
 * las factories.
 */
object AppContainer {
    val authRepository: AuthRepository by lazy { FakeAuthRepository() }
}
