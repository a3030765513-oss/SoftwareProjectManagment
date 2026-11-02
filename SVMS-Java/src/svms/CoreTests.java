package svms;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
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
    public static void main(String[] args) throws Exception {
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
        check(bill.toText().equals("创作者：A\n结算月份：2026-10\n总收益：1250.50元\n"
                + "服务费：125.05元\n应结算：1125.45元\n状态：DRAFT\n"));
        fails(IllegalStateException.class, bill::markPaid);
        check(bill.status() == Status.DRAFT);
        bill.confirm();
        check(bill.toText().contains("状态：CONFIRMED\n"));
        fails(IllegalStateException.class, bill::confirm);
        bill.markPaid();
        check(bill.status() == Status.PAID);
        var temp = Files.createTempDirectory("svms-export-test-");
        var target = temp.resolve("账单.txt");
        try {
            bill.exportTo(target);
            check(Files.readString(target, StandardCharsets.UTF_8).equals(bill.toText()));
            check(Files.readString(target, StandardCharsets.UTF_8).contains("状态：PAID\n"));
            check(bill.status() == Status.PAID);
            var originalBytes = Files.readAllBytes(target);
            boolean rejected = false;
            try { bill.exportTo(target); }
            catch (FileAlreadyExistsException expected) { rejected = true; }
            check(rejected);
            check(java.util.Arrays.equals(originalBytes, Files.readAllBytes(target)));
        } finally {
            Files.deleteIfExists(target);
            Files.deleteIfExists(temp);
        }
        fails(IllegalStateException.class, bill::markPaid);
        fails(IllegalStateException.class, () -> service.settle("A", month, policy));
        fails(IllegalStateException.class, () -> service.addRevenue(new Revenue("R5", "A", month, BigDecimal.ONE)));
        fails(IllegalStateException.class, () -> service.settle("C", month, policy));
        System.out.println("PASS: " + checks + " business checks");
    }
}
