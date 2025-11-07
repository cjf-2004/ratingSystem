package com.community.rating.service;

import com.community.rating.dto.ContentDataDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 核心评级算法组件：严格实现两阶段评级数学公式。
 * 所有计算均使用 BigDecimal 确保精度。
 */
public class RatingAlgorithm {

    // --- 阶段一: CIS 权重参数 ---
    private static final BigDecimal W_READ = new BigDecimal("0.05");
    private static final BigDecimal W_LIKE = new BigDecimal("0.1");
    private static final BigDecimal W_COMMENT = new BigDecimal("0.3");
    private static final BigDecimal W_SHARE = new BigDecimal("0.5");
    private static final BigDecimal W_COLLECT = new BigDecimal("4.0");
    private static final BigDecimal W_LENGTH = new BigDecimal("0.25");
    private static final BigDecimal W_HATE = new BigDecimal("10.0");

    // --- 阶段二: DES 评级阈值 (需要根据实际数据分布调整) ---
    // C1: 领域新手 -> 领域探索者
    private static final BigDecimal C1_EXPLORER = new BigDecimal("50");
    // C2: 领域探索者 -> 领域贡献者
    private static final BigDecimal C2_CONTRIBUTOR = new BigDecimal("200");
    // C3: 领域贡献者 -> 领域专家
    private static final BigDecimal C3_EXPERT = new BigDecimal("500");
    // C4: 领域专家 -> 领域大师
    private static final BigDecimal C4_MASTER = new BigDecimal("1000");
    
    // 保持小数点后 4 位精度
    private static final int SCALE = 4;
    private static final RoundingMode MODE = RoundingMode.HALF_UP;


    /**
     * 1. 核心方法：计算内容影响力分数 (Content Influence Score - CIS)
     * CIS = (BaseScore * QualityFactor) - NegativePenalty
     *
     * @param content 包含快照数据的 DTO
     * @return CIS 分数 (BigDecimal)
     */
    public BigDecimal calculateCIS(ContentDataDTO content) {
        // Step 1.1: BaseScore (基础得分)
        // BaseScore = (W_read * 阅读数) + ... + (W_share * 转发数)
        BigDecimal baseScore = BigDecimal.ZERO
                .add(new BigDecimal(content.getReadCount()).multiply(W_READ))
                .add(new BigDecimal(content.getLikeCount()).multiply(W_LIKE))
                .add(new BigDecimal(content.getCommentCount()).multiply(W_COMMENT))
                .add(new BigDecimal(content.getShareCount()).multiply(W_SHARE));

        // Step 1.2: QualityFactor (质量因子)
        // QualityFactor = 1 - e^(- (W_Collect * 收藏数) * (W_Length * PostLengthLevel))
        
        // QualityInput = (W_Collect * 收藏数) * (W_Length * PostLengthLevel)
        BigDecimal collectTerm = new BigDecimal(content.getCollectCount()).multiply(W_COLLECT);
        BigDecimal lengthTerm = new BigDecimal(content.getPostLengthLevel()).multiply(W_LENGTH);
        BigDecimal qualityInput = collectTerm.multiply(lengthTerm);
        
        // 确保 qualityInput 不为 0，防止 Math.exp(0) = 1 导致 QualityFactor=0 (除非收藏和长度等级都为 0)
        double expInput = qualityInput.negate().doubleValue(); 
        
        // QualityFactor = 1 - e^(-QualityInput)
        BigDecimal qualityFactor = BigDecimal.ONE.subtract(
            new BigDecimal(String.valueOf(Math.exp(expInput)))
        ).setScale(SCALE, MODE);

        // Step 1.3: NegativePenalty (负面惩罚)
        // NegativePenalty = W_Hate * 点踩数
        BigDecimal negativePenalty = new BigDecimal(content.getHateCount()).multiply(W_HATE);
        
        // Step 1.4: Final CIS
        // CIS = (BaseScore * QualityFactor) - NegativePenalty
        BigDecimal cis = baseScore.multiply(qualityFactor).subtract(negativePenalty);

        return cis.setScale(SCALE, MODE);
    }
    
    /**
     * 2. 时效性衰减因子 (RecencyFactor)
     * 采用分段衰减逻辑。
     *
     * @param publishTime 内容发布时间
     * @return RecencyFactor (BigDecimal)
     */
    public BigDecimal calculateRecencyFactor(LocalDateTime publishTime) {
        long daysSincePublish = ChronoUnit.DAYS.between(publishTime, LocalDateTime.now());

        if (daysSincePublish <= 30) {
            return BigDecimal.ONE; // 1.0 (近 30 天)
        } else if (daysSincePublish <= 90) {
            return new BigDecimal("0.7"); // 31 - 90 天
        } else if (daysSincePublish <= 180) {
            return new BigDecimal("0.4"); // 91 - 180 天
        } else {
            return new BigDecimal("0.1"); // 大于 180 天
        }
    }

    /**
     * 3. 核心方法：计算成员领域专精度得分 (Domain Expertise Score - DES)
     * DES_K = Sum(CIS_i * RecencyFactor_i)
     *
     * @param contentDTOs 成员在特定领域 K 内的所有内容及其 CIS
     * @return DES 分数 (BigDecimal)
     */
    public BigDecimal calculateDES(List<ContentDataDTO> contentDTOs) {
        BigDecimal desScore = BigDecimal.ZERO;
        
        for (ContentDataDTO dto : contentDTOs) {
            // 获取时效性因子
            BigDecimal recencyFactor = calculateRecencyFactor(dto.getPublishTime());
            
            // 累加项: CIS_i * RecencyFactor_i
            BigDecimal term = dto.getCisScore().multiply(recencyFactor);
            desScore = desScore.add(term);
        }

        return desScore.setScale(SCALE, MODE);
    }

    /**
     * 4. 最终评级映射：将 DES 分数映射到预定义的评级等级。
     *
     * @param desScore DES 分数
     * @return 评级等级描述
     */
    public String determineRatingLevel(BigDecimal desScore) {
        if (desScore.compareTo(C4_MASTER) >= 0) {
            return "L5";
        } else if (desScore.compareTo(C3_EXPERT) >= 0) {
            return "L4";
        } else if (desScore.compareTo(C2_CONTRIBUTOR) >= 0) {
            return "L3";
        } else if (desScore.compareTo(C1_EXPLORER) >= 0) {
            return "L2";
        } else {
            // 注意: DES_K 可能为负值 (因为 CIS 可能为负)，意味着该用户在该领域产生了大量负面内容
            if (desScore.compareTo(BigDecimal.ZERO) < 0) {
                 return "L0";
            }
            return "L1";
        }
    }
}