package mealplanner

import java.sql.Connection
import java.sql.Statement
import java.time.DayOfWeek

object DatabaseOperations {
    fun initializeDB(connection: Connection) {
        val statement = connection.createStatement()
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS meals (" +
                    "meal_id INTEGER PRIMARY KEY, " +
                    "category TEXT, " +
                    "meal TEXT)"
        )
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS ingredients (" +
                    "ingredient_id INTEGER PRIMARY KEY, " +
                    "ingredient TEXT, " +
                    "meal_id INTEGER, " +
                    "FOREIGN KEY(meal_id) REFERENCES meals(meal_id))"
        )
        statement.executeUpdate(
            """
                CREATE TABLE IF NOT EXISTS plan (
                    day_of_week TEXT,
                    meal_name TEXT,
                    meal_category TEXT,
                    meal_id INTEGER,
                    FOREIGN KEY(meal_id) REFERENCES meals(meal_id)
                )
            """.trimIndent()
                )

        statement.close()
    }

    fun getMealsFromDB(connection: Connection): MutableList<Recipe> {
        val statement = connection.createStatement()
        val recipes = mutableListOf<Recipe>()

        val resultSet = statement.executeQuery(
            """
        SELECT m.category, m.meal, i.ingredient
        FROM meals m
        LEFT JOIN ingredients i ON m.meal_id = i.meal_id
        ORDER BY m.meal_id ASC, i.ingredient_id"""
        )

        var currentMeal: String? = null
        var currentCategory: String? = null
        var currentIngredients = mutableListOf<String>()

        while (resultSet.next()) {
            val category = resultSet.getString("category")
            val meal = resultSet.getString("meal")
            val ingredient = resultSet.getString("ingredient")

            if (meal != currentMeal) {
                if (currentMeal != null) {
                    recipes.add(Recipe(currentCategory!!, currentMeal!!, currentIngredients.toList()))
                    currentIngredients.clear()
                }
                currentMeal = meal
                currentCategory = category
                currentIngredients = mutableListOf<String>() // Create a new list for the new meal
            }

            if (ingredient != null && ingredient.isNotBlank()) { // Check if ingredient is not null and not blank
                currentIngredients.add(ingredient)
            }
        }

        if (currentMeal != null) {
            recipes.add(Recipe(currentCategory!!, currentMeal!!, currentIngredients.toList()))
        }

        statement.close()
        return recipes
    }

    fun addRecipeToDB(
        connection: Connection,
        recipe: Recipe
    ) {
        // Prepare and execute statement to insert the meal
        val mealStatement = connection.prepareStatement(
            "INSERT INTO meals (category, meal) VALUES (?, ?)",
            Statement.RETURN_GENERATED_KEYS
        )
        mealStatement.setString(1, recipe.category)
        mealStatement.setString(2, recipe.name)
        mealStatement.executeUpdate()

        // Retrieve the generated meal_id
        val generatedKeys = mealStatement.generatedKeys
        if (generatedKeys.next()) {
            val mealId = generatedKeys.getInt(1)

            // Insert each ingredient with the meal_id as a foreign key
            recipe.ingredients.forEach { ingredient ->
                val ingredientStatement = connection.prepareStatement(
                    "INSERT INTO ingredients (ingredient, meal_id) VALUES (?, ?)"
                )
                ingredientStatement.setString(1, ingredient)
                ingredientStatement.setInt(2, mealId)
                ingredientStatement.executeUpdate()
            }
        }
    }

    fun addToPlan(
        connection: Connection,
        recipe: Recipe,
        dayOfWeek: String
    ){
        var mealId: Int

        val mealIdStatement = connection.prepareStatement("""
            SELECT meal_id
            FROM meals
            WHERE meal = ?
        """.trimIndent())
        mealIdStatement.setString(1, recipe.name)
        val mealIdQuery = mealIdStatement.executeQuery()

        if (mealIdQuery.next()){
            mealId = mealIdQuery.getInt(1)
        }else return

        val checkStatement = connection.prepareStatement("""
            SELECT *
            FROM plan
            WHERE day_of_week = ? AND meal_category = ?
        """.trimIndent())
        checkStatement.setString(1, dayOfWeek)
        checkStatement.setString(2, recipe.category)
        val checkQuery = checkStatement.executeQuery()

        if (checkQuery.next()){
            val updateStatement = connection.prepareStatement("""
                UPDATE plan
                SET meal_name = ?,
                    meal_id = ?
                WHERE day_of_week = ? and meal_category = ?
            """.trimIndent())
            updateStatement.setString(1, recipe.name)
            updateStatement.setString(2, mealId.toString())
            updateStatement.setString(3, dayOfWeek)
            updateStatement.setString(4, recipe.category)
            updateStatement.executeUpdate()
            updateStatement.close()
        }else {
            val insertStatement = connection.prepareStatement("""
            INSERT INTO plan (day_of_week, meal_name, meal_category, meal_id)
            VALUES (?, ?, ?, ?)
        """.trimIndent())
            insertStatement.setString(1, dayOfWeek)
            insertStatement.setString(2, recipe.name)
            insertStatement.setString(3, recipe.category)
            insertStatement.setInt(4, mealId)
            insertStatement.executeUpdate()
            insertStatement.close()
        }
        mealIdQuery.close()
        checkQuery.close()
    }

    fun getPlanIngredients(connection: Connection): List<String> {
        val ingredientMap = mutableMapOf<String, Int>()

        connection.createStatement().use { planStatement ->
            val planResults = planStatement.executeQuery("SELECT meal_id FROM plan")
            while (planResults.next()) {
                val mealId = planResults.getString("meal_id")

                connection.createStatement().use { ingredientStatement ->
                    val ingredientList = ingredientStatement.executeQuery("SELECT ingredient FROM ingredients WHERE meal_id = $mealId")
                    while (ingredientList.next()) {
                        val ingredientName = ingredientList.getString(1)
                        ingredientMap[ingredientName] = ingredientMap.getOrDefault(ingredientName, 0) + 1
                    }
                }
            }
        }

        return ingredientMap.map {
            if (it.value > 1) "${it.key} x${it.value}" else it.key
        }
    }
}