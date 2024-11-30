package com.example.dto
import kotlinx.serialization.Serializable

@Serializable
data class RequestDto(val empId : Int,
                      val empName : String,
                      val empMailId : String,
                      val topicsSelected : MutableList<Int>)

@Serializable
data class ResponseDto(val topicId : Int,
                       val topic: String,
                       val subTopic: String,
                       val description : String)
