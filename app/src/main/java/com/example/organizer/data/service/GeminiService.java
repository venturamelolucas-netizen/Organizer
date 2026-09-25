package com.example.organizer.data.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import com.example.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiService {

    private static final String MODEL_NAME = "gemini-3.5-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent";

    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public static class KaizenSuggestionResult {
        public List<String> steps;
        public String rationale;

        public KaizenSuggestionResult(List<String> steps, String rationale) {
            this.steps = steps;
            this.rationale = rationale;
        }
    }

    public static class ParetoFactor {
        public String title;
        public int impactPercentage;
        public boolean isTopTwenty;

        public ParetoFactor(String title, int impactPercentage, boolean isTopTwenty) {
            this.title = title;
            this.impactPercentage = impactPercentage;
            this.isTopTwenty = isTopTwenty;
        }
    }

    public static class ParetoAnalysisResult {
        public String summary;
        public List<ParetoFactor> factors;
        public List<String> recommendations;
        public String explanation;

        public ParetoAnalysisResult(String summary, List<ParetoFactor> factors, List<String> recommendations, String explanation) {
            this.summary = summary;
            this.factors = factors;
            this.recommendations = recommendations;
            this.explanation = explanation;
        }
    }

    public GeminiService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private String getApiKey() {
        String key = BuildConfig.GEMINI_API_KEY;
        if (key != null && !key.isEmpty() && !key.equals("MY_GEMINI_API_KEY")) {
            return key;
        }
        return null;
    }

    public void suggestKaizenProgression(Context context, String goalTitle, String currentState, String targetState, ApiCallback<KaizenSuggestionResult> callback) {
        if (!isNetworkAvailable(context)) {
            // Provide offline intelligent fallback
            mainHandler.post(() -> callback.onSuccess(getOfflineKaizenFallback(goalTitle, currentState, targetState)));
            return;
        }

        String apiKey = getApiKey();
        if (apiKey == null) {
            mainHandler.post(() -> callback.onSuccess(getOfflineKaizenFallback(goalTitle, currentState, targetState)));
            return;
        }

        executor.execute(() -> {
            try {
                String prompt = "Você é um especialista no método Kaizen de melhoria contínua gradual. " +
                        "Analise o objetivo: \"" + goalTitle + "\", Situação atual: \"" + currentState + "\", Meta final: \"" + targetState + "\". " +
                        "Retorne EXCLUSIVAMENTE um objeto JSON válido (sem tags markdown ```json) no seguinte formato: " +
                        "{\"steps\": [\"Etapa 1: ...\", \"Etapa 2: ...\", \"Etapa 3: ...\", \"Etapa 4: ...\"], \"rationale\": \"Explicação breve do método gradual\"}";

                String responseBody = callGeminiApi(apiKey, prompt);
                JSONObject json = parseJsonResponse(responseBody);

                List<String> steps = new ArrayList<>();
                JSONArray stepsArray = json.optJSONArray("steps");
                if (stepsArray != null) {
                    for (int i = 0; i < stepsArray.length(); i++) {
                        steps.add(stepsArray.getString(i));
                    }
                }
                String rationale = json.optString("rationale", "Progressão gradual baseada em micro-hábitos e constância diária.");

                KaizenSuggestionResult result = new KaizenSuggestionResult(steps, rationale);
                mainHandler.post(() -> callback.onSuccess(result));

            } catch (Exception e) {
                // Fallback gracefully on parsing/network failure
                mainHandler.post(() -> callback.onSuccess(getOfflineKaizenFallback(goalTitle, currentState, targetState)));
            }
        });
    }

    public void analyzePareto(Context context, String userReport, ApiCallback<ParetoAnalysisResult> callback) {
        if (!isNetworkAvailable(context)) {
            mainHandler.post(() -> callback.onSuccess(getOfflineParetoFallback(userReport)));
            return;
        }

        String apiKey = getApiKey();
        if (apiKey == null) {
            mainHandler.post(() -> callback.onSuccess(getOfflineParetoFallback(userReport)));
            return;
        }

        executor.execute(() -> {
            try {
                String prompt = "Você é um especialista em produtividade e no Princípio de Pareto (Lei 80/20). " +
                        "Analise o relato do usuário sobre a rotina/trabalho dele: \"" + userReport + "\". " +
                        "Identifique as 20% das causas/atividades que geram 80% do impacto positivo, e as demais atividades de menor retorno. " +
                        "Retorne EXCLUSIVAMENTE um objeto JSON válido (sem marcadores markdown ```json) com a estrutura: " +
                        "{" +
                        "  \"summary\": \"Resumo objetivo em 1 ou 2 frases\"," +
                        "  \"explanation\": \"Explicação concisa do raciocínio 80/20 aplicado ao relato\"," +
                        "  \"factors\": [" +
                        "    {\"title\": \"Fator crítico 1\", \"impactPercentage\": 50, \"isTopTwenty\": true}," +
                        "    {\"title\": \"Fator crítico 2\", \"impactPercentage\": 30, \"isTopTwenty\": true}," +
                        "    {\"title\": \"Fator secundário 1\", \"impactPercentage\": 10, \"isTopTwenty\": false}," +
                        "    {\"title\": \"Fator secundário 2\", \"impactPercentage\": 10, \"isTopTwenty\": false}" +
                        "  ]," +
                        "  \"recommendations\": [" +
                        "    \"Recomendação prática 1\"," +
                        "    \"Recomendação prática 2\"," +
                        "    \"Recomendação prática 3\"" +
                        "  ]" +
                        "}";

                String responseBody = callGeminiApi(apiKey, prompt);
                JSONObject json = parseJsonResponse(responseBody);

                String summary = json.optString("summary", "Análise de Pareto concluída.");
                String explanation = json.optString("explanation", "Focar nos pontos vitais maximiza o retorno.");

                List<ParetoFactor> factors = new ArrayList<>();
                JSONArray factorsArr = json.optJSONArray("factors");
                if (factorsArr != null) {
                    for (int i = 0; i < factorsArr.length(); i++) {
                        JSONObject fObj = factorsArr.getJSONObject(i);
                        factors.add(new ParetoFactor(
                                fObj.getString("title"),
                                fObj.getInt("impactPercentage"),
                                fObj.getBoolean("isTopTwenty")
                        ));
                    }
                }

                List<String> recommendations = new ArrayList<>();
                JSONArray recArr = json.optJSONArray("recommendations");
                if (recArr != null) {
                    for (int i = 0; i < recArr.length(); i++) {
                        recommendations.add(recArr.getString(i));
                    }
                }

                ParetoAnalysisResult result = new ParetoAnalysisResult(summary, factors, recommendations, explanation);
                mainHandler.post(() -> callback.onSuccess(result));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onSuccess(getOfflineParetoFallback(userReport)));
            }
        });
    }

    private String callGeminiApi(String apiKey, String prompt) throws IOException {
        String url = BASE_URL + "?key=" + apiKey;

        try {
            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject requestJson = new JSONObject();
            requestJson.put("contents", contents);

            RequestBody body = RequestBody.create(
                    requestJson.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response.code());
                }
                return response.body() != null ? response.body().string() : "";
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private JSONObject parseJsonResponse(String responseBody) throws Exception {
        JSONObject raw = new JSONObject(responseBody);
        JSONArray candidates = raw.getJSONArray("candidates");
        JSONObject first = candidates.getJSONObject(0);
        JSONObject content = first.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        String text = parts.getJSONObject(0).getString("text").trim();

        // Strip Markdown code block if present
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        return new JSONObject(text);
    }

    private KaizenSuggestionResult getOfflineKaizenFallback(String goalTitle, String currentState, String targetState) {
        List<String> steps = new ArrayList<>();
        steps.add("Etapa 1: Estabelecer ancoragem do hábito (focar na consistência mínima nos primeiros 7 dias)");
        steps.add("Etapa 2: Aumentar gradualmente em 20% a intensidade ou frequência semanal");
        steps.add("Etapa 3: Avaliar pontos de atrito e consolidar a rotina intermediária");
        steps.add("Etapa 4: Atingir a meta pretendida de \"" + targetState + "\" com estabilidade");

        String rationale = "Princípio Kaizen: melhorias incrementais de 1% reduzem a resistência cognitiva e garantem retenção de longo prazo sem estresse de ruptura.";
        return new KaizenSuggestionResult(steps, rationale);
    }

    private ParetoAnalysisResult getOfflineParetoFallback(String userReport) {
        String summary = "Análise 80/20: Identificação dos fatores com maior alavancagem de resultados.";
        String explanation = "Aproximadamente 20% das suas ações diárias são responsáveis por 80% do seu rendimento e clareza mental. Focar nessas prioridades vitais reduz o tempo gasto com tarefas secundárias.";

        List<ParetoFactor> factors = new ArrayList<>();
        factors.add(new ParetoFactor("Atividades de Foco Profundo e Resolução Direta", 55, true));
        factors.add(new ParetoFactor("Planejamento Prévio e Priorização Matinal", 25, true));
        factors.add(new ParetoFactor("Comunicações Reativas e Checagem de Mensagens", 12, false));
        factors.add(new ParetoFactor("Tarefas Acessórias e Ajustes Menores", 8, false));

        List<String> recommendations = new ArrayList<>();
        recommendations.add("Bloqueie as primeiras 2 horas do dia para tarefas do quadrante Importante (20% vital).");
        recommendations.add("Agrupe comunicações e mensagens em 2 janelas fixas para evitar quebra de foco.");
        recommendations.add("Elimine ou automatize demandas repetitivas que tomam tempo sem gerar valor direto.");

        return new ParetoAnalysisResult(summary, factors, recommendations, explanation);
    }
}
