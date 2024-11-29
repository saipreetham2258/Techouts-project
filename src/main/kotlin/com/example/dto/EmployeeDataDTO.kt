package com.example.dto
import kotlinx.serialization.Serializable

@Serializable
data class EmployeeDataDTO(val empId : Int,
                            val empMailId : String)
