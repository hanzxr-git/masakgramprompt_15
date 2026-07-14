package util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.IngredientResult;
import model.NutritionResult;

import java.util.Map;

/**
 * Converts the LLM JSON response into NutritionResult and IngredientResult objects.
 *
 * This parser is forgiving because local LLMs may:
 * - add markdown fences such as ```json
 * - add explanation before/after JSON
 * - use nested wrapper objects such as nutrition_result/result/data
 * - use slightly different key names
 * - return ingredients as an array or object map
 */
public class JsonParserUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonParserUtil() {
        // Utility class.
    }

    public static NutritionResult parseNutritionResult(String llmResponse, int experimentId) {
        NutritionResult result = new NutritionResult();
        result.setExperimentId(experimentId);
        result.setRawJsonOutput(llmResponse);

        String jsonText = extractJsonObject(llmResponse);
        if (jsonText == null) {
            result.setJsonValid(false);
            return result;
        }

        try {
            JsonElement rootElement = JsonParser.parseString(jsonText);

            if (!rootElement.isJsonObject()) {
                result.setJsonValid(false);
                return result;
            }

            JsonObject root = unwrapRoot(rootElement.getAsJsonObject());

            result.setRawJsonOutput(GSON.toJson(root));
            result.setJsonValid(true);

            JsonObject recipeInfo = firstObject(root,
                    "recipe_info",
                    "recipe_details",
                    "metadata",
                    "recipe_metadata"
            );

            result.setRecipeName(firstString(root, recipeInfo,
                    "recipe_name",
                    "dish_name",
                    "title",
                    "name",
                    "recipe_title"
            ));

            result.setServingsEstimated(firstInteger(root, recipeInfo,
                    "servings_estimated",
                    "servings",
                    "number_of_servings",
                    "serving_count",
                    "estimated_servings"
            ));

            JsonObject serving = firstObject(root,
                    "amount_per_serving",
                    "serving",
                    "per_serving",
                    "serving_nutrition",
                    "nutrition_per_serving",
                    "per_serving_nutrition",
                    "nutrition_facts_per_serving"
            );

            JsonObject total = firstObject(root,
                    "total_recipe",
                    "recipe_total",
                    "total",
                    "totals",
                    "total_nutrition",
                    "whole_recipe",
                    "nutrition_total",
                    "nutrition_facts_total",
                    "total_nutrition_facts"
            );

            result.setServingCalories(firstDouble(root, serving,
                    "serving_calories",
                    "calories_per_serving",
                    "calorie_per_serving",
                    "energy_per_serving_kcal",
                    "serving_energy_kcal",
                    "calories",
                    "energy_kcal",
                    "kcal"
            ));

            result.setServingTotalFatG(firstDouble(root, serving,
                    "serving_total_fat_g",
                    "total_fat_per_serving_g",
                    "fat_per_serving_g",
                    "total_fat_g",
                    "fat_g"
            ));

            result.setServingSaturatedFatG(firstDouble(root, serving,
                    "serving_saturated_fat_g",
                    "saturated_fat_per_serving_g",
                    "saturated_fat_g"
            ));

            result.setServingCholesterolMg(firstDouble(root, serving,
                    "serving_cholesterol_mg",
                    "cholesterol_per_serving_mg",
                    "cholesterol_mg"
            ));

            result.setServingSodiumMg(firstDouble(root, serving,
                    "serving_sodium_mg",
                    "sodium_per_serving_mg",
                    "sodium_mg"
            ));

            result.setServingCarbohydrateG(firstDouble(root, serving,
                    "serving_carbohydrate_g",
                    "serving_total_carbohydrate_g",
                    "carbohydrate_per_serving_g",
                    "total_carbohydrate_g",
                    "carbohydrate_g",
                    "carbs_g"
            ));

            result.setServingFiberG(firstDouble(root, serving,
                    "serving_fiber_g",
                    "serving_dietary_fiber_g",
                    "fiber_per_serving_g",
                    "dietary_fiber_g",
                    "fiber_g"
            ));

            result.setServingSugarsG(firstDouble(root, serving,
                    "serving_sugars_g",
                    "serving_total_sugars_g",
                    "sugars_per_serving_g",
                    "total_sugars_g",
                    "sugars_g",
                    "sugar_g"
            ));

            result.setServingProteinG(firstDouble(root, serving,
                    "serving_protein_g",
                    "protein_per_serving_g",
                    "protein_g"
            ));

            result.setServingVitaminDMcg(firstDouble(root, serving,
                    "serving_vitamin_d_mcg",
                    "vitamin_d_per_serving_mcg",
                    "vitamin_d_mcg"
            ));

            result.setServingCalciumMg(firstDouble(root, serving,
                    "serving_calcium_mg",
                    "calcium_per_serving_mg",
                    "calcium_mg"
            ));

            result.setServingIronMg(firstDouble(root, serving,
                    "serving_iron_mg",
                    "iron_per_serving_mg",
                    "iron_mg"
            ));

            result.setServingPotassiumMg(firstDouble(root, serving,
                    "serving_potassium_mg",
                    "potassium_per_serving_mg",
                    "potassium_mg"
            ));

            result.setTotalCalories(firstDouble(root, total,
                    "total_calories",
                    "total_energy_kcal",
                    "total_kcal",
                    "recipe_calories",
                    "recipe_energy_kcal",
                    "calories",
                    "energy_kcal",
                    "kcal"
            ));

            result.setTotalFatG(firstDouble(root, total,
                    "total_fat_g",
                    "recipe_total_fat_g",
                    "fat_g"
            ));

            result.setTotalSaturatedFatG(firstDouble(root, total,
                    "total_saturated_fat_g",
                    "recipe_saturated_fat_g",
                    "saturated_fat_g"
            ));

            result.setTotalCholesterolMg(firstDouble(root, total,
                    "total_cholesterol_mg",
                    "recipe_cholesterol_mg",
                    "cholesterol_mg"
            ));

            result.setTotalSodiumMg(firstDouble(root, total,
                    "total_sodium_mg",
                    "recipe_sodium_mg",
                    "sodium_mg"
            ));

            result.setTotalCarbohydrateG(firstDouble(root, total,
                    "total_carbohydrate_g",
                    "recipe_total_carbohydrate_g",
                    "carbohydrate_g",
                    "carbs_g"
            ));

            result.setTotalFiberG(firstDouble(root, total,
                    "total_fiber_g",
                    "total_dietary_fiber_g",
                    "recipe_fiber_g",
                    "dietary_fiber_g",
                    "fiber_g"
            ));

            result.setTotalSugarsG(firstDouble(root, total,
                    "total_sugars_g",
                    "recipe_total_sugars_g",
                    "sugars_g",
                    "sugar_g"
            ));

            result.setTotalProteinG(firstDouble(root, total,
                    "total_protein_g",
                    "recipe_protein_g",
                    "protein_g"
            ));

            result.setTotalVitaminDMcg(firstDouble(root, total,
                    "total_vitamin_d_mcg",
                    "recipe_vitamin_d_mcg",
                    "vitamin_d_mcg"
            ));

            result.setTotalCalciumMg(firstDouble(root, total,
                    "total_calcium_mg",
                    "recipe_calcium_mg",
                    "calcium_mg"
            ));

            result.setTotalIronMg(firstDouble(root, total,
                    "total_iron_mg",
                    "recipe_iron_mg",
                    "iron_mg"
            ));

            result.setTotalPotassiumMg(firstDouble(root, total,
                    "total_potassium_mg",
                    "recipe_potassium_mg",
                    "potassium_mg"
            ));

            JsonElement ingredientsElement = firstElement(root, 3,
                    "ingredients",
                    "ingredient_results",
                    "ingredient_result",
                    "items",
                    "extracted_ingredients",
                    "ingredient_list",
                    "ingredients_list"
            );

            parseIngredientsIntoResult(ingredientsElement, result);

            return result;

        } catch (Exception e) {
            result.setJsonValid(false);
            return result;
        }
    }

    public static String extractJsonObject(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String cleaned = text.trim();

        cleaned = cleaned.replace("```json", "")
                .replace("```JSON", "")
                .replace("```Json", "")
                .replace("```", "")
                .trim();

        String bestCandidate = null;
        int bestScore = -1;

        for (int i = 0; i < cleaned.length(); i++) {
            if (cleaned.charAt(i) != '{') {
                continue;
            }

            String candidate = extractBalancedObjectFrom(cleaned, i);
            if (candidate == null) {
                continue;
            }

            candidate = sanitizeJson(candidate);

            try {
                JsonElement parsed = JsonParser.parseString(candidate);
                if (parsed != null && parsed.isJsonObject()) {
                    int score = scoreJsonCandidate(parsed.getAsJsonObject(), candidate.length());
                    if (score > bestScore) {
                        bestScore = score;
                        bestCandidate = candidate;
                    }
                }
            } catch (Exception ignored) {
                // Try next candidate.
            }
        }

        return bestCandidate;
    }

    public static boolean isValidJson(String text) {
        String json = extractJsonObject(text);
        if (json == null) {
            return false;
        }

        try {
            JsonParser.parseString(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractBalancedObjectFrom(String text, int startIndex) {
        int braceCount = 0;
        boolean insideString = false;
        boolean escaped = false;

        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                insideString = !insideString;
                continue;
            }

            if (!insideString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;

                    if (braceCount == 0) {
                        return text.substring(startIndex, i + 1);
                    }
                }
            }
        }

        return null;
    }

    private static String sanitizeJson(String json) {
        if (json == null) {
            return null;
        }

        String cleaned = json.trim();

        // Remove trailing commas before object/array endings.
        cleaned = cleaned.replaceAll(",\\s*}", "}");
        cleaned = cleaned.replaceAll(",\\s*]", "]");

        // Remove invisible control characters except normal whitespace.
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        return cleaned;
    }

    private static int scoreJsonCandidate(JsonObject object, int length) {
        int score = 0;

        if (hasAny(object, "recipe_name", "dish_name", "title", "name")) {
            score += 100;
        }

        if (firstElement(object, 3,
                "ingredients",
                "ingredient_results",
                "ingredient_result",
                "items",
                "extracted_ingredients",
                "ingredient_list",
                "ingredients_list") != null) {
            score += 100;
        }

        if (hasAny(object,
                "serving_calories",
                "total_calories",
                "amount_per_serving",
                "total_recipe",
                "nutrition_result",
                "result",
                "analysis",
                "nutritional_analysis")) {
            score += 50;
        }

        score += Math.min(length / 100, 50);

        return score;
    }

    private static JsonObject unwrapRoot(JsonObject root) {
        if (root == null) {
            return null;
        }

        if (hasAny(root,
                "recipe_name",
                "dish_name",
                "title",
                "serving_calories",
                "total_calories",
                "ingredients",
                "ingredient_results",
                "ingredient_list")) {
            return root;
        }

        String[] wrapperNames = {
                "nutrition_result",
                "result",
                "analysis",
                "nutritional_analysis",
                "nutrition_analysis",
                "output",
                "data",
                "response"
        };

        for (String wrapperName : wrapperNames) {
            JsonElement element = root.get(wrapperName);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        }

        if (root.entrySet().size() == 1) {
            JsonElement onlyValue = root.entrySet().iterator().next().getValue();
            if (onlyValue != null && onlyValue.isJsonObject()) {
                return onlyValue.getAsJsonObject();
            }
        }

        return root;
    }

    private static void parseIngredientsIntoResult(JsonElement ingredientsElement, NutritionResult result) {
        if (ingredientsElement == null || ingredientsElement.isJsonNull()) {
            return;
        }

        if (ingredientsElement.isJsonArray()) {
            JsonArray array = ingredientsElement.getAsJsonArray();
            for (JsonElement element : array) {
                if (element != null && element.isJsonObject()) {
                    result.addIngredientResult(parseIngredient(element.getAsJsonObject(), null));
                }
            }
            return;
        }

        if (ingredientsElement.isJsonObject()) {
            JsonObject object = ingredientsElement.getAsJsonObject();

            if (looksLikeIngredient(object)) {
                result.addIngredientResult(parseIngredient(object, null));
                return;
            }

            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue() != null && entry.getValue().isJsonObject()) {
                    result.addIngredientResult(parseIngredient(entry.getValue().getAsJsonObject(), entry.getKey()));
                }
            }
        }
    }

    private static IngredientResult parseIngredient(JsonObject object, String fallbackName) {
        IngredientResult ingredient = new IngredientResult();

        JsonObject nutrition = firstObject(object,
                "nutrition",
                "nutrients",
                "nutrition_values",
                "nutritional_values",
                "nutrition_per_ingredient"
        );

        JsonObject quantity = firstObject(object,
                "quantity",
                "amount_info",
                "quantity_info",
                "measurement"
        );

        String nameOriginal = firstString(object, null,
                "name_original",
                "ingredient_name_raw",
                "ingredient_raw",
                "raw_name",
                "ingredient_name",
                "name",
                "ingredient"
        );

        if (isBlank(nameOriginal)) {
            nameOriginal = fallbackName;
        }

        ingredient.setNameOriginal(nameOriginal);

        ingredient.setNameEn(firstString(object, null,
                "name_en",
                "ingredient_name_en",
                "english_name",
                "name_english",
                "translated_name",
                "ingredient_english"
        ));

        ingredient.setQuantityExpression(firstString(object, quantity,
                "quantity_expression",
                "quantity_raw",
                "quantity_text",
                "quantity",
                "amount_text",
                "measurement_text"
        ));

        ingredient.setQuantityCategory(firstString(object, quantity,
                "quantity_category",
                "category",
                "quantity_type"
        ));

        ingredient.setQuantityValue(firstDouble(object, quantity,
                "quantity_value",
                "quantity_number",
                "amount",
                "value",
                "count"
        ));

        ingredient.setUnitOriginal(firstString(object, quantity,
                "unit_original",
                "unit_raw",
                "unit",
                "quantity_unit",
                "quantity_unit_culinary",
                "measurement_unit"
        ));

        ingredient.setUnitEn(firstString(object, quantity,
                "unit_en",
                "unit_english",
                "english_unit",
                "unit_translated"
        ));

        ingredient.setLanguageTag(firstString(object, null,
                "language_tag",
                "language",
                "language_mentioned"
        ));

        ingredient.setHallucinated(firstBoolean(object, null,
                "is_hallucinated",
                "hallucinated",
                "hallucination_flag"
        ));

        ingredient.setEstimatedWeightG(firstDouble(object, quantity,
                "estimated_weight_g",
                "estimated_weight",
                "weight_g",
                "grams",
                "gram"
        ));

        ingredient.setCalories(firstDouble(object, nutrition,
                "calories",
                "energy_kcal",
                "kcal"
        ));

        ingredient.setTotalFatG(firstDouble(object, nutrition,
                "total_fat_g",
                "fat_g"
        ));

        ingredient.setSaturatedFatG(firstDouble(object, nutrition,
                "saturated_fat_g"
        ));

        ingredient.setCholesterolMg(firstDouble(object, nutrition,
                "cholesterol_mg"
        ));

        ingredient.setSodiumMg(firstDouble(object, nutrition,
                "sodium_mg"
        ));

        ingredient.setTotalCarbohydrateG(firstDouble(object, nutrition,
                "total_carbohydrate_g",
                "carbohydrate_g",
                "carbs_g"
        ));

        ingredient.setDietaryFiberG(firstDouble(object, nutrition,
                "dietary_fiber_g",
                "fiber_g"
        ));

        ingredient.setTotalSugarsG(firstDouble(object, nutrition,
                "total_sugars_g",
                "sugars_g",
                "sugar_g"
        ));

        ingredient.setProteinG(firstDouble(object, nutrition,
                "protein_g"
        ));

        ingredient.setVitaminDMcg(firstDouble(object, nutrition,
                "vitamin_d_mcg"
        ));

        ingredient.setCalciumMg(firstDouble(object, nutrition,
                "calcium_mg"
        ));

        ingredient.setIronMg(firstDouble(object, nutrition,
                "iron_mg"
        ));

        ingredient.setPotassiumMg(firstDouble(object, nutrition,
                "potassium_mg"
        ));

        return ingredient;
    }

    private static JsonObject firstObject(JsonObject root, String... names) {
        JsonElement element = firstElement(root, 2, names);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return null;
    }

    private static JsonElement firstElement(JsonObject root, int maxDepth, String... names) {
        if (root == null || maxDepth < 0) {
            return null;
        }

        for (String name : names) {
            JsonElement element = root.get(name);
            if (element != null && !element.isJsonNull()) {
                return element;
            }
        }

        if (maxDepth == 0) {
            return null;
        }

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            JsonElement value = entry.getValue();

            if (value != null && value.isJsonObject()) {
                JsonElement found = firstElement(value.getAsJsonObject(), maxDepth - 1, names);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static boolean looksLikeIngredient(JsonObject object) {
        return hasAny(object,
                "name_original",
                "ingredient_name_raw",
                "ingredient_raw",
                "raw_name",
                "ingredient_name",
                "name",
                "ingredient",
                "calories",
                "energy_kcal",
                "estimated_weight_g",
                "weight_g"
        );
    }

    private static boolean hasAny(JsonObject object, String... names) {
        if (object == null) {
            return false;
        }

        for (String name : names) {
            if (object.has(name) && !object.get(name).isJsonNull()) {
                return true;
            }
        }

        return false;
    }

    private static String firstString(JsonObject root, JsonObject nestedObject, String... names) {
        if (nestedObject != null) {
            String nested = getString(nestedObject, names);
            if (!isBlank(nested)) {
                return nested;
            }
        }

        return getString(root, names);
    }

    private static Integer firstInteger(JsonObject root, JsonObject nestedObject, String... names) {
        Double value = firstDouble(root, nestedObject, names);
        return value == null ? null : value.intValue();
    }

    private static Double firstDouble(JsonObject root, JsonObject nestedObject, String... names) {
        if (nestedObject != null) {
            Double nested = getDouble(nestedObject, names);
            if (nested != null) {
                return nested;
            }
        }

        return getDouble(root, names);
    }

    private static Boolean firstBoolean(JsonObject root, JsonObject nestedObject, String... names) {
        if (nestedObject != null) {
            Boolean nested = getBoolean(nestedObject, names);
            if (nested != null) {
                return nested;
            }
        }

        return getBoolean(root, names);
    }

    private static String getString(JsonObject object, String... names) {
        if (object == null) {
            return null;
        }

        for (String name : names) {
            JsonElement element = object.get(name);
            if (element == null || element.isJsonNull()) {
                continue;
            }

            try {
                if (element.isJsonPrimitive()) {
                    String value = element.getAsString();
                    return value == null ? null : value.trim();
                }

                if (element.isJsonObject()) {
                    JsonObject child = element.getAsJsonObject();
                    String nested = getString(child,
                            "recipe_name",
                            "dish_name",
                            "title",
                            "name",
                            "value",
                            "text"
                    );
                    if (!isBlank(nested)) {
                        return nested;
                    }
                }
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    private static Double getDouble(JsonObject object, String... names) {
        if (object == null) {
            return null;
        }

        for (String name : names) {
            JsonElement element = object.get(name);
            if (element == null || element.isJsonNull()) {
                continue;
            }

            try {
                if (element.isJsonPrimitive()) {
                    String raw = element.getAsString().trim();

                    if (raw.isEmpty()
                            || raw.equalsIgnoreCase("null")
                            || raw.equalsIgnoreCase("unknown")
                            || raw.equalsIgnoreCase("not mentioned")
                            || raw.equalsIgnoreCase("n/a")) {
                        return null;
                    }

                    raw = raw.replace(",", "");
                    raw = raw.replaceAll("[^0-9+\\-.]", "");

                    if (raw.isEmpty()
                            || raw.equals("-")
                            || raw.equals(".")
                            || raw.equals("+")) {
                        return null;
                    }

                    return Double.parseDouble(raw);
                }

                if (element.isJsonObject()) {
                    Double nested = getDouble(element.getAsJsonObject(),
                            "value",
                            "amount",
                            "number",
                            "estimated",
                            "estimate"
                    );

                    if (nested != null) {
                        return nested;
                    }
                }

            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    private static Boolean getBoolean(JsonObject object, String... names) {
        if (object == null) {
            return null;
        }

        for (String name : names) {
            JsonElement element = object.get(name);
            if (element == null || element.isJsonNull()) {
                continue;
            }

            try {
                if (element.isJsonPrimitive()) {
                    String raw = element.getAsString().trim().toLowerCase();

                    if (raw.equals("true")
                            || raw.equals("yes")
                            || raw.equals("1")
                            || raw.equals("y")) {
                        return true;
                    }

                    if (raw.equals("false")
                            || raw.equals("no")
                            || raw.equals("0")
                            || raw.equals("n")) {
                        return false;
                    }
                }
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}