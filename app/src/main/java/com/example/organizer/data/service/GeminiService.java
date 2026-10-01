package com.example.organizer.data.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import com.example.BuildConfig;
import com.example.organizer.data.entity.KaizenGoal;
import com.example.organizer.data.entity.KaizenProgress;

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

    // Centralized model configuration supported in current environment
    private static final String MODEL_NAME = "gemini-2.5-flash";
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
        public String suggestedReminderTime;
        public String suggestedReminderDays;

        public KaizenSuggestionResult(List<String> steps, String rationale, String suggestedReminderTime, String suggestedReminderDays) {
            this.steps = steps;
            this.rationale = rationale;
            this.suggestedReminderTime = suggestedReminderTime;
            this.suggestedReminderDays = suggestedReminderDays;
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

    public void suggestKaizenProgression(
            Context context,
            KaizenGoal goal,
            List<KaizenProgress> existingSteps,
            String rejectedSuggestion,
            ApiCallback<KaizenSuggestionResult> callback
    ) {
        if (!isNetworkAvailable(context)) {
            mainHandler.post(() -> callback.onError("Sem conexão com a internet. Verifique sua rede e tente novamente."));
            return;
        }

        String apiKey = getApiKey();
        if (apiKey == null) {
            mainHandler.post(() -> callback.onError("Chave da API Gemini não configurada nos Segredos do app. Configure GEMINI_API_KEY para gerar sugestões com IA."));
            return;
        }

        executor.execute(() -> {
            try {
                StringBuilder contextBuilder = new StringBuilder();
                contextBuilder.append("Você é um especialista em melhoria contínua e metodologia Kaizen (evolução em micro-etapas de 1%).\n");
                contextBuilder.append("Analise detalhadamente o seguinte objetivo do usuário:\n");
                contextBuilder.append("- Título: \"").append(goal.getTitle()).append("\"\n");
                if (goal.getDescription() != null && !goal.getDescription().isEmpty()) {
                    contextBuilder.append("- Descrição/Motivação: \"").append(goal.getDescription()).append("\"\n");
                }
                contextBuilder.append("- Situação Atual: \"").append(goal.getCurrentState()).append("\"\n");
                contextBuilder.append("- Meta Desejada: \"").append(goal.getTargetState()).append("\"\n");
                contextBuilder.append("- Progresso Atual: ").append(goal.getProgress()).append("%\n");
                contextBuilder.append("- Frequência pretendida: \"").append(goal.getFrequency()).append("\"\n");
                contextBuilder.append("- Horário/Dias de lembrete atual: \"").append(goal.getNotificationTime()).append(" - ").append(goal.getNotificationDays()).append("\"\n");

                if (existingSteps != null && !existingSteps.isEmpty()) {
                    contextBuilder.append("- Etapas já registradas:\n");
                    for (KaizenProgress p : existingSteps) {
                        contextBuilder.append("  * ").append(p.getStepTitle())
                                .append(" [").append(p.isAchieved() ? "Concluída" : "Pendente").append("]\n");
                    }
                }

                if (rejectedSuggestion != null && !rejectedSuggestion.trim().isEmpty()) {
                    contextBuilder.append("\nATENÇÃO: O usuário rejeitou a seguinte sugestão anterior:\n\"")
                            .append(rejectedSuggestion)
                            .append("\"\nPor favor, adote uma abordagem/estratégia COMPLETAMENTE DIFERENTE, com outro ritmo ou forma de progressão gradual.\n");
                }

                contextBuilder.append("\nRetorne EXCLUSIVAMENTE um objeto JSON válido (sem marcadores ```json ou markdown) com a seguinte estrutura:\n");
                contextBuilder.append("{\n");
                contextBuilder.append("  \"steps\": [\"Etapa 1: ...\", \"Etapa 2: ...\", \"Etapa 3: ...\", \"Etapa 4: ...\"],\n");
                contextBuilder.append("  \"rationale\": \"Explicação breve de como essa progressão gradual evita atrito cognitivo e cria consistência\",\n");
                contextBuilder.append("  \"suggestedReminderTime\": \"HH:mm\",\n");
                contextBuilder.append("  \"suggestedReminderDays\": \"Ex: Todos os dias ou Seg, Qua, Sex\"\n");
                contextBuilder.append("}\n");

                String responseBody = callGeminiApi(apiKey, contextBuilder.toString());
                JSONObject json = parseJsonResponse(responseBody);

                List<String> steps = new ArrayList<>();
                JSONArray stepsArray = json.optJSONArray("steps");
                if (stepsArray != null) {
                    for (int i = 0; i < stepsArray.length(); i++) {
                        steps.add(stepsArray.getString(i));
                    }
                }
                String rationale = json.optString("rationale", "Progressão gradual baseada em micro-hábitos incrementais.");
                String suggestedTime = json.optString("suggestedReminderTime", "20:00");
                String suggestedDays = json.optString("suggestedReminderDays", "Todos os dias");

                KaizenSuggestionResult result = new KaizenSuggestionResult(steps, rationale, suggestedTime, suggestedDays);
                mainHandler.post(() -> callback.onSuccess(result));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Falha na comunicação com o Gemini: " + e.getMessage() + ". Tente novamente."));
            }
        });
    }

    public void analyzePareto(Context context, String userReport, ApiCallback<ParetoAnalysisResult> callback) {
        if (!isNetworkAvailable(context)) {
            mainHandler.post(() -> callback.onError("Sem conexão com a internet. Conecte-se para que o Gemini analise seu relato."));
            return;
        }

        String apiKey = getApiKey();
        if (apiKey == null) {
            mainHandler.post(() -> callback.onError("Chave da API Gemini não configurada nos Segredos do app. Configure GEMINI_API_KEY para habilitar a análise com IA."));
            return;
        }

        executor.execute(() -> {
            try {
                String prompt = "Você é um especialista em produtividade, eficiência operacional e no Princípio de Pareto (Lei 80/20).\n" +
                        "Analise criticamente o seguinte relato REAL fornecido pelo usuário:\n" +
                        "\"" + userReport + "\"\n\n" +
                        "Identifique as causas, atividades ou fatores vitais que geram aproximadamente 80% do retorno ou impacto, " +
                        "e os fatores secundários de menor alavancagem.\n" +
                        "Retorne EXCLUSIVAMENTE um objeto JSON válido (sem marcadores ```json ou formatação markdown) com o formato exato:\n" +
                        "{\n" +
                        "  \"summary\": \"Resumo executivo da análise do relato em 1 a 2 frases\",\n" +
                        "  \"explanation\": \"Explicação lógica fundamentando a distribuição 80/20 no caso específico\",\n" +
                        "  \"factors\": [\n" +
                        "    {\"title\": \"Fator Crítico 1\", \"impactPercentage\": 50, \"isTopTwenty\": true},\n" +
                        "    {\"title\": \"Fator Crítico 2\", \"impactPercentage\": 30, \"isTopTwenty\": true},\n" +
                        "    {\"title\": \"Fator Secundário 1\", \"impactPercentage\": 12, \"isTopTwenty\": false},\n" +
                        "    {\"title\": \"Fator Secundário 2\", \"impactPercentage\": 8, \"isTopTwenty\": false}\n" +
                        "  ],\n" +
                        "  \"recommendations\": [\n" +
                        "    \"Recomendação prática 1\",\n" +
                        "    \"Recomendação prática 2\",\n" +
                        "    \"Recomendação prática 3\"\n" +
                        "  ]\n" +
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
                mainHandler.post(() -> callback.onError("Não foi possível processar a análise com Gemini: " + e.getMessage() + ". Tente novamente."));
            }
        });
    }

    public static ParetoAnalysisResult getLocalDemoParetoAnalysis() {
        String summary = "[Demonstração Local] Análise 80/20 de rotina padrão de trabalho e estudos.";
        String explanation = "Demonstração: Aproximadamente 20% das suas ações diárias geram 80% do rendimento. Focar em tarefas de alta concentração gera a maior alavancagem.";

        List<ParetoFactor> factors = new ArrayList<>();
        factors.add(new ParetoFactor("Trabalho Focado em Entregas Principais", 55, true));
        factors.add(new ParetoFactor("Planejamento e Priorização Matinal", 25, true));
        factors.add(new ParetoFactor("Respostas a Mensagens e E-mails", 12, false));
        factors.add(new ParetoFactor("Reuniões sem Pauta Definida", 8, false));

        List<String> recommendations = new ArrayList<>();
        recommendations.add("Proteja suas primeiras horas da manhã com blocos de trabalho ininterrupto.");
        recommendations.add("Agrupe a leitura de mensagens em duas janelas específicas ao longo do dia.");
        recommendations.add("Elimine ou delegue demandas de baixo impacto.");

        return new ParetoAnalysisResult(summary, factors, recommendations, explanation);
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
                    String errBody = response.body() != null ? response.body().string() : "";
                    throw new IOException("HTTP " + response.code() + ": " + errBody);
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
}
