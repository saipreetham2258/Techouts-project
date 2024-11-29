package com.example.config

import com.example.dto.*
import com.mongodb.client.MongoCollection
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection

object DataBaseConnection {
    lateinit var topicsCollection: MongoCollection<DataDto>
    lateinit var mainUserCollection : MongoCollection<UsersDataDto>
    lateinit var employeeDataDB : MongoCollection<EmployeeDataDTO>
    init {
        val url = "mongodb://localhost:27017"
        val client = KMongo.createClient(url)
        val dataBase = client.getDatabase("TechoutsMain")
        topicsCollection = dataBase.getCollection<DataDto>("Topics")
        mainUserCollection = dataBase.getCollection<UsersDataDto>("UsersData")
        employeeDataDB = dataBase.getCollection<EmployeeDataDTO>("EmployeeData")

    }
}