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

    private static final double SALARY_NET_RATE = 0.8;
    private static final double PENSION_NET_RATE = 0.9;

    private static final double DC_NET_RATE = 0.9;
    private static final double NISA_NET_RATE = 1.0;
    private static final double OTHER_NET_RATE = 0.8;
    private static final int DC_MIN_WITHDRAW_AGE = 60;

    private static final List<String> DEFAULT_ORDER = List.of("DC", "NASDAQ", "OTHER");

    private static final double DEFAULT_NISA_BALANCE = 5_000_000;
    private static final double DEFAULT_OTHER_BALANCE = 10_000_000;
    private static final int DEFAULT_DC_START_AGE = 60;
    private static final int DEFAULT_DC_ANNUITY_YEARS = 20;

    public SimulationResponse simulate(SimulationRequest req) {

        final double salaryNet = req.salaryNetRate != null ? req.salaryNetRate : SALARY_NET_RATE;
        final double pensionNet = req.pensionNetRate != null ? req.pensionNetRate : PENSION_NET_RATE;
        final double dcNet = req.dcNetRate != null ? req.dcNetRate : DC_NET_RATE;

        double dc = req.dcBalance;
        double nisa = req.nisaBalance;
        double other = req.otherBalance;

        int dcStartAge = (int)(req.dcStartAge != 0 ? req.dcStartAge : DEFAULT_DC_START_AGE);
        double dcLumpSum = req.dcLumpSumAmount;
        int dcAnnuityYears = req.dcAnnuityYears > 0 ? req.dcAnnuityYears : DEFAULT_DC_ANNUITY_YEARS;

        List<YearResult> results = new ArrayList<>();

        double inflationMult = 1.0;
        double pensionMult = 1.0;
        Integer depletionAge = null;

        double dcRemainingForAnnuity = 0;
        double dcAnnuityPayment = 0;
        boolean dcAnnuityStarted = false;

        List<String> order = resolveOrder(req.withdrawalOrder, dcStartAge);

        for (int age = req.startAge; age <= req.lifeExpectancy; age++) {

            double income = 0;

            double yearlyExpense =
                (req.basicLivingCost + req.leisureCost) * 12 * inflationMult;

            double specialExp = calcSpecialExpense(req, age);
            yearlyExpense += specialExp;

            if (age < req.retirementAge) {
                income += req.salaryAfter60 * 12 * salaryNet * inflationMult;
            }

            if (age >= req.publicPensionStartAge) {
                income += req.publicPension * 12 * pensionMult * pensionNet;
            }

            double dcWithdrawal = 0;

            if (age >= dcStartAge && dc > 0 && !dcAnnuityStarted) {
                if (dcLumpSum > 0) {
                    double withdraw = Math.min(dcLumpSum, dc);
                    dc -= withdraw;
                    income += withdraw * dcNet;
                    dcWithdrawal += withdraw;
                }

                double remainingForAnnuity = dc;
                if (remainingForAnnuity > 0 && dcAnnuityYears > 0) {
                    dcAnnuityPayment = remainingForAnnuity / dcAnnuityYears * dcNet;
                    dcRemainingForAnnuity = remainingForAnnuity;
                    dcAnnuityStarted = true;
                }
                dc = 0;
            }

            if (dcAnnuityStarted && dcAnnuityYears > 0) {
                int yearsPassed = age - dcStartAge;
                if (yearsPassed < dcAnnuityYears && dcRemainingForAnnuity > 0) {
                    double payment = Math.min(dcAnnuityPayment, dcRemainingForAnnuity);
                    income += payment;
                    dcRemainingForAnnuity = Math.max(0, dcRemainingForAnnuity - payment / dcNet);
                    int yearsRemaining = dcAnnuityYears - yearsPassed - 1;
                    dcAnnuityPayment = yearsRemaining > 0 ? dcRemainingForAnnuity / yearsRemaining * dcNet : 0;
                    dcWithdrawal += payment / dcNet;
                }
            }

            double shortfall = yearlyExpense - income;

            if (shortfall > 0) {
                double remaining = shortfall;
                for (String asset : order) {
                    if (remaining <= 0) break;
                    if ("DC".equals(asset)) continue;
                    double netRate = getNetRate(asset, dcNet);
                    double bal = getBalance(nisa, other, asset);
                    if (bal <= 0) continue;

                    double grossNeeded = remaining / netRate;
                    double withdrawn = Math.min(grossNeeded, bal);
                    double received = withdrawn * netRate;

                    switch (asset) {
                        case "NASDAQ" -> nisa -= withdrawn;
                        case "OTHER" -> other -= withdrawn;
                    }
                    income += received;
                    remaining -= received;
                }
            }

            nisa = Math.max(0, nisa * (1 + req.dcReturnRate));
            other = Math.max(0, other * (1 + req.dcReturnRate));
            if (dcAnnuityStarted) {
                dcRemainingForAnnuity = Math.max(0, dcRemainingForAnnuity * (1 + req.dcReturnRate));
            }

            double total = nisa + other;

            if (dcAnnuityStarted) {
                total += dcRemainingForAnnuity;
            }

            if (total == 0 && depletionAge == null) {
                depletionAge = age;
            }

            YearResult yr = new YearResult();
            yr.age = age;
            yr.income = Math.round(income);
            yr.expense = Math.round(yearlyExpense);
            yr.dcBalance = Math.round(dcAnnuityStarted ? dcRemainingForAnnuity : dc);
            yr.nisaBalance = Math.round(nisa);
            yr.otherBalance = Math.round(other);
            yr.totalBalance = Math.round(total);
            yr.specialExpense = Math.round(specialExp);
            yr.dcWithdrawal = Math.round(dcWithdrawal);
            yr.dcAnnuityPayment = Math.round(dcAnnuityStarted ? dcAnnuityPayment : 0);
            results.add(yr);

            inflationMult *= (1 + req.inflationRate);
            if (age >= req.publicPensionStartAge) {
                pensionMult *= (1 + req.inflationRate * 0.8);
            }
        }

        SimulationResponse response = new SimulationResponse();
        response.yearlyResults = results;
        response.assetDepletionAge = (depletionAge == null) ? -1 : depletionAge;
        response.failureProbability = calculateFailureProbability(req);

        return response;
    }

    private double calculateFailureProbability(SimulationRequest req) {

        final double salaryNet = req.salaryNetRate != null ? req.salaryNetRate : SALARY_NET_RATE;
        final double pensionNet = req.pensionNetRate != null ? req.pensionNetRate : PENSION_NET_RATE;
        final double dcNet = req.dcNetRate != null ? req.dcNetRate : DC_NET_RATE;

        int simulations = 10_000;
        int failureCount = 0;
        Random rand = new Random();

        int dcStartAge = (int)(req.dcStartAge != 0 ? req.dcStartAge : DEFAULT_DC_START_AGE);
        List<String> order = resolveOrder(req.withdrawalOrder, dcStartAge);

        for (int i = 0; i < simulations; i++) {

            double dc = req.dcBalance;
            double nisa = req.nisaBalance;
            double other = req.otherBalance;

            double dcLumpSum = req.dcLumpSumAmount;
            int dcAnnuityYears = req.dcAnnuityYears > 0 ? req.dcAnnuityYears : DEFAULT_DC_ANNUITY_YEARS;

            double inflationMult = 1.0;
            double pensionMult = 1.0;

            double dcRemainingForAnnuity = 0;
            double dcAnnuityPayment = 0;
            boolean dcAnnuityStarted = false;

            for (int age = req.startAge; age <= req.lifeExpectancy; age++) {

                double income = 0;
                double yearlyExpense =
                    (req.basicLivingCost + req.leisureCost) * 12 * inflationMult;

                yearlyExpense += calcSpecialExpense(req, age);

                if (age < req.retirementAge) {
                    income += req.salaryAfter60 * 12 * salaryNet * inflationMult;
                }
                if (age >= req.publicPensionStartAge) {
                    income += req.publicPension * 12 * pensionMult * pensionNet;
                }

                if (age >= dcStartAge && dc > 0 && !dcAnnuityStarted) {
                    if (dcLumpSum > 0) {
                        double withdraw = Math.min(dcLumpSum, dc);
                        dc -= withdraw;
                        income += withdraw * dcNet;
                    }

                    double remainingForAnnuity = dc;
                    if (remainingForAnnuity > 0 && dcAnnuityYears > 0) {
                        dcAnnuityPayment = remainingForAnnuity / dcAnnuityYears * dcNet;
                        dcRemainingForAnnuity = remainingForAnnuity;
                        dcAnnuityStarted = true;
                    }
                    dc = 0;
                }

                if (dcAnnuityStarted && dcAnnuityYears > 0) {
                    int yearsPassed = age - dcStartAge;
                    if (yearsPassed < dcAnnuityYears && dcRemainingForAnnuity > 0) {
                        double payment = Math.min(dcAnnuityPayment, dcRemainingForAnnuity);
                        income += payment;
                        dcRemainingForAnnuity = Math.max(0, dcRemainingForAnnuity - payment / dcNet);
                        int yearsRemaining = dcAnnuityYears - yearsPassed - 1;
                        dcAnnuityPayment = yearsRemaining > 0 ? dcRemainingForAnnuity / yearsRemaining * dcNet : 0;
                    }
                }

                double shortfall = yearlyExpense - income;

                if (shortfall > 0) {
                    double remaining = shortfall;
                    for (String asset : order) {
                        if (remaining <= 0) break;
                        if ("DC".equals(asset)) continue;
                        double netRate = getNetRate(asset, dcNet);
                        double bal = getBalance(nisa, other, asset);
                        if (bal <= 0) continue;

                        double grossNeeded = remaining / netRate;
                        double withdrawn = Math.min(grossNeeded, bal);
                        double received = withdrawn * netRate;

                        switch (asset) {
                            case "NASDAQ" -> nisa -= withdrawn;
                            case "OTHER" -> other -= withdrawn;
                        }
                        remaining -= received;
                    }
                }

                double randomReturn = req.dcReturnRate
                    + req.returnVolatility * rand.nextGaussian();
                nisa = Math.max(0, nisa * (1 + randomReturn));
                other = Math.max(0, other * (1 + randomReturn));
                if (dcAnnuityStarted) {
                    dcRemainingForAnnuity = Math.max(0, dcRemainingForAnnuity * (1 + req.dcReturnRate));
                }

                double total = nisa + other;
                if (dcAnnuityStarted) {
                    total += dcRemainingForAnnuity;
                }

                if (total <= 0) {
                    failureCount++;
                    break;
                }

                inflationMult *= (1 + req.inflationRate);
                if (age >= req.publicPensionStartAge) {
                    pensionMult *= (1 + req.inflationRate * 0.8);
                }
            }
        }

        return (double) failureCount / simulations;
    }

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

    private List<String> resolveOrder(List<String> order, int dcStartAge) {
        if (order == null || order.isEmpty()) {
            return List.of("NASDAQ", "OTHER");
        }
        return order.stream()
            .filter(s -> !s.equals("DC"))
            .toList();
    }

    private double getNetRate(String asset, double dcNetRate) {
        return switch (asset) {
            case "NASDAQ" -> NISA_NET_RATE;
            case "OTHER" -> OTHER_NET_RATE;
            default -> dcNetRate;
        };
    }

    private double getBalance(double nisa, double other, String asset) {
        return switch (asset) {
            case "NASDAQ" -> nisa;
            case "OTHER" -> other;
            default -> 0;
        };
    }
}
