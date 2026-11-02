package svms;

import java.math.BigDecimal;
import java.time.YearMonth;
import static svms.Main.*;

/** 无第三方依赖的业务回归测试，失败时抛出 AssertionError。 */
public class CoreTests {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("检查失败：" + checks);
    }
    private static void fails(Class<? extends Exception> type, Runnable action) {
        checks++;
        try { action.run(); } catch (Exception e) {
            if (type.isInstance(e)) return;
            throw new AssertionError("异常类型错误", e);
        }
        throw new AssertionError("未拒绝无效操作：" + checks);
    }
    public static void main(String[] args) {
        var month = YearMonth.of(2026, 10);
        var service = new Service();
        var material = new Material("M1", "A", "素材", MaterialType.IMAGE);
        service.addMaterial(material);
        check(service.findMaterials("A").size() == 1);
        check(service.findMaterials("B").isEmpty());
        fails(IllegalArgumentException.class, () -> service.addMaterial(material));
        fails(IllegalArgumentException.class, () -> new Revenue("x", "A", month, new BigDecimal("-1")));
        fails(IllegalArgumentException.class, () -> new Revenue("x", "A", month, new BigDecimal("1.001")));
        fails(IllegalArgumentException.class, () -> new ServiceFeePolicy(new BigDecimal("1.1")));
        var policy = new ServiceFeePolicy(new BigDecimal("0.10"));
        check(policy.calculate(new BigDecimal("0.05")).equals(new BigDecimal("0.04")));
        check(policy.calculate(BigDecimal.ZERO).equals(new BigDecimal("0.00")));
        var r = new Revenue("R1", "A", month, new BigDecimal("1000.00"));
        service.addRevenue(r);
        fails(IllegalArgumentException.class, () -> service.addRevenue(r));
        service.addRevenue(new Revenue("R2", "A", month, new BigDecimal("250.50")));
        service.addRevenue(new Revenue("R3", "B", month, new BigDecimal("900.00")));
        service.addRevenue(new Revenue("R4", "A", month.plusMonths(1), new BigDecimal("999.00")));
        var bill = service.settle("A", month, policy);
        check(bill.gross.equals(new BigDecimal("1250.50")));
        check(bill.net.equals(new BigDecimal("1125.45")));
        fails(IllegalStateException.class, bill::markPaid);
        check(bill.status() == Status.DRAFT);
        bill.confirm();
        fails(IllegalStateException.class, bill::confirm);
        bill.markPaid();
        check(bill.status() == Status.PAID);
        fails(IllegalStateException.class, bill::markPaid);
        fails(IllegalStateException.class, () -> service.settle("A", month, policy));
        fails(IllegalStateException.class, () -> service.addRevenue(new Revenue("R5", "A", month, BigDecimal.ONE)));
        fails(IllegalStateException.class, () -> service.settle("C", month, policy));
        System.out.println("PASS: " + checks + " business checks");
    }
}
