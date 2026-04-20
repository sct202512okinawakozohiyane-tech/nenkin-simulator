package com.example.pension.model;

import java.util.List;

public class SimulationRequest {

    public int startAge;
    public int lifeExpectancy;
    public int retirementAge;
    public int publicPensionStartAge;

    public double basicLivingCost;
    public double leisureCost;

    public double salaryAfter60;
    public double publicPension;

    public double dcBalance;
    public double dcStartAge;
    public double dcLumpSumAmount;
    public int dcAnnuityYears;

    public double nisaBalance;
    public double otherBalance;

    public double dcReturnRate;
    public double inflationRate;
    public double returnVolatility;

    public List<String> withdrawalOrder;

    // 特別支出リスト（最大10件）
    public List<SpecialExpense> specialExpenses;

    // ユーザー調整可能な手取り率。null の場合はデフォルト値を使用
    public Double salaryNetRate;
    public Double pensionNetRate;
    public Double dcNetRate;
}
