package com.example.dto
import kotlinx.serialization.Serializable

@Serializable
data class DataDto(val topicId : Int,
                   val isActive : Boolean,
                   val topic: String,
                   val subTopic: String,
                   val description : String)
