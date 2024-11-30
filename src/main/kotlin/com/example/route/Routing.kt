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
                        log.info("No data found in DataBase")
                        call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("No data found in DataBase"))
                    }
                }
                else {
                    log.info("Registration closed.")
                    call.respond(HttpStatusCode.OK,ErroeMsgDto("Registration closed."))
                }

            }
            catch (e : Exception) {
                log.info("Exception occurred while getting data from DataBase")
                call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("error : ${e.message}"))
            }
        }

        post("/add-user") {
            try {
                log.info("entered to add user")
                val user = call.receive<RequestDto>()
                log.info("${user.toString()}")
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
                                            log.info("User not found in usersData DataBase")
                                            call.respond(HttpStatusCode.OK,ErroeMsgDto("User not found in DataBase"))
                                        }

                                    }
                                    else {
                                        log.info("User Not added to DataBase")
                                        call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("User Not added to DataBase"))
                                    }
                                }
                                else {
                                    log.info("Error while mapping data to main DataBase")
                                    call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("Error while mapping data to main DataBase"))
                                }
                            }
                            else {
                                log.info("No Data Found in Db, All topics get selected")
                                call.respond(HttpStatusCode.OK,ErroeMsgDto("No Data Found in Db, All topics get selected"))
                            }
                        }
                        else {
                            log.info("User of employeeId : ${user.empId} is already selected the topics")
                            call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("User of employeeId : ${user.empId} is already selected the topics"))
                        }
                    }
                    else {
                        log.info("User mailId of ${user.empMailId} and empId of ${user.empId} not matching with the data in employee DB, please provide valid details")
                        call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("User mailId of ${user.empMailId} and empId of ${user.empId} not matching with the data in employee DB, please provide valid details"))
                    }

                }
                else {
                    log.info("Not Received Data from front end")
                    call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("Not Received Data from front end"))
                }
            }
            catch (e : Exception) {
                log.info("Exception message : ${e.message}")
                call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("Exception message : ${e.message}"))
            }



        }

        get("/get-all-users") {
            try {
                log.info("getting selected users")
                val data : List<UsersDataDto>? = DataBaseConnection.mainUserCollection.find().toList() ?: null
                data?.takeIf { it.isNotEmpty() }?.let {
                    val usersData = it.map { res ->
                        UserResponseDto(res.empId, res.empName, res.topicsSelected)
                    }
                    log.info("Users fetched successfully")
                    call.respond(HttpStatusCode.OK, usersData)
                } ?: run {
                    call.respond(HttpStatusCode.OK, ErroeMsgDto("No users have selected topics."))
                }

            }
            catch (e : Exception) {
                call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("Exception : ${e.message}"))
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
                        log.info("User not found with empId $userId")
                        call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("User not found with empId $userId"))
                    }
                }
                else {
                    log.info("Id not received from front end")
                    call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("Id not received from front end"))
                }
            }
            catch (e : Exception) {
                log.info("Exception message : ${e.message}")
                call.respond(HttpStatusCode.BadRequest,ErroeMsgDto("Exception message : ${e.message}"))
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
    log?.info("Details getting from front end request $empId and $empMail")
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

    log?.info("Data mapping done for response")
    return UsersDataDto(data.empId,
                        data.empName,
                        data.empMailId,
                        subTopicNames)
}

fun getDataFromDb(empId : Int) : UserResponseDto? {
    if(empId != null) {
        val userData = DataBaseConnection.mainUserCollection.findOne(UsersDataDto::empId eq empId)
        if (userData != null) {
            return UserResponseDto(userData.empId,userData.empName,userData.topicsSelected)
        }
        return null
    }
   return null
}