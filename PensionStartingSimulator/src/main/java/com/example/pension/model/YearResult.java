package com.example.pension.model;

public class YearResult {

    public int age;
    public long income;
    public long expense;

    // 各資産残高
    public long dcBalance;
    public long nisaBalance;
    public long otherBalance;
    public long totalBalance;    // dcBalance + nisaBalance + otherBalance
    public long specialExpense;  // その年の特別支出合計
}
