package com.interview.aichat.file;

import com.interview.kevin.exception.BusinessException;
import com.interview.kevin.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class DocumentParseService {

    private final int MAX_TEXT_LENGTH = 5 * 1024 * 1024; //5MB


    private final TextCleaningService textCleaningService;

    public DocumentParseService(TextCleaningService textCleaningService) {
        this.textCleaningService = textCleaningService;
    }

    public String parseContent(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        log.info("开始解析文件： {}", fileName);

        //处理空文件
        if (file.isEmpty() || file.getSize() == 0) {
            log.warn("空文件: {}", fileName);
            return "";
        }

        try(InputStream inputStream = file.getInputStream()) {
            String content = parseContentActor(inputStream);
            return textCleaningService.cleanText(content);
        }catch (IOException | TikaException | SAXException e) {
            log.error("文件解析失败： {}",e.getMessage(),e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,"文件解析失败： "+e.getMessage());

        }

    }

    public String parseContent(byte[] fileBytes, String fileName) {
        log.info("开始解析文件（从字节数组）: {}", fileName);

        // 处理空文件
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("文件字节数组为空: {}", fileName);
            return "";
        }

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            String content = parseContentActor(inputStream);
            String cleanedContent = textCleaningService.cleanText(content);
            log.info("文件解析成功，提取文本长度: {} 字符", cleanedContent.length());
            return cleanedContent;
        } catch (IOException | TikaException | SAXException e) {
            log.error("文件解析失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件解析失败: " + e.getMessage());
        }
    }

    public String downloadAndParseContent(FileStorageService storageService, String storageKey,String originalFilename){
        try {
            byte[] fileBytes = storageService.downloadFile(storageKey);
            if (fileBytes == null || fileBytes.length == 0) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "下载文件失败");
            }
            return parseContent(fileBytes, originalFilename);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("下载并解析文件失败: storageKey={}, error={}", storageKey, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "下载并解析文件失败: " + e.getMessage());
        }
    }

    /**
     * 核心解析方法：使用显式 Parser + Context 方式解析文档
     *
     * 优化点：
     * 1. 使用 BodyContentHandler 只提取正文内容
     * 2. 禁用 EmbeddedDocumentExtractor，不解析嵌入资源（图片、附件）
     * 3. 配置 PDFParserConfig，关闭图片和注释提取
     * 4. 显式指定 Parser 到 Context，增强健壮性
     *
     * @param inputStream 文件输入流
     * @return 提取的文本内容
     * @throws IOException     IO 异常
     * @throws TikaException   Tika 解析异常
     * @throws SAXException    SAX 解析异常
     */
    private String parseContentActor(InputStream inputStream) throws IOException, SAXException, TikaException {
        //创建自动检测解析器
        AutoDetectParser parser = new AutoDetectParser();

        //创建内容处理器，只读正文，最大5MB
        BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);

        //创建元数据
        Metadata metadata = new Metadata();

        //创建解析上下文
        ParseContext parseContext = new ParseContext();

        //显示指定Parser 到 Context
        parseContext.set(Parser.class, parser);

        // 6. 禁用嵌入文档解析（关键：避免提取图片引用和临时文件路径）
        parseContext.set(EmbeddedDocumentExtractor.class, new NoOpEmbeddedDocumentExtractor());

        // 7. PDF 专用配置：关闭图片提取
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(false);
        // 注意：Tika 2.9.2 中 setExtractAnnotations 方法可能不存在，关闭图片提取已足够
        parseContext.set(PDFParserConfig.class, pdfConfig);

        // 8. 执行解析
        parser.parse(inputStream, handler, metadata, parseContext);

        // 9. 返回提取的文本内容
        return handler.toString();


    }

}
