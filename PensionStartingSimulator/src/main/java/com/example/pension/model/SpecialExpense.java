package com.example.pension.model;

public class SpecialExpense {

    /** カテゴリ（教育費/車両費/住居/旅行/介護/その他） */
    public String category;

    /** ユーザーが入力する自由記述ラベル（例：「長男大学費用」） */
    public String label;

    /** 発生開始年齢（その年齢の年度から加算） */
    public int fromAge;

    /** 発生終了年齢（その年齢の年度まで加算） */
    public int toAge;

    /** 年間支出額（税込・額面。インフレ連動なし） */
    public double annualAmount;
}
