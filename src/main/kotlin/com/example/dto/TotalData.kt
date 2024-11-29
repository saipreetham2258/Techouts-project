package com.example.dto
import kotlinx.serialization.Serializable

@Serializable
data class TotalData(val topicId : Int,
                     val topic : String,
                     val subTopic : String,
                     val description : String)
