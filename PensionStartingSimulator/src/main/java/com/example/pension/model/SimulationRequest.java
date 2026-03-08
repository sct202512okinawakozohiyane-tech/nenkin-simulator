package com.example.pension.model;

import java.util.List;

public class SimulationRequest {

    public int startAge;
    public int lifeExpectancy;
    public int retirementAge;
    public int pensionStartAge;

    public double basicLivingCost;
    public double leisureCost;

    public double salaryAfter60;
    public double publicPension;

    // 3種類の資産残高
    public double dcBalance;
    public double nisaBalance;
    public double otherBalance;

    public double dcReturnRate;
    public double inflationRate;
    public double returnVolatility;

    // 取り崩し順序。例: ["DC", "NISA", "OTHER"]
    // null または空の場合はデフォルト順 DC→NISA→OTHER を使用
    public List<String> withdrawalOrder;

    // 特別支出リスト（最大10件）
    public List<SpecialExpense> specialExpenses;

    // ユーザー調整可能な手取り率（null の場合はデフォルト値を使用）
    public Double salaryNetRate;  // 給与手取り率   (デフォルト: 0.80, 範囲: 0.70〜0.85)
    public Double pensionNetRate; // 年金手取り率   (デフォルト: 0.90, 範囲: 0.70〜0.99)
    public Double dcNetRate;      // DC取り崩し手取り率 (デフォルト: 0.90, 範囲: 0.70〜0.99)
}