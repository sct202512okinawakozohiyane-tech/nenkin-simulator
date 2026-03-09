package com.example.pension;

import com.example.pension.model.SimulationRequest;
import com.example.pension.model.SimulationResponse;
import com.example.pension.model.YearResult;
import com.example.pension.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DC 受取年齢ロックおよびランダム値プロパティベーステスト
 */
@SpringBootTest
class SimulationServiceTest {

    @Autowired
    private SimulationService service;

    // =========================================================================
    // ヘルパー
    // =========================================================================

    /** 固定値リクエスト（DC ロックテスト用） */
    private static SimulationRequest baseReq(int startAge) {
        SimulationRequest req = new SimulationRequest();
        req.startAge         = startAge;
        req.lifeExpectancy   = 90;
        req.retirementAge    = 65;
        req.pensionStartAge  = 65;
        req.basicLivingCost  = 200_000;
        req.leisureCost      = 100_000;
        req.salaryAfter60    = 0;
        req.publicPension    = 0;
        req.dcBalance        = 20_000_000;
        req.nisaBalance      = 0;
        req.otherBalance     = 0;
        req.dcReturnRate     = 0.0;
        req.inflationRate    = 0.0;
        req.returnVolatility = 0.1;
        req.withdrawalOrder  = List.of("DC", "NISA", "OTHER");
        return req;
    }

    /** 理論上有効な範囲でランダムなリクエストを生成 */
    private static SimulationRequest randomReq(Random rng) {
        SimulationRequest req = new SimulationRequest();

        // 年齢系（制約: startAge < lifeExpectancy, retirement/pensionStart >= startAge）
        int startAge       = 40 + rng.nextInt(41);            // 40〜80
        // lifeExpectancy: startAge+1〜110
        int lifeExpectancy = startAge + 1 + rng.nextInt(Math.max(1, 110 - startAge));
        // retirementAge: startAge〜min(80, lifeExpectancy)
        int retireMax      = Math.min(80, lifeExpectancy);
        int retirementAge  = startAge + rng.nextInt(Math.max(1, retireMax - startAge + 1));
        // pensionStartAge: startAge〜min(75, lifeExpectancy)
        int pensionMax     = Math.min(75, lifeExpectancy);
        int pensionStartAge = startAge + rng.nextInt(Math.max(1, pensionMax - startAge + 1));

        req.startAge        = startAge;
        req.lifeExpectancy  = lifeExpectancy;
        req.retirementAge   = retirementAge;
        req.pensionStartAge = pensionStartAge;

        // 生活費・娯楽費（月額）
        req.basicLivingCost = 50_000 + rng.nextInt(451) * 1_000;  // 5万〜50万
        req.leisureCost     = rng.nextInt(301) * 1_000;            // 0〜30万

        // 収入（月額）
        req.salaryAfter60  = rng.nextInt(1_001) * 1_000;           // 0〜100万
        req.publicPension  = rng.nextInt(501)   * 1_000;           // 0〜50万

        // 資産残高
        req.dcBalance    = rng.nextInt(1_001) * 100_000.0;         // 0〜1億
        req.nisaBalance  = rng.nextInt(1_001) * 100_000.0;         // 0〜1億
        req.otherBalance = rng.nextInt(1_001) * 100_000.0;         // 0〜1億

        // 利回り・インフレ
        req.dcReturnRate     = -0.05 + rng.nextDouble() * 0.20;     // -5%〜+15%
        req.inflationRate    = rng.nextDouble() * 0.05;             // 0〜5%
        req.returnVolatility = rng.nextDouble() * 0.30;             // 0〜30%

        // 手取り率
        req.salaryNetRate  = 0.70 + rng.nextDouble() * 0.15;        // 70〜85%
        req.pensionNetRate = 0.70 + rng.nextDouble() * 0.29;        // 70〜99%
        req.dcNetRate      = 0.70 + rng.nextDouble() * 0.29;        // 70〜99%

        // 取り崩し順序（ランダムに並び替え）
        List<String> order = new ArrayList<>(List.of("DC", "NISA", "OTHER"));
        Collections.shuffle(order, rng);
        req.withdrawalOrder = order;

        req.specialExpenses = List.of();
        return req;
    }

    // =========================================================================
    // テスト1〜3: DC 60歳未満ロック
    // =========================================================================

    /** 55〜59歳の間は DC 残高が変化しないことを確認（利回り0%） */
    @Test
    void testDcNotWithdrawnBeforeAge60() {
        SimulationRequest req = baseReq(55);
        SimulationResponse resp = service.simulate(req);
        double initialDc = req.dcBalance;
        for (YearResult yr : resp.yearlyResults) {
            if (yr.age < 60) {
                assertEquals((long) initialDc, yr.dcBalance,
                    "age=" + yr.age + " のDC残高は " + (long)initialDc + " のはずが " + yr.dcBalance);
            }
        }
    }

    /** startAge=55 のとき 60歳以降は DC が取り崩されることを確認 */
    @Test
    void testDcWithdrawnFromAge60() {
        SimulationRequest req = baseReq(55);
        SimulationResponse resp = service.simulate(req);
        double initialDc = req.dcBalance;
        YearResult at60 = resp.yearlyResults.stream()
            .filter(yr -> yr.age == 60).findFirst()
            .orElseThrow(() -> new AssertionError("60歳のデータが見つからない"));
        assertTrue(at60.dcBalance < initialDc, "60歳でDCが取り崩されていない: " + at60.dcBalance);
    }

    /** startAge=60 のとき 60歳から取り崩されることを確認 */
    @Test
    void testDcWithdrawnWhenStartAge60() {
        SimulationRequest req = baseReq(60);
        SimulationResponse resp = service.simulate(req);
        YearResult first = resp.yearlyResults.get(0);
        assertEquals(60, first.age);
        assertTrue(first.dcBalance < req.dcBalance,
            "startAge=60 のとき60歳でDCが取り崩されていない: " + first.dcBalance);
    }

    // =========================================================================
    // テスト4: プロパティベーステスト（1,000回ランダム実行）
    //
    // 検証するインバリアント:
    //   I1. 各資産残高（DC/NISA/その他）が負にならない
    //   I2. totalBalance = dcBalance + nisaBalance + otherBalance
    //   I3. yearlyResults の行数 = lifeExpectancy - startAge + 1
    //   I4. 年齢が startAge〜lifeExpectancy の連続した整数
    //   I5. assetDepletionAge が -1 か [startAge, lifeExpectancy] の範囲内
    //   I6. failureProbability が [0.0, 1.0] の範囲内
    // =========================================================================

    @Test
    void propertyBasedRandomTest() {
        final int TRIALS = 1_000;
        Random rng = new Random(42L); // シード固定で再現性を確保

        for (int trial = 0; trial < TRIALS; trial++) {
            SimulationRequest req = randomReq(rng);
            SimulationResponse resp;
            try {
                resp = service.simulate(req);
            } catch (Exception e) {
                fail("Trial " + trial + " で例外が発生: " + e.getMessage()
                    + "\n  startAge=" + req.startAge
                    + ", lifeExpectancy=" + req.lifeExpectancy
                    + ", retirementAge=" + req.retirementAge
                    + ", pensionStartAge=" + req.pensionStartAge);
                return;
            }

            String ctx = String.format(
                "Trial %d (startAge=%d, lifeExp=%d, retire=%d, pension=%d, dcReturn=%.3f, inflation=%.3f)",
                trial, req.startAge, req.lifeExpectancy, req.retirementAge,
                req.pensionStartAge, req.dcReturnRate, req.inflationRate);

            // I3: 行数チェック
            int expectedRows = req.lifeExpectancy - req.startAge + 1;
            assertEquals(expectedRows, resp.yearlyResults.size(), ctx + ": yearlyResults の行数が不正");

            int prevAge = req.startAge - 1;
            for (YearResult yr : resp.yearlyResults) {

                // I4: 年齢の連続性
                assertEquals(prevAge + 1, yr.age, ctx + ": age が連続していない（prevAge=" + prevAge + ")");
                prevAge = yr.age;

                // I1: 残高が負にならない
                assertTrue(yr.dcBalance    >= 0, ctx + " age=" + yr.age + ": dcBalance が負 (" + yr.dcBalance + ")");
                assertTrue(yr.nisaBalance  >= 0, ctx + " age=" + yr.age + ": nisaBalance が負 (" + yr.nisaBalance + ")");
                assertTrue(yr.otherBalance >= 0, ctx + " age=" + yr.age + ": otherBalance が負 (" + yr.otherBalance + ")");
                assertTrue(yr.totalBalance >= 0, ctx + " age=" + yr.age + ": totalBalance が負 (" + yr.totalBalance + ")");

                // I2: 合計残高の整合性（Math.round() による ±1 の丸め誤差を許容）
                long expectedTotal = yr.dcBalance + yr.nisaBalance + yr.otherBalance;
                assertTrue(Math.abs(expectedTotal - yr.totalBalance) <= 1,
                    ctx + " age=" + yr.age + ": totalBalance=" + yr.totalBalance
                    + " が dc+nisa+other=" + expectedTotal + " と ±1 を超えて不一致");
            }

            // I5: assetDepletionAge の範囲
            int depAge = resp.assetDepletionAge;
            assertTrue(depAge == -1 || (depAge >= req.startAge && depAge <= req.lifeExpectancy),
                ctx + ": assetDepletionAge=" + depAge + " が範囲外");

            // I6: failureProbability の範囲
            assertTrue(resp.failureProbability >= 0.0 && resp.failureProbability <= 1.0,
                ctx + ": failureProbability=" + resp.failureProbability + " が [0,1] 外");
        }
    }
}
