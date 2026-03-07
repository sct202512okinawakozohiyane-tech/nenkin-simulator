package com.example.pension.model;

public class SimulationRequest {

	public int startAge;
	public int lifeExpectancy;
	public int retirementAge;
    public int pensionStartAge;

    public double basicLivingCost;
    public double leisureCost;

    public double salaryAfter60;
    public double publicPension;

    public double dcBalance;
    public double dcReturnRate;
       
    public double inflationRate;

    // ★追加：利回りのブレ（標準偏差）
    public double returnVolatility;
}