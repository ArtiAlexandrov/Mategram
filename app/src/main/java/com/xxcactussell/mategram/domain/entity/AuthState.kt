package com.xxcactussell.mategram.domain.entity

sealed class AuthState {
    data object Initial : AuthState()                      // Начальное состояние
    data object WaitTdlibParameters : AuthState()         // Ожидание параметров TDLib
    data object WaitPhone : AuthState()                   // Ожидание ввода номера телефона
    data object WaitCode : AuthState()                    // Ожидание ввода кода подтверждения
    data object WaitPassword : AuthState()                // Ожидание ввода пароля (двухфакторная аутентификация)
    data object Ready : AuthState()                       // Авторизация завершена
    data object NoAuth : AuthState()                      // Авторизация недействительна
}
