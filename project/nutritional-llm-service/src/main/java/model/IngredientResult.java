package model;

public class IngredientResult {

    private int ingredientId;
    private int resultId;

    private String nameOriginal;
    private String nameEn;
    private String quantityExpression;
    private String quantityCategory;
    private Double quantityValue;
    private String unitOriginal;
    private String unitEn;
    private String languageTag;
    private Boolean hallucinated;

    private Double estimatedWeightG;
    private Double calories;
    private Double totalFatG;
    private Double saturatedFatG;
    private Double cholesterolMg;
    private Double sodiumMg;
    private Double totalCarbohydrateG;
    private Double dietaryFiberG;
    private Double totalSugarsG;
    private Double proteinG;
    private Double vitaminDMcg;
    private Double calciumMg;
    private Double ironMg;
    private Double potassiumMg;

    public IngredientResult() {
    }

    public int getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(int ingredientId) {
        this.ingredientId = ingredientId;
    }

    public int getResultId() {
        return resultId;
    }

    public void setResultId(int resultId) {
        this.resultId = resultId;
    }

    public String getNameOriginal() {
        return nameOriginal;
    }

    public void setNameOriginal(String nameOriginal) {
        this.nameOriginal = nameOriginal;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getQuantityExpression() {
        return quantityExpression;
    }

    public void setQuantityExpression(String quantityExpression) {
        this.quantityExpression = quantityExpression;
    }

    public String getQuantityCategory() {
        return quantityCategory;
    }

    public void setQuantityCategory(String quantityCategory) {
        this.quantityCategory = quantityCategory;
    }

    public Double getQuantityValue() {
        return quantityValue;
    }

    public void setQuantityValue(Double quantityValue) {
        this.quantityValue = quantityValue;
    }

    public String getUnitOriginal() {
        return unitOriginal;
    }

    public void setUnitOriginal(String unitOriginal) {
        this.unitOriginal = unitOriginal;
    }

    public String getUnitEn() {
        return unitEn;
    }

    public void setUnitEn(String unitEn) {
        this.unitEn = unitEn;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    public void setLanguageTag(String languageTag) {
        this.languageTag = languageTag;
    }

    public Boolean getHallucinated() {
        return hallucinated;
    }

    public void setHallucinated(Boolean hallucinated) {
        this.hallucinated = hallucinated;
    }

    public Double getEstimatedWeightG() {
        return estimatedWeightG;
    }

    public void setEstimatedWeightG(Double estimatedWeightG) {
        this.estimatedWeightG = estimatedWeightG;
    }

    public Double getCalories() {
        return calories;
    }

    public void setCalories(Double calories) {
        this.calories = calories;
    }

    public Double getTotalFatG() {
        return totalFatG;
    }

    public void setTotalFatG(Double totalFatG) {
        this.totalFatG = totalFatG;
    }

    public Double getSaturatedFatG() {
        return saturatedFatG;
    }

    public void setSaturatedFatG(Double saturatedFatG) {
        this.saturatedFatG = saturatedFatG;
    }

    public Double getCholesterolMg() {
        return cholesterolMg;
    }

    public void setCholesterolMg(Double cholesterolMg) {
        this.cholesterolMg = cholesterolMg;
    }

    public Double getSodiumMg() {
        return sodiumMg;
    }

    public void setSodiumMg(Double sodiumMg) {
        this.sodiumMg = sodiumMg;
    }

    public Double getTotalCarbohydrateG() {
        return totalCarbohydrateG;
    }

    public void setTotalCarbohydrateG(Double totalCarbohydrateG) {
        this.totalCarbohydrateG = totalCarbohydrateG;
    }

    public Double getDietaryFiberG() {
        return dietaryFiberG;
    }

    public void setDietaryFiberG(Double dietaryFiberG) {
        this.dietaryFiberG = dietaryFiberG;
    }

    public Double getTotalSugarsG() {
        return totalSugarsG;
    }

    public void setTotalSugarsG(Double totalSugarsG) {
        this.totalSugarsG = totalSugarsG;
    }

    public Double getProteinG() {
        return proteinG;
    }

    public void setProteinG(Double proteinG) {
        this.proteinG = proteinG;
    }

    public Double getVitaminDMcg() {
        return vitaminDMcg;
    }

    public void setVitaminDMcg(Double vitaminDMcg) {
        this.vitaminDMcg = vitaminDMcg;
    }

    public Double getCalciumMg() {
        return calciumMg;
    }

    public void setCalciumMg(Double calciumMg) {
        this.calciumMg = calciumMg;
    }

    public Double getIronMg() {
        return ironMg;
    }

    public void setIronMg(Double ironMg) {
        this.ironMg = ironMg;
    }

    public Double getPotassiumMg() {
        return potassiumMg;
    }

    public void setPotassiumMg(Double potassiumMg) {
        this.potassiumMg = potassiumMg;
    }

    @Override
    public String toString() {
        return "IngredientResult{" +
                "ingredientId=" + ingredientId +
                ", resultId=" + resultId +
                ", nameOriginal='" + nameOriginal + '\'' +
                ", nameEn='" + nameEn + '\'' +
                ", quantityValue=" + quantityValue +
                ", unitOriginal='" + unitOriginal + '\'' +
                '}';
    }
}
