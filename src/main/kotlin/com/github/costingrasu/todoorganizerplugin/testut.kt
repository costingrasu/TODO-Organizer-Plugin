package com.github.costingrasu.todoorganizerplugin

data class Student(
    val nume: String,
    val prenume: String,
    val grupa: String,
    val likePA: String,
    val dislikePA: String,
    val vara: String,
    val viitor: String
)
//BUG something wrong

fun main() {
//TODO wrong
    val eu = Student(
        "Grasu",
        "Costin-Alexandru",
        "321AC",
        "",
        "",
        "",
        ""
    )

    println("nume: ${eu.nume}")
    println("prenume: ${eu.prenume}")
    println("grupa: ${eu.grupa}")
    println(": : ${eu.likePA}")
    println(": : ${eu.dislikePA}")
    println(": : ${eu.vara}")
    println(": : ${eu.viitor}")
}
//FIXME bloop