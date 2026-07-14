package model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NutritionResult {

    private int resultId;
    private int experimentId;
    private String recipeName;
    private Integer servingsEstimated;

    private Double servingCalories;
    private Double servingTotalFatG;
    private Double servingSaturatedFatG;
    private Double servingCholesterolMg;
    private Double servingSodiumMg;
    private Double servingCarbohydrateG;
    private Double servingFiberG;
    private Double servingSugarsG;
    private Double servingProteinG;
    private Double servingVitaminDMcg;
    private Double servingCalciumMg;
    private Double servingIronMg;
    private Double servingPotassiumMg;

    private Double totalCalories;
    private Double totalFatG;
    private Double totalSaturatedFatG;
    private Double totalCholesterolMg;
    private Double totalSodiumMg;
    private Double totalCarbohydrateG;
    private Double totalFiberG;
    private Double totalSugarsG;
    private Double totalProteinG;
    private Double totalVitaminDMcg;
    private Double totalCalciumMg;
    private Double totalIronMg;
    private Double totalPotassiumMg;

    private String rawJsonOutput;
    private boolean jsonValid;
    private Timestamp createdAt;

    private List<IngredientResult> ingredientResults = new ArrayList<>();

    public NutritionResult() {
    }

    public int getResultId() {
        return resultId;
    }

    public void setResultId(int resultId) {
        this.resultId = resultId;
    }

    public int getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(int experimentId) {
        this.experimentId = experimentId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public Integer getServingsEstimated() {
        return servingsEstimated;
    }

    public void setServingsEstimated(Integer servingsEstimated) {
        this.servingsEstimated = servingsEstimated;
    }

    public Double getServingCalories() {
        return servingCalories;
    }

    public void setServingCalories(Double servingCalories) {
        this.servingCalories = servingCalories;
    }

    public Double getServingTotalFatG() {
        return servingTotalFatG;
    }

    public void setServingTotalFatG(Double servingTotalFatG) {
        this.servingTotalFatG = servingTotalFatG;
    }

    public Double getServingSaturatedFatG() {
        return servingSaturatedFatG;
    }

    public void setServingSaturatedFatG(Double servingSaturatedFatG) {
        this.servingSaturatedFatG = servingSaturatedFatG;
    }

    public Double getServingCholesterolMg() {
        return servingCholesterolMg;
    }

    public void setServingCholesterolMg(Double servingCholesterolMg) {
        this.servingCholesterolMg = servingCholesterolMg;
    }

    public Double getServingSodiumMg() {
        return servingSodiumMg;
    }

    public void setServingSodiumMg(Double servingSodiumMg) {
        this.servingSodiumMg = servingSodiumMg;
    }

    public Double getServingCarbohydrateG() {
        return servingCarbohydrateG;
    }

    public void setServingCarbohydrateG(Double servingCarbohydrateG) {
        this.servingCarbohydrateG = servingCarbohydrateG;
    }

    public Double getServingFiberG() {
        return servingFiberG;
    }

    public void setServingFiberG(Double servingFiberG) {
        this.servingFiberG = servingFiberG;
    }

    public Double getServingSugarsG() {
        return servingSugarsG;
    }

    public void setServingSugarsG(Double servingSugarsG) {
        this.servingSugarsG = servingSugarsG;
    }

    public Double getServingProteinG() {
        return servingProteinG;
    }

    public void setServingProteinG(Double servingProteinG) {
        this.servingProteinG = servingProteinG;
    }

    public Double getServingVitaminDMcg() {
        return servingVitaminDMcg;
    }

    public void setServingVitaminDMcg(Double servingVitaminDMcg) {
        this.servingVitaminDMcg = servingVitaminDMcg;
    }

    public Double getServingCalciumMg() {
        return servingCalciumMg;
    }

    public void setServingCalciumMg(Double servingCalciumMg) {
        this.servingCalciumMg = servingCalciumMg;
    }

    public Double getServingIronMg() {
        return servingIronMg;
    }

    public void setServingIronMg(Double servingIronMg) {
        this.servingIronMg = servingIronMg;
    }

    public Double getServingPotassiumMg() {
        return servingPotassiumMg;
    }

    public void setServingPotassiumMg(Double servingPotassiumMg) {
        this.servingPotassiumMg = servingPotassiumMg;
    }

    public Double getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(Double totalCalories) {
        this.totalCalories = totalCalories;
    }

    public Double getTotalFatG() {
        return totalFatG;
    }

    public void setTotalFatG(Double totalFatG) {
        this.totalFatG = totalFatG;
    }

    public Double getTotalSaturatedFatG() {
        return totalSaturatedFatG;
    }

    public void setTotalSaturatedFatG(Double totalSaturatedFatG) {
        this.totalSaturatedFatG = totalSaturatedFatG;
    }

    public Double getTotalCholesterolMg() {
        return totalCholesterolMg;
    }

    public void setTotalCholesterolMg(Double totalCholesterolMg) {
        this.totalCholesterolMg = totalCholesterolMg;
    }

    public Double getTotalSodiumMg() {
        return totalSodiumMg;
    }

    public void setTotalSodiumMg(Double totalSodiumMg) {
        this.totalSodiumMg = totalSodiumMg;
    }

    public Double getTotalCarbohydrateG() {
        return totalCarbohydrateG;
    }

    public void setTotalCarbohydrateG(Double totalCarbohydrateG) {
        this.totalCarbohydrateG = totalCarbohydrateG;
    }

    public Double getTotalFiberG() {
        return totalFiberG;
    }

    public void setTotalFiberG(Double totalFiberG) {
        this.totalFiberG = totalFiberG;
    }

    public Double getTotalSugarsG() {
        return totalSugarsG;
    }

    public void setTotalSugarsG(Double totalSugarsG) {
        this.totalSugarsG = totalSugarsG;
    }

    public Double getTotalProteinG() {
        return totalProteinG;
    }

    public void setTotalProteinG(Double totalProteinG) {
        this.totalProteinG = totalProteinG;
    }

    public Double getTotalVitaminDMcg() {
        return totalVitaminDMcg;
    }

    public void setTotalVitaminDMcg(Double totalVitaminDMcg) {
        this.totalVitaminDMcg = totalVitaminDMcg;
    }

    public Double getTotalCalciumMg() {
        return totalCalciumMg;
    }

    public void setTotalCalciumMg(Double totalCalciumMg) {
        this.totalCalciumMg = totalCalciumMg;
    }

    public Double getTotalIronMg() {
        return totalIronMg;
    }

    public void setTotalIronMg(Double totalIronMg) {
        this.totalIronMg = totalIronMg;
    }

    public Double getTotalPotassiumMg() {
        return totalPotassiumMg;
    }

    public void setTotalPotassiumMg(Double totalPotassiumMg) {
        this.totalPotassiumMg = totalPotassiumMg;
    }

    public String getRawJsonOutput() {
        return rawJsonOutput;
    }

    public void setRawJsonOutput(String rawJsonOutput) {
        this.rawJsonOutput = rawJsonOutput;
    }

    public boolean isJsonValid() {
        return jsonValid;
    }

    public void setJsonValid(boolean jsonValid) {
        this.jsonValid = jsonValid;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<IngredientResult> getIngredientResults() {
        return ingredientResults;
    }

    public void setIngredientResults(List<IngredientResult> ingredientResults) {
        this.ingredientResults = ingredientResults;
    }

    public void addIngredientResult(IngredientResult ingredientResult) {
        this.ingredientResults.add(ingredientResult);
    }

    @Override
    public String toString() {
        return "NutritionResult{" +
                "resultId=" + resultId +
                ", experimentId=" + experimentId +
                ", recipeName='" + recipeName + '\'' +
                ", servingsEstimated=" + servingsEstimated +
                ", servingCalories=" + servingCalories +
                ", ingredientCount=" + ingredientResults.size() +
                ", jsonValid=" + jsonValid +
                '}';
    }
}
