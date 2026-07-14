package edu.utem.ftmk.llm;

/**
 * Standalone runner to verify all local Ollama models are reachable.
 */
public class LLMServiceTest {

    public static void main(String[] args) {
        LLMService service = new LLMService();
        String testPrompt = "Name one common cooking ingredient. Reply in one sentence.";

        String[] models = {
                LLMService.LLAMA,
                LLMService.PHI,
                LLMService.QWEN,
                LLMService.SEALION,
                LLMService.MEDGEMMA
        };

        for (String model : models) {
            System.out.println("--------------------------------------------------");
            System.out.println("Model    : " + model);
            System.out.print("Response : ");
            try {
                String response = service.prompt(model, testPrompt);
                System.out.println(response);
            } catch (Exception e) {
                System.out.println("ERROR - " + e.getMessage());
            }
        }

        System.out.println("--------------------------------------------------");
        System.out.println("Test complete.");
    }
}