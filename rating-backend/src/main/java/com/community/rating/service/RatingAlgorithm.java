package com.community.rating.service;

import com.community.rating.dto.ContentDataDTO;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 核心评级算法组件：严格实现两阶段评级数学公式。
 * 所有计算均使用 BigDecimal 确保精度。
 * 
 * 【阶段一】CIS (Content Influence Score) 公式：
 * CIS = (BaseScore × QualityFactor × ShareBoost) - NegativePenalty
 * 
 * 【阶段二】DES (Domain Expertise Score) 公式：
 * DES_K = Σ(CIS_i × RecencyFactor_i)
 */
@Component
public class RatingAlgorithm {

    // --- 阶段一: CIS 权重参数 ---
    // BaseScore 权重
    private static final BigDecimal W_READ = new BigDecimal("0.05");
    private static final BigDecimal W_LIKE = new BigDecimal("0.1");
    private static final BigDecimal W_COMMENT = new BigDecimal("0.3");
    private static final BigDecimal W_SHARE = new BigDecimal("0.5");
    
    // QualityFactor 权重（Sigmoid 逻辑函数的输入）
    private static final BigDecimal W_COLLECT = new BigDecimal("4.0");
    private static final BigDecimal W_LENGTH = new BigDecimal("0.25");
    private static final BigDecimal W_QUALITY_LIKE = new BigDecimal("0.1");
    private static final BigDecimal W_HATE = new BigDecimal("5.0");
    private static final BigDecimal B_OFFSET = new BigDecimal("-1.0"); // 平移常数
    private static final BigDecimal S_SIGMOID_SCALE = new BigDecimal("0.15"); // sigmoid 缩放因子
    
    // ShareBoost 参数
    private static final BigDecimal GAMMA_SHARE_BOOST = new BigDecimal("0.12");
    
    // NegativePenalty 权重
    private static final BigDecimal W_NEGATIVE_HATE = new BigDecimal("10.0");

    // --- 阶段二: DES 评级阈值 ---
    // 根据实际分数分布调整（目标分布：L1 39%, L2 25%, L3 23%, L4 10%, L5 2%）
    private static final BigDecimal C1_EXPLORER = new BigDecimal("100");       // L1 → L2 分界线
    private static final BigDecimal C2_CONTRIBUTOR = new BigDecimal("320");    // L2 → L3 分界线
    private static final BigDecimal C3_EXPERT = new BigDecimal("700");         // L3 → L4 分界线
    private static final BigDecimal C4_MASTER = new BigDecimal("1300");        // L4 → L5 分界线
    
    // 保持小数点后 4 位精度
    private static final int SCALE = 4;
    private static final RoundingMode MODE = RoundingMode.HALF_UP;


    /**
     * 1. 核心方法：计算内容影响力分数 (Content Influence Score - CIS)
     * CIS = (BaseScore × QualityFactor × ShareBoost) - NegativePenalty
     *
     * @param content 包含快照数据的 DTO
     * @return CIS 分数 (BigDecimal)
     */
    public BigDecimal calculateCIS(ContentDataDTO content) {
        // Step 1.1: BaseScore (基础得分)
        // BaseScore = (W_read × 阅读数) + (W_Like × 点赞数) + (W_Comment × 评论数) + (W_Share × 转发数)
        BigDecimal baseScore = BigDecimal.ZERO
                .add(new BigDecimal(content.getReadCount()).multiply(W_READ))
                .add(new BigDecimal(content.getLikeCount()).multiply(W_LIKE))
                .add(new BigDecimal(content.getCommentCount()).multiply(W_COMMENT))
                .add(new BigDecimal(content.getShareCount()).multiply(W_SHARE));

        // Step 1.2: QualityFactor (质量因子 - 使用 Sigmoid 逻辑函数)
        // Z = W_Collect × Collects + W_Length × PostLengthLevel + W_Like × Likes + W_Hate × Hates + b
        BigDecimal collectTerm = new BigDecimal(content.getCollectCount()).multiply(W_COLLECT);
        BigDecimal lengthTerm = new BigDecimal(content.getPostLengthLevel()).multiply(W_LENGTH);
        BigDecimal likeTerm = new BigDecimal(content.getLikeCount()).multiply(W_QUALITY_LIKE);
        BigDecimal hateTerm = new BigDecimal(content.getHateCount()).multiply(W_HATE);
        
        BigDecimal Z = collectTerm
                .add(lengthTerm)
                .add(likeTerm)
                .add(hateTerm)
                .add(B_OFFSET);
        
        // QualityFactor = σ(s × Z) = 1 / (1 + e^(-s×Z))
        // 其中 s = 0.15（sigmoid 缩放因子）
        BigDecimal scaledZ = Z.multiply(S_SIGMOID_SCALE);
        BigDecimal qualityFactor = calculateSigmoid(scaledZ);
        
        // Step 1.3: ShareBoost (转发放大系数)
        // ShareBoost = 1 + γ × log(1 + shares)
        // 其中 γ = 0.12（转发增量权重）
        BigDecimal shareBoost = calculateShareBoost(content.getShareCount());
        
        // Step 1.4: NegativePenalty (负面惩罚)
        // NegativePenalty = W_Hate × 点踩数
        BigDecimal negativePenalty = new BigDecimal(content.getHateCount()).multiply(W_NEGATIVE_HATE);
        
        // Step 1.5: Final CIS
        // CIS = (BaseScore × QualityFactor × ShareBoost) - NegativePenalty
        BigDecimal cis = baseScore
                .multiply(qualityFactor)
                .multiply(shareBoost)
                .subtract(negativePenalty);

        return cis.setScale(SCALE, MODE);
    }
    
    /**
     * 辅助方法：计算 Sigmoid 函数值
     * σ(x) = 1 / (1 + e^(-x))
     *
     * @param x 输入值
     * @return Sigmoid(x) 的值，范围 [0, 1]
     */
    private BigDecimal calculateSigmoid(BigDecimal x) {
        // 使用 e^(-x)
        double expNegX = Math.exp(x.negate().doubleValue());
        
        // sigmoid = 1 / (1 + e^(-x))
        BigDecimal denominator = BigDecimal.ONE.add(new BigDecimal(String.valueOf(expNegX)));
        BigDecimal sigmoid = BigDecimal.ONE.divide(denominator, SCALE, MODE);
        
        return sigmoid;
    }
    
    /**
     * 辅助方法：计算转发放大系数
     * ShareBoost = 1 + γ × log(1 + shares)
     * 其中 γ = 0.12
     *
     * @param shareCount 转发数
     * @return ShareBoost 系数
     */
    private BigDecimal calculateShareBoost(long shareCount) {
        // log(1 + shares) 使用自然对数
        double logValue = Math.log(1.0 + shareCount);
        BigDecimal logTerm = new BigDecimal(String.valueOf(logValue));
        
        // ShareBoost = 1 + γ × log(1 + shares)
        BigDecimal shareBoost = BigDecimal.ONE.add(GAMMA_SHARE_BOOST.multiply(logTerm));
        
        return shareBoost.setScale(SCALE, MODE);
    }
    
    /**
     * 2. 时效性衰减因子 (RecencyFactor)
     * 采用分段衰减逻辑：
     * - 近 30 天： 1.0 (权重不变)
     * - 31 - 90 天： 0.7 (权重轻度衰减)
     * - 91 - 180 天： 0.4 (权重中度衰减)
     * - 大于 180 天： 0.1 (权重显著衰减)
     *
     * @param publishTime 内容发布时间
     * @return RecencyFactor (BigDecimal)
     */
    public BigDecimal calculateRecencyFactor(LocalDateTime publishTime) {
        // 使用虚拟时间
        long daysSincePublish = ChronoUnit.DAYS.between(publishTime, com.community.rating.simulation.TimeSimulation.now());

        if (daysSincePublish <= 30) {
            return BigDecimal.ONE; // 1.0
        } else if (daysSincePublish <= 90) {
            return new BigDecimal("0.7"); // 轻度衰减
        } else if (daysSincePublish <= 180) {
            return new BigDecimal("0.4"); // 中度衰减
        } else {
            return new BigDecimal("0.1"); // 显著衰减
        }

        //         // 衰减率常数 k = 0.01（每天衰减约 1%）
        // // 可根据实际测试效果调整，如：
        // // k = 0.005: 衰减更慢，旧内容保持较长时间的影响力
        // // k = 0.01:  中等衰减速度（推荐）
        // // k = 0.02:  衰减较快，新内容优势明显
        // double k = 0.01;
        
        // // 计算 RecencyFactor = e^(-k × t)
        // // 当 t = 0 时，RecencyFactor = 1.0
        // // 当 t = 30 时，RecencyFactor ≈ 0.741
        // // 当 t = 69 时，RecencyFactor ≈ 0.501（约半衰期）
        // // 当 t = 100 时，RecencyFactor ≈ 0.368
        // double decayValue = Math.exp(-k * daysSincePublish);
        // BigDecimal recencyFactor = new BigDecimal(String.valueOf(decayValue));
        
        // // 确保值在有效范围内
        // if (recencyFactor.compareTo(BigDecimal.ZERO) <= 0) {
        //     recencyFactor = new BigDecimal("0.0001"); // 最小值，防止完全消失
        // }
        
        // return recencyFactor.setScale(SCALE, MODE);
    }

    /**
     * 3. 核心方法：计算成员领域专精度得分 (Domain Expertise Score - DES)
     * DES_K = Σ(CIS_i × RecencyFactor_i)
     * 
     * 累加成员在特定领域 K 中发布的所有内容的 CIS，加权以时效性因子。
     *
     * @param contentDTOs 成员在特定领域 K 内的所有内容及其 CIS
     * @return DES 分数 (BigDecimal)
     */
    public BigDecimal calculateDES(List<ContentDataDTO> contentDTOs) {
        BigDecimal desScore = BigDecimal.ZERO;
        
        for (ContentDataDTO dto : contentDTOs) {
            // 获取时效性因子
            BigDecimal recencyFactor = calculateRecencyFactor(dto.getPublishTime());
            
            // 累加项: CIS_i × RecencyFactor_i
            BigDecimal term = dto.getCisScore().multiply(recencyFactor);
            desScore = desScore.add(term);
        }

        return desScore.setScale(SCALE, MODE);
    }

    /**
     * 4. 最终评级映射：将 DES 分数映射到预定义的评级等级。
     * 
     * Rating_K = {
     *   L1: DES_K < C1                    (领域新手)
     *   L2: C1 ≤ DES_K < C2               (领域探索者)
     *   L3: C2 ≤ DES_K < C3               (领域贡献者)
     *   L4: C3 ≤ DES_K < C4               (领域专家)
     *   L5: DES_K ≥ C4                    (领域大师)
     * }
     *
     * @param desScore DES 分数
     * @return 评级等级描述 (L0 - L5，其中 L0 为负分用户)
     */
    public String determineRatingLevel(BigDecimal desScore) {
        // 处理负分（表示用户贡献了大量低质内容）
        if (desScore.compareTo(BigDecimal.ZERO) < 0) {
            return "L0";
        }
        
        // 正常分级
        if (desScore.compareTo(C4_MASTER) >= 0) {
            return "L5"; // 领域大师
        } else if (desScore.compareTo(C3_EXPERT) >= 0) {
            return "L4"; // 领域专家
        } else if (desScore.compareTo(C2_CONTRIBUTOR) >= 0) {
            return "L3"; // 领域贡献者
        } else if (desScore.compareTo(C1_EXPLORER) >= 0) {
            return "L2"; // 领域探索者
        } else {
            return "L1"; // 领域新手
        }
    }

    /**
     * 5. 格式化平均评级分数
     * 
     * 将所有成员的平均 DES 分数转换为对应的等级字符串。
     * 例如：平均 DES = 250 → "L3 (250)"
     * 
     * @param avgDesScore 平均 DES 分数 (Double)
     * @return 格式化的评级字符串，例如 "L3" 或 "N/A"
     */
    public String formatAverageRatingLevel(Double avgDesScore) {
        if (avgDesScore == null) {
            return "N/A";
        }
        
        // 将 Double 转换为 BigDecimal
        BigDecimal avgScore = BigDecimal.valueOf(avgDesScore);
        
        // 获取等级
        String level = determineRatingLevel(avgScore);
        
        // 格式化输出：等级 + 分数（四舍五入到整数）
        BigDecimal roundedScore = avgScore.setScale(0, RoundingMode.HALF_UP);
        return level + " (" + roundedScore.toPlainString() + ")";
    }
}