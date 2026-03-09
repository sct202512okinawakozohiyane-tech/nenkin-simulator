package com.example.pension.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.example.pension.model.SimulationRequest;
import com.example.pension.model.SimulationResponse;
import com.example.pension.model.SpecialExpense;
import com.example.pension.model.YearResult;

@Service
public class SimulationService {

    // 給与・年金の手取率
    private static final double SALARY_NET_RATE  = 0.8;
    private static final double PENSION_NET_RATE = 0.9;

    // 各資産の取り崩し手取率
    private static final double DC_NET_RATE    = 0.9; // DC: 約10%課税（退職・年金所得）
    private static final double NISA_NET_RATE  = 1.0; // NISA: 非課税
    private static final double OTHER_NET_RATE = 0.8; // その他: 20%源泉分離課税

    // DC 受取可能最低年齢（法律上の制約）
    private static final int DC_MIN_WITHDRAW_AGE = 60;

    // デフォルト取り崩し順
    private static final List<String> DEFAULT_ORDER = List.of("DC", "NISA", "OTHER");

    // ---- メインシミュレーション ----------------------------------------

    public SimulationResponse simulate(SimulationRequest req) {

        // リクエストの手取り率を優先、null の場合はデフォルト定数にフォールバック
        final double salaryNet  = req.salaryNetRate  != null ? req.salaryNetRate  : SALARY_NET_RATE;
        final double pensionNet = req.pensionNetRate != null ? req.pensionNetRate : PENSION_NET_RATE;
        final double dcNet      = req.dcNetRate      != null ? req.dcNetRate      : DC_NET_RATE;

        double dc    = req.dcBalance;
        double nisa  = req.nisaBalance;
        double other = req.otherBalance;

        List<YearResult> results = new ArrayList<>();

        double inflationMult = 1.0;
        double pensionMult   = 1.0;
        Integer depletionAge = null;

        List<String> order = resolveOrder(req.withdrawalOrder);

        for (int age = req.startAge; age <= req.lifeExpectancy; age++) {

            double income = 0;

            double yearlyExpense =
                (req.basicLivingCost + req.leisureCost) * 12 * inflationMult;

            // 特別支出（年齢範囲が一致する項目を加算）
            double specialExp = calcSpecialExpense(req, age);
            yearlyExpense += specialExp;

            // 給与（インフレ連動）
            if (age < req.retirementAge) {
                income += req.salaryAfter60 * 12 * salaryNet * inflationMult;
            }

            // 公的年金
            if (age >= req.pensionStartAge) {
                income += req.publicPension * 12 * pensionMult * pensionNet;
            }

            double shortfall = yearlyExpense - income;

            // 取り崩し（指定順序で各資産から）
            if (shortfall > 0) {
                double remaining = shortfall;
                for (String asset : order) {
                    if (remaining <= 0) break;
                    // DC は 60 歳未満では受け取り不可（法律上の制約）
                    if ("DC".equals(asset) && age < DC_MIN_WITHDRAW_AGE) continue;
                    double netRate = getNetRate(asset, dcNet);
                    double bal = getBalance(dc, nisa, other, asset);
                    if (bal <= 0) continue;

                    double grossNeeded = remaining / netRate;
                    double withdrawn   = Math.min(grossNeeded, bal);
                    double received    = withdrawn * netRate;

                    switch (asset) {
                        case "DC"    -> dc    -= withdrawn;
                        case "NISA"  -> nisa  -= withdrawn;
                        case "OTHER" -> other -= withdrawn;
                    }
                    income    += received;
                    remaining -= received;
                }
            }

            // 年末運用（取り崩し後の残高に利回り適用）
            dc    = Math.max(0, dc    * (1 + req.dcReturnRate));
            nisa  = Math.max(0, nisa  * (1 + req.dcReturnRate));
            other = Math.max(0, other * (1 + req.dcReturnRate));

            double total = dc + nisa + other;

            if (total == 0 && depletionAge == null) {
                depletionAge = age;
            }

            YearResult yr = new YearResult();
            yr.age           = age;
            yr.income        = Math.round(income);
            yr.expense       = Math.round(yearlyExpense);
            yr.dcBalance     = Math.round(dc);
            yr.nisaBalance   = Math.round(nisa);
            yr.otherBalance  = Math.round(other);
            yr.totalBalance  = Math.round(total);
            yr.specialExpense = Math.round(specialExp);
            results.add(yr);

            inflationMult *= (1 + req.inflationRate);
            if (age >= req.pensionStartAge) {
                pensionMult *= (1 + req.inflationRate * 0.8);
            }
        }

        SimulationResponse response = new SimulationResponse();
        response.yearlyResults      = results;
        response.assetDepletionAge  = (depletionAge == null) ? -1 : depletionAge;
        response.failureProbability = calculateFailureProbability(req);

        return response;
    }

    // ---- モンテカルロ（10,000回） ----------------------------------------

    private double calculateFailureProbability(SimulationRequest req) {

        // リクエストの手取り率を優先、null の場合はデフォルト定数にフォールバック
        final double salaryNet  = req.salaryNetRate  != null ? req.salaryNetRate  : SALARY_NET_RATE;
        final double pensionNet = req.pensionNetRate != null ? req.pensionNetRate : PENSION_NET_RATE;
        final double dcNet      = req.dcNetRate      != null ? req.dcNetRate      : DC_NET_RATE;

        int simulations  = 10_000;
        int failureCount = 0;
        Random rand = new Random();
        List<String> order = resolveOrder(req.withdrawalOrder);

        for (int i = 0; i < simulations; i++) {

            double dc    = req.dcBalance;
            double nisa  = req.nisaBalance;
            double other = req.otherBalance;

            double inflationMult = 1.0;
            double pensionMult   = 1.0;

            for (int age = req.startAge; age <= req.lifeExpectancy; age++) {

                double income = 0;
                double yearlyExpense =
                    (req.basicLivingCost + req.leisureCost) * 12 * inflationMult;

                // 特別支出をモンテカルロにも反映
                yearlyExpense += calcSpecialExpense(req, age);

                if (age < req.retirementAge) {
                    income += req.salaryAfter60 * 12 * salaryNet * inflationMult;
                }
                if (age >= req.pensionStartAge) {
                    income += req.publicPension * 12 * pensionMult * pensionNet;
                }

                double shortfall = yearlyExpense - income;

                if (shortfall > 0) {
                    double remaining = shortfall;
                    for (String asset : order) {
                        if (remaining <= 0) break;
                        // DC は 60 歳未満では受け取り不可（法律上の制約）
                        if ("DC".equals(asset) && age < DC_MIN_WITHDRAW_AGE) continue;
                        double netRate = getNetRate(asset, dcNet);
                        double bal = getBalance(dc, nisa, other, asset);
                        if (bal <= 0) continue;

                        double grossNeeded = remaining / netRate;
                        double withdrawn   = Math.min(grossNeeded, bal);
                        double received    = withdrawn * netRate;

                        switch (asset) {
                            case "DC"    -> dc    -= withdrawn;
                            case "NISA"  -> nisa  -= withdrawn;
                            case "OTHER" -> other -= withdrawn;
                        }
                        remaining -= received;
                    }
                }

                // 年末運用（ランダム利回り）
                double randomReturn = req.dcReturnRate
                    + req.returnVolatility * rand.nextGaussian();
                dc    = Math.max(0, dc    * (1 + randomReturn));
                nisa  = Math.max(0, nisa  * (1 + randomReturn));
                other = Math.max(0, other * (1 + randomReturn));

                if (dc + nisa + other <= 0) {
                    failureCount++;
                    break;
                }

                inflationMult *= (1 + req.inflationRate);
                if (age >= req.pensionStartAge) {
                    pensionMult *= (1 + req.inflationRate * 0.8);
                }
            }
        }

        return (double) failureCount / simulations;
    }

    // ---- ヘルパー --------------------------------------------------------

    /** 指定年齢に該当する特別支出の合計を返す */
    private double calcSpecialExpense(SimulationRequest req, int age) {
        if (req.specialExpenses == null || req.specialExpenses.isEmpty()) return 0;
        double total = 0;
        for (SpecialExpense se : req.specialExpenses) {
            if (age >= se.fromAge && age <= se.toAge) {
                total += se.annualAmount;
            }
        }
        return total;
    }

    private List<String> resolveOrder(List<String> order) {
        if (order == null || order.isEmpty()) return DEFAULT_ORDER;
        return order;
    }

    private double getNetRate(String asset, double dcNetRate) {
        return switch (asset) {
            case "NISA"  -> NISA_NET_RATE;
            case "OTHER" -> OTHER_NET_RATE;
            default      -> dcNetRate;   // DC: ユーザー指定またはデフォルト
        };
    }

    private double getBalance(double dc, double nisa, double other, String asset) {
        return switch (asset) {
            case "NISA"  -> nisa;
            case "OTHER" -> other;
            default      -> dc;
        };
    }
}