package com.example.pension.model;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 年齢の上限はシミュレーション年数（＝計算量・メモリ使用量）を抑えるための防御も兼ねる
public class SimulationRequest {

    // DCは60歳から受取可能なため、60歳未満からのシミュレーションは対象外
    @Min(60) @Max(80)
    public int startAge;
    @Min(60) @Max(110)
    public int lifeExpectancy;
    @Min(40) @Max(80)
    public int retirementAge;
    @Min(60) @Max(75)
    public int publicPensionStartAge;

    @DecimalMin("0") @DecimalMax("10000000000")
    public double basicLivingCost;
    @DecimalMin("0") @DecimalMax("10000000000")
    public double leisureCost;

    @DecimalMin("0") @DecimalMax("10000000000")
    public double salaryAfter60;
    @DecimalMin("0") @DecimalMax("10000000000")
    public double publicPension;

    @DecimalMin("0") @DecimalMax("10000000000")
    public double dcBalance;
    // 0 はデフォルト（60歳）扱い
    @DecimalMin("0") @DecimalMax("75")
    public double dcStartAge;
    @DecimalMin("0") @DecimalMax("10000000000")
    public double dcLumpSumAmount;
    // 0 はデフォルト（20年）扱い
    @Min(0) @Max(20)
    public int dcAnnuityYears;

    @DecimalMin("0") @DecimalMax("10000000000")
    public double nisaBalance;
    @DecimalMin("0") @DecimalMax("10000000000")
    public double otherBalance;

    @DecimalMin("-0.5") @DecimalMax("0.5")
    public double dcReturnRate;
    @DecimalMin("-0.1") @DecimalMax("0.2")
    public double inflationRate;
    @DecimalMin("0") @DecimalMax("1")
    public double returnVolatility;

    @Size(max = 10)
    public List<@NotNull @Size(max = 20) String> withdrawalOrder;

    // 特別支出リスト（最大10件）
    @Size(max = 10)
    public List<@NotNull @Valid SpecialExpense> specialExpenses;

    // ユーザーが調整できる手取り率。null の場合はデフォルト値を使用
    @DecimalMin("0") @DecimalMax("1")
    public Double salaryNetRate;
    @DecimalMin("0") @DecimalMax("1")
    public Double pensionNetRate;
    // DC手取り率は計算内で除数になるため 0 を許可しない
    @DecimalMin("0.01") @DecimalMax("1")
    public Double dcLumpSumNetRate;
    @DecimalMin("0.01") @DecimalMax("1")
    public Double dcAnnuityNetRate;
}
