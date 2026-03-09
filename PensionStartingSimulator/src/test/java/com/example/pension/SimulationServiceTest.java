package com.example.pension;

import com.example.pension.model.SimulationRequest;
import com.example.pension.model.SimulationResponse;
import com.example.pension.model.YearResult;
import com.example.pension.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DC 受取年齢ロック（60歳未満では取り崩し不可）の動作を検証するテスト
 */
@SpringBootTest
class SimulationServiceTest {

    @Autowired
    private SimulationService service;

    // ---- テスト用リクエスト生成ヘルパー ----

    private static SimulationRequest baseReq(int startAge) {
        SimulationRequest req = new SimulationRequest();
        req.startAge        = startAge;
        req.lifeExpectancy  = 90;
        req.retirementAge   = 65;
        req.pensionStartAge = 65;
        req.basicLivingCost = 200_000;   // 月20万
        req.leisureCost     = 100_000;   // 月10万
        req.salaryAfter60   = 0;         // 給与なし → 必ずショートフォール発生
        req.publicPension   = 0;
        req.dcBalance       = 20_000_000; // DC 2000万
        req.nisaBalance     = 0;
        req.otherBalance    = 0;
        req.dcReturnRate    = 0.0;        // 利回り0%（純粋な取り崩しのみ確認）
        req.inflationRate   = 0.0;
        req.returnVolatility = 0.1;
        req.withdrawalOrder = List.of("DC", "NISA", "OTHER");
        return req;
    }

    /**
     * テスト1: startAge=55 のとき、55〜59歳の間（age < 60）は
     * DC残高が取り崩しで減らないことを確認する。
     * （利回り0%なら DC残高は開始値のまま変化しないはず）
     */
    @Test
    void testDcNotWithdrawnBeforeAge60() {
        SimulationRequest req = baseReq(55);

        SimulationResponse resp = service.simulate(req);

        // 55〜59歳の DC 残高は初期値のまま変わらないはず（利回り0%）
        double initialDc = req.dcBalance;
        for (YearResult yr : resp.yearlyResults) {
            if (yr.age < 60) {
                assertEquals((long) initialDc, yr.dcBalance,
                        "age=" + yr.age + " のDC残高は " + (long)initialDc + " のはずが " + yr.dcBalance);
            }
        }
    }

    /**
     * テスト2: startAge=55 のとき、60歳以降は
     * DC が正しく取り崩されることを確認する。
     */
    @Test
    void testDcWithdrawnFromAge60() {
        SimulationRequest req = baseReq(55);

        SimulationResponse resp = service.simulate(req);

        // 60歳時点の DC 残高は初期値より減っているはず
        double initialDc = req.dcBalance;
        YearResult at60 = resp.yearlyResults.stream()
                .filter(yr -> yr.age == 60)
                .findFirst()
                .orElseThrow(() -> new AssertionError("60歳のデータが見つからない"));

        assertTrue(at60.dcBalance < initialDc,
                "60歳でDCが取り崩されていない: " + at60.dcBalance);
    }

    /**
     * テスト3: startAge=60 の場合は従来通り60歳から取り崩される。
     */
    @Test
    void testDcWithdrawnWhenStartAge60() {
        SimulationRequest req = baseReq(60);

        SimulationResponse resp = service.simulate(req);

        // 60歳の1年目からDCが取り崩されているはず（利回り0%なので残高は減少）
        YearResult first = resp.yearlyResults.get(0);
        assertEquals(60, first.age);
        assertTrue(first.dcBalance < req.dcBalance,
                "startAge=60 のとき60歳でDCが取り崩されていない: " + first.dcBalance);
    }
}
