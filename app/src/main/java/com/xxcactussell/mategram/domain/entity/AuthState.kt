package com.xxcactussell.mategram.domain.entity

sealed class AuthState {
    object Initial : AuthState()                      // Начальное состояние
    object WaitTdlibParameters : AuthState()         // Ожидание параметров TDLib
    object WaitPhone : AuthState()                   // Ожидание ввода номера телефона
    object WaitCode : AuthState()                    // Ожидание ввода кода подтверждения
    object WaitPassword : AuthState()                // Ожидание ввода пароля (двухфакторная аутентификация)
    object Ready : AuthState()                       // Авторизация завершена
    object NoAuth : AuthState()                      // Авторизация недействительна
}
