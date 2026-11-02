package svms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** 课程实验核心：素材、收益和结算。数据保存在内存，重启不保留。 */
public class Main {
    public enum MaterialType { VIDEO, AUDIO, IMAGE }
    public enum Status { DRAFT, CONFIRMED, PAID }

    public record Material(String id, String creatorId, String name, MaterialType type) {
        public Material {
            requireText(id); requireText(creatorId); requireText(name);
            Objects.requireNonNull(type);
        }
    }

    public record Revenue(String id, String creatorId, YearMonth month, BigDecimal amount) {
        public Revenue {
            requireText(id); requireText(creatorId); Objects.requireNonNull(month);
            Objects.requireNonNull(amount);
            if (amount.signum() < 0 || amount.scale() > 2)
                throw new IllegalArgumentException("收入必须非负且最多两位小数");
        }
    }

    public interface RevenuePolicy {
        BigDecimal calculate(BigDecimal gross);
    }

    public static abstract class AbstractRevenuePolicy implements RevenuePolicy {
        protected BigDecimal money(BigDecimal value) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
    }

    /** 示例规则：平台服务费比例，非实际平台或税务规则。 */
    public static class ServiceFeePolicy extends AbstractRevenuePolicy {
        private final BigDecimal feeRate;
        public ServiceFeePolicy(BigDecimal feeRate) {
            Objects.requireNonNull(feeRate);
            if (feeRate.signum() < 0 || feeRate.compareTo(BigDecimal.ONE) > 0)
                throw new IllegalArgumentException("费率须在0至1之间");
            this.feeRate = feeRate;
        }
        public BigDecimal calculate(BigDecimal gross) {
            if (gross.signum() < 0) throw new IllegalArgumentException("收益不能为负数");
            return money(gross.subtract(money(gross.multiply(feeRate))));
        }
    }

    public static final class Settlement {
        public final String creatorId;
        public final YearMonth month;
        public final BigDecimal gross;
        public final BigDecimal net;
        private Status status = Status.DRAFT;
        private Settlement(String creatorId, YearMonth month, BigDecimal gross, BigDecimal net) {
            this.creatorId = creatorId; this.month = month; this.gross = gross; this.net = net;
        }
        public Status status() { return status; }
        public String toText() {
            return "创作者：" + creatorId + "\n"
                    + "结算月份：" + month + "\n"
                    + "总收益：" + gross.toPlainString() + "元\n"
                    + "服务费：" + gross.subtract(net).toPlainString() + "元\n"
                    + "应结算：" + net.toPlainString() + "元\n"
                    + "状态：" + status + "\n";
        }
        /** UTF-8导出；文件已存在时拒绝覆盖，由调用者选择其他路径。 */
        public void exportTo(Path target) throws IOException {
            Files.writeString(target, toText(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        public void confirm() {
            if (status != Status.DRAFT) throw new IllegalStateException("仅草稿可以确认");
            status = Status.CONFIRMED;
        }
        public void markPaid() {
            if (status != Status.CONFIRMED) throw new IllegalStateException("仅已确认账单可以登记付款");
            status = Status.PAID;
        }
    }

    private record Period(String creatorId, YearMonth month) {}

    public static class Service {
        private final Map<String, Material> materials = new LinkedHashMap<>();
        private final Map<String, Revenue> revenues = new LinkedHashMap<>();
        private final Map<Period, Settlement> settlements = new HashMap<>();
        public void addMaterial(Material material) {
            if (materials.putIfAbsent(material.id(), material) != null)
                throw new IllegalArgumentException("素材编号重复");
        }
        public List<Material> findMaterials(String creatorId) {
            return materials.values().stream().filter(m -> m.creatorId().equals(creatorId)).toList();
        }
        public void addRevenue(Revenue revenue) {
            if (settlements.containsKey(new Period(revenue.creatorId(), revenue.month())))
                throw new IllegalStateException("该期间已生成账单，示例实现不支持补录");
            if (revenues.putIfAbsent(revenue.id(), revenue) != null)
                throw new IllegalArgumentException("收入编号重复");
        }
        public Settlement settle(String creatorId, YearMonth month, RevenuePolicy policy) {
            requireText(creatorId); Objects.requireNonNull(month); Objects.requireNonNull(policy);
            var key = new Period(creatorId, month);
            if (settlements.containsKey(key)) throw new IllegalStateException("该期间不得重复结算");
            var matching = revenues.values().stream()
                    .filter(r -> r.creatorId().equals(creatorId) && r.month().equals(month)).toList();
            if (matching.isEmpty()) throw new IllegalStateException("该期间没有收益记录");
            var gross = matching.stream().map(Revenue::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            var bill = new Settlement(creatorId, month, gross, policy.calculate(gross));
            settlements.put(key, bill);
            return bill;
        }
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("编号或名称不能为空");
    }

    public static void main(String[] args) throws IOException {
        var service = new Service();
        service.addMaterial(new Material("M001", "C001", "城市风景视频", MaterialType.VIDEO));
        service.addRevenue(new Revenue("R001", "C001", YearMonth.of(2026, 10), new BigDecimal("1000.00")));
        service.addRevenue(new Revenue("R002", "C001", YearMonth.of(2026, 10), new BigDecimal("250.50")));
        var bill = service.settle("C001", YearMonth.of(2026, 10), new ServiceFeePolicy(new BigDecimal("0.10")));
        bill.confirm(); bill.markPaid();
        System.out.println("素材：" + service.findMaterials("C001"));
        System.out.print(bill.toText());
        if (args.length > 0) {
            var target = Path.of(args[0]);
            bill.exportTo(target);
            System.out.println("账单已导出：" + target.toAbsolutePath());
        }
        System.out.println("以上为演示数据，PAID仅登记状态，没有调用真实支付。");
    }
}
