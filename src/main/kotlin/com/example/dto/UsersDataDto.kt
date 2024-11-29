package com.example.dto
import kotlinx.serialization.Serializable

@Serializable
data class UsersDataDto(val empId : Int,
                        val empName : String,
                        val empMailId : String,
                        val topicsSelected : List<String>)

