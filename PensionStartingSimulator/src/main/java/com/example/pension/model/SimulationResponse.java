package com.example.pension.model;

import java.util.List;

public class SimulationResponse {

    public double remainingBalanceAt100;
    public int assetDepletionAge;
    public List<YearResult> yearlyResults;

    // ★追加：破綻確率
    public double failureProbability;
}