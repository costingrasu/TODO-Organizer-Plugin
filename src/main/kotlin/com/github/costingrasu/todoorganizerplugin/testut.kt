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
//FIXME something wrong

fun main() {
//TODO altceva
    val eu = Student(
        "Grasu",
        "Costin-Alexandru",
        "321AC",
        "Mi a placut la Pa faptul ca la laborator au fost prezentate si explicate concepte ce se folosesc in industrie si am fost familiarizati cu intrebari si gandire de tip interview. Faptul ca temele erau saptamanale m au ajutat sa retin mai bine ce am invatat. Prof. Chis a explicat materia intr un mod out of the box si am putut intelege mai bine",
        "Mi ar fi placut daca la teme am fi avut un fel de checker sau o metoda de a ne verifica temele, cat si eficienta lor",
        "Vara asta am fost intr o tabara unde am invatat realizatul de aplicatii desktop folosind Rust pentru backend si slint (un framework si markup language) pentru frontend, cat si Rust in sine. De asemenea, am invatat Java in contextul OOP. Mi am vizitat niste prieteni ce fac facultatea in Olanda, unde am vizitat atractii, am mers la un parc de distractii si am incercat un nou sport, Bouldering (catarat)",
        "Dupa ce termin facultatea as vrea sa fiu angajat ca Backend developer sau DevOps la o firma de domeniu de unde sa pot sa invat mai multe in aceste directii."
    )



    println("nume: ${eu.nume}")
    println("prenume: ${eu.prenume}")
    println("grupa: ${eu.grupa}")
    println("Ce mi a placut la lab la PA: : ${eu.likePA}")
    println("Ce recomandari am pentru viitor la PA: : ${eu.dislikePA}")
    println("Ce am facut vara asta pentru dezvoltare personala: : ${eu.vara}")
    println("Ce mi as dori sa fac dupa ce termin facultatea: : ${eu.viitor}")
}
//FIXME yaaay