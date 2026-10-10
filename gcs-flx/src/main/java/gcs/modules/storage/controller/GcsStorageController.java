package gcs.modules.storage.controller;

import gcs.base.core.bean.GcsResult;
import gcs.modules.storage.domain.GcsResource;
import gcs.modules.storage.service.GcsStorageService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gcsStorage")
public class GcsStorageController {

    @Autowired
    private GcsStorageService gcsStorageService;

    /**
     * 上传文件
     */
    @PostMapping("/uploadFile")
    public GcsResult uploadFile(HttpServletRequest request) {
        List<MultipartFile> filesToUpload = new ArrayList<>();
        String uploadType = null;
        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            uploadType = multipartRequest.getParameter("upload_type");
            Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();

            for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
                filesToUpload.add(entry.getValue());
            }
        }

        if (filesToUpload.isEmpty()) {
            return GcsResult.failedResult("没有找到要上传的文件");
        }

        List<GcsResource> gcsResourceList = new ArrayList<>();
        for (MultipartFile multipartFile : filesToUpload) {
            try {
                String fileName = FilenameUtils.getName(multipartFile.getOriginalFilename());
                GcsResource resource = gcsStorageService.save(
                        multipartFile.getInputStream(),
                        fileName,
                        multipartFile.getContentType(),
                        uploadType
                );
                gcsResourceList.add(resource);
            } catch (IOException e) {
                return GcsResult.failedResult("文件上传失败: " + e.getMessage());
            }
        }

        return GcsResult.successResult().singleData(gcsResourceList);
    }

    /**
     * 下载文件
     */
    @GetMapping("/downloadFile/{id}")
    public void downloadFile(@PathVariable String id,
                             @RequestParam(required = false) Boolean inline,
                             HttpServletResponse response) {
        try {
            // 防止路径暴漏
            GcsResource gcsResource = gcsStorageService.get(id, true);
            // 文件元信息
            response.setContentType(gcsResource.getContentType());
            response.setDateHeader("Expires", System.currentTimeMillis() + 10 * 60 * 1000);
            String disposition = inline != null && inline ? "inline" : "attachment";
            String filename = URLEncoder.encode(gcsResource.getFileName(), "UTF-8");
            response.setHeader("Content-disposition",
                    String.format("%s; filename=\"%s\"; size=%d",
                            disposition, filename, gcsResource.getLength()
                    )
            );
            // 写出文件
            gcsStorageService.writeTo(gcsResource, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                GcsResult result = GcsResult.failedResult("资源文件不存在");
                response.getWriter().write(result.toString());
            } catch (IOException ex) {
                // 处理写入错误
            }
        }
    }
}
