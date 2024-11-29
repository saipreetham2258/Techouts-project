package com.example.dto
import kotlinx.serialization.Serializable

@Serializable
data class UserResponseDto(val empId : Int,
                           val topicsSelected : List<String>)
