package com.example.route

import com.example.config.DataBaseConnection
import com.example.dto.*
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Updates.set
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.*
import org.slf4j.LoggerFactory

val log: org.slf4j.Logger? = LoggerFactory.getLogger("MyApp")
fun Application.apiRoute() {
    routing {
        get("/fetch-data") {
            log.info("Entered to fetch topics")
            try {
                val totalTopics = fetchDataByQuery(DataBaseConnection.topicsCollection.find().toList())
                if(totalTopics.isNotEmpty()) {
                    val remainingData = responseDataMapping(totalTopics)
                    if(remainingData.isNotEmpty()) {
                        call.respond(HttpStatusCode.OK,remainingData)
                        log.info("Data fetched successfully")
                    }
                    else {
                        call.respond(HttpStatusCode.BadRequest,"No data found in DataBase")
                    }
                }
                else {
                    call.respond(HttpStatusCode.OK,"Registration closed.")
                }

            }
            catch (e : Exception) {
                log.info("Exception occurred while getting data from DataBase")
                call.respond(HttpStatusCode.BadRequest,"error : ${e.message}")
            }
        }

        post("/add-user") {
            try {
                log.info("entered to add user")
                val user = call.receive<RequestDto>()

                if(user != null) {
                    val userDetailsISPresentOrNot = checkingEmployeeDataInDb(user.empId,user.empMailId)
                    log.info("$userDetailsISPresentOrNot")
                    if(userDetailsISPresentOrNot) {
                        val userIsExistOrNot = checkingDataInMainDataDB(user.empId,user.empMailId)
                        if(!userIsExistOrNot) {
                            val data = DataBaseConnection.topicsCollection.find().toList()
                            if(data.isNotEmpty()) {
                                val addUser = mapDataForMainDb(user)
                                if(addUser != null) {
                                    val userAddedToDb = DataBaseConnection.mainUserCollection.insertOne(addUser)
                                    if(userAddedToDb.wasAcknowledged()) {
                                        DataBaseConnection.topicsCollection.updateMany(
                                            `in`("topicId", user.topicsSelected),
                                            set("isActive", false)
                                        )
                                        val userFromDb = getDataFromDb(user.empId)
                                        if(userFromDb != null) {
                                            log.info("user registered successfully")
                                            val result = RegisteredResponseDto("Employee registered successfully",userFromDb)
                                            call.respond(HttpStatusCode.OK, result)
                                        }
                                        else {
                                            call.respond(HttpStatusCode.OK,"User not found in DataBase")
                                        }

                                    }
                                    else {
                                        call.respond(HttpStatusCode.BadRequest,"User Not added to DataBase")
                                    }
                                }
                                else {
                                    call.respond(HttpStatusCode.BadRequest,"Error while mapping data to main DataBase")
                                }
                            }
                            else {
                                call.respond(HttpStatusCode.OK,"No Data Found in Db, All topics get selected")
                            }
                        }
                        else {
                            call.respond(HttpStatusCode.BadRequest,"User of employeeId : ${user.empId} is already selected the topics")
                        }
                    }
                    else {
                        call.respond(HttpStatusCode.BadRequest,"User mailId and empId not matching with the data in employee DB, please provide valid details")
                    }

                }
                else {
                    call.respond(HttpStatusCode.BadRequest,"Not Received Data from front end")
                }
            }
            catch (e : Exception) {
                call.respond(HttpStatusCode.BadRequest,"Exception message : ${e.message}")
            }



        }

        get("/get-specific-user")  {
            try {
                val userId = call.request.headers["empId"]
                if(userId != null) {
                    val userData = DataBaseConnection.mainUserCollection.findOne(UsersDataDto::empId eq userId.toInt())
                    if(userData != null) {
                        call.respond(HttpStatusCode.OK,userData)
                    }
                    else {
                        call.respond(HttpStatusCode.BadRequest,"User not found with empId $userId")
                    }
                }
                else {
                    call.respond(HttpStatusCode.BadRequest,"Id not received from front end")
                }
            }
            catch (e : Exception) {
                call.respond(HttpStatusCode.BadRequest,"Exception message : ${e.message}")
            }

        }


    }
}

fun responseDataMapping(data : List<DataDto>) : List<ResponseDto> {
    return data.map { res ->
        ResponseDto(topicId = res.topicId,
                    topic = res.topic,
                    subTopic = res.subTopic,
                    description = res.description)
    }
}

fun fetchDataByQuery(data : List<DataDto>) : List<DataDto> {
    val query = and(
        DataDto::isActive eq true
    )
    return DataBaseConnection.topicsCollection.find(query).toList()
}
fun checkingEmployeeDataInDb(empId : Int , empMail : String) : Boolean {
    val employee = DataBaseConnection.employeeDataDB.find(and(EmployeeDataDTO::empId eq empId, EmployeeDataDTO::empMailId eq empMail)).first()
    if(employee != null) {
        return true
    }
    return false
}
fun checkingDataInMainDataDB(empId : Int , empMail: String) : Boolean {
    val res =  DataBaseConnection.mainUserCollection.find(and(
        RequestDto::empId eq empId,
        RequestDto::empMailId eq empMail
    )).first()
    if(res != null) {
        return true
    }
    return false
}

fun mapDataForMainDb(data : RequestDto) : UsersDataDto{
    val topicsSelected: List<Int> = data.topicsSelected
    val subTopicNames : List<String> = topicsSelected.mapNotNull { topicId ->
        val topic = DataBaseConnection.topicsCollection.findOne(ResponseDto::topicId eq topicId)
        topic?.subTopic
    }


    return UsersDataDto(data.empId,
                        data.empName,
                        data.empMailId,
                        subTopicNames)
}

fun getDataFromDb(empId : Int) : UserResponseDto? {
    if(empId != null) {
        val userData = DataBaseConnection.mainUserCollection.findOne(UsersDataDto::empId eq empId)
        if (userData != null) {
            return UserResponseDto(userData.empId,userData.topicsSelected)
        }
        return null
    }
   return null
}