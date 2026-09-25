package com.example.organizer.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "a3_documents")
public class A3Document {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    
    // Left side:
    private String problem;
    private String context;
    private String currentConditions;
    private String rootCause;

    // Right side:
    private String improvementProposals;
    private String actionPlan;
    private String followUpResults;

    private long updatedAt;

    public A3Document(String title, String problem, String context, String currentConditions, String rootCause, String improvementProposals, String actionPlan, String followUpResults, long updatedAt) {
        this.title = title;
        this.problem = problem;
        this.context = context;
        this.currentConditions = currentConditions;
        this.rootCause = rootCause;
        this.improvementProposals = improvementProposals;
        this.actionPlan = actionPlan;
        this.followUpResults = followUpResults;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getCurrentConditions() { return currentConditions; }
    public void setCurrentConditions(String currentConditions) { this.currentConditions = currentConditions; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getImprovementProposals() { return improvementProposals; }
    public void setImprovementProposals(String improvementProposals) { this.improvementProposals = improvementProposals; }

    public String getActionPlan() { return actionPlan; }
    public void setActionPlan(String actionPlan) { this.actionPlan = actionPlan; }

    public String getFollowUpResults() { return followUpResults; }
    public void setFollowUpResults(String followUpResults) { this.followUpResults = followUpResults; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
