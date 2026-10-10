package gcs.modules.others.service;

import gcs.base.YmlConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 头像生成服务
 * 固定头像前缀            avatar-[939839327763].png
 * 文件夹随机头像(fdimgs)  seed-[939839327763].png
 */
@Service
public class GcsAvatarService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private YmlConfig ymlConfig;

    // 缓存随机文件
    private List<File> seedFiles = null;

    /**
     * 裁切头像
     * @param contentStream  头像文件流
     * @param fileSuffix     头像格式 png | jpg | jpeg
     * @param x              起始X坐标
     * @param y              起始Y坐标
     * @param width          保留宽度
     * @param height         保留高度
     * @return 裁切后头像
     */
    public byte[] cropAvatar(InputStream contentStream, String fileSuffix, Integer x, Integer y, Integer width, Integer height) throws IOException {
        if (!Arrays.asList("png", "jpg", "jpeg").contains(fileSuffix)) {
            throw new IllegalArgumentException("不支持的文件格式: " + fileSuffix);
        }
        // 读入并裁切头像
        BufferedImage bufferedImage = ImageIO.read(contentStream);
        bufferedImage = bufferedImage.getSubimage(x, y, width, height);
        // 写出裁切后头像
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, fileSuffix, bout);
        return bout.toByteArray();
    }

    /**
     * 保存头像
     * @param contentStream  头像文件流
     * @param fileSuffix     头像格式 png | jpg | jpeg
     * @return 保存后头像， eg：avatar-838267379.png
     */
    public String saveAvatar(InputStream contentStream, String fileSuffix) throws IOException {
        if (!Arrays.asList("png", "jpg", "jpeg").contains(fileSuffix)) {
            throw new IllegalArgumentException("不支持的文件格式: " + fileSuffix);
        }
        // 生成头像文件
        String avatarName = "avatar-" + RandomStringUtils.random(9, false, true) + "." + fileSuffix;
        File avatarFile = new File(ymlConfig.getToolsAvatarRootDir(), avatarName);

        // 确保目录存在
        if (!avatarFile.getParentFile().exists()) {
            avatarFile.getParentFile().mkdirs();
        }
        avatarFile.createNewFile();

        // 写入头像文件
        try (OutputStream outputStream = new FileOutputStream(avatarFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = contentStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return avatarName;
    }

    /**
     * 获取Avatar头像
     * 如果avatarName以avatar-开头，则尝试直接读取头像；
     * 如无头像，尝试根据配置的头像生成策略['bdparts', 'fdimgs'] -> ['生成随机头像', 'Hash选择头像']来生成头像
     * 文件夹随机头像(fdimgs)应该以seed-xxx开头
     * @param avatarName  头像名
     * @param avatarSize   头像大小
     * @return 头像二进制数据
     */
    public byte[] getAvatar(String avatarName, Integer avatarSize) throws IOException {
        if (avatarName == null || avatarName.isEmpty()) {
            throw new IllegalArgumentException("头像名称不能为空");
        }

        // 尝试从文件夹获取头像
        if (avatarName.startsWith("avatar-")) {
            File avatarFile = new File(ymlConfig.getToolsAvatarRootDir(), avatarName);
            if (avatarFile.exists() && avatarFile.isFile()) {
                return Files.readAllBytes(avatarFile.toPath());
            }
        }

        // 截取头像后缀(默认png)
        String avatarSeed = avatarName;
        String avatarSuffix = "png";
        int dotIndex = avatarName.indexOf('.');
        if (dotIndex > 0) {
            avatarSeed = avatarName.substring(0, dotIndex);
            avatarSuffix = avatarName.substring(dotIndex + 1);
        }

        // 写出随机头像
        byte[] avatarBytes;
        if ("bdparts".equalsIgnoreCase(ymlConfig.getToolsAvatarRandStrategy())) {
            // BodyPart随机头像
            long randomSeed = avatarSeed.hashCode();
            Random random = new Random(randomSeed);
            BufferedImage bufferedImage = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);

            // 创建画布，多部分叠加
            Graphics graphics = bufferedImage.getGraphics();
            Map<String, Integer> bodyParts = new LinkedHashMap<>();
            bodyParts.put("legs", 5);
            bodyParts.put("hair", 5);
            bodyParts.put("arms", 5);
            bodyParts.put("body", 15);
            bodyParts.put("eyes", 15);
            bodyParts.put("mouth", 10);

            for (Map.Entry<String, Integer> entry : bodyParts.entrySet()) {
                String bodyPart = entry.getKey();
                int noOfVariants = entry.getValue();
                String resourcePath = "/" + bodyPart + "_" + (random.nextInt(noOfVariants) + 1) + ".png";
                BufferedImage image = ImageIO.read(this.getClass().getResourceAsStream(resourcePath));
                graphics.drawImage(image, 0, 0, null);
            }

            // 写出头像
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            bufferedImage = scaleImage(bufferedImage, avatarSize, avatarSize);
            ImageIO.write(bufferedImage, avatarSuffix, bout);
            avatarBytes = bout.toByteArray();
        } else {
            // 文件随机头像(默认)
            String fullSuffix = "." + avatarSuffix;
            List<File> suffixSeedFiles = getSeedFiles(ymlConfig.getToolsAvatarRootDir()).stream()
                    .filter(file -> file.getName().endsWith(fullSuffix))
                    .sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());

            int seedFileSize = suffixSeedFiles.size();
            if (seedFileSize > 0) {
                // 根据avatarSeed返回随机文件
                int randomSeed = avatarSeed.hashCode();
                File randomFile = suffixSeedFiles.get(Math.abs(randomSeed) % seedFileSize);
                logger.info("rand -- " + randomFile.getAbsolutePath());

                // 写出头像
                BufferedImage bufferedImage = ImageIO.read(randomFile);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                bufferedImage = scaleImage(bufferedImage, avatarSize, avatarSize);
                ImageIO.write(bufferedImage, avatarSuffix, bout);
                avatarBytes = bout.toByteArray();
            } else {
                // 写出空头像（直接画目标尺寸，TYPE_INT_RGB 避免 JPEG 编码 alpha 通道颜色偏移）
                BufferedImage bufferedImage = new BufferedImage(avatarSize, avatarSize, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = bufferedImage.createGraphics();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, avatarSize, avatarSize);
                g2d.dispose();
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, avatarSuffix, bout);
                avatarBytes = bout.toByteArray();
            }
        }
        return avatarBytes;
    }

    /**
     * 按前缀初始化头像文件：检查 toolsAvatar.rootDir，若缺少对应前缀的文件，
     * 则从 classpath 的 images/avatar/ 目录拷贝过去。
     *
     * @param prefix 文件前缀，如 "seed-" 或 "avatar-"
     */
    public void ensurePrefixAvatars(String prefix) {
        File avatarDir = new File(ymlConfig.getToolsAvatarRootDir());
        // 确保目录存在
        if (!avatarDir.exists() && !avatarDir.mkdirs()) {
            logger.warn("头像目录创建失败: {}", avatarDir.getAbsolutePath());
            return;
        }
        // 从 classpath 拷贝指定前缀的头像文件（已存在则跳过）
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:images/avatar/" + prefix + "*");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                File target = new File(avatarDir, filename);
                if (target.exists()) {
                    continue;
                }
                try (InputStream in = resource.getInputStream();
                     OutputStream out = new FileOutputStream(target)) {
                    StreamUtils.copy(in, out);
                }
                logger.info("拷贝头像文件: {}", target.getName());
            }
        } catch (IOException e) {
            logger.warn("初始化头像文件失败, prefix={}", prefix, e);
        }
    }

    // 随机文件缓存工具
    private List<File> getSeedFiles(String avatarDir) {
        // 检查缓存
        if (seedFiles != null) {
            return seedFiles;
        }

        // 种子文件不存在则先从 classpath 拷贝
        ensurePrefixAvatars("seed-");

        // 读取全部seed-开头文件
        File avatarDirFile = new File(avatarDir);
        File[] seedFilesArray = avatarDirFile.listFiles((dir, name) -> name.startsWith("seed-"));

        if (seedFilesArray == null) {
            seedFiles = Collections.emptyList();
        } else {
            // 缓存并返回文件，按文件名排序
            seedFiles = Arrays.asList(seedFilesArray);
            seedFiles.sort(Comparator.comparing(File::getName));
        }
        return seedFiles;
    }

    // 扩大缩小图像工具
    private BufferedImage scaleImage(BufferedImage image, int width, int height) {
        // 内存图像
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // 创建画布
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // 写入图像
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();

        // 返回图像
        return scaledImage;
    }
}