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

    // ���ʎx�o���X�g�i�ő�10���j
    public List<SpecialExpense> specialExpenses;

    // ���[�U�[�����\�Ȏ��藦�Bnull �̏ꍇ�̓f�t�H���g�l���g�p
    public Double salaryNetRate;
    public Double pensionNetRate;
    public Double dcLumpSumNetRate;
    public Double dcAnnuityNetRate;
}
