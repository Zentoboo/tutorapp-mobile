package com.example.tutor_app

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val userType: String = "", // "student" or "tutor"
    val createdAt: Long = System.currentTimeMillis()
)