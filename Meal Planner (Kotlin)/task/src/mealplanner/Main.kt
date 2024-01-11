package mealplanner

import mealplanner.DatabaseOperations.addRecipeToDB
import mealplanner.DatabaseOperations.addToPlan
import mealplanner.DatabaseOperations.getMealsFromDB
import mealplanner.DatabaseOperations.getPlanIngredients
import mealplanner.DatabaseOperations.initializeDB
import java.io.File
import java.sql.Connection
import java.sql.DriverManager


fun main() {
    //initialize DB
    val connection = DriverManager.getConnection("jdbc:sqlite:meals.db")
    initializeDB(connection)

    val meals: MutableList<Recipe> = getMealsFromDB(connection)
    var exit = false

    while(!exit){
        println("What would you like to do (add, show, plan, save, exit)?")
        val input = readln()

        when (input.lowercase()){
            "add" ->{
                val inputMeal = add(connection)
                meals.add(inputMeal)
                println("The meal has been added!")
            }
            "show" ->{
                if (meals.isEmpty()){ println("No meals saved. Add a meal first.") }
                else{
                    show(connection)
                }
            }
            "plan" ->{
                plan(connection)
            }
            "save" ->{
                saveMeal(connection)
            }
            "exit" ->{
                println("Bye!")
                exit = true
            }
        }
    }
}

data class Recipe(val category: String, val name: String, val ingredients: List<String>) {
    override fun toString(): String {
        return buildString {
            append("Name: $name\n")
            append("Ingredients:\n")
            ingredients.forEach { ingredient ->
                append("$ingredient\n")
            }
        }
    }
}

fun show(connection: Connection){
    var isValidAnswer = false
    var answer = ""
    val validAnswers = setOf("breakfast", "lunch", "dinner")

    while (!isValidAnswer){
        println("Which category do you want to print (breakfast, lunch, dinner)?")
        answer = readln().lowercase()

        isValidAnswer = answer in validAnswers

        if (!isValidAnswer) { println("Wrong meal category! Choose from: breakfast, lunch, dinner.") }
    }


    val recipes = getMealsFromDB(connection)

    when (answer){
        "breakfast" -> {
            val breakfastRecipes = recipes.filter { it.category == "breakfast" }
            if (breakfastRecipes.isEmpty()){
                println("No meals found.")
            }
            else{
                println("Category: breakfast\n")
                breakfastRecipes.forEach{
                    println(it)
                }
            }
        }
        "lunch" -> {
            val lunchRecipes = recipes.filter { it.category == "lunch" }
            if (lunchRecipes.isEmpty()){
                println("No meals found.")
            }
            else{
                println("Category: lunch\n")
                lunchRecipes.forEach{
                    println(it)
                }
            }
        }
        "dinner" -> {
            val dinnerRecipes = recipes.filter { it.category == "dinner" }
            if (dinnerRecipes.isEmpty()){
                println("No meals found.")
            }
            else{
                println("Category: dinner\n")
                dinnerRecipes.forEach{
                    println(it)
                }
            }
        }
    }
}

fun add(connection: Connection): Recipe{
    val validCategories = setOf("breakfast", "lunch", "dinner")
    var isTrueMeal = false
    var isValidName = false
    var isValidIngredients = false

    var category = ""
    var mealName = ""
    var ingredList: List<String> = listOf("")

    while(!isTrueMeal){
        println("Which meal do you want to add (breakfast, lunch, dinner)?")
        category = readln()

        if (category.lowercase() in validCategories){
            isTrueMeal = true
        }else{
            println("Wrong meal category! Choose from: breakfast, lunch, dinner.")
        }
    }
    while(!isValidName){
        println("Input the meal's name:")
        mealName = readln().trim()

        if (mealName.isEmpty()){
            println("Wrong format. Use letters only!")
        }else{
            val allowedChars = ('a'..'z') + ('A'..'Z') + ' '
            val isOnlyLetters = mealName.all { it in allowedChars}

            if(isOnlyLetters){
                isValidName = true
            }else{
                println("Wrong format. Use letters only!")
            }
        }
    }
    while(!isValidIngredients){
        println("Input the ingredients:")
        val ingredients = readln().trim()

        if (ingredients.isEmpty()){
            println("Wrong format. The ingredients cannot be empty. Use letters, spaces, and commas only!")
        } else {
            val allowedChars = ('a'..'z') + ('A'..'Z') + ' ' + ','
            val isValidIngredientFormat = ingredients.all { it in allowedChars }

            if (isValidIngredientFormat && ingredients.split(",").all { it.trim().isNotEmpty() }) {
                ingredList = ingredients.split(",").map { it.trim() }
                isValidIngredients = true
            } else {
                println("Wrong format. Use letters only!")
            }
        }
    }
    val recipe = Recipe(category, mealName, ingredList)
    addRecipeToDB(connection, recipe)

    return recipe
}

fun plan(connection: Connection){
    val mealsInDb = getMealsFromDB(connection)
    val plannedMeals = mutableMapOf<String, List<Recipe>>()
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    daysOfWeek.forEach() { dayOfWeek ->
        println(dayOfWeek)
        var validBreakfastAnswer = false
        var validLunchAnswer = false
        var validDinnerAnswer = false
        val recipesAdded = mutableListOf<Recipe>()

        while (!validBreakfastAnswer) {
            val breakfastMeals = mealsInDb.filter { it.category == "breakfast" }.sortedBy { it.name }
            breakfastMeals.forEach(){
                println(it.name)
            }
            println("Choose the breakfast for $dayOfWeek from the list above: ")
            val answer = readln().lowercase()

            val selectedRecipe = breakfastMeals.find { it.name == answer }
            if (selectedRecipe != null) {
                addToPlan(connection, selectedRecipe, dayOfWeek)
                recipesAdded.add(selectedRecipe)
                validBreakfastAnswer = true
            }else {
                println("This meal doesn’t exist. Choose a meal from the list above.")
            }
        }
        while (!validLunchAnswer) {
            val lunchMeals = mealsInDb.filter { it.category == "lunch" }.sortedBy { it.name }
            lunchMeals.forEach(){
                println(it.name)
            }
            println("Choose the lunch for $dayOfWeek from the list above: ")
            val answer = readln().lowercase()

            val selectedRecipe = lunchMeals.find { it.name == answer }
            if (selectedRecipe != null) {
                addToPlan(connection, selectedRecipe, dayOfWeek)
                recipesAdded.add(selectedRecipe)
                validLunchAnswer = true
            }else {
                println("This meal doesn’t exist. Choose a meal from the list above.")
            }
        }
        while (!validDinnerAnswer) {
            val dinnerMeals = mealsInDb.filter { it.category == "dinner" }.sortedBy { it.name }
            dinnerMeals.forEach(){
                println(it.name)
            }
            println("Choose the dinner for $dayOfWeek from the list above: ")
            val answer = readln().lowercase()

            val selectedRecipe = dinnerMeals.find { it.name == answer }
            if (selectedRecipe != null) {
                addToPlan(connection, selectedRecipe, dayOfWeek)
                recipesAdded.add(selectedRecipe)
                validDinnerAnswer = true
            }else {
                println("This meal doesn’t exist. Choose a meal from the list above.")
            }
        }
        println("Yeah! We planned the meals for $dayOfWeek.\n")
        plannedMeals[dayOfWeek] = recipesAdded
    }

    plannedMeals.forEach() { (dayOfWeek, recipe) ->
        val breakfastMeal = recipe.find { it.category == "breakfast" }
        val lunchMeal = recipe.find {it.category == "lunch"}
        val dinnerMeal = recipe.find { it.category == "dinner" }

        println("")
        println(dayOfWeek)
        println("Breakfast: ${breakfastMeal?.name}")
        println("Lunch: ${lunchMeal?.name}")
        println("Dinner: ${dinnerMeal?.name}")
    }
}

fun saveMeal(connection: Connection){
    val ingredientList = getPlanIngredients(connection)

    if(ingredientList.isEmpty()){
        println("Unable to save. Plan your meals first.")
    }else {
        println("Input a filename:")
        val fileName = readln()
        val file = File(fileName)

        ingredientList.forEach{
            file.appendText("$it\n")
        }

        println("Saved!")
    }
}