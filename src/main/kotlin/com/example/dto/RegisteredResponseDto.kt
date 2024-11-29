package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisteredResponseDto(val message : String,
                                 val userData : UserResponseDto)
