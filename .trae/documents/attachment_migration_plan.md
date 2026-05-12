# 附件上传功能迁移计划

## 1. 项目分析

### 1.1 ecology 项目附件架构

**核心文件：**
- [FileUpload.java](file:///d:/workproject/ecology/src/weaver/file/FileUpload.java) - 主上传处理类
- [MultipartRequest.java](file:///d:/workproject/ecology/src/weaver/file/multipart/MultipartRequest.java) - 底层 multipart 解析器
- [FileManage.java](file:///d:/workproject/ecology/src/weaver/file/FileManage.java) - 文件管理工具

**数据库表结构：**

#### ImageFile 表（普通附件存储）
| 字段名 | 类型 | 说明 |
|--------|------|------|
| imagefileid | int | 文件ID（主键） |
| imagefilename | varchar | 原始文件名 |
| imagefiletype | varchar | 文件类型（MIME类型） |
| imagefileused | int | 是否使用（1=使用） |
| filerealpath | varchar | 文件真实存储路径 |
| iszip | int | 是否压缩（0/1） |
| isencrypt | int | 是否加密（0/1） |
| fileSize | bigint | 文件大小（字节） |
| isaesencrypt | int | 是否AES加密（0/1） |
| aescode | varchar | AES加密密钥 |
| TokenKey | varchar | OSS Token |
| secretLevel | int | 密级 |
| secretValidity | varchar | 密级有效期 |

#### MailResourceFile 表（邮件附件存储）
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | int | 主键 |
| mailid | int | 邮件ID |
| filename | varchar | 文件名 |
| filetype | varchar | 文件类型 |
| filerealpath | varchar | 文件真实路径 |
| iszip | int | 是否压缩 |
| isencrypt | int | 是否加密 |
| isfileattrachment | int | 是否附件 |
| fileContentId | varchar | 内容ID |
| isEncoded | varchar | 是否编码 |
| filesize | bigint | 文件大小 |
| mrf_uuid | varchar | UUID标识 |
| isaesencrypt | int | AES加密标识 |
| aescode | varchar | AES密钥 |
| secretLevel | int | 密级 |
| secretDeadline | varchar | 密级有效期 |

**存储过程实现：**

| 存储过程 | Java实现 | 功能 |
|----------|----------|------|
| `ImageFile_Insert` | [Imagefile_insert.java](file:///d:/workproject/ecology/src/com/weaver/procedure/imagefile/Imagefile_insert.java) | 基础插入（8个字段） |
| `ImageFile_Insert_New` | [Imagefile_insert_new.java](file:///d:/workproject/ecology/src/com/weaver/procedure/imagefile/Imagefile_insert_new.java) | 支持AES加密（10个字段） |
| `MailResourceFile_Insert` | [Mailresourcefile_insert.java](file:///d:/workproject/ecology/src/com/weaver/procedure/mailresourcefile/Mailresourcefile_insert.java) | 邮件附件插入（11个字段） |

### 1.2 blade-tool 项目现有架构

**现有文件模块：**
- [BladeFile.java](file:///d:/workproject/blade-tool/blade-core-boot/src/main/java/org/springblade/core/boot/file/BladeFile.java) - 文件封装类
- [BladeFileUtil.java](file:///d:/workproject/blade-tool/blade-core-boot/src/main/java/org/springblade/core/boot/file/BladeFileUtil.java) - 文件工具类

**OSS 支持：**
- `blade-starter-oss` 模块支持阿里云、七牛、MinIO、腾讯云

## 2. 迁移计划

### 2.1 创建新模块 `blade-starter-attachment`

**模块结构：**
```
blade-starter-attachment/
├── src/main/java/org/springblade/core/attachment/
│   ├── controller/
│   │   └── AttachmentController.java
│   ├── service/
│   │   ├── AttachmentService.java
│   │   └── impl/AttachmentServiceImpl.java
│   ├── entity/
│   │   ├── ImageFile.java
│   │   └── MailResourceFile.java
│   ├── mapper/
│   │   ├── ImageFileMapper.java
│   │   └── MailResourceFileMapper.java
│   ├── config/
│   │   └── AttachmentAutoConfiguration.java
│   ├── props/
│   │   └── AttachmentProperties.java
│   ├── util/
│   │   ├── AESCoder.java
│   │   ├── FileSuffixCheckUtil.java
│   │   └── PicCompression.java
│   ├── multipart/
│   │   ├── MultipartRequest.java
│   │   ├── MultipartParser.java
│   │   ├── FilePart.java
│   │   ├── UploadedFile.java
│   │   ├── FileRenamePolicy.java
│   │   └── DefaultFileRenamePolicy.java
│   └── UploadResult.java
├── src/main/resources/
│   ├── mapper/
│   │   ├── ImageFileMapper.xml
│   │   └── MailResourceFileMapper.xml
│   └── schema/
│       ├── imagefile.sql
│       └── mailresourcefile.sql
└── pom.xml
```

### 2.2 迁移核心代码

**步骤 1：迁移 MultipartRequest 及相关类**

| 文件 | 来源 | 目标 |
|------|------|------|
| `MultipartRequest.java` | `ecology/src/weaver/file/multipart/` | `blade-starter-attachment/src/main/java/org/springblade/core/attachment/multipart/` |
| `MultipartParser.java` | `ecology/src/weaver/file/multipart/` | `blade-starter-attachment/src/main/java/org/springblade/core/attachment/multipart/` |
| `FilePart.java` | `ecology/src/weaver/file/multipart/` | `blade-starter-attachment/src/main/java/org/springblade/core/attachment/multipart/` |
| `UploadedFile.java` | `ecology/src/weaver/file/multipart/` | `blade-starter-attachment/src/main/java/org/springblade/core/attachment/multipart/` |
| `FileRenamePolicy.java` | `ecology/src/weaver/file/multipart/` | `blade-starter-attachment/src/main/java/org/springblade/core/attachment/multipart/` |
| `DefaultFileRenamePolicy.java` | `ecology/src/weaver/file/multipart/` | `blade-starter-attachment/src/main/java/org/springblade/core/attachment/multipart/` |

**步骤 2：迁移工具类**

| 文件 | 来源 | 目标 |
|------|------|------|
| `AESCoder.java` | `ecology/src/weaver/file/` | `blade-starter-attachment/src/main/java/org/springblade/core/attachment/util/` |
| `FileSuffixCheckUtil.java` | `ecology/src/weaver/file/util/` | `blade-starter-attachment/src/main/java/org/springblade/core/attachment/util/` |
| `PicCompression.java` | `ecology/src/weaver/file/util/` | `blade-starter-attachment/src/main/java/org/springblade/core/attachment/util/` |

**步骤 3：创建实体类**

```java
// ImageFile.java
@Data
@TableName("ImageFile")
public class ImageFile extends BaseEntity {
    @TableId("imagefileid")
    private Long imagefileid;
    private String imagefilename;
    private String imagefiletype;
    private Integer imagefileused;
    private String filerealpath;
    private Integer iszip;
    private Integer isencrypt;
    private Long fileSize;
    private Integer isaesencrypt;
    private String aescode;
    private String TokenKey;
    private Integer secretLevel;
    private String secretValidity;
}
```

**步骤 4：创建 Mapper 接口**

```java
// ImageFileMapper.java
public interface ImageFileMapper extends BaseMapper<ImageFile> {
    @Insert("INSERT INTO ImageFile(imagefileid, imagefilename, imagefiletype, imagefileused, " +
            "filerealpath, iszip, isencrypt, fileSize, isaesencrypt, aescode) " +
            "VALUES(#{imagefileid}, #{imagefilename}, #{imagefiletype}, #{imagefileused}, " +
            "#{filerealpath}, #{iszip}, #{isencrypt}, #{fileSize}, #{isaesencrypt}, #{aescode})")
    int insertWithAes(ImageFile entity);
}
```

**步骤 5：创建 Service**

```java
// AttachmentService.java
public interface AttachmentService {
    UploadResult upload(MultipartFile file, String dir);
    List<UploadResult> upload(List<MultipartFile> files, String dir);
    UploadResult uploadToEmail(MultipartFile file);
    boolean delete(Long fileId);
    ImageFile getFile(Long fileId);
}
```

### 2.3 数据库脚本

**imagefile.sql：**
```sql
CREATE TABLE IF NOT EXISTS ImageFile (
    imagefileid BIGINT PRIMARY KEY,
    imagefilename VARCHAR(500),
    imagefiletype VARCHAR(100),
    imagefileused INT DEFAULT 1,
    filerealpath VARCHAR(1000),
    iszip INT DEFAULT 0,
    isencrypt INT DEFAULT 0,
    fileSize BIGINT,
    isaesencrypt INT DEFAULT 0,
    aescode VARCHAR(50),
    TokenKey VARCHAR(100),
    secretLevel INT DEFAULT 4,
    secretValidity VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**mailresourcefile.sql：**
```sql
CREATE TABLE IF NOT EXISTS MailResourceFile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mailid BIGINT,
    filename VARCHAR(500),
    filetype VARCHAR(100),
    filerealpath VARCHAR(1000),
    iszip INT DEFAULT 0,
    isencrypt INT DEFAULT 0,
    isfileattrachment INT DEFAULT 1,
    fileContentId VARCHAR(100),
    isEncoded VARCHAR(10),
    filesize BIGINT,
    mrf_uuid VARCHAR(50) UNIQUE,
    isaesencrypt INT DEFAULT 0,
    aescode VARCHAR(50),
    secretLevel INT DEFAULT 4,
    secretDeadline VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 2.4 配置集成

**AttachmentProperties.java：**
```java
@ConfigurationProperties(prefix = "blade.attachment")
public class AttachmentProperties {
    private String uploadPath = "/opt/upload";
    private boolean needZip = false;
    private boolean needEncrypt = false;
    private AesConfig aes = new AesConfig();
    
    @Data
    public static class AesConfig {
        private boolean enabled = false;
        private int keyLength = 128;
    }
}
```

**AttachmentAutoConfiguration.java：**
```java
@Configuration
@EnableConfigurationProperties(AttachmentProperties.class)
public class AttachmentAutoConfiguration {
    @Bean
    public AttachmentService attachmentService(ImageFileMapper imageFileMapper, 
                                               MailResourceFileMapper mailResourceFileMapper,
                                               AttachmentProperties properties) {
        return new AttachmentServiceImpl(imageFileMapper, mailResourceFileMapper, properties);
    }
}
```

### 2.5 控制器层

```java
@RestController
@RequestMapping("/api/attachment")
public class AttachmentController extends BladeController {
    
    @Autowired
    private AttachmentService attachmentService;
    
    @PostMapping("/upload")
    public R<UploadResult> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "dir", defaultValue = "image") String dir) {
        return data(attachmentService.upload(file, dir));
    }
    
    @PostMapping("/upload/batch")
    public R<List<UploadResult>> uploadBatch(@RequestParam("files") List<MultipartFile> files,
                                              @RequestParam(value = "dir", defaultValue = "image") String dir) {
        return data(attachmentService.upload(files, dir));
    }
    
    @PostMapping("/upload/email")
    public R<UploadResult> uploadToEmail(@RequestParam("file") MultipartFile file) {
        return data(attachmentService.uploadToEmail(file));
    }
    
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return status(attachmentService.delete(id));
    }
}
```

## 3. 依赖与配置

### 3.1 pom.xml 依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springblade</groupId>
        <artifactId>blade-core-tool</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springblade</groupId>
        <artifactId>blade-core-secure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>
</dependencies>
```

### 3.2 application.yml 配置

```yaml
blade:
  attachment:
    upload-path: /opt/upload
    need-zip: false
    need-encrypt: false
    aes:
      enabled: false
      key-length: 128
```

## 4. 迁移风险评估

| 风险点 | 风险等级 | 描述 | 应对措施 |
|--------|----------|------|----------|
| 存储过程迁移 | 高 | ecology 使用 Java 存储过程，需迁移为 MyBatis | 将存储过程逻辑转换为 Mapper XML |
| 文件路径配置 | 高 | ecology 使用绝对路径配置 | 提供可配置的上传路径 |
| AES加密迁移 | 中 | ecology 的 AESCoder 需适配 | 迁移 AESCoder 工具类 |
| OSS 集成差异 | 中 | ecology 有自己的 OSS 逻辑 | 集成 blade-tool 现有的 OSS 模块 |
| 类依赖冲突 | 低 | 部分工具类名可能冲突 | 使用独立包名避免冲突 |

## 5. 测试验证

### 5.1 单元测试

| 测试项 | 测试内容 |
|--------|----------|
| 单文件上传 | 测试正常文件上传流程 |
| 多文件上传 | 测试批量上传功能 |
| 文件类型校验 | 测试禁止危险文件上传 |
| AES加密 | 测试加密上传功能 |
| 文件删除 | 测试文件删除功能 |

### 5.2 集成测试

| 测试项 | 测试内容 |
|--------|----------|
| 数据库写入 | 验证文件元数据正确写入 |
| 文件存储 | 验证文件正确保存到磁盘 |
| OSS上传 | 验证文件上传到云存储 |

## 6. 迁移步骤

| 阶段 | 任务 | 估计时间 |
|------|------|----------|
| 1 | 创建 blade-starter-attachment 模块 | 1小时 |
| 2 | 迁移 multipart 解析器相关类 | 2小时 |
| 3 | 迁移工具类（AESCoder、FileSuffixCheckUtil 等） | 1小时 |
| 4 | 创建实体类和 Mapper | 2小时 |
| 5 | 创建 AttachmentService | 3小时 |
| 6 | 创建 AttachmentController | 1小时 |
| 7 | 创建配置类和属性类 | 1小时 |
| 8 | 编写数据库初始化脚本 | 1小时 |
| 9 | 单元测试和集成测试 | 3小时 |
| 10 | 验证迁移功能 | 2小时 |

---

**计划状态**: 待审核

**创建时间**: 2026-05-09

**版本**: 2.0