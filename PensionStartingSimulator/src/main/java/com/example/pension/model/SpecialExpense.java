package com.example.pension.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class SpecialExpense {

    /** カテゴリ（教育費/車両費/住居/旅行/介護/その他） */
    @Size(max = 20)
    public String category;

    /** ユーザーが入力する自由記述ラベル（例：「長男大学費用」） */
    @Size(max = 30)
    public String label;

    /** 発生開始年齢（その年齢の年度から加算） */
    @Min(0) @Max(120)
    public int fromAge;

    /** 発生終了年齢（その年齢の年度まで加算） */
    @Min(0) @Max(120)
    public int toAge;

    /** 年間支出額（税込・額面。インフレ連動なし） */
    @DecimalMin("0") @DecimalMax("10000000000")
    public double annualAmount;
}
