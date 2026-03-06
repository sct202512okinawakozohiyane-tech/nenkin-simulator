package com.example.pension.model;

import java.util.List;

public class SimulationResponse {

    public double remainingBalanceAt100;
    public int assetDepletionAge;   // ←追加
    public List<YearResult> yearlyResults;
}
